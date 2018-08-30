package ca.onepair.authID.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.Wallet;

import ca.onepair.authID.BTCAuthID;
import ca.onepair.authID.store.AuthIDStore;
import ca.onepair.authid.common.exceptions.DoesNotExistException;
import ca.onepair.authid.common.exceptions.MissingKeyException;

public class BTCIDUtils {

	private static final int MINING_FEE = 4013;

	public static Transaction buildAuthIDTransaction(NetworkParameters networkParams, AuthIDStore authIDStore,
			Wallet wallet, Transaction transferTransaction, String address, String id, int fee)
			throws MissingKeyException, InsufficientMoneyException {
		if (fee == 0)
			fee = MINING_FEE;

		// Shuffle transactions
		Set<Transaction> transactionSet = wallet.getTransactions(false);
		List<Transaction> transactionList = new ArrayList(transactionSet);
		Collections.shuffle(transactionList);
		
		List<TransactionOutput> availableOutputs = BTCIDUtils.getUsableOutputs(transactionList,
				authIDStore);

		// address key map
		// Map<String, ECKey> signingKeys = new HashMap<String, ECKey>();
		Map<String, ECKey> walletKeys = BTCIDUtils.getKeysFromWallet(networkParams, wallet);

		/*
		 * Calculate the total amount of satoshis that we need to spend
		 */
		int amount = 0;
		List<TransactionOutput> inputOuts = new ArrayList<TransactionOutput>();

		if (transferTransaction != null) {
			TransactionOutput transferOutput = transferTransaction.getOutput(0);
			inputOuts.add(transferOutput);

			Address transferAddress = BitcoinUtils.getOutputAddress(transferTransaction.getOutput(0), networkParams);
			ECKey transferKey = walletKeys.get((transferAddress.toString()));
			if (transferKey == null)
				throw new MissingKeyException();

			amount += transferOutput.getValue().value;
		}

		for (TransactionOutput output : availableOutputs) {
			if (amount >= (fee * 5)) {
				break;
			}

			String outputAddress = BitcoinUtils.getOutputAddress(output, networkParams).toString();
			if (walletKeys.containsKey(outputAddress)) {
				amount += output.getValue().value;
				inputOuts.add(output);
			}
		}

		if (amount < fee * 5)
			throw new InsufficientMoneyException(Coin.valueOf(fee * 5 - amount));

		Transaction transaction = new Transaction(networkParams);

		transaction.addOutput(Coin.valueOf(fee * 4), Address.fromBase58(networkParams, address));

		// Return change
		ECKey changeKey = new ECKey();
		wallet.importKey(changeKey);
		transaction.addOutput(Coin.valueOf(amount - (fee * 5)), changeKey.toAddress(networkParams));

		if (!id.startsWith(BTCAuthID.PREFIX_IDENTIFIER)) {
			id = id.replaceFirst(BTCAuthID.PREFIX_IDENTIFIER, "");
		}

		// hash the trimmed id
		String idHash = Utils.HEX.encode(Sha256Hash.hash(id.trim().getBytes()));

		// Script opReturn = ScriptBuilder.createOpReturnScript(id.getBytes());
		Script opReturn = ScriptBuilder.createOpReturnScript((BTCAuthID.PREFIX_IDENTIFIER + idHash).getBytes());
		transaction.addOutput(Coin.valueOf(0), opReturn);

		for (TransactionOutput output : inputOuts) {
			String signingAddress = BitcoinUtils.getOutputAddress(output, networkParams).toString();
			BitcoinUtils.createTransactionInput(transaction, output, walletKeys.get(signingAddress), networkParams);
		}

		return transaction;
	}

