package ca.onepair.authID.utils;


import javax.xml.bind.DatatypeConverter;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.Script.ScriptType;

public class BitcoinUtils {

	public static String decodeOpReturn(TransactionOutput output) {
		if (output.getScriptPubKey().getChunks().size() < 2) {
			return "";
		}
		byte[] data = output.getScriptPubKey().getChunks().get(1).data;
		String opReturnHex = Utils.HEX.encode(data);
		byte[] decoded = DatatypeConverter.parseHexBinary(opReturnHex);
		return new String(decoded);
	}

	public static ECKey stringToKey(String keyString) {
		return DumpedPrivateKey.fromBase58(null, keyString).getKey();
	}

	public static Address getOutputAddress(TransactionOutput output, NetworkParameters networkParams) {
		Address address = null;
		if (output.getScriptPubKey().getScriptType().equals(ScriptType.P2PKH)) {
			address = output.getAddressFromP2PKHScript(networkParams);
		} else if (output.getScriptPubKey().getScriptType().equals(ScriptType.PUB_KEY)) {
			address = ECKey.fromPublicOnly(output.getScriptPubKey().getPubKey()).toAddress(networkParams);
		} else if (output.getScriptPubKey().getScriptType().equals(ScriptType.P2SH)) {
			address = output.getAddressFromP2SH(networkParams);
		}

		return address;
	}

	public static TransactionInput createTransactionInput(Transaction transaction, TransactionOutput output,
			ECKey signingKey, NetworkParameters networkParams) {
		TransactionInput input = new TransactionInput(networkParams, transaction, new byte[] {},
				output.getOutPointFor());
		transaction.addInput(input);

		Sha256Hash hash = transaction.hashForSignature(transaction.getInputs().size() - 1, output.getScriptPubKey(),
				SigHash.ALL, true);
		ECKey.ECDSASignature ecSig = signingKey.sign(hash);
		TransactionSignature txSig = new TransactionSignature(ecSig, SigHash.ALL, true);

		input.setScriptSig(ScriptBuilder.createInputScript(txSig, signingKey));

		return input;
	}

	/*
	 * Parses the authID uri result[0] = context result[1] = driver result[2] =
	 * id
	 */
	public static String[] parseAuthIDURI(String authIDURI) {
		return authIDURI.split(":");
	}
}
