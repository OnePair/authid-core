package ca.onepair.authID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.TestNet3Params;
import org.json.JSONObject;

public class BTCAuthID implements Serializable {

	// this needs to be loaded from some sort of properties file
	private static final NetworkParameters NETWORK_PARAMS = TestNet3Params.get();

	public static final String PREFIX_IDENTIFIER = "authID:";
	public static final String TRANSFER_INDICATOR = "authID.transfer";
	public static final String BLOCK_HASH = "block_hash";
	public static final String TRANSACTION_ID = "txID";
	public static final String NAME = "name";
	public static final String ADDRESS = "address";
	public static final String TRANSFER_ID = "transfer_id";
	public static final String TRANSFER_TRANSACTION = "TRANSFER_TRANSACTION";

	private String blockHash;
	private String transactionID;
	private String name;
	private String address;
	private byte[] transaferTransactionBytes;

	public BTCAuthID(String blockHash, String transactionID, String name, String address) {
		this.blockHash = blockHash;
		this.transactionID = transactionID;
		this.name = name;
		this.address = address;
	}

	public String toString() {
		return this.toJson().toString();
	}

	public JSONObject toJson() {
		JSONObject authIDJson = new JSONObject();

		authIDJson.put(BLOCK_HASH, this.blockHash);
		authIDJson.put(TRANSACTION_ID, this.transactionID);
		authIDJson.put(NAME, this.name);
		authIDJson.put(ADDRESS, this.address);
		if (this.transaferTransactionBytes != null) {
			authIDJson.put(TRANSFER_ID, this.getTransferTransaction().getHashAsString());
			String transferTxString = Utils.HEX.encode(transaferTransactionBytes);
			authIDJson.put(TRANSFER_TRANSACTION, transferTxString);
		}
		return authIDJson;
	}

	public String getBlockHash() {
		return this.blockHash;
	}

	public String getTransactionId() {
		return this.transactionID;
	}

	public void setTransactionID(String transactionID) {
		this.transactionID = transactionID;
	}

	public String getName() {
		return this.name;
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Transaction getTransferTransaction() {
		if (this.transaferTransactionBytes != null)
			return new Transaction(BTCAuthID.NETWORK_PARAMS, this.transaferTransactionBytes);
		return null;
	}

	public void setTransferTransaction(Transaction transaction) {
		// this.transferOutput = output.bitcoinSerialize();
		this.transaferTransactionBytes = transaction.bitcoinSerialize();
	}

	public static byte[] serialize(BTCAuthID domain) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] bytes = null;

		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(domain);
			out.flush();
			bytes = bos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return bytes;
	}

	public static BTCAuthID fromBytes(byte[] bytes) {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		BTCAuthID domain = null;

		try {
			in = new ObjectInputStream(bis);
			domain = (BTCAuthID) in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return domain;

	}

	public static BTCAuthID fromJson(JSONObject json) {
		BTCAuthID btcAuthID = new BTCAuthID(json.getString(BLOCK_HASH), json.getString(TRANSACTION_ID),
				json.getString(NAME), json.getString(ADDRESS));
		if (json.has(TRANSFER_ID)) {
			byte[] transferTransactionBytes = Utils.HEX.decode(json.getString(TRANSFER_TRANSACTION));
			Transaction transferTransaction = new Transaction(NETWORK_PARAMS, transferTransactionBytes);
			btcAuthID.setTransferTransaction(transferTransaction);
		}
		return btcAuthID;
	}

}
