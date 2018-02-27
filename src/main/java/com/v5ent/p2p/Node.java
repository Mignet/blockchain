package com.v5ent.p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.v5ent.domain.Block;

/**
 * 区块结点
 * @author Mignet
 *
 */
public class Node {
	private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

	/** 区块链 */
	private static List<Block> blockChain = new LinkedList<Block>();
	// 假如其他结点并发发送给本结点，用队列做接收处理
	private Queue<Block> bcServer;

	public static void main(String[] args) {
		// 区块链的核心部分
		// 维护一个在启动时可以连接的对等节点列表。当一个完整的节点第一次启动时，它必须被自举(bootstrapped)到网络。
		// 自举过程完成后，节点向其对等节点发送一个包含其自身IP地址的addr消息。其对等的每个节点向它们自己的对等节点转发这个信息，以便进一步扩大连接池。
		// 块广播
		// 在与对等节点建立连接后，双方互发包含最新块哈希值的getblocks消息。
		// 如果某个节点坚信其拥有最新的块信息或者有更长的链，它将发送一个inv消息，其中包含至多500个最新块的哈希值，以此来表明它的链更长。
		// 收到的节点使用getdata来请求块的详细信息，而远程的节点通过命令block来发送这些信息。
		// 在500个块的信息被处理完之后，节点可以通过getblocks请求更多的块信息。这些块在被接收节点认证之后得到确认。
		// 新块的确认也可通过矿工挖到并发布的块来发现。其扩散过程和上述类似。
		// 通过之前的连接，新块以inv消息发布出去，而接收节点可以通过getdata请求这些块的详细信息。
		boolean isRunning = true;
		ServerSocket serverSocket = null;
		try {
			// 发现对等网络(从DNS服务器中获取一个可连接的IP地址列表)，获取对等网络中其他结点的区块链(最长的那个)。
			// 如果没有，则生成创世块。
			// 如果有，则下载区块链到本结点。
			Block genesisBlock = new Block();
			genesisBlock.setIndex(0);
			genesisBlock.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			genesisBlock.setVac(0);
			genesisBlock.setPrevHash("");
			genesisBlock.setHash(Common.calculateHash(genesisBlock));
			blockChain.add(genesisBlock);

			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			LOGGER.info(gson.toJson(blockChain));

			// 建立TCP监听新块的产生(块的发现)
			serverSocket = new ServerSocket(9000);

			LOGGER.info("*** Node is started,waiting for others ***");
			// 监听对等网络中的结点
			while (isRunning) {
				final Socket socket = serverSocket.accept();
				// 创建一个新的线程 ,和建立连接的结点通讯
				new NodeThread(socket, blockChain).start();

				// 模拟网络结点广播
				new BroadcastThread(socket, blockChain).start();
			}
		} catch (IOException e) {
			LOGGER.error("socket create error:", e);
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				LOGGER.error("socket close error:", e);
			}
			LOGGER.info("bye bye!");
		}
	}

}
