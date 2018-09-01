package desu.webSocketServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Server implements Runnable {
	public static final int PORT = 8082;
	
	private ServerSocket server;
	private Thread worker;
	
	public Server() throws IOException {
		server = new ServerSocket(PORT);
		worker = new Thread(this);
		
		worker.start();
		
	}
	
	
	private void process(Socket socket) throws IOException {
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
		PrintWriter pw = new PrintWriter(dos);
		
		String wsKey = "";
		String line;
		while(!(line = reader.readLine()).isEmpty()) {
			
			if(line.contains("Sec-WebSocket-Key: "))
				wsKey = line.substring("Sec-WebSocket-Key: ".length());
		}
		if(wsKey == "") {
			pw.close();
			reader.close();
			socket.close();
			return;
		}
		
		pw.println("HTTP/1.1 101 Switching Protocols");
		pw.println("Upgrade: websocket");
		pw.println("Connection: Upgrade");
		pw.println("Sec-WebSocket-Accept: " + calcKey(wsKey));
		pw.println("Sec-WebSocket-Protocol: chat");
		pw.println("");
		pw.flush();
		
		
		byte[] ddt = new byte[1000000];
		
		while(true) {
			if(!readPacket(dis))
				break;
			//sendPacket(dos, System.currentTimeMillis() + " ms");
			sendPacket(dos, ddt);
		}
		
		pw.close();
		reader.close();
		socket.close();
	}
	
	private boolean readPacket(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		int b;
		
		b = is.read();
		int opcode = ((b & 0xF));
		System.out.println("Opcode: " + opcode);
		b = is.read();
		boolean hasMask = ((b & 0x80) >> 7) == 1;
		int length = ((b & 0x7F));
		if(length == 126)
			length = dis.readUnsignedShort();
		if(length == 127)
			length = (int)dis.readLong();
		System.out.println("Payload length: " + length);
		int[] mask = new int[0];
		if(hasMask)
			mask = new int[]{is.read(),is.read(),is.read(),is.read()};
		
		byte[] utf8Data = new byte[length];
		is.read(utf8Data);
		
		if(hasMask)
			for(int i = 0;i < length;i++) 
				utf8Data[i] ^= mask[i % 4];
		
		//System.out.println(Arrays.toString(utf8Data));
		
		//System.out.println(new String(utf8Data));
		
		return opcode != 8;
	}
	
	private void sendPacket(DataOutputStream os, String data) throws IOException {
		os.writeByte(0b10000001);
		if(data.length() > 125) {
			os.writeByte(126);
			os.writeShort(data.length());
				
		}
		os.writeByte(data.length());
		
		os.write(data.getBytes());
		os.flush();
		
	}
	private void sendPacket(DataOutputStream os, byte[] data) throws IOException {
		os.writeByte(0b10000010);
		if(data.length > 65536) {
			os.writeByte(127);
			os.writeLong(data.length);
			
		}
		else if(data.length > 125) {
			os.writeByte(126);
			os.writeShort(data.length);
				
		}
		else
			os.writeByte(data.length);
		
		os.write(data);
		os.flush();
		
	}
	


	public void run() {
		Socket socket;
		
		while(true) {
			try {
				socket = server.accept();
				process(socket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	private String calcKey(String key) {
		final String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		try {
			MessageDigest mDigest = MessageDigest.getInstance("SHA1");
			
			return Base64.getEncoder().encodeToString(
					mDigest.digest((key+magic).getBytes())
			);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
