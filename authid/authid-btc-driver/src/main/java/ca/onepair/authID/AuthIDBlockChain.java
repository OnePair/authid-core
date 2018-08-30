package ca.onepair.authID;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PrunedException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionOutputChanges;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.script.Script.ScriptType;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.wallet.Wallet;

import ca.onepair.authID.store.AuthIDStore;
import ca.onepair.authID.utils.BitcoinUtils;

public class AuthIDBlockChain extends AbstractBlockChain {

	protected final BlockStore blockStore;
	protected final AuthIDStore authIDStore;

	public AuthIDBlockChain(Context context, Wallet wallet, BlockStore blockStore, AuthIDStore authIDStore)
			throws BlockStoreException {
		this(context, new ArrayList<Wallet>(), blockStore, authIDStore);
		addWallet(wallet);
	}

	public AuthIDBlockChain(NetworkParameters params, Wallet wallet, BlockStore blockStore, AuthIDStore authIDStore)
			throws BlockStoreException {
		this(Context.getOrCreate(params), wallet, blockStore, authIDStore);
		// this.rollbackBlockStore(1225626);
	}

	public AuthIDBlockChain(Context context, BlockStore blockStore, AuthIDStore authIDStore)
			throws BlockStoreException {
		this(context, new ArrayList<Wallet>(), blockStore, authIDStore);
	}

	public AuthIDBlockChain(NetworkParameters params, BlockStore blockStore, AuthIDStore authIDStore)
			throws BlockStoreException {
		this(params, new ArrayList<Wallet>(), blockStore, authIDStore);
	}

	public AuthIDBlockChain(Context params, List<? extends Wallet> wallets, BlockStore blockStore,
			AuthIDStore authIDStore) throws BlockStoreException {
		super(params, wallets, blockStore);
		this.authIDStore = authIDStore;
		this.blockStore = blockStore;
	}

	public AuthIDBlockChain(NetworkParameters params, List<? extends Wallet> wallets, BlockStore blockStore,
			AuthIDStore authIDStore) throws BlockStoreException {
		this(Context.getOrCreate(params), wallets, blockStore, authIDStore);
	}

	@Override
	protected StoredBlock addToBlockStore(StoredBlock storedPrev, Block blockHeader,
			TransactionOutputChanges txOutChanges) throws BlockStoreException, VerificationException {
		StoredBlock newBlock = storedPrev.build(blockHeader);
		this.blockStore.put(newBlock);
		return newBlock;
	}

	@Override
	protected StoredBlock addToBlockStore(StoredBlock storedPrev, Block blockHeader)
			throws BlockStoreException, VerificationException {
		StoredBlock newBlock = storedPrev.build(blockHeader);
		this.blockStore.put(newBlock);
		return newBlock;
	}

	@Override
	protected void rollbackBlockStore(int height) throws BlockStoreException {
		throw new BlockStoreException("Unsupported");

		/*
		 * lock.lock(); try { int currentHeight = getBestChainHeight();
		 * checkArgument(height >= 0 && height <= currentHeight,
		 * "Bad height: %s", height); if (height == currentHeight) return; //
		 * nothing to do
		 * 
		 * // Look for the block we want to be the new chain head StoredBlock
		 * newChainHead = blockStore.getChainHead(); while
		 * (newChainHead.getHeight() > height) { newChainHead =
		 * newChainHead.getPrev(blockStore); if (newChainHead == null) throw new
		 * BlockStoreException("Unreachable height"); }
		 * 
		 * // Modify store directly blockStore.put(newChainHead);
		 * this.setChainHead(newChainHead); } finally { lock.unlock(); }
		 */
	}

	@Override
	protected boolean shouldVerifyTransactions() {
		return true;
	}

	@Override
	protected TransactionOutputChanges connectTransactions(int height, Block block) {
		// System.out.println(
		// "number of transactions: " + block.getTransactions().size() + " in
		// block: " + block.getHashAsString());
		this.storeAuthIDs(block);
		return null;
	}

	@Override
	protected TransactionOutputChanges connectTransactions(StoredBlock newBlock) {
		// System.out.println("number of transactions: " +
		// newBlock.getHeader().getTransactions().size() + " in block: "
		// + newBlock.getHeader().getHashAsString());
		this.storeAuthIDs(newBlock.getHeader());
		return null;
	}

