package ca.onepair.authID.model;

import org.json.JSONObject;

public class AuthIDChallengeDoc {

	/*
	 * JSON fields
	 */
	public static final String DOC_TYPE = "doc_type";
	public static final String ID = "id";
	public static final String CHALLENGE = "challenge";
	public static final String CHALLENGE_ID = "challenge_id";

	/*
	 * authid-challenge specific values
	*/
	private static final String CHALLENGE_DOC = "challenge-doc";

	private String id;
	private String challenge;
	private String challengeId;

	public AuthIDChallengeDoc(String id, String challenge, String challengeId) {
		this.id = id;
		this.challenge = challenge;
		this.challengeId = challengeId;
	}

	/*
	 * Convert to json
	 * 
	 * @return JSONObject
	 */
	public JSONObject toJSON() {
		JSONObject jsonDoc = new JSONObject();

		jsonDoc.put(DOC_TYPE, CHALLENGE_DOC);
		jsonDoc.put(ID, this.id);
		jsonDoc.put(CHALLENGE, this.challenge);
		jsonDoc.put(CHALLENGE_ID, this.challengeId);

		return jsonDoc;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getChallenge() {
		return this.challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public String getChallengeId() {
		return this.challengeId;
	}

	public void setChallengeId(String challengeId) {
		this.challengeId = challengeId;
	}

	/*
	 * Parse JSONObject to AuthIDChallengeDoc.
	 * 
	 * @param JSONObject json AuthID challenge document
	 * 
	 * @return AuthIDChallengeDoc
	 */
	public static AuthIDChallengeDoc fromJSON(JSONObject json) {
		return new AuthIDChallengeDoc(json.getString(ID), json.getString(CHALLENGE), json.getString(CHALLENGE_ID));
	}
}
