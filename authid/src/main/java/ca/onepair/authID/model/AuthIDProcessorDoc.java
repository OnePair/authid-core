package ca.onepair.authID.model;

import org.json.JSONObject;

/*
 * The AuthID controller key document. All of this
 * info would be retrieved from the registry.
*/
public class AuthIDProcessorDoc {
	/*
	 * JSON fields
	 */
	public static final String PROTOCOL = "protocol";
	public static final String DOC_TYPE = "doc_type";
	public static final String ADDRESS = "address";
	public static final String ID = "id";
	public static final String SIGNATURE = "signature";

	/*
	 * authid-processor specific values
	 */
	private static final String PROCESSOR_DOC = "authid-processor";

	private String protocol;
	private String address;
	private String id;
	private String signature;

	public AuthIDProcessorDoc(String protocol, String address, String id, String signature) {
		this.protocol = protocol;
		this.address = address;
		this.id = id;
		this.signature = signature;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getDocType() {
		return PROCESSOR_DOC;
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

	/*
	 * Convert to Json
	 * 
	 * @return JSONObject
	 */
	public JSONObject toJSON() {
		JSONObject jsonDoc = new JSONObject();

		jsonDoc.put(PROTOCOL, this.protocol);
		jsonDoc.put(DOC_TYPE, PROCESSOR_DOC);
		jsonDoc.put(ADDRESS, this.address);
		jsonDoc.put(ID, this.id);
		jsonDoc.put(SIGNATURE, this.signature);

		return jsonDoc;
	}

	/*
	 * Parse JSONObject to AuthIDProcessorDoc.
	 * 
	 * @param JSONObject json AuthID processor document
	 * 
	 * @return AuthIDControllerDoc
	 */
	public AuthIDProcessorDoc fromJSON(JSONObject json) {
		return new AuthIDProcessorDoc(json.getString(PROTOCOL), json.getString(ADDRESS), json.getString(ID),
				json.getString(SIGNATURE));
	}

}