	/*
	 * TODO: java.lang.NullPointerException: null at
	 * ca.onepair.authID.AuthIDBlockChain.disconnectTransactions(
	 * AuthIDBlockChain.java:111)
	 */
	@Override
	protected void disconnectTransactions(StoredBlock block) {
		String blockHash = block.getHeader().getHashAsString();
		this.authIDStore.removeWithBlockHash(blockHash);

		/*
		 * for (Transaction transaction : block.getHeader().getTransactions()) {
		 * try {
		 * this.authIDStore.removeAuthIDromTxID(transaction.getHashAsString());
		 * } catch (AuthIDStoreException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } }
		 */
	}

	@Override
	protected void doSetChainHead(StoredBlock chainHead) throws BlockStoreException {
		this.blockStore.setChainHead(chainHead);
	}

	@Override
	protected void notSettingChainHead() throws BlockStoreException {
		// We don't use DB transactions here, so we don't need to do anything
	}

	@Override
	protected StoredBlock getStoredBlockInCurrentScope(Sha256Hash hash) throws BlockStoreException {
		return this.blockStore.get(hash);
	}

	@Override
	public boolean add(FilteredBlock block) throws VerificationException, PrunedException {
		boolean success = super.add(block);
		if (success) {
			trackFilteredTransactions(block.getTransactionCount());
		}
		return success;
	}

