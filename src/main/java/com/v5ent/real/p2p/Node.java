package com.v5ent.real.p2p;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
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

	/** 本地存储的区块链 */
	private static List<Block> blockChain = new LinkedList<Block>();
	private static final String VERSION = "0.1";

	public static void main(String[] args) throws IOException, InterruptedException {
		final Gson gson = new GsonBuilder().create();
		final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
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
			FileUtils.writeStringToFile(peerFile, host+":"+port,StandardCharsets.UTF_8,true);
		}else{
			for (String peer : FileUtils.readLines(peerFile,StandardCharsets.UTF_8)) {
				String[] addr = peer.split(":");
				if(CommonUtils.isLocal(addr[0])&&String.valueOf(port).equals(addr[1])){
					continue;
				}
				peers.add(peer);
				//raw ipv4
				peerNetwork.connect(addr[0], Integer.parseInt(addr[1]));
			}
		}

		File dataFile = new File("block.bin");
		if (!dataFile.exists()) {
			// hard code genesisBlock
			Block genesisBlock = new Block();
			genesisBlock.setIndex(0);
			genesisBlock.setTimestamp("2017-07-13 22:32:00");//my son's birthday
			genesisBlock.setVac(0);
			genesisBlock.setPrevHash("");
			genesisBlock.setHash(BlockUtils.calculateHash(genesisBlock));
			blockChain.add(genesisBlock);
			FileUtils.writeStringToFile(dataFile,gson.toJson(genesisBlock), StandardCharsets.UTF_8,true);
		}else{
			List<String> list = FileUtils.readLines(dataFile, StandardCharsets.UTF_8);
			for(String line:list){
				blockChain.add(gson.fromJson(line, Block.class));
			}
		}
		TimeUnit.SECONDS.sleep(2);
		//pretty print
		LOGGER.info(prettyGson.toJson(blockChain));

		int bestHeight = blockChain.size();
		//建立socket连接后，给大家广播握手
		peerNetwork.broadcast("VERSION "+ bestHeight+" " + VERSION);

		/**
		 * p2p 通讯
		 */
		while (true) {
			//对新连接过的peer写入文件，下次启动直接连接
			for (String peer : peerNetwork.peers) {
				if (!peers.contains(peer)) {
					peers.add(peer);
					LOGGER.info("add peer to file:"+peer);
					FileUtils.writeStringToFile(peerFile, "\r\n"+peer,StandardCharsets.UTF_8,true);
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
					LOGGER.info("Null return, retry.");
					System.exit(-5);
					break;
				}

				for (String data:dataList) {
					LOGGER.info("[p2p] COMMAND:: " + data);
					int flag = data.indexOf(' ');
					String cmd = flag >= 0 ? data.substring(0, flag) : data;
					String payload = flag >= 0 ? data.substring(flag + 1) : "";
					if (StringUtils.isNotBlank(cmd)) {
						if ("VERACK".equalsIgnoreCase(cmd)) {
							// 对方确认知道了,并给我区块高度
							String[] parts = payload.split(" ");
							bestHeight = Integer.parseInt(parts[0]);
							//哈希暂时不校验
						} else if ("VERSION".equalsIgnoreCase(cmd)) {
							// 对方发来握手信息
							// 获取区块高度和版本号信息
							String[] parts = payload.split(" ");
							bestHeight = Integer.parseInt(parts[0]);
							//我方回复：知道了
							pt.peerWriter.write("VERACK " + blockChain.size() + " " + blockChain.get(blockChain.size() - 1).getHash());
						} else if ("BLOCK".equalsIgnoreCase(cmd)) {
							//把对方给的块存进链中
							Block newBlock = gson.fromJson(payload, Block.class);
							if (!blockChain.contains(newBlock)) {
								LOGGER.info("Attempting to add Block: " + payload);
								// 校验区块，如果成功，将其写入本地区块链
								if (BlockUtils.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
									blockChain.add(newBlock);
									LOGGER.info("Added block " + newBlock.getIndex() + " with hash: ["+ newBlock.getHash() + "]");
									FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);
									peerNetwork.broadcast("BLOCK " + payload);
								}
							}
						} else if ("GET_BLOCK".equalsIgnoreCase(cmd)) {
							//把对方请求的块给对方
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
				LOGGER.info("Local chain height: " + localHeight+" Best chain Height: " + bestHeight);
				TimeUnit.MILLISECONDS.sleep(300);
				
				for (int i = localHeight; i < bestHeight; i++) {
					LOGGER.info("request get block[" + i + "]...");
					peerNetwork.broadcast("GET_BLOCK " + i);
				}
			}

			// ********************************
			// 处理RPC服务
			// ********************************
			for (RpcThread th:rpcAgent.rpcThreads) {
				String request = th.req;
				if (request != null) {
					String[] parts = request.split(" ");
					parts[0] = parts[0].toLowerCase();
					if ("getinfo".equals(parts[0])) {
						String res = prettyGson.toJson(blockChain);
						th.res = res;
					} else if ("send".equals(parts[0])) {
						try {
							int vac = Integer.parseInt(parts[1]);
							// 根据vac创建区块
							Block newBlock = BlockUtils.generateBlock(blockChain.get(blockChain.size() - 1), vac);
							if (BlockUtils.isBlockValid(newBlock, blockChain.get(blockChain.size() - 1))) {
								blockChain.add(newBlock);
								th.res = "Block write Success!";
								FileUtils.writeStringToFile(dataFile,"\r\n"+gson.toJson(newBlock), StandardCharsets.UTF_8,true);
								peerNetwork.broadcast("BLOCK " + gson.toJson(newBlock));
							} else {
								th.res = "RPC 500: Invalid vac Error";
							}
						} catch (Exception e) {
							th.res = "Syntax (no '<' or '>'): send <vac> - Virtual Asset Count(Integer)";
							LOGGER.error("invalid vac - Virtual Asset Count(Integer)");
						}
					} else {
						th.res = "Unknown command: \"" + parts[0] + "\" ";
					}
				}
			}

			// ****************
			// 循环结束
			// ****************
			TimeUnit.MILLISECONDS.sleep(100);
		}
	}

}
