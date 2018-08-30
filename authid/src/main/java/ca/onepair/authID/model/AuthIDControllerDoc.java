package ca.onepair.authID.model;

import org.json.JSONObject;

/*
 * The AuthID controller key document. All of this
 * info would be retrieved from the registry.
*/
public class AuthIDControllerDoc {

	/*
	 * JSON fields
	 */
	public static final String PROTOCOL = "protocol";
	public static final String DOC_TYPE = "doc_type";
	public static final String ADDRESS = "address";
	public static final String ID = "id";
	public static final String REFERENCE = "reference";
	public static final String ACTIVE = "active";
	public static final String TRANSFER_ID = "transfer_id";

	/*
	 * authid-controller specific values
	 */
	private static String CONTROLLER_DOC = "authid-controller";

	private String protocol;
	private String address;
	private String id;
	private String reference;
	private boolean active;
	private String transferId;

	public AuthIDControllerDoc(String protocol, String address, String id, String reference, boolean active,
			String transferId) {
		this.protocol = protocol;
		this.address = address;
		this.id = id;
		this.reference = reference;
		this.active = active;
		this.transferId = transferId;
	}

	/*
	 * Convert to a Json
	 * 
	 * @return JSONObject
	 */
	public JSONObject toJSON() {
		JSONObject jsonDoc = new JSONObject();

		jsonDoc.put(PROTOCOL, this.protocol);
		jsonDoc.put(DOC_TYPE, CONTROLLER_DOC);
		jsonDoc.put(ADDRESS, this.address);
		jsonDoc.put(ID, this.id);
		jsonDoc.put(REFERENCE, this.reference);
		jsonDoc.put(ACTIVE, this.active);
		jsonDoc.put(TRANSFER_ID, this.transferId);

		return jsonDoc;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getDocType() {
		return CONTROLLER_DOC;
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getReference() {
		return this.reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getTransferId() {
		return this.transferId;
	}

	public void setTransferId(String transferId) {
		this.transferId = transferId;
	}

	/*
	 * Parse JSONObject to AuthIDControllerDoc.
	 * 
	 * @param JSONObject json AuthID controller document
	 * 
	 * @return AuthIDControllerDoc
	 */
	public static AuthIDControllerDoc fromJSON(JSONObject json) {
		String transferId = null;

		if (json.has(TRANSFER_ID))
			transferId = json.getString(TRANSFER_ID);
		return new AuthIDControllerDoc(json.getString(PROTOCOL), json.getString(ADDRESS), json.getString(ID),
				json.getString(REFERENCE), json.getBoolean(ACTIVE), transferId);

	}

}
