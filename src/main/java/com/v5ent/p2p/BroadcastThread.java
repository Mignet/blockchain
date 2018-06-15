package com.v5ent.p2p;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.v5ent.entity.Block;

/**
 * 广播线程
 * 
 * @author Mignet
 *
 */
public class BroadcastThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastThread.class);
	final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private Socket socket = null;
	private List<Block> blockChain;

	public BroadcastThread(Socket socket, List<Block> blockChain) {
		this.socket = socket;
		this.blockChain = blockChain;
	}

	@Override
	public void run() {
		for (;;) {
			PrintWriter pw = null;
			try {
				Thread.sleep(30000);
				LOGGER.info("\n------------broadcast-------------\n");
				LOGGER.info(gson.toJson(blockChain));
				pw = new PrintWriter(socket.getOutputStream());
				// 发送到其他结点
				pw.write("------------broadcast-------------\n");
				pw.write(gson.toJson(blockChain));
				pw.flush();
			} catch (InterruptedException e) {
				LOGGER.error("error:", e);
			} catch (IOException e) {
				LOGGER.error("error:", e);
			} 
		}
	}
}
