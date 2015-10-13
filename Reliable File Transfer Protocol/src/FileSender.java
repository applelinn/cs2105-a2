import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class FileSender {

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 3) {
			System.err.println("Usage: FileSender <host> <port> <file>");
			System.exit(-1);
		}

		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		DatagramSocket sk = new DatagramSocket();
		DatagramPacket pkt;
		CRC32 crc = new CRC32();
		
		String filename = args[2];
		//open file
		FileInputStream fis = new FileInputStream(filename);
		//stream file into stream
		DataInputStream dis = new DataInputStream(fis);
		int len = 1000+63+31;
		byte[] byteArray = new byte[len];
		ByteBuffer buffData = ByteBuffer.wrap(byteArray);
		// read 1000 bytes from file
		//if the file has ended then stop
		int offset = 63 + 31;
		
		int sn = 0;
		while(dis.read(byteArray, offset, len) != -1)
		{
			//but em in packets
			//put crc first
			crc.reset();
			crc.update(byteArray, 8, byteArray.length-8);
			long chksum = crc.getValue();
			buffData.putLong(chksum);
			//put in sequence num
			buffData.putInt(sn);
			//packet time
			pkt = new DatagramPacket(byteArray, byteArray.length, addr);
			System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(byteArray));
			//send off via socket
			sk.send(pkt);
			
			buffData.clear();
		}
		
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
