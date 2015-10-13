import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.io.*;


public class FileReceiver {

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 1) {
			System.err.println("Usage: FileReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		byte[] data = new byte[1500];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);
		CRC32 crc = new CRC32();
		boolean isPkt0 = true;
		System.out.println("hey");
		while(true)
		{
			pkt.setLength(data.length);
			sk.receive(pkt);
			if (pkt.getLength() < 8)
			{
				System.out.println("Pkt too short");
				continue;
			}
			b.rewind();
			long chksum = b.getLong();
			crc.reset();
			crc.update(data, 8, pkt.getLength()-8);
			// Debug output
		//	System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (crc.getValue() != chksum)
			{
				System.out.println("Pkt corrupt");
				// if corrupt send the old ack
			}
			else
			{
				String newFileName = null;  
				FileOutputStream fos = null;
				DataOutputStream dos = null;
				
				if(isPkt0)
				{
					
					byte[] nameBytes = new byte[255*2];
					if (b.getInt() != 0)
					{
						System.out.println("error in detecting pkt 0");
						continue;
					}
					int nameLen = 0;
					nameLen=b.getInt();
					b.get(nameBytes, 0, 255*2);
					newFileName = new String (nameBytes, 0, nameLen*2); 
					System.out.println(newFileName);
					File f = new File("apple");
					f.createNewFile();
					fos = new FileOutputStream(f);
					dos = new DataOutputStream (fos);
					isPkt0 = false;
					continue;
				}
				
				System.out.println("Pkt " + b.getInt());
				int tempDataLen = 1000-12;
				byte[] tempData = new byte[tempDataLen];
				b.get(tempData);
				//save it to a file
				dos.write(tempData,0,tempDataLen);

				//update ack
				DatagramPacket ack = new DatagramPacket(new byte[0], 0, 0,
						pkt.getSocketAddress());
				sk.send(ack);
				// store old ack (?)
			}	
		}
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}