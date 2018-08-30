package ca.onepair.authID;

import org.json.JSONObject;

public class AuthID {
	public static String ID = "id";
	public static String ADDRESS = "address";
	// reference id could be the transaction hash
	public static String REFERENCE_ID = "reference";
	public static String TRANSFER_ID = "transfer_id";
	public static String PROTOCOL = "protocol";
	public static String REVOKED = "revoked";

	private String id;
	private String address;
	private String referenceID;
	private String transferID;
	private String protocol;
	private boolean revoked;

	@Deprecated
	public AuthID(String id, String address, String referenceID, String transferID, String protocol, boolean revoked) {
		this.id = id;
		this.address = address;
		this.referenceID = referenceID;
		this.transferID = transferID;
		this.revoked = revoked;
		this.protocol = protocol;
	}

	public String getID() {
		return this.id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setReferenceID(String referenceID) {
		this.referenceID = referenceID;
	}

	public String getReferenceID() {
		return this.referenceID;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public boolean isRevoked() {
		return this.revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	public String toString() {
		JSONObject authIDJson = new JSONObject();
		authIDJson.put(ID, this.id);
		authIDJson.put(ADDRESS, this.address);
		authIDJson.put(REFERENCE_ID, this.referenceID);
		authIDJson.put(TRANSFER_ID, this.transferID);
		authIDJson.put(REVOKED, revoked);
		authIDJson.put(PROTOCOL, this.protocol);
		return authIDJson.toString();
	}
}
