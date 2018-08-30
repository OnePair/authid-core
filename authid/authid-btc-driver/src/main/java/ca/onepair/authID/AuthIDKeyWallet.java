package ca.onepair.authID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.wallet.Wallet;

import ca.onepair.authid.common.exceptions.MissingKeyException;
import ca.onepair.authid.common.model.AuthIDControllerDoc;
import ca.onepair.authid.common.model.AuthIDProcessorDoc;


/*
 * AuthID Bitcoin wallet
*/
public interface AuthIDKeyWallet {

	/*
	 * Get the Wallet object for interactions with the bitcoinj client.
	 * 
	 * @return Wallet
	 */
	public Wallet getBitcoinWallet();

	/*
	 * Import bitcoin private keys
	 * 
	 * @param String[] keys Array of wif private keys
	 */
	public void importBitcoinKeys(String[] keys) throws IOException;

	/*
	 * Create and store processor keys.
	 * 
	 * @param AuthIDControllerDoc controllerDoc The controller key document
	 * 
	 * @param ECKey controllerKey The controller key
	 */
	public void createAndSignProcessorKeys(AuthIDControllerDoc controllerDoc, ECKey controllerKey)
			throws MissingKeyException, SQLException;

	/*
	 * Create and store processor keys.
	 *
	 * @param AuthIDControllerDoc controllerDoc The controller key document
	 * 
	 * @param ECKey controllerKey The controller key
	 * 
	 * @param int amount The number of keys to generate
	 */
	public void createAndSignProcessorKeys(AuthIDControllerDoc controllerDoc, ECKey controllerKey, int amount)
			throws MissingKeyException, SQLException;

	/*
	 * Generate unsigned processor keys
	 * 
	 * @param String AuthIDControllerDock controllerDoc The controller key
	 * document
	 * 
	 * @param int amount The number of keys to be generated
	 */
	public List<AuthIDProcessorDoc> generateProcessorKeys(AuthIDControllerDoc controllerDoc, int amount);

	/*
	 * Add a list of processor keys to the wallet.
	 * 
	 * @param List<AuthIDProcessorDoc> processorKeys The list of processor keys
	 */
	public void addProcessorKeys(List<AuthIDProcessorDoc> processorKeys) throws SQLException;

	/*
	 * Get the unsigned processor keys
	 * 
	 * @param String id The id of the processor keys
	 * 
	 * @return List<AuthIDProcessorDoc> The list of processor keys
	 */
	public List<AuthIDProcessorDoc> getUnsignedProcessorKeys(String id) throws SQLException;

	/*
	 * Update processor keys in the db. This would be used to update with signed
	 * keys.
	 * 
	 * @param List<AuthIDProcessorDoc> processorKeys The processor keys
	 * 
	 */
	public void updateProcessorKeys(List<AuthIDProcessorDoc> processorKeys) throws SQLException;

	/*
	 * Get a randomly selected processor key
	 *
	 * @param String id
	 * 
	 * @return AuthIDProcessorDoc Randomly selected processor key
	 */
	public AuthIDProcessorDoc getProcessorKey(String id) throws SQLException, MissingKeyException;

	/*
	 * Generate and store a new bitcoin key
	 * 
	 * @return ECKey The newly generated key piar
	 */
	public ECKey newBitcoinKey() throws IOException;

	/*
	 * Save the state of the wallet
	 */
	public void save() throws IOException;

}
