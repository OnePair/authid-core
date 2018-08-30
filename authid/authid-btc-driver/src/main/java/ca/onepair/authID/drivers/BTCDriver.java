package ca.onepair.authID.drivers;

import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.PrunedException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.BlockChainListener;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.wallet.Wallet;

public class BTCDriver {

	private NetworkParameters networkParams;
	private AbstractBlockChain blockChain;
	private PeerGroup peerGroup;
	private Wallet wallet;

	public BTCDriver(NetworkParameters networkParams, Wallet wallet, AbstractBlockChain blockChain) {
		this.networkParams = networkParams;
		this.wallet = wallet;
		this.blockChain = blockChain;
	}

	public void connect() {
		this.peerGroup = new PeerGroup(this.networkParams, this.blockChain);
		this.peerGroup.addPeerDiscovery(new DnsDiscovery(this.networkParams));
		if (this.wallet != null)
			this.peerGroup.addWallet(this.wallet);
		this.peerGroup.startAsync();
		this.peerGroup.startBlockChainDownload(new DownloadProgressTracker());
	}

	public void disconnect() {
		if (this.peerGroup != null && this.peerGroup.isRunning()) {
			this.peerGroup.stopAsync();
		}
	}

	public void addBlockChainListener(BlockChainListener listener) {
		this.blockChain.addListener(listener);
	}

	public void broadcastTransaction(Transaction transaction) {
		this.peerGroup.broadcastTransaction(transaction);
	}

	public Wallet getWallet() {
		return this.wallet;
	}

	public NetworkParameters getNetworkParams() {
		return this.networkParams;
	}

	public PeerGroup getPeerGroup() {
		return this.peerGroup;
	}

}












