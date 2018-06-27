package com.v5ent.real.p2p;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.google.gson.GsonBuilder;

public class CommonUtils {
	public static boolean isLocal(String host) {
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = allNetInterfaces.nextElement();

				// 去除回环接口\子接口\未运行接口
				if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
					continue;
				}

				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress ip = addresses.nextElement();
					if (ip != null) {
						if (ip.getHostAddress().equals(host))
							return true;
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Short hand helper to turn Object into a json string
	public static String toPrettyJson(Object o) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(o);
	}
}
