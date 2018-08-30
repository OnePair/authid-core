package ca.onepair.authID.store;

import java.util.List;

import org.bitcoinj.core.NetworkParameters;
import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import ca.onepair.authID.BTCAuthID;
import ca.onepair.authid.common.exceptions.AuthIDStoreException;

public class MongoDBAuthIDStore implements AuthIDStore {

	private static final String AUTHID_COLLECTION = "authid_collection";

	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> authIDCollection;

	public MongoDBAuthIDStore(NetworkParameters networkParams, String dbName, String dbHost, int dbPort) {
		this.initStore(dbName + "-" + networkParams.getPaymentProtocolId().replaceAll("\\.", "-"), dbHost, dbPort);
	}

	@Override
	public void updateID(BTCAuthID authID) {
		Document replacedDoc = this.authIDCollection.findOneAndReplace(Filters.eq(BTCAuthID.NAME, authID.getName()),
				Document.parse(authID.toString()));
		if (replacedDoc == null) {
			this.storeID(authID);
		}
	}

	@Override
	public boolean blockIndexingSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void indexBlockHash(String blockHash, List<String> txHashes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeWithBlockHash(String blockHash) {
		this.authIDCollection.deleteMany(Filters.eq(BTCAuthID.BLOCK_HASH, blockHash));

	}

	@Override
	public List<String> getBlockTransactions(String blockHash) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAuthIDromTxID(String txID) throws AuthIDStoreException {
		throw new AuthIDStoreException(AuthIDStoreException.UNSUPPORTED);
	}

	@Override
	public BTCAuthID getAuthIDFromName(String name) {
		Document authIDDocument = this.authIDCollection.find(Filters.eq(BTCAuthID.NAME, name)).limit(1).first();
		if (authIDDocument == null)
			return null;
		JSONObject authIDJson = new JSONObject(authIDDocument.toJson());

		return BTCAuthID.fromJson(authIDJson);
	}

	@Override
	public BTCAuthID getAuthIDFromTxID(String txID) {
		Document authIDDocument = this.authIDCollection.find(Filters.eq(BTCAuthID.TRANSACTION_ID, txID)).limit(1)
				.first();
		if (authIDDocument == null)
			return null;
		JSONObject authIDJson = new JSONObject(authIDDocument.toJson());

		return BTCAuthID.fromJson(authIDJson);
	}

	@Override
	public Iterable<BTCAuthID> getAuthIDs() {
		return null;
	}

	private void storeID(BTCAuthID authID) {
		Document authIDDocument = Document.parse(authID.toString());
		this.authIDCollection.insertOne(authIDDocument);
	}

	private void initStore(String dbName, String dbHost, int dbPort) {
		this.mongoClient = new MongoClient(dbHost, dbPort);
		this.db = this.mongoClient.getDatabase(dbName);
		this.authIDCollection = this.db.getCollection(AUTHID_COLLECTION);
	}

}
