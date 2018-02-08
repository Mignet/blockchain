package com.v5ent.utils;

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
/**
 * SHA256 digest for java and js
 * @author Mignet
 *
 */
public class SHA256 {
	public static String crypt(String src){
		MessageDigest digest = DigestUtils.getSha256Digest();
			byte[] hash = digest.digest(StringUtils.getBytesUtf8(src));
			return  Hex.encodeHexString(hash);
	}
	
	public static void main(String[] args) {
		String src = "123456";
		String s = SHA256.crypt(src);
		System.out.println(s);
	}
}
