import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.Charset;
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
		
		//ok lets deal with pkt0 here
		pkt.setLength(data.length);
		sk.receive(pkt);
	//	while (!isPktUncorrupt(pkt, b))
		{
			//send wrong ack
			
		}
		//create file and shit
		b.rewind();
		b.getLong();
		int sn = b.getInt();
		if (sn != 0)
		{
			System.out.println("error in detecting pkt 0");
		}
		int nameLen = b.getInt();
		byte[] nameBytes = new byte[nameLen*2];
		b.get(nameBytes, 0, nameLen*2);
		System.out.println(Arrays.toString(nameBytes));
		String newFileName = new String (nameBytes, "UTF-16"); 
		byte[] trial = "trial2.txt".getBytes(Charset.forName("UTF-16"));
		System.out.println(Arrays.toString(trial)+ " here");
		System.out.println(newFileName.compareTo("trial2.txt"));
		File f = new File(newFileName);
		FileOutputStream fos = new FileOutputStream(f);
		DataOutputStream dos = new DataOutputStream (fos);	
		f.createNewFile();
		//send ack
		byte[] ackByte0 = new byte[4];
		ByteBuffer ackB0 = ByteBuffer.wrap(ackByte0);
		ackB0.putInt(sn);
		DatagramPacket ack0 = new DatagramPacket(ackByte0, 0, 4,
				pkt.getSocketAddress());
		sk.send(ack0);
		
		
		while(true)
		{
			pkt.setLength(data.length);
			sk.receive(pkt);
			// Debug output
		//	System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (isPktUncorrupt(pkt, b))
			{

				sn = b.getInt();
				System.out.println("Pkt " + sn);
				int tempDataLen = 1000-12;
				byte[] tempData = new byte[tempDataLen];
				b.get(tempData);
				//save it to a file
				dos.write(tempData,0,tempDataLen);

				//update ack
				byte[] ackByte = new byte[4];
				ByteBuffer ackB = ByteBuffer.wrap(ackByte);
				ackB.putInt(sn);
				DatagramPacket ack = new DatagramPacket(ackByte, 0, 4,
						pkt.getSocketAddress());
				sk.send(ack);
				// store old ack (?)
			}
			else
				sk.send(ack0);
			
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
	public static boolean isPktUncorrupt (DatagramPacket pkt, ByteBuffer b) {
		boolean isOk = true;
		CRC32 crc = new CRC32();
		if (pkt.getLength() < 8)
		{
			System.out.println("Pkt too short");
			isOk = false;
		}
		b.rewind();
		long chksum = b.getLong();
		crc.reset();
		crc.update(pkt.getData(), 8, pkt.getLength()-8);
		// Debug output
	//	System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
		if (crc.getValue() != chksum)
		{
			System.out.println("Pkt corrupt");
			isOk = false;
		}
		return isOk;
	}
}