package ca.onepair.authid.common.certs;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import ca.onepair.authid.common.model.AuthIDProcessorDoc;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

public class GenericAuthIDCert implements AuthIDCert {
	public static final String TOKEN = "token";
	public static final String SIGNATURE = "signature";
	public static final String ID_DOC = "id_doc";

	private Map<String, String> claims;

	private String signature;
	private AuthIDProcessorDoc idDoc;

	public GenericAuthIDCert(Map<String, String> claims) {
		this.claims = claims;
	}

	@Override
	public String getToken() {
		JwtBuilder tokenBuilder = Jwts.builder();

		for (String key : this.claims.keySet()) {
			tokenBuilder.claim(key, this.claims.get(key));
		}
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

	public static GenericAuthIDCert fromClaimsJson(JSONObject claimsJson) {
		Map<String, String> claims = new HashMap<String, String>();

		for (String key : claimsJson.keySet()) {
			claims.put(key, claimsJson.getString(key));
		}

		return new GenericAuthIDCert(claims);
	}

}
