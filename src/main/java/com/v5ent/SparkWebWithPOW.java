package com.v5ent;

import static spark.Spark.get;
import static spark.Spark.post;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.v5ent.entity.Block;
import com.v5ent.entity.Message;

/**
 * 工作量证明算法 Proof-of-work
 * 
 * 创建新块并加入到链上之前需要完成“工作量证明”过程。我们先写一个简单的函数来检查给定的哈希值是否满足要求。 哈希值必须具有给定位的“前导0”
 * “前导0”的位数是由难度（difficulty）决定的 可以动态调整难度（difficulty）来确保 Proof-of-Work 更难解
 * 
 * @author Mignet
 *
 */
public class SparkWebWithPOW {
	private static final Logger LOGGER = LoggerFactory.getLogger(SparkWebWithPOW.class);
	private static List<Block> blockChain = new LinkedList<>();
	private static int difficulty = 1;

	/**
	 * 计算区块的hash值
	 * 
	 * @param block
	 *            区块
	 * @return
	 */
	public static String calculateHash(Block block) {
		String record = block.getIndex() + block.getTimestamp() + (block.getVac()) + block.getPrevHash()+block.getNonce();
		MessageDigest digest = DigestUtils.getSha256Digest();
		byte[] hash = digest.digest(StringUtils.getBytesUtf8(record));
		return Hex.encodeHexString(hash);
	}

	/**
	 * 区块的生成
	 * 
	 * @param oldBlock
	 * @param vac
	 * @return
	 */
	public static Block generateBlock(Block oldBlock, int vac) {
		Block newBlock = new Block();

		newBlock.setIndex(oldBlock.getIndex() + 1);
		newBlock.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		newBlock.setVac(vac);
		newBlock.setPrevHash(oldBlock.getHash());
		newBlock.setDifficulty(difficulty);

		/*
		 * 这里的 for 循环很重要： 获得 i 的十六进制表示 ，将 Nonce 设置为这个值，并传入 calculateHash 计算哈希值。
		 * 之后通过上面的 isHashValid 函数判断是否满足难度要求，如果不满足就重复尝试。 这个计算过程会一直持续，
		 * 直到求得了满足要求的 Nonce 值，之后将新块加入到链上。
		 */
		for (int i = 0;; i++) {
			String hex = String.format("%x", i);
			newBlock.setNonce(hex);
			if (!isHashValid(calculateHash(newBlock), newBlock.getDifficulty())) {
				LOGGER.info("{} need do more work!", calculateHash(newBlock));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error("error:", e);
					Thread.currentThread().interrupt();
				}
			} else {
				LOGGER.info("{} work done!", calculateHash(newBlock));
				newBlock.setHash(calculateHash(newBlock));
				break;
			}
		}
		return newBlock;
	}

	private static String repeat(String str, int repeat) {
		final StringBuilder buf = new StringBuilder();
		for (int i = 0; i < repeat; i++) {
			buf.append(str);
		}
		return buf.toString();
	}

	/**
	 * 校验HASH的合法性
	 * 
	 * @param hash
	 * @param difficulty
	 * @return
	 */
	public static boolean isHashValid(String hash, int difficulty) {
		String prefix = repeat("0", difficulty);
		return hash.startsWith(prefix);
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
	 * 如果有别的链比你长，就用比你长的链作为区块链
	 * 
	 * @param oldBlocks
	 * @param newBlocks
	 * @return 结果链
	 */
	public List<Block> replaceChain(List<Block> oldBlocks,List<Block> newBlocks) {
		if (newBlocks.size() > oldBlocks.size()) {
			return newBlocks;
		}else{
			return oldBlocks;
		}
	}

	public static void main(String[] args) {
		// 创世块
		Block genesisBlock = new Block();
		genesisBlock.setIndex(0);
		genesisBlock.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		genesisBlock.setVac(0);
		genesisBlock.setPrevHash("");
		genesisBlock.setHash(calculateHash(genesisBlock));
		genesisBlock.setDifficulty(difficulty);
		genesisBlock.setNonce("");
		blockChain.add(genesisBlock);

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		/**
		 * get /
		 */
		get("/", (request, response) ->gson.toJson(blockChain));

		/***
		 * post / {"vac":75} 
		 * curl -X POST -i http://localhost:4567/ --data {"vac":75}
		 */
		post("/", (request, response) ->{
				String body = request.body();
				Message m = gson.fromJson(body, Message.class);
				if (m == null) {
					return "vac is NULL";
				}
				int vac = m.getVac();
				Block lastBlock = blockChain.get(blockChain.size() - 1);
				Block newBlock = generateBlock(lastBlock, vac);
				if (isBlockValid(newBlock, lastBlock)) {
					blockChain.add(newBlock);
					LOGGER.info(gson.toJson(blockChain));
				} else {
					return "HTTP 500: Invalid Block Error";
				}
				return "success!";
		});

		LOGGER.info(gson.toJson(blockChain));
	}
}