	/*
	 * /* TODO: check if name exist
	 */
	public void storeAuthIDs(Block block) {
		List<BTCAuthID> ids = new ArrayList<BTCAuthID>();
		// List<Transaction> pathTransactions = new ArrayList<Transaction>();
		List<Transaction> transferTransactions = new ArrayList<Transaction>();

		/*
		 * Get the available id transactions
		 */
		for (Transaction transaction : block.getTransactions()) {
			try {
				for (TransactionOutput output : transaction.getOutputs()) {
					if (output.getScriptPubKey().isOpReturn()) {
						String address = this.getReceiverAddress(transaction);
						String opReturn = BitcoinUtils.decodeOpReturn(output).trim();
						if (address != null) {
							if (opReturn.startsWith(BTCAuthID.PREFIX_IDENTIFIER)) {
								BTCAuthID id = new BTCAuthID(block.getHashAsString(), transaction.getHashAsString(),
										opReturn.replaceFirst(BTCAuthID.PREFIX_IDENTIFIER, ""), address);

								if (this.verifyNewAuthID(transaction, id)) {
									System.out.println("adding did : " + id);
									ids.add(id);
								} else {
									System.out.println("did verification failed");
								}

							} else if (opReturn.equals(BTCAuthID.TRANSFER_INDICATOR)) {
								transferTransactions.add(transaction);
							}
						}
					}
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

		// this.updateDIDs(dids, block.getTimeSeconds());
		this.updateIDs(ids, block.getHashAsString());

		for (Transaction transaction : transferTransactions) {
			String nameTxID = transaction.getInput(0).getOutpoint().getHash().toString();

			BTCAuthID authID = this.authIDStore.getAuthIDFromTxID(nameTxID);

			if (authID != null) {
				// domain.setTransferId(transaction.getHashAsString());
				// domain.setTransferOutput(transaction.getOutput(0));
				authID.setTransferTransaction(transaction);
				this.authIDStore.updateID(authID);
			}
		}
	}

	/*
	 * @param: rawDomains - domainName list that may contain conflicts
	 */
	public void updateIDs(List<BTCAuthID> rawIDs, String blockHash) {
		if (rawIDs.size() > 0) {
			Map<String, List<BTCAuthID>> conflictingID = AuthIDBlockChain.sortIDConflicts(rawIDs);
			// List<BTCAuthID> winners =
			// AuthIDBlockChain.getWinnerDIDs(conflictingDID, blockTime);
			List<BTCAuthID> winners = AuthIDBlockChain.getWinnerIDs(conflictingID, blockHash);

			for (BTCAuthID id : winners) {
				this.authIDStore.updateID(id);
			}

			// Index block
			if (this.authIDStore.blockIndexingSupported())
				this.authIDStore.indexBlockHash(blockHash, idListToTxIDList(winners));
		}
	}

	public boolean verifyNewAuthID(Transaction transaction, BTCAuthID did) throws BlockStoreException {
		System.out.println("trying to verify did : " + did);

		BTCAuthID storedID = this.authIDStore.getAuthIDFromName(did.getName());

		System.out.println("stored did : " + String.valueOf(storedID));
		// if name does not exist we are good to go
		if (storedID == null) {
			return true;
		}

		Transaction transferTransaction = storedID.getTransferTransaction();

		// transaction hash of the transfer transaction
		String transferID = transferTransaction.getHashAsString();

		if (transferID == null) {
			return false;
		}
		transferID = transferID.trim();

		// get the first input hash of the transaction
		String txTransferInput = transaction.getInput(0).getOutpoint().getHash().toString().trim();

		// if input hash is equal to the transafer id return true
		if (txTransferInput.equals(transferID)) {
			return true;
		}

		return false;
	}

	private String getReceiverAddress(Transaction transaction) {
		// the new receiver address is the first output
		return AuthIDBlockChain.getOutputAddress(transaction.getOutput(0), this.params).toString();
	}

	/*
	 * Sort domain conflits into lists
	 */
	private static Map<String, List<BTCAuthID>> sortIDConflicts(List<BTCAuthID> dids) {
		Map<String, List<BTCAuthID>> sortedNames = new HashMap<String, List<BTCAuthID>>();

		for (BTCAuthID id : dids) {
			System.out.println("sorting conflicting name : " + id.getName());
			if (sortedNames.containsKey(id.getName())) {
				sortedNames.get(id.getName()).add(id);
			} else {
				List<BTCAuthID> domainConflicts = new ArrayList<BTCAuthID>();
				domainConflicts.add(id);
				sortedNames.put(id.getName(), domainConflicts);
			}
		}

		return sortedNames;
	}

	/*
	 * This function gets the winner domains in the case that more than one has
	 * been registered in a block Use block time to determine winner Note: this
	 * algorithm is deprecated. We now decide the winners with the hashCode of
	 * the block hash
	 */
	@Deprecated
	public static List<BTCAuthID> getWinnerDIDs(Map<String, List<BTCAuthID>> dids, long blockTime) {
		List<BTCAuthID> winners = new ArrayList<BTCAuthID>();

		String blockTimeString = Long.toString(blockTime);
		// get the decider number from the time stamp
		float decider = Character.getNumericValue((blockTimeString.charAt(blockTimeString.length() - 1))) / 10;

		for (String name : dids.keySet()) {
			List<BTCAuthID> didNames = dids.get(name);
			System.out.println("deciding winner for : " + name);
			System.out.println("name conflicts : " + didNames.size());
			if (didNames.size() == 1) {
				winners.add(didNames.get(0));
			} else {
				int decidedIndex = Math.round(decider * didNames.size());
				winners.add(didNames.get(decidedIndex));
			}

		}

		return winners;
	}

	public static List<BTCAuthID> getWinnerIDs(Map<String, List<BTCAuthID>> ids, String blockHash) {
		List<BTCAuthID> winners = new ArrayList<BTCAuthID>();

		// get the first digit
		int firstDigit = Integer.parseInt(Integer.toString(blockHash.hashCode()).replaceAll("-", "").substring(0, 1));

		// get the decider number from the time stamp
		float decider = firstDigit / 10;

		for (String name : ids.keySet()) {
			List<BTCAuthID> idNames = ids.get(name);
			Collections.sort(idNames, new Comparator<BTCAuthID>() {

				@Override
				public int compare(BTCAuthID id1, BTCAuthID id2) {
					return id1.getBlockHash().compareTo(id2.getBlockHash());
				}

			});

			System.out.println("deciding winner for : " + name);
			System.out.println("name conflicts : " + idNames.size());
			if (idNames.size() == 1) {
				winners.add(idNames.get(0));
			} else {
				int decidedIndex = Math.round(decider * idNames.size());
				winners.add(idNames.get(decidedIndex));
			}

		}

		return winners;
	}

	public static Address getOutputAddress(TransactionOutput output, NetworkParameters networkParams) {
		Address address = null;
		if (output.getScriptPubKey().getScriptType().equals(ScriptType.P2PKH)) {
			address = output.getAddressFromP2PKHScript(networkParams);

		} else if (output.getScriptPubKey().getScriptType().equals(ScriptType.PUB_KEY)) {
			address = ECKey.fromPublicOnly(output.getScriptPubKey().getPubKey()).toAddress(networkParams);
		}
		return address;
	}

	/*
	 * Convert list of ids to a list of transaction hashes
	 */
	public static List<String> idListToTxIDList(List<BTCAuthID> ids) {
		List<String> txHashList = new ArrayList<String>();

		for (BTCAuthID id : ids) {
			txHashList.add(id.getTransactionId());
		}
		return txHashList;
	}

}
