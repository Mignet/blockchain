package com.v5ent.nmcoin;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

public class Block {
	/**这个块在整个链中的高度*/
	private int index;
	/**显而易见就是块生成时的时间戳*/
	private String timestamp;
	/**是这个块通过 SHA256 算法生成的散列值*/
	private String hash;
	/**指向前一个块的 SHA256 散列值*/
	private String prevHash;
	/**默克尔根 */
	private String merkleRoot;
	/**挖块难度 */
	private int difficulty;
	private int nonce;
	
	/**每个块包含的交易 */
	public transient List<Transaction> transactions = new ArrayList<Transaction>(); 
	
	/**
	 * Calculate new hash based on blocks contents
	 * @param block 区块
	 * @return
	 */
	public static String calculateHash(Block block) {
		String record = block.getIndex() + block.getTimestamp() + block.getNonce() + block.getPrevHash()+block.getMerkleRoot();
		MessageDigest digest = DigestUtils.getSha256Digest();
		byte[] hash = digest.digest(StringUtils.getBytesUtf8(record));
		return Hex.encodeHexString(hash);
	}
	
	/**
	 * Increases nonce value until hash target is reached.
	 * @param difficulty
	 * @return hash
	 */
	public String mineBlock() {
		this.merkleRoot = CommonUtils.getMerkleRoot(transactions);
		String target = CommonUtils.getDificultyString(difficulty); //Create a string with difficulty * "0" 
		hash = calculateHash(this);
		while(!hash.substring( 0, difficulty).equals(target)) {
			nonce ++;
			hash = calculateHash(this);
		}
		return hash;
	}
	
	/**
	 * Add transactions to this block
	 * @param transaction
	 * @return
	 */
	public boolean addTransaction(Transaction transaction) {
		//process transaction and check if valid, unless block is genesis block then ignore.
		if(transaction == null){
			return false;		
		}
		if((!"0".equals(prevHash))) {
			if((transaction.processTransaction() != true)) {
				System.out.println("Transaction failed to process. Discarded.");
				return false;
			}
		}

		transactions.add(transaction);
		System.out.println("Transaction Successfully added to Block");
		return true;
	}
	
	/**
	 * 校验区块的合法性（有效性）
	 * 
	 * @param newBlock
	 * @param oldBlock
	 * @return
	 */
	public static boolean isBlockValid(Block newBlock, Block oldBlock) {
		if (oldBlock.getIndex() + 1 != newBlock.getIndex()) {
			return false;
		}
		if (!oldBlock.getHash().equals(newBlock.getPrevHash())) {
			return false;
		}
		if (!calculateHash(newBlock).equals(newBlock.getHash())) {
			return false;
		}
		return true;
	}
	
	/**
	 * 区块的生成
	 * 
	 * @param oldBlock
	 * @param transactions 
	 * @param vac
	 * @return
	 */
	public static Block generateBlock(Block oldBlock, int difficulty, List<Transaction> transactions) {
		Block newBlock = new Block();
		newBlock.transactions = transactions;
		newBlock.setIndex(oldBlock.getIndex() + 1);
		newBlock.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		newBlock.setDifficulty(difficulty);
		newBlock.setPrevHash(oldBlock.getHash());
		newBlock.setHash(newBlock.mineBlock());
		return newBlock;
	}
	
	/** getters and setters**/
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getPrevHash() {
		return prevHash;
	}
	public void setPrevHash(String prevHash) {
		this.prevHash = prevHash;
	}
	public int getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}
	public int getNonce() {
		return nonce;
	}
	public void setNonce(int nonce) {
		this.nonce = nonce;
	}

	/**
	 * @return the merkleRoot
	 */
	public String getMerkleRoot() {
		return merkleRoot;
	}

	/**
	 * @param merkleRoot the merkleRoot to set
	 */
	public void setMerkleRoot(String merkleRoot) {
		this.merkleRoot = merkleRoot;
	}
}
