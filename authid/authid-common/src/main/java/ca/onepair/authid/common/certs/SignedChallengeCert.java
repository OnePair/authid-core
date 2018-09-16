package ca.onepair.authid.common.certs;

import org.json.JSONObject;

import ca.onepair.authid.common.model.AuthIDProcessorDoc;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

public class SignedChallengeCert implements AuthIDCert {

	public static final String TOKEN = "cert_token";
	public static final String SIGNED_CHALLENGE_CERT = "signed-challenge";

	public static final String CERT_TYPE = "cert_type";
	public static final String SIGNATURE = "signature";
	public static final String SIGNING_KEY = "signing_key";
	public static final String RESPONSE_KEY = "response_key";
	public static final String SIGNED_CHALLENGE = "signed_challenge";
	public static final String ID_DOC = "id_doc";

	private String signature;
	private String responseKey;
	private String signedChallenge;
	// The challenge signing key
	private AuthIDProcessorDoc signingKey;
	private AuthIDProcessorDoc idDoc;

	/*
	 * 
	*/
	public SignedChallengeCert(String responseKey, String signedChallenge, AuthIDProcessorDoc signingKey,
			String signature, AuthIDProcessorDoc idDoc) {
		this.signature = signature;
		this.responseKey = responseKey;
		this.signedChallenge = signedChallenge;
		this.signingKey = signingKey;
		this.idDoc = idDoc;
	}

	public SignedChallengeCert(String responseKey, String signedChallenge, AuthIDProcessorDoc signingKey) {
		this.responseKey = responseKey;
		this.signedChallenge = signedChallenge;
		this.signingKey = signingKey;
	}

	@Override
	public String getToken() {
		JwtBuilder tokenBuilder = Jwts.builder().claim(RESPONSE_KEY, this.responseKey)
				.claim(SIGNED_CHALLENGE, this.signedChallenge).claim(SIGNING_KEY, this.signingKey)
				.claim(CERT_TYPE, SIGNED_CHALLENGE_CERT);
		return tokenBuilder.compact();
	}

	@Override
	public String getSignature() {
		return this.signature;
	}

	@Override
	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Override
	public void setIDDoc(AuthIDProcessorDoc idDoc) {
		this.idDoc = idDoc;
	}

	@Override
	public AuthIDProcessorDoc getIDDoc() {
		return this.idDoc;
	}

	public String getResponseKey() {
		return this.responseKey;
	}

	public String getSignedChallenge() {
		return this.signedChallenge;
	}

	public AuthIDProcessorDoc getSigningKey() {
		return this.signingKey;
	}

	@Override
	public JSONObject toJson() {
		JSONObject certJson = new JSONObject();

		certJson.put(SIGNATURE, this.signature);
		certJson.put(ID_DOC, this.idDoc.toJSON());
		certJson.put(TOKEN, this.getToken());

		return certJson;
	}

	public static SignedChallengeCert fromJson(JSONObject json) {
		AuthIDProcessorDoc idDoc = AuthIDProcessorDoc.fromJSON(json.getJSONObject(ID_DOC));

		Claims claims = Jwts.parser().parseClaimsJwt(json.getString(TOKEN)).getBody();
		AuthIDProcessorDoc signingKey = AuthIDProcessorDoc.fromJSON((JSONObject) claims.get(SIGNING_KEY));

		return new SignedChallengeCert((String) claims.get(RESPONSE_KEY), (String) claims.get(SIGNED_CHALLENGE),
				signingKey, (String) json.getString(SIGNATURE), idDoc);
	}

}
