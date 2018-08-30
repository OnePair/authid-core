package ca.onepair.authID;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;
import org.json.JSONObject;

import ca.onepair.authID.utils.BTCIDUtils;
import ca.onepair.authid.common.exceptions.MissingKeyException;
import ca.onepair.authid.common.model.AuthIDControllerDoc;
import ca.onepair.authid.common.model.AuthIDProcessorDoc;

public class SqliteAuthIDKeyWallet implements AuthIDKeyWallet {

	private static final String KEYS_DB = "authid-keys.db";
	private static final String KEYS_TABLE = "keys";
	private static final String KEY_ID_FIELD = "id";
	private static final String KEY_DOC_FIELD = "doc";
	private static final String ADDRESS_FIELD = "address";
	private static final String KEY_TYPE_FIELD = "key_type";
	private static final String SIGNED_FIELD = "signed";

	private Wallet wallet;
	private NetworkParameters networkParams;
	private File subWalletDir;
	private File bitcoinWalletFile;

	private Connection dbConnection;

	public SqliteAuthIDKeyWallet(NetworkParameters networkParams, File walletDir)
			throws ClassNotFoundException, SQLException, UnreadableWalletException, IOException {
		this.networkParams = networkParams;
		this.setupWallet(walletDir, networkParams);
		this.setupDB();
	}

	@Override
	public Wallet getBitcoinWallet() {
		return this.wallet;
	}

	@Override
	public void importBitcoinKeys(String[] keys) throws IOException {
		List<ECKey> keyPairs = new ArrayList<ECKey>();

		for (String key : keys) {
			System.out.println("key:" + key);
			ECKey keyPair = DumpedPrivateKey.fromBase58(this.networkParams, key).getKey();
			keyPairs.add(keyPair);
		}

		this.wallet.importKeys(keyPairs);

		this.save();
	}

	@Override
	public void createAndSignProcessorKeys(AuthIDControllerDoc controllerDoc, ECKey controllerKey)
			throws MissingKeyException, SQLException {
		this.createAndSignProcessorKeys(controllerDoc, controllerKey, 10);

	}

	@Override
	public void createAndSignProcessorKeys(AuthIDControllerDoc controllerDoc, ECKey controllerKey, int amount)
			throws MissingKeyException, SQLException {
		// unsigned processor keys
		List<AuthIDProcessorDoc> processorKeys = this.generateProcessorKeys(controllerDoc, amount);

		for (AuthIDProcessorDoc processorKey : processorKeys) {
			String fingerprint = processorKey.getFingerprint();
			String hashedFingerprint = Utils.HEX.encode(Sha256Hash.hash(fingerprint.getBytes()));
			String signature = controllerKey.signMessage(hashedFingerprint);
			processorKey.setSignature(signature);
			this.storeProcessorKey(processorKey);
		}

	}

	@Override
	public List<AuthIDProcessorDoc> generateProcessorKeys(AuthIDControllerDoc controllerDoc, int amount) {
		List<AuthIDProcessorDoc> processorKeys = new ArrayList<AuthIDProcessorDoc>();

		String protocol = controllerDoc.getProtocol();
		String id = controllerDoc.getId();

		for (int i = 0; i < amount; i++) {
			// Create and store key
			ECKey key = new ECKey();
			String processorAddress = key.toAddress(this.networkParams).toString();

			this.wallet.importKey(key);

			AuthIDProcessorDoc processorKeyDoc = new AuthIDProcessorDoc(protocol, processorAddress, id, null);
			processorKeys.add(processorKeyDoc);
		}

		return processorKeys;
	}

	@Override
	public void addProcessorKeys(List<AuthIDProcessorDoc> processorKeys) throws SQLException {
		for (AuthIDProcessorDoc keyDoc : processorKeys) {
			this.storeProcessorKey(keyDoc);
		}
	}

	@Override
	public List<AuthIDProcessorDoc> getUnsignedProcessorKeys(String id) throws SQLException {
		return this.getProcessorKeys(id + ".BTC", false);
	}

	@Override
	public void updateProcessorKeys(List<AuthIDProcessorDoc> processorKeys) throws SQLException {
		for (AuthIDProcessorDoc processorKey : processorKeys) {
			this.updateProcessorKey(processorKey);
		}
	}

	/*
	 * TODO: fix .BTC thing
	 */
	@Override
	public AuthIDProcessorDoc getProcessorKey(String id) throws SQLException, MissingKeyException {
		List<AuthIDProcessorDoc> processorKeys = this.getProcessorKeys(id + ".BTC", true);
		if (processorKeys.size() == 0)
			throw new MissingKeyException("There are no processor keys for " + id);
		return processorKeys.get(new Random().nextInt(processorKeys.size()));
	}

	@Override
	public ECKey newBitcoinKey() throws IOException {
		ECKey bitcoinKey = new ECKey();
		this.wallet.importKey(bitcoinKey);
		this.save();
		return bitcoinKey;
	}

	@Override
	public void save() throws IOException {
		this.wallet.saveToFile(this.bitcoinWalletFile);
	}

