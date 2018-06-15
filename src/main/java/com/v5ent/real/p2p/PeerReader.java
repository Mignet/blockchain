package com.v5ent.real.p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 	InputThread只从对等结点读取数据。
 *	读取的所有数据都存储在ArrayList中，每一行单独存储。
 *	通过PeerNetwork通过一个通道访问数据。
 *	@author Mignet
 */
public class PeerReader extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(PeerReader.class);
	
    private Socket socket;

    /**缓冲区*/
    private ArrayList<String> receivedData = new ArrayList<>();

    /**
     * 传入套接字
     * @param socket
     */
    public PeerReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input;
            while ((input = in.readLine()) != null) {
                receivedData.add(input);
            }
        } catch (Exception e) {
        	LOGGER.error("Peer " + socket.getInetAddress() + " disconnected.",e);
        }
    }

    /**
     * 取出缓冲数据
     * @return List<String> Data pulled from receivedData
     */
    public List<String> readData() {
        ArrayList<String> inputBuffer = new ArrayList<>(receivedData);
        receivedData.clear(); //clear 'buffer'
        return inputBuffer;
    }
}