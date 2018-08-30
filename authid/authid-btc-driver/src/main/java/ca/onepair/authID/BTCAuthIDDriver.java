package ca.onepair.authID;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.LevelDBBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.json.JSONObject;

import ca.onepair.authID.drivers.BTCDriver;
import ca.onepair.authID.store.AuthIDStore;
import ca.onepair.authID.store.LevelDBAuthIDStore;
import ca.onepair.authID.utils.BTCIDUtils;
import ca.onepair.authid.common.certs.AuthIDCert;
import ca.onepair.authid.common.certs.DHChallengeCert;
import ca.onepair.authid.common.certs.SignedChallengeCert;
import ca.onepair.authid.common.drivers.AuthIDDriver;
import ca.onepair.authid.common.exceptions.AuthIDDriverException;
import ca.onepair.authid.common.exceptions.DoesNotExistException;
import ca.onepair.authid.common.exceptions.IDClaimedException;
import ca.onepair.authid.common.exceptions.InvalidCertException;
import ca.onepair.authid.common.exceptions.InvalidChallengeException;
import ca.onepair.authid.common.exceptions.MissingKeyException;
import ca.onepair.authid.common.exceptions.RevokedException;
import ca.onepair.authid.common.model.AuthIDControllerDoc;
import ca.onepair.authid.common.model.AuthIDProcessorDoc;

public class BTCAuthIDDriver implements AuthIDDriver {

	private static final String BLOCK_STORE_PATH = "blockchain.spvchain";
	private static final String ID_STORE_PATH = "authID-store.db";
	private static final String BLOCKS_STORE_PATH = "blocks-store.db";
	private static final String PROTOCOL = "BTC";

	private static final int CHALLENGE_KEY_LENGTH = 2048;
	private static final String CHALLENGE_KEY_ALGORITHM = "DH";

	private NetworkParameters networkParams;
	private AuthIDKeyWallet wallet;
	// private Wallet wallet;
	private BTCDriver btcDriver;
	private BlockStore blockStore;
	private AuthIDStore authIDStore;
	private AbstractBlockChain authIDBlockChain;
	// ID / keypair
	private Map<String, KeyPair> sentChallenges;

	public BTCAuthIDDriver(NetworkParameters networkParameters) throws BlockStoreException {
		this(networkParameters, "");
	}

	public BTCAuthIDDriver(NetworkParameters networkParameters, String directoryPath) throws BlockStoreException {
		this(networkParameters, null, directoryPath);
	}

	public BTCAuthIDDriver(NetworkParameters networkParams, AuthIDKeyWallet wallet, String directoryPath)
			throws BlockStoreException {
		this(networkParams, wallet,
				new LevelDBBlockStore(Context.getOrCreate(networkParams), new File(directoryPath + BLOCK_STORE_PATH)),
				new LevelDBAuthIDStore(new File(directoryPath + ID_STORE_PATH),
						new File(directoryPath + BLOCKS_STORE_PATH)));
	}

	public BTCAuthIDDriver(NetworkParameters networkParams, AuthIDKeyWallet wallet, BlockStore blockStore,
			AuthIDStore authIDStore) throws BlockStoreException {
		this.networkParams = networkParams;
		this.wallet = wallet;
		this.blockStore = blockStore;
		this.authIDStore = authIDStore;
		this.sentChallenges = new HashMap<String, KeyPair>();

		Wallet bitcoinWallet;

		if (this.wallet == null)
			bitcoinWallet = new Wallet(this.networkParams);
		else
			bitcoinWallet = this.getBitcoinWallet();

		if (this.wallet == null) {
			this.authIDBlockChain = new AuthIDBlockChain(this.networkParams, this.blockStore, this.authIDStore);
		} else {
			this.authIDBlockChain = new AuthIDBlockChain(this.networkParams, bitcoinWallet, this.blockStore,
					this.authIDStore);
		}

		this.btcDriver = new BTCDriver(this.networkParams, this.getBitcoinWallet(), this.authIDBlockChain);
		this.btcDriver.connect();
	}

