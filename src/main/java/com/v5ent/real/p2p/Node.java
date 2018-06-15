package com.v5ent.real.p2p;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.v5ent.entity.Block;

import spark.utils.StringUtils;

/**
 * 结点
 * 
 * @author Mignet
 *
 */
public class Node {
	private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

	/** 本地区块链 */
	private static List<Block> blockChain = new LinkedList<Block>();

	public static void main(String[] args) throws IOException, InterruptedException {
		int port = 8015;
		LOGGER.info("Starting peer network...  ");
		PeerNetwork peerNetwork = new PeerNetwork(port);
		peerNetwork.start();
		LOGGER.info("[  Node is Started in port:"+port+"  ]");

		LOGGER.info("Starting RPC daemon...  ");
		RpcServer rpcAgent = new RpcServer(port+1);
		rpcAgent.start();
		LOGGER.info("[  RPC agent is Started in port:"+(port+1)+"  ]");
		ArrayList<String> peers = new ArrayList<>();
		File peerFile = new File("peers.list");
		if (!peerFile.exists()) {
			String host = InetAddress.getLocalHost().toString();
			FileUtils.writeStringToFile(peerFile, host+":"+port);
		}
		for (Object peer : FileUtils.readLines(peerFile)) {
			String[] addr = peer.toString().split(":");
			peerNetwork.connect(addr[0], Integer.parseInt(addr[1]));
		}
		TimeUnit.SECONDS.sleep(2);

		peerNetwork.broadcast("VERSION");

		// hard code genesisBlock
		Block genesisBlock = new Block();
		genesisBlock.setIndex(0);
		genesisBlock.setTimestamp("2017-07-13 22:32:00");
		genesisBlock.setVac(0);
		genesisBlock.setPrevHash("");
		genesisBlock.setHash(BlockUtils.calculateHash(genesisBlock));
		blockChain.add(genesisBlock);

		final Gson gson = new GsonBuilder().create();
		LOGGER.info(gson.toJson(blockChain));
		int bestHeight = 0;
		boolean catchupMode = true;

		/**
		 * p2p 通讯
		 */
		while (true) {
			//对新连接过的peer写入文件，下次启动直接连接
			for (String peer : peerNetwork.peers) {
				if (!peers.contains(peer)) {
					peers.add(peer);
					FileUtils.writeStringToFile(peerFile, peer);
				}
			}
			peerNetwork.peers.clear();

			// 处理通讯
			for (PeerThread pt : peerNetwork.peerThreads) {
				if (pt == null || pt.peerReader == null) {
					break;
				}
				List<String> dataList = pt.peerReader.readData();
				if (dataList == null) {
					LOGGER.info("Null ret retry.");
					System.exit(-5);
					break;
				}

				for (String data:dataList) {
					LOGGER.info("Got data: " + data);
					int flag = data.indexOf(' ');
					String cmd = flag >= 0 ? data.substring(0, flag) : data;
					String payload = flag >= 0 ? data.substring(flag + 1) : "";
					if (StringUtils.isNotBlank(cmd)) {
						if ("VERACK".equalsIgnoreCase(cmd)) {
							// 获取区块高度
							String[] parts = payload.split(" ");
							bestHeight = Integer.parseInt(parts[0]);
							//哈希暂时不校验
						} else if ("VERSION".equalsIgnoreCase(cmd)) {
							// 对方发来握手信息，我方发给对方区块高度和最新区块的hash
							pt.peerWriter.write("VERACK " + blockChain.size() + " " + blockChain.get(blockChain.size() - 1).getHash());
						} else if ("BLOCK".equalsIgnoreCase(cmd)) {
							//把对方给的块存进链中
							LOGGER.info("Attempting to add block...");
							LOGGER.info("Block: " + payload);
							Block newBlock = gson.fromJson(payload, Block.class);
							if (!blockChain.contains(newBlock)) {
								// 校验区块，如果成功，将其写入本地区块链
								if (BlockUtils.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
									if (blockChain.add(newBlock) && !catchupMode) {
										LOGGER.info("Added block " + newBlock.getIndex() + " with hash: ["+ newBlock.getHash() + "]");
										peerNetwork.broadcast("BLOCK " + payload);
									}
								}
							}
						} else if ("GET_BLOCK".equalsIgnoreCase(cmd)) {
							//把对方请求的块给对方
							LOGGER.info("Sending block[" + payload + "] to peer");
							Block block = blockChain.get(Integer.parseInt(payload));
							if (block != null) {
								LOGGER.info("Sending block " + payload + " to peer");
								pt.peerWriter.write("BLOCK " + gson.toJson(block));
							}
						} else if ("ADDR".equalsIgnoreCase(cmd)) {
							// 对方发来地址，建立连接并保存
							if (!peers.contains(payload)) {
								String peerAddr = payload.substring(0, payload.indexOf(":"));
								int peerPort = Integer.parseInt(payload.substring(payload.indexOf(":") + 1));
								peerNetwork.connect(peerAddr, peerPort);
								peers.add(payload);
								PrintWriter out = new PrintWriter(peerFile);
								for (int k = 0; k < peers.size(); k++) {
									out.println(peers.get(k));
								}
								out.close();
							}
						} else if ("GET_ADDR".equalsIgnoreCase(cmd)) {
							//对方请求更多peer地址，随机给一个
							Random random = new Random();
							pt.peerWriter.write("ADDR " + peers.get(random.nextInt(peers.size())));
						} 
					}
				}
			}

			// ********************************
			// 		比较区块高度,同步区块
			// ********************************

			int localHeight = blockChain.size();

			if (bestHeight > localHeight) {
				catchupMode = true;
				LOGGER.info("Local chain height: " + localHeight);
				LOGGER.info("Best chain Height: " + bestHeight);
				TimeUnit.MILLISECONDS.sleep(300);
				
				for (int i = localHeight; i < bestHeight; i++) {
					LOGGER.info("请求块 " + i + "...");
					peerNetwork.broadcast("GET_BLOCK " + i);
				}
			} else {
				if (catchupMode) {
					LOGGER.info("[p2p] - Caught up with network.");
				}
				catchupMode = false;
			}

			/**
			 * 处理RPC服务
			 */
			for (RpcThread th:rpcAgent.rpcThreads) {
				String request = th.req;
				if (request != null) {
					String[] parts = request.split(" ");
					parts[0] = parts[0].toLowerCase();
					if ("getinfo".equals(parts[0])) {
						String res = gson.toJson(blockChain);
						th.res = res;
					} else if ("send".equals(parts[0])) {
						try {
							int vac = Integer.parseInt(parts[1]);
							// 根据vac创建区块
							Block newBlock = BlockUtils.generateBlock(blockChain.get(blockChain.size() - 1), vac);
							if (BlockUtils.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
								blockChain.add(newBlock);
								th.res = "write Success!";
								peerNetwork.broadcast("BLOCK " + gson.toJson(newBlock));
							} else {
								th.res = "RPC 500: Invalid vac Error\n";
							}
						} catch (Exception e) {
							th.res = "Syntax (no '<' or '>'): send <vac> <privateKey>";
							LOGGER.error("invalid vac", e);
						}
					} else {
						th.res = "Unknown command: \"" + parts[0] + "\"";
					}
				}
			}

			// ****************
			// 循环结束
			// ****************
			TimeUnit.MILLISECONDS.sleep(200);
		}
	}

}
