package ca.onepair.authid.common.drivers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;

import ca.onepair.authid.common.certs.AuthIDCert;
import ca.onepair.authid.common.certs.DHChallengeCert;
import ca.onepair.authid.common.certs.SignedChallengeCert;
import ca.onepair.authid.common.exceptions.AuthIDDriverException;
import ca.onepair.authid.common.exceptions.DoesNotExistException;
import ca.onepair.authid.common.exceptions.InvalidCertException;
import ca.onepair.authid.common.exceptions.MissingKeyException;
import ca.onepair.authid.common.model.AuthIDControllerDoc;
import ca.onepair.authid.common.model.AuthIDProcessorDoc;

/*
 * The interface used to implement AuthID drivers.
 * 
 * @author Noah Bouma
 * @version 0.0.1
*/
public interface AuthIDDriver {

	/*
	 * Register a new AuthID.
	 * 
	 * @param id the id
	 * 
	 * @param address the id's controller address
	 * 
	 * @param fee the transaction fee
	 * 
	 * @return the transaction reference
	 */
	public String registerId(String id, String address, int fee) throws Exception;

	/*
	 * Transfer an AuthID to a new address.
	 * 
	 * @param id the id
	 * 
	 * @param transferAddress the address to transfer ownership to
	 * 
	 * @return the transaction reference
	 */
	public String transferID(String id, String transferAddress) throws Exception;

	/*
	 * Retrieve an AuthID controller doc.
	 * 
	 * @param id the id
	 * 
	 * @return the AuthID controller doc
	 */
	public AuthIDControllerDoc retrieveID(String id) throws Exception;

	/*
	 * Sign a certificate.
	 * 
	 * @param cert the certificate to sign
	 * 
	 * @param id the id used to sign the certificate
	 * 
	 * @return the signed certificate
	 */
	public AuthIDCert signCert(AuthIDCert cert, String id)
			throws SQLException, MissingKeyException, AuthIDDriverException;

	/*
	 * Verify an AuthID certificate.
	 *
	 * @param cert json encoding of an AuthID cert
	 * 
	 * @return the verification result
	 * 
	 */
	public boolean verifyCert(JSONObject cert) throws Exception;

	/*
	 * Create an AuthID challenge for mutual authentication.
	 * 
	 * @param challengerID the challenger's id
	 * 
	 * @param receiverID the id that will receive the challenge
	 * 
	 * @return the challenge document
	 */
	public DHChallengeCert createChallenge(String challengerID, String receiverID)
			throws AuthIDDriverException, SQLException, MissingKeyException, NoSuchAlgorithmException;

	/*
	 * Sign a challenge document for mutual authentication.
	 * 
	 * @param challenge the challenge document
	 * 
	 * @return the signed challenge certificate
	 */
	public SignedChallengeCert signChallenge(DHChallengeCert challenge) throws Exception;

	/*
	 * Verify challenge.
	 * 
	 * @param signedChallengeCert the signed challenge certificate
	 * 
	 * @return the verification result
	 */
	public boolean verifyChallenge(SignedChallengeCert signedChallengeCert) throws InvalidCertException, Exception;

	/*
	 * Generate and store a batch of processor keys.
	 * 
	 * @param id the id to generate processor keys for
	 * 
	 * @param amount the amount of processor keys to generate
	 */
	public void generateProcessorKeys(String id, int amount) throws DoesNotExistException, SQLException;

	/*
	 * Get the list of unsigned processor keys of an AuthID.
	 * 
	 * @param id the id of the processor keys
	 * 
	 * @return list of unsigned processor keys
	 */
	public List<AuthIDProcessorDoc> getUnsignedProcessorKeys(String id) throws SQLException;

	/*
	 * Sign the processor keys of an AuthID.
	 * 
	 * @param id the id of the processor keys
	 */
	public void signProcessorKeys(String id) throws DoesNotExistException, AuthIDDriverException, SQLException;

	/*
	 * Get a random processor key.
	 * 
	 * @param id the id of the processor key
	 * 
	 * @return a processor key document
	 */
	public AuthIDProcessorDoc getProcessorKey(String id) throws SQLException, MissingKeyException;

	/*
	 * Verify the authorization of a processor key.
	 * 
	 * @param processorKey a processor key document
	 * 
	 * @return the verification result
	 */
	public boolean verifyProcessorKey(AuthIDProcessorDoc processorKey) throws Exception;

	/*
	 * Create a new address.
	 * 
	 * @return an address
	 */
	public String newAddress() throws IOException;

}
