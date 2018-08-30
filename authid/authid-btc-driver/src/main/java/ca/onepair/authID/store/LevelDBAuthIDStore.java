package ca.onepair.authID.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;

import ca.onepair.authID.BTCAuthID;

/*
 * TODO: dynamically assign directory
*/
public class LevelDBAuthIDStore implements AuthIDStore {

	private File storePath;
	private File blocksStorePath;
	private DB idDB;
	private DB blocksDB;

	public LevelDBAuthIDStore(File idStorePath, File blocksStorePath) {
		this(idStorePath, blocksStorePath, JniDBFactory.factory);
	}

	public LevelDBAuthIDStore(File idStorePath, File blocksStorePath, DBFactory dbFactory) {
		this.storePath = idStorePath;
		this.blocksStorePath = blocksStorePath;

		Options options = new Options();
		options.createIfMissing();

		try {
			this.idDB = this.tryOpen(this.storePath, dbFactory, options);
			this.blocksDB = this.tryOpen(this.blocksStorePath, dbFactory, options);
		} catch (IOException e) {
			try {
				dbFactory.repair(this.storePath, options);
				dbFactory.repair(this.blocksStorePath, options);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private synchronized DB tryOpen(File directory, DBFactory dbFactory, Options options) throws IOException {
		idDB = dbFactory.open(directory, options);
		return idDB;
	}

	@Override
	public synchronized void updateID(BTCAuthID authID) {
		BTCAuthID storedDID = this.getAuthIDFromName(authID.getName());

		if (storedDID != null) {
			this.idDB.delete(storedDID.getTransactionId().getBytes());
		}
		this.storeID(authID);
	}

	@Override
	public boolean blockIndexingSupported() {
		return true;
	}

	@Override
	public synchronized void indexBlockHash(String blockHash, List<String> txHashes) {
		List<String> blockTxs = this.getBlockTransactions(blockHash);
		if (blockTxs != null) {
			this.blocksDB.put(blockHash.getBytes(), serializeList(txHashes));
		}
	}

	@Override
	public synchronized void removeWithBlockHash(String blockHash) {
		List<String> blockTxs = this.getBlockTransactions(blockHash);
		if (blockTxs != null) {
			this.blocksDB.delete(blockHash.getBytes());
		}
	}

	@Override
	public List<String> getBlockTransactions(String blockHash) {
		System.out.println("get block transactions: " + blockHash);
		System.out.println("blocks db:" + String.valueOf(this.blocksDB));
		byte[] txHashesBytes = this.blocksDB.get(blockHash.getBytes());

		if (txHashesBytes != null) {
			return listFromBytes(txHashesBytes);
		}

		return null;
	}

	@Override
	public synchronized void removeAuthIDromTxID(String txID) {
		BTCAuthID authID = this.getAuthIDFromTxID(txID);
		if (authID != null) {
			this.idDB.delete(txID.getBytes());
			this.idDB.delete(authID.getName().getBytes());
		}
	}

	@Override
	public synchronized BTCAuthID getAuthIDFromName(String name) {
		byte[] bytes = this.idDB.get(name.getBytes());
		if (bytes == null)
			return null;
		return BTCAuthID.fromBytes(bytes);
	}

	@Override
	public synchronized BTCAuthID getAuthIDFromTxID(String txID) {
		byte[] bytes = this.idDB.get(txID.getBytes());
		if (bytes == null)
			return null;
		String name = new String(this.idDB.get(txID.getBytes()));
		return this.getAuthIDFromName(name);
	}

	/*
	 * TODO : figure this shit out
	 */
	@Override
	public synchronized Iterable<BTCAuthID> getAuthIDs() {
		return null;
		// return (Iterable<BTCAuthID>) this.db.iterator();
	}

	private synchronized void storeID(BTCAuthID id) {
		byte[] bytes = BTCAuthID.serialize(id);
		this.idDB.put(id.getName().getBytes(), bytes); // index name
		this.idDB.put(id.getTransactionId().getBytes(), id.getName().getBytes());
	}

	/*
	 * TODO: Put this in a utils class which would not just be used for lists
	 * but for all serializable objects
	 */
	private static byte[] serializeList(List<String> list) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] bytes = null;

		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(list);
			out.flush();
			bytes = bos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return bytes;
	}

	private static List<String> listFromBytes(byte[] bytes) {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		List<String> list = null;

		try {
			in = new ObjectInputStream(bis);
			list = (List<String>) in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return list;
	}

}
