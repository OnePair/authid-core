package ca.onepair.authid.common.certs;

import org.json.JSONObject;

import ca.onepair.authid.common.model.AuthIDProcessorDoc;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

public class DHChallengeCert implements AuthIDCert {

	public static final String TOKEN = "cert_token";
	public static final String DH_CHALLENGE_CERT = "dh-challenge-cert";

	public static final String CERT_TYPE = "cert_type";
	public static final String PUBLIC_KEY = "public_key";
	public static final String RECEIVER_ID = "receiver_id";
	public static final String SIGNATURE = "signature";
	public static final String ID_DOC = "id_doc";

	private String publicKey;
	private String receiverId;
	private String signature;
	private AuthIDProcessorDoc idDoc;

	/*
	 * Constructor
	 * 
	 * @param publicKey the Diffie-Hellman public key
	 * 
	 * @param signature the signature of the document
	 * 
	 * @param receiverId the id of the challenge receiver
	 * 
	 * @param idDoc the id doc used to sign the cert
	 */
	public DHChallengeCert(String publicKey, String signature, String receiverId, AuthIDProcessorDoc idDoc) {
		this.publicKey = publicKey;
		this.signature = signature;
		this.receiverId = receiverId;
		this.idDoc = idDoc;
	}

	public DHChallengeCert(String publicKey, String receiverId) {
		this.publicKey = publicKey;
		this.receiverId = receiverId;
	}

	public String getPublicKey() {
		return this.publicKey;
	}

	public String getReceiverId() {
		return this.receiverId;
	}

	@Override
	public String getToken() {
		JwtBuilder tokenBuilder = Jwts.builder().claim(PUBLIC_KEY, this.publicKey).claim(RECEIVER_ID, this.receiverId)
				.claim(CERT_TYPE, DH_CHALLENGE_CERT);
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

	@Override
	public JSONObject toJson() {
		JSONObject certJson = new JSONObject();

		certJson.put(TOKEN, this.getToken());
		certJson.put(SIGNATURE, this.signature);
		certJson.put(ID_DOC, this.idDoc.toJSON());

		return certJson;
	}

	public static DHChallengeCert fromJson(JSONObject jsonCert) {
		AuthIDProcessorDoc idDoc = AuthIDProcessorDoc.fromJSON(jsonCert.getJSONObject(ID_DOC));
		Claims claims = Jwts.parser().parseClaimsJwt(jsonCert.getString(TOKEN)).getBody();

		String publicKey = (String) claims.get(PUBLIC_KEY);
		String receiverID = (String) claims.get(RECEIVER_ID);

		return new DHChallengeCert(publicKey, jsonCert.getString(SIGNATURE), receiverID, idDoc);
	}

}
