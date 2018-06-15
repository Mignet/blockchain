package com.v5ent.real.p2p;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import com.v5ent.entity.Block;
/**
 * 关于链处理的公共方法类
 * @author Mignet
 *
 */
public class BlockUtils {
	/**
	 * 计算区块的hash值
	 * 
	 * @param block
	 *            区块
	 * @return
	 */
	public static String calculateHash(Block block) {
		String record = (block.getIndex()) + block.getTimestamp() + (block.getVac()) + block.getPrevHash();
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
		newBlock.setHash(calculateHash(newBlock));
		return newBlock;
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
}
