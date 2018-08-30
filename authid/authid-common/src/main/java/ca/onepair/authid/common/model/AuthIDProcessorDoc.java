package ca.onepair.authid.common.model;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.json.JSONObject;

/*
 * This class implements the AuthID processor key document.
 * 
 * @author Noah Bouma
 * @version 0.0.1
*/
public class AuthIDProcessorDoc {

	public static final String PROTOCOL = "protocol";
	public static final String DOC_TYPE = "doc_type";
	public static final String ADDRESS = "address";
	public static final String ID = "id";
	public static final String SIGNATURE = "signature";

	public static final String PROCESSOR_DOC = "authid-processor";

	private String protocol;
	private String address;
	private String id;
	private String signature;

	/*
	 * Constructor
	 * 
	 * @param protocol the id's protocol
	 * 
	 * @param address id's processor address
	 * 
	 * @param id the id
	 *
	 * @param signature the document's signature
	 */
	public AuthIDProcessorDoc(String protocol, String address, String id, String signature) {
		this.protocol = protocol;
		this.address = address;
		this.id = id;
		this.signature = signature;
	}

	public String getFingerprint() {
		String normalized = PROTOCOL + ":" + this.protocol + "\n" + DOC_TYPE + ":" + PROCESSOR_DOC + "\n" + ADDRESS
				+ ":" + this.address + "\n" + ID + ":" + this.id;
		return Utils.HEX.encode(Sha256Hash.hash(normalized.getBytes()));

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

	public String getCanonicalID() {
		return this.id + "." + this.protocol.toUpperCase();
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSignature() {
		return this.signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public JSONObject toJSON() {
		JSONObject jsonDoc = new JSONObject();

		jsonDoc.put(PROTOCOL, this.protocol);
		jsonDoc.put(DOC_TYPE, PROCESSOR_DOC);
		jsonDoc.put(ADDRESS, this.address);
		jsonDoc.put(ID, this.id);
		jsonDoc.put(SIGNATURE, this.signature);

		return jsonDoc;
	}

	public static AuthIDProcessorDoc fromJSON(JSONObject json) {
		String signature = null;
		if (json.has(SIGNATURE))
			signature = json.getString(SIGNATURE);
		return new AuthIDProcessorDoc(json.getString(PROTOCOL), json.getString(ADDRESS), json.getString(ID), signature);
	}

}
