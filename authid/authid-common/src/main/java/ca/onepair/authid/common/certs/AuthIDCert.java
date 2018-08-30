package ca.onepair.authid.common.certs;

import org.json.JSONObject;

import ca.onepair.authid.common.model.AuthIDProcessorDoc;

public interface AuthIDCert {

	/*
	 * @return the token
	 */
	public String getToken();

	/*
	 * @return the signature
	 */
	public String getSignature();

	/*
	 * @param signature the signature to set
	 */
	public void setSignature(String signature);

	/*
	 * @param idDoc the id document used to sign the cert
	 */
	public void setIDDoc(AuthIDProcessorDoc idDoc);

	/*
	 * @return the id doc used to sign the cert
	 */
	public AuthIDProcessorDoc getIDDoc();

	/*
	 * @return json encoding of the cert
	 */
	public JSONObject toJson();

}
