package ca.onepair.authID.model;

import org.json.JSONObject;

public class AuthIDChallengeResponseDoc {

	/*
	 * JSON fields
	 */
	public static final String DOC_TYPE = "doc_type";
	public static final String CHALLENGE_ID = "challenge_id";
	public static final String CHALLENGE_RESPONSE = "challenge_response";
	public static final String ID_DOC = "id_doc";

	/*
	 * authid-challenge-response specific values
	 */
	private static final String CHALLENGE_RESPONSE_DOC = "authid-challenge-response";

	private String challengeId;
	private String challengeResponse;
	private JSONObject idDoc;

	public AuthIDChallengeResponseDoc(String challengeId, String challengeResponse, JSONObject idDoc) {
		this.challengeId = challengeId;
		this.challengeResponse = challengeResponse;
		this.idDoc = idDoc;
	}

	/*
	 * Convert to Json.
	 * 
	 * @return JSONObject
	 */
	public JSONObject toJSON() {
		JSONObject jsonDoc = new JSONObject();

		jsonDoc.put(DOC_TYPE, CHALLENGE_RESPONSE_DOC);
		jsonDoc.put(CHALLENGE_ID, this.challengeId);
		jsonDoc.put(CHALLENGE_RESPONSE, this.challengeResponse);
		jsonDoc.put(ID_DOC, this.idDoc);

		return jsonDoc;
	}

	public String getChallengeId() {
		return this.challengeId;
	}

	public void setChallengeId(String id) {
		this.challengeId = id;
	}

	public String getChallengeResponse() {
		return this.challengeResponse;
	}

	public void setChallengeResponse(String response) {
		this.challengeResponse = response;
	}

	public JSONObject getIdDoc() {
		return this.idDoc;
	}

	public void setIdDoc(JSONObject idDoc) {
		this.idDoc = idDoc;
	}

	/*
	 * Parse JSONObject to AuthIDProcessorDoc.
	 * 
	 * @param JSONObject json authid-challenge-response document
	 * 
	 * @return AuthIDControllerDoc
	 */
	public static AuthIDChallengeResponseDoc fromJSON(JSONObject json) {
		return new AuthIDChallengeResponseDoc(json.getString(CHALLENGE_ID), json.getString(CHALLENGE_RESPONSE),
				json.getJSONObject(ID_DOC));
	}

}