	public static Transaction buildTransferTransaction(NetworkParameters networkParams, Transaction authIDTransaction,
			String destinationAddress, Wallet wallet, int fee)
			throws InsufficientMoneyException, MissingKeyException, DoesNotExistException {
		if (fee == 0)
			fee = MINING_FEE;

		if (authIDTransaction == null)
			throw new DoesNotExistException();

		// Address address =
		// ECKey.fromPublicOnly(authIDTransaction.getInput(0).getScriptSig().getPubKey())
		// .toAddress(networkParams);
		Address address = BitcoinUtils.getOutputAddress(authIDTransaction.getOutput(0), networkParams);

		ECKey signingKey = BTCIDUtils.getKeysFromWallet(networkParams, wallet).get(address.toString());

		if (signingKey == null)
			throw new MissingKeyException();

		long amount = authIDTransaction.getOutput(0).getValue().value;

		if (amount < (fee * 3)) {
			throw new InsufficientMoneyException(Coin.valueOf(fee - amount));
		}

		Transaction transferTransaction = new Transaction(networkParams);

		// TODO : mining fee is not static here
		// transfer output
		// transferTransaction.addOutput(Coin.valueOf(fee * 2),
		// Address.fromBase58(this.networkParams, destinationAddress));
		transferTransaction.addOutput(Coin.valueOf(0), Address.fromBase58(networkParams, destinationAddress));
		// if (amount > fee * 3)
		if (amount > fee) {
			// change (includes mining fee)
			ECKey changeAddress = new ECKey();
			wallet.importKey(changeAddress);

			// transferTransaction.addOutput(Coin.valueOf(amount - fee),
			// Address.fromBase58(networkParams,
			// signingKey.toAddress(networkParams).toString()));
			transferTransaction.addOutput(Coin.valueOf(amount - fee), changeAddress);
		}

		// transfer indicator
		Script opReturn = ScriptBuilder.createOpReturnScript(BTCAuthID.TRANSFER_INDICATOR.getBytes());
		transferTransaction.addOutput(Coin.valueOf(0), opReturn);

		// transferTransaction.addSignedInput(authIDTransaction.getOutput(0),
		// signingKey);
		transferTransaction.addSignedInput(authIDTransaction.getOutput(0).getOutPointFor(),
				authIDTransaction.getOutput(0).getScriptPubKey(), signingKey, SigHash.ALL, true);

		// BitcoinUtils.createTransactionInput(transferTransaction,
		// authIDTransaction.getOutput(0), signingKey, networkParams);

		return transferTransaction;
	}

	/*
	 * gets a map of address/key from a wallet object
	 */
	public static Map<String, ECKey> getKeysFromWallet(NetworkParameters networkParams, Wallet wallet) {
		Map<String, ECKey> addressKeyMap = new HashMap<String, ECKey>();

		for (ECKey key : wallet.getImportedKeys()) {
			String address = key.toAddress(networkParams).toString();
			addressKeyMap.put(address, key);
		}

		return addressKeyMap;
	}

	private static List<TransactionOutput> getUsableOutputs(List<Transaction> transactions, AuthIDStore idStore) {
		List<TransactionOutput> usableOutputs = new ArrayList<TransactionOutput>();
		for (Transaction transaction : transactions) {
			List<TransactionOutput> availableOutputs = new ArrayList<TransactionOutput>();
			String txOpReturn = "";

			for (TransactionOutput output : transaction.getOutputs()) {
				// get the transaction output
				if (output.getScriptPubKey().isOpReturn()) {
					try {
						txOpReturn = BitcoinUtils.decodeOpReturn(output).trim();
					} catch (Exception ignored) {
					}
				} else if (output.isAvailableForSpending()) {
					availableOutputs.add(output);
				}
			}

			if (txOpReturn.startsWith(BTCAuthID.PREFIX_IDENTIFIER) || txOpReturn.equals(BTCAuthID.TRANSFER_INDICATOR)) {

				BTCAuthID id;
				if (txOpReturn.startsWith(BTCAuthID.PREFIX_IDENTIFIER)) {
					id = idStore.getAuthIDFromTxID(transaction.getHashAsString());
				} else { // is a transfer transaction so check if it exist
					id = idStore.getAuthIDFromTxID(transaction.getInput(0).getOutpoint().getHash().toString());
				}

				if (id == null) {
					usableOutputs.addAll(availableOutputs);

				} else {
					for (TransactionOutput output : availableOutputs) {
						if (output.getIndex() != 0) {
							usableOutputs.add(output);
						}
					}
				}
			} else {
				// System.out.println("adding all outouts from tx : " +
				// transaction.getHashAsString());
				usableOutputs.addAll(availableOutputs);
			}

		}

		return usableOutputs;
	}
}