	/*
	 * Get all the id's processor keys
	 * 
	 * @param String id
	 * 
	 * @return List<AuthIDProcessorDoc> List of processor keys
	 */
	private List<AuthIDProcessorDoc> getProcessorKeys(String id, boolean signed) throws SQLException {
		List<AuthIDProcessorDoc> processorKeys = new ArrayList<AuthIDProcessorDoc>();

		String selectQuery = "SELECT " + KEY_DOC_FIELD + " FROM " + KEYS_TABLE + " WHERE " + KEY_ID_FIELD + "=? and "
				+ KEY_TYPE_FIELD + "=? and " + SIGNED_FIELD + "=?";

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = this.dbConnection.prepareStatement(selectQuery);
			stmt.setString(1, id);
			stmt.setString(2, AuthIDProcessorDoc.PROCESSOR_DOC);
			stmt.setBoolean(3, signed);

			rs = stmt.executeQuery();

			while (rs.next()) {
				JSONObject docJson = new JSONObject(rs.getString(KEY_DOC_FIELD));
				AuthIDProcessorDoc processorKey = AuthIDProcessorDoc.fromJSON(docJson);

				processorKeys.add(processorKey);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (stmt != null)
				stmt.close();
		}

		return processorKeys;
	}

	private void storeProcessorKey(AuthIDProcessorDoc processorKey) throws SQLException {
		String json = processorKey.toJSON().toString();
		String address = processorKey.getAddress();
		String type = processorKey.getDocType();

		String sql = "INSERT INTO " + KEYS_TABLE + "(" + KEY_ID_FIELD + "," + KEY_DOC_FIELD + "," + ADDRESS_FIELD + ","
				+ KEY_TYPE_FIELD + "," + SIGNED_FIELD + ") " + "VALUES(?,?,?,?,?)";

		PreparedStatement stmt = null;

		try {
			stmt = this.dbConnection.prepareStatement(sql);
			stmt.setString(1, processorKey.getCanonicalID());
			stmt.setString(2, json);
			stmt.setString(3, address);
			stmt.setString(4, type);
			stmt.setBoolean(5, processorKey.getSignature() != null);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}

	private void updateProcessorKey(AuthIDProcessorDoc processorKey) {
		String address = processorKey.getAddress();

		String sql = "UPDATE " + KEYS_TABLE + " SET " + KEY_DOC_FIELD + " = ?," + SIGNED_FIELD + " = ? " + " WHERE "
				+ ADDRESS_FIELD + " = ?";

		PreparedStatement stmt = null;

		try {
			stmt = this.dbConnection.prepareStatement(sql);

			stmt.setString(1, processorKey.toJSON().toString());
			stmt.setBoolean(2, processorKey.getSignature() != null);
			stmt.setString(3, address);

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/*
	 * Setup the wallet if it doesn't exists
	 */
	private void setupWallet(File walletDir, NetworkParameters networkParams)
			throws UnreadableWalletException, IOException {
		this.wallet = new Wallet(networkParams);
		if (!walletDir.exists())
			walletDir.mkdir();

		String subWalletFileName = networkParams.getId();
		this.subWalletDir = new File(walletDir, subWalletFileName);

		if (!this.subWalletDir.exists()) {
			this.subWalletDir.mkdir();
			this.wallet = new Wallet(networkParams);
			this.bitcoinWalletFile = new File(this.subWalletDir.getAbsolutePath(), "bitcoin-wallet");
			this.wallet.saveToFile(this.bitcoinWalletFile);
		} else {
			this.bitcoinWalletFile = new File(this.subWalletDir.getAbsolutePath(), "bitcoin-wallet");
			if (this.bitcoinWalletFile.exists()) {
				this.wallet = Wallet.loadFromFile(this.bitcoinWalletFile);
			} else {
				this.wallet = new Wallet(networkParams);
				this.wallet.saveToFile(this.bitcoinWalletFile);
			}

		}

		this.wallet.autosaveToFile(this.bitcoinWalletFile, 15, TimeUnit.SECONDS, null);

	}

	/*
	 * Setup the Sqlite DB
	 */
	private void setupDB() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		this.dbConnection = DriverManager
				.getConnection("jdbc:sqlite:" + this.subWalletDir.getAbsolutePath() + "/" + KEYS_DB);

		// String createTableSql = "CREATE TABLE IF NOT EXISTS " + KEYS_TABLE +
		// "(" + KEY_DOC_FIELD + " TEXT PRIMARY KEY,"
		// + ADDRESS_FIELD + " TEXT," + KEY_TYPE_FIELD + " TEXT)";
		String createTableSql = "CREATE TABLE IF NOT EXISTS " + KEYS_TABLE + "(" + KEY_ID_FIELD + " TEXT,"
				+ KEY_DOC_FIELD + " TEXT," + ADDRESS_FIELD + " TEXT," + KEY_TYPE_FIELD + " TEXT," + SIGNED_FIELD
				+ " BOOLEAN)";
		System.out.println("Create table sql: " + createTableSql);

		Statement stmt = null;

		try {
			stmt = this.dbConnection.createStatement();
			stmt.executeUpdate(createTableSql);
		} catch (SQLException e) {
			System.out.println(e.getClass().getName() + ":" + e.getMessage());
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

}
