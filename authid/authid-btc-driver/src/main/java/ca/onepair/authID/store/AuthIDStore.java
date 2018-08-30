package ca.onepair.authID.store;

import java.util.List;

import ca.onepair.authID.BTCAuthID;
import ca.onepair.authid.common.exceptions.AuthIDStoreException;

public interface AuthIDStore {

	/*
	 * Update the newest ID in the store.
	 * 
	 * @param BTCAuthID id
	 */
	public void updateID(BTCAuthID id);

	/*
	 * Return true if the indexBlockHash needs to be uses
	 * 
	 * @return boolean
	 */
	public boolean blockIndexingSupported();

	/*
	 * Create a blockhash/txs index in the store.
	 * 
	 * @param string blockHash
	 * 
	 * @param List<String> list of AuthID transaction hashes
	 */
	public void indexBlockHash(String blockHash, List<String> txHashes);

	/*
	 * Remove all the transactions mapped to the block hash.
	 * 
	 * @param String blockHash
	 */
	public void removeWithBlockHash(String blockHash);

	/*
	 * Get the transactions mapped to the block.
	 * 
	 * @return List<String> transaction hashes
	 */
	public List<String> getBlockTransactions(String blockHash);

	/*
	 * Remove the id from the store using its transaction hash.
	 * 
	 * @param String txID Transaction hash
	 */
	public void removeAuthIDromTxID(String txID) throws AuthIDStoreException;

	/*
	 * Get the AuthID from the store with its name.
	 * 
	 * @param String name
	 * 
	 * @return BTCAuthID the ID
	 */
	public BTCAuthID getAuthIDFromName(String name);

	/*
	 * Get the AuthID from the store using the transaction id
	 * 
	 * @param String txID
	 * 
	 * @return BTCAuthID The ID
	 */
	public BTCAuthID getAuthIDFromTxID(String txID);

	public Iterable<BTCAuthID> getAuthIDs();
}