	@Override
	public String registerId(String id, String address, int fee) throws MissingKeyException, InsufficientMoneyException,
			IDClaimedException, AuthIDDriverException, SQLException, IOException {
		System.out.println("number of keys:" + this.getBitcoinWallet().getImportedKeys().size());
		System.out.println("balance:" + this.getBitcoinWallet().getBalance());

		if (wallet == null)
			throw new AuthIDDriverException(AuthIDDriverException.NO_WALLET_EXCEPTION);

		BTCAuthID authID = this.authIDStore.getAuthIDFromName(Utils.HEX.encode(Sha256Hash.hash(id.getBytes())));

		Transaction transferTransaction = null;

		if (authID != null && (transferTransaction = authID.getTransferTransaction()) == null)
			throw new IDClaimedException();

		Transaction registrationTransaction = BTCIDUtils.buildAuthIDTransaction(this.networkParams, this.authIDStore,
				this.getBitcoinWallet(), transferTransaction, address, id, 0);
		this.wallet.save();

		System.out.println("registration transaction: " + Utils.HEX.encode(registrationTransaction.bitcoinSerialize()));
		this.btcDriver.broadcastTransaction(registrationTransaction);
		return registrationTransaction.getHashAsString();
	}

	@Override
	public String transferID(String id, String destinationAddress) throws InsufficientMoneyException,
			MissingKeyException, DoesNotExistException, RevokedException, AuthIDDriverException, IOException {
		if (wallet == null)
			throw new AuthIDDriverException(AuthIDDriverException.NO_WALLET_EXCEPTION);

		BTCAuthID authID = this.authIDStore.getAuthIDFromName(getHashFromID(id));

		if (authID == null)
			throw new DoesNotExistException();
		if (authID.getTransferTransaction() != null)
			throw new RevokedException();
		Transaction authIDTransaction = this.getBitcoinWallet()
				.getTransaction(Sha256Hash.wrap(authID.getTransactionId()));

		Transaction transferTransaction = BTCIDUtils.buildTransferTransaction(this.networkParams, authIDTransaction,
				destinationAddress, this.getBitcoinWallet(), 0);
		this.wallet.save();
		this.btcDriver.broadcastTransaction(transferTransaction);

		return transferTransaction.getHashAsString();
	}

	@Override
	public AuthIDControllerDoc retrieveID(String id) throws DoesNotExistException {
		String hashedID = Utils.HEX.encode(Sha256Hash.hash(id.getBytes()));
		BTCAuthID btcAuthID = this.authIDStore.getAuthIDFromName(hashedID);

		if (btcAuthID == null)
			throw new DoesNotExistException();

		Transaction transferTransaction = btcAuthID.getTransferTransaction();
		String transferID = null;
		if (transferTransaction != null)
			transferID = transferTransaction.getHashAsString();

		AuthIDControllerDoc authID = new AuthIDControllerDoc(PROTOCOL, btcAuthID.getAddress(), id,
				btcAuthID.getTransactionId(), btcAuthID.getTransferTransaction() == null, transferID);

		return authID;
	}

	/*
	 * TODO: create function to get the signing key
	 */
	@Override
	public AuthIDCert signCert(AuthIDCert cert, String id)
			throws SQLException, MissingKeyException, AuthIDDriverException {
		if (wallet == null)
			throw new AuthIDDriverException(AuthIDDriverException.NO_WALLET_EXCEPTION);

		// Get the processor key document
		AuthIDProcessorDoc processorKeyDoc = this.getProcessorKey(id);

		// Get the signing key
		Map<String, ECKey> keys = BTCIDUtils.getKeysFromWallet(this.networkParams, this.getBitcoinWallet());
		ECKey signingKey = keys.get(processorKeyDoc.getAddress());

		if (signingKey == null)
			throw new MissingKeyException();

		System.out.println("getting signature!!!!");
		// Sign the certificate
		String signature = signingKey.signMessage(cert.getToken());

		cert.setIDDoc(processorKeyDoc);
		cert.setSignature(signature);

		return cert;
	}

	/*
	 * TODO put fields in constants
	 */
	@Override
	public boolean verifyCert(JSONObject cert) throws Exception {
		AuthIDProcessorDoc processorKey = AuthIDProcessorDoc.fromJSON(cert.getJSONObject("id_doc"));

		if (!this.verifyProcessorKey(processorKey))
			return false;

		ECKey signingKey;
		try {
			signingKey = ECKey.signedMessageToKey(cert.getString("cert_token"), cert.getString("signature"));
		} catch (SignatureException e) {
			return false;
		}

		// verify the message
		if (signingKey.toAddress(this.networkParams).toString().equals(processorKey.getAddress()))
			return true;

		return false;
	}

