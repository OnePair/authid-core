package ca.onepair.authID;

public interface AuthIDDriver {

	/*
	 * Register ID ON system
	 */
	public String registerId(String id, String privKey) throws Exception;

	/*
	 * Fecrypt challenge with the did's corresponding private key. This is used
	 * for authentication
	 */
	public String signChallenge(String did, String challenge) throws Exception;

	/*
	 * Retrieve the did and it's info. TODO : make this return a generic did
	 * object
	 */
	public String retrieveID(String did);

	/*
	 * Create challenge for a did owner. This can be used as an authentication
	 * request.
	 */
	public String createChallenge(String did, String pubKey) throws Exception;

	/*
	 * Verify teh challenge response.
	 */
	public boolean verifyChallengeResponse(String did, String challenge, String response) throws Exception;

}
