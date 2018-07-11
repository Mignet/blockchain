package com.v5ent.entity;
/**
 * 区块
 */
public class Block {
	/**是这个块在整个链中的位置*/
	private int index;
	/**显而易见就是块生成时的时间戳*/
	private String timestamp;
	/**虚拟资产额度。我们要记录的数据*/
	private int vac;
	/**是这个块通过 SHA256 算法生成的散列值*/
	private String hash;
	/**指向前一个块的 SHA256 散列值*/
	private String prevHash;
	
	private int difficulty;
	private String nonce;
	
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
	public int getVac() {
		return vac;
	}
	public void setVac(int vac) {
		this.vac = vac;
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
	public String getNonce() {
		return nonce;
	}
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + difficulty;
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + index;
		result = prime * result + ((nonce == null) ? 0 : nonce.hashCode());
		result = prime * result + ((prevHash == null) ? 0 : prevHash.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + vac;
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Block other = (Block) obj;
		if (difficulty != other.difficulty)
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (index != other.index)
			return false;
		if (nonce == null) {
			if (other.nonce != null)
				return false;
		} else if (!nonce.equals(other.nonce))
			return false;
		if (prevHash == null) {
			if (other.prevHash != null)
				return false;
		} else if (!prevHash.equals(other.prevHash))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (vac != other.vac)
			return false;
		return true;
	}
	
}