	@Override
	public DHChallengeCert createChallenge(String challengerID, String receiverID)
			throws AuthIDDriverException, SQLException, MissingKeyException, NoSuchAlgorithmException {

		// Generate the challenge key
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CHALLENGE_KEY_ALGORITHM);
		keyPairGenerator.initialize(CHALLENGE_KEY_LENGTH, new SecureRandom());
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		// Get the public key String
		String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
		System.out.println("generated public key: " + publicKey);

		// Create the challenge document
		DHChallengeCert challengeCert = new DHChallengeCert(publicKey, receiverID);

		// Sign the certificate
		this.signCert(challengeCert, challengerID);

		// store the challenge key for later
		this.sentChallenges.put(receiverID, keyPair);

		return challengeCert;
	}

	@Override
	public SignedChallengeCert signChallenge(DHChallengeCert challengeCert) throws Exception {
		// verify certificate
		if (!this.verifyCert(challengeCert.toJson()))
			throw new InvalidCertException();

		// Diffie-Hellman challenge public key
		byte[] challengeKeyEncoded = Base64.getDecoder().decode(challengeCert.getPublicKey());

		KeyFactory keyFactory = KeyFactory.getInstance(CHALLENGE_KEY_ALGORITHM);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(challengeKeyEncoded);

		PublicKey challengeKey = keyFactory.generatePublic(x509KeySpec);

		// Get the Diffie-Hellman key parameters
		DHParameterSpec challengeKeyParams = ((DHPublicKey) challengeKey).getParams();

		// Generate the response key
		KeyPairGenerator responseKeyFactory = KeyPairGenerator.getInstance(CHALLENGE_KEY_ALGORITHM);
		responseKeyFactory.initialize(challengeKeyParams);
		KeyPair responseKeyPair = responseKeyFactory.generateKeyPair();

		// Initialize the key agreement
		KeyAgreement responseKeyAgree = KeyAgreement.getInstance(CHALLENGE_KEY_ALGORITHM);
		responseKeyAgree.init(responseKeyPair.getPrivate());

		// get the response public key
		byte[] responsePubKeyEncoded = responseKeyPair.getPublic().getEncoded();
		String responsePublicKeyString = Base64.getEncoder().encodeToString(responsePubKeyEncoded);

		// Generate the challenge secret
		responseKeyAgree.doPhase(challengeKey, true);
		String challengeSecret = Base64.getEncoder().encodeToString(responseKeyAgree.generateSecret());

		// TODO: create a dedicated method for generic signature

		// Get the processor key for the challenge
		AuthIDProcessorDoc challengeSigningDoc = this.getProcessorKey(challengeCert.getReceiverId());

		// Get the signing key
		Map<String, ECKey> keys = BTCIDUtils.getKeysFromWallet(this.networkParams, this.getBitcoinWallet());
		ECKey signingKey = keys.get(challengeSigningDoc.getAddress());

		if (signingKey == null)
			throw new MissingKeyException();

		// Sign the challenge
		String signedChallenge = signingKey.signMessage(challengeSecret);

		// Create the signed challenge certificate

		SignedChallengeCert signedChallengeCert = new SignedChallengeCert(responsePublicKeyString, signedChallenge,
				challengeSigningDoc);

		// Sign the certificate
		this.signCert(signedChallengeCert, challengeCert.getReceiverId());

		return signedChallengeCert;
	}

	@Override
	public boolean verifyChallenge(SignedChallengeCert signedChallengeCert) throws InvalidCertException, Exception {
		if (!this.verifyCert(signedChallengeCert.toJson()))
			throw new InvalidCertException();

		// Get the challenge key pair

		KeyPair challengeKeyPair = this.sentChallenges.get(signedChallengeCert.getSigningKey().getId());

		if (challengeKeyPair == null)
			throw new InvalidChallengeException();

		KeyFactory keyFactory = KeyFactory.getInstance(CHALLENGE_KEY_ALGORITHM);

		// Get the response key from the signed challenge certificate

		byte[] responseKeyEncoded = Base64.getDecoder().decode(signedChallengeCert.getResponseKey());
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(responseKeyEncoded);
		PublicKey responsePublicKey = keyFactory.generatePublic(x509KeySpec);

		// Generate the shared challenge secret

		KeyAgreement keyAgreement = KeyAgreement.getInstance(CHALLENGE_KEY_ALGORITHM);
		keyAgreement.init(challengeKeyPair.getPrivate());

		keyAgreement.doPhase(responsePublicKey, true);

		String challengeSecret = Base64.getEncoder().encodeToString(keyAgreement.generateSecret());

		// Verify the signingKey

		AuthIDProcessorDoc signingProcessorDoc = signedChallengeCert.getSigningKey();

		if (!this.verifyProcessorKey(signingProcessorDoc))
			throw new InvalidCertException();

		// Verify the signature

		ECKey signingKey = ECKey.signedMessageToKey(challengeSecret, signedChallengeCert.getSignedChallenge());

		if (signingKey.toAddress(this.networkParams).toString().equals(signingProcessorDoc.getAddress()))
			return true;

		return false;
	}

	@Override
	public void generateProcessorKeys(String id, int amount) throws DoesNotExistException, SQLException {
		AuthIDControllerDoc controllerKey = this.retrieveID(id);
		List<AuthIDProcessorDoc> processorKeys = this.wallet.generateProcessorKeys(controllerKey, amount);
		this.wallet.addProcessorKeys(processorKeys);
	}

	@Override
	public List<AuthIDProcessorDoc> getUnsignedProcessorKeys(String id) throws SQLException {
		return this.wallet.getUnsignedProcessorKeys(id);
	}

	/*
	 * Note this should not be part of the protocol
	 */
	@Override
	public void signProcessorKeys(String id) throws DoesNotExistException, AuthIDDriverException, SQLException {
		AuthIDControllerDoc controllerDoc = this.retrieveID(id);

		if (controllerDoc == null)
			throw new AuthIDDriverException("Could not find ID!");

		if (wallet == null)
			throw new AuthIDDriverException(AuthIDDriverException.NO_WALLET_EXCEPTION);

		// ECKey key = BTCDriver.getDIDBitcoinWallet().getKey(address);
		Map<String, ECKey> keys = BTCIDUtils.getKeysFromWallet(this.networkParams, this.getBitcoinWallet());
		ECKey key = keys.get(controllerDoc.getAddress());

		List<AuthIDProcessorDoc> processorKeys = this.getUnsignedProcessorKeys(id);

		for (AuthIDProcessorDoc processorKey : processorKeys) {
			String fingerprint = processorKey.getFingerprint();
			// String hashedFingerprint =
			// Utils.HEX.encode(Sha256Hash.hash(fingerprint.getBytes()));
			String signedFingerprint = key.signMessage(fingerprint);
			processorKey.setSignature(signedFingerprint);
		}

		this.wallet.updateProcessorKeys(processorKeys);
	}

	@Override
	public AuthIDProcessorDoc getProcessorKey(String id) throws SQLException, MissingKeyException {
		return this.wallet.getProcessorKey(id);
	}

	/*
	 * Validate a processor key
	 * 
	 * @param AuthIDProcessorDoc processorKey
	 * 
	 * @return boolean result
	 */
	@Override
	public boolean verifyProcessorKey(AuthIDProcessorDoc processorKey) throws DoesNotExistException {
		String fingerPrint = processorKey.getFingerprint();
		return this.verifyChallengeResponseString(processorKey.getId(), fingerPrint, processorKey.getSignature());
	}

	@Override
	public String newAddress() throws IOException {
		ECKey keyPair = this.wallet.newBitcoinKey();
		return keyPair.toAddress(this.networkParams).toString();
	}

	public AbstractBlockChain getBlockChain() {
		return this.authIDBlockChain;
	}

	public PeerGroup getPeers() {
		return this.btcDriver.getPeerGroup();
	}

	public Wallet getBitcoinWallet() {
		if (this.wallet != null)
			return this.wallet.getBitcoinWallet();
		return null;
	}

	private boolean verifyChallengeResponseString(String id, String challenge, String response)
			throws DoesNotExistException {

		ECKey signingKey;
		try {
			signingKey = ECKey.signedMessageToKey(challenge, response);
		} catch (SignatureException e) {
			return false;
		}

		String address = this.retrieveID(id).getAddress();

		if (address == null)
			throw new DoesNotExistException();

		if (signingKey.toAddress(this.networkParams).toString().equals(address)) {
			return true;
		}

		return false;
	}

	private static String getHashFromID(String id) {
		return Utils.HEX.encode(Sha256Hash.hash(id.getBytes()));
	}

}
