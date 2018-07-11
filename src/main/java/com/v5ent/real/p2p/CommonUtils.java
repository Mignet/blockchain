package com.v5ent.real.p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {

	public static String INTRANET_IP = getIntranetIp(); // 内网IP
	public static String INTERNET_IP = getInternetIp(); // 外网IP

	/**
	 * 判断ip地址是不是本机
	 * 
	 * @param host
	 * @return
	 */
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

	/**
	 * 获得内网IP
	 * 
	 * @return 内网IP
	 */
	private static String getIntranetIp() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获得外网IP
	 * 
	 * @return 外网IP
	 */
	public static String getInternetIp() {
		try {
			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			Enumeration<InetAddress> addrs;
			while (networks.hasMoreElements()) {
				addrs = networks.nextElement().getInetAddresses();
				while (addrs.hasMoreElements()) {
					ip = addrs.nextElement();
					if (ip != null && ip instanceof Inet4Address && ip.isSiteLocalAddress()
							&& !ip.getHostAddress().equals(INTRANET_IP)) {
						return ip.getHostAddress();
					}
				}
			}

			// 如果没有外网IP，就返回内网IP
			return INTRANET_IP;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getV4IP() {
		String ip = "";
		String chinaz = "http://2018.ip138.com/ic.asp";

		String inputLine = "";
		String read = "";
		try {
			URL url = new URL(chinaz);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			while ((read = in.readLine()) != null) {
				inputLine += read;
			}
//			System.out.println(inputLine);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Pattern p = Pattern.compile("\\[(.*?)\\]");
		Matcher m = p.matcher(inputLine);
		if (m.find()) {
			String ipstr = m.group(1);
//			System.out.println(ipstr);
			ip = ipstr;
		}
		return ip;
	}

	public static void main(String[] args) {
//		System.out.println(isLocal("10.16.3.77"));
		System.out.println(getV4IP());
//		System.out.println(getIntranetIp());
	}
}
