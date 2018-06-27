package com.v5ent.nmcoin;

import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * p2p通讯线程
 * 在接受套接字后，分成两个独立的线程，一个用于输入数据，一个用于输出数据，因此单向数据不会阻塞
 * @author Mignet
 */
public class PeerThread extends Thread
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PeerThread.class);
    private Socket socket;
    PeerReader peerReader;
    PeerWriter peerWriter;
    
    /**
     * 构造函数
     * @param socket Socket with peer
     */
    public PeerThread(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
    	LOGGER.info("Got connection from " + socket.getInetAddress() + ".");
        peerReader = new PeerReader(socket);
        peerReader.start();
        peerWriter = new PeerWriter(socket);
        peerWriter.start();
    }

    /**
     * 发送数据
     * @param data String of data to send
     */
    public void send(String data)
    {
        if (peerWriter == null)
        {
        	LOGGER.error("Couldn't send " + data + " when outputThread is null");
        }
        else
        {
            peerWriter.write(data);
        }
    }
}