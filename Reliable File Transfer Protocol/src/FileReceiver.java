import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.zip.*;
import java.io.*;


public class FileReceiver {

	public static void main(String[] args)
	{
		try
		{
		if (args.length != 1) {
			System.err.println("Usage: FileReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		byte[] data = new byte[1000];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);

		//ok lets deal with pkt0 here
	//	pkt.setLength(data.length);
		b.clear();
		sk.receive(pkt);
		b.rewind();
		b.getLong();
		int sn = b.getInt();
		
		int snCorrect = 0;
		while (!isPktUncorrupt(pkt, b) || sn != snCorrect)
		{
			//send wrong ack ack-1
			DatagramPacket nack = Ack(-1, pkt.getSocketAddress());
			sk.send(nack);
			b.clear();
			sk.receive(pkt);
			b.rewind();
			b.getLong();
			sn = b.getInt();
		}
		b.rewind();
		b.getLong();
		sn = b.getInt();
		int nameLen = b.getInt();
		//send ack0
		DatagramPacket ack0 = Ack(sn, pkt.getSocketAddress());
		sk.send(ack0);
		++snCorrect;

		//create file and shit
		System.out.println(nameLen);
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

		while(true)
		{
			pkt.setLength(data.length);
			b.clear();
			sk.receive(pkt);
			System.out.println("received new packet");
			// Debug output
			//	System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));

			if (isPktUncorrupt(pkt, b))
			{
				sn = b.getInt();
				if(sn != snCorrect)
				{
					DatagramPacket ack = Ack(snCorrect-1, pkt.getSocketAddress());
					sk.send(ack);
					System.out.println("sent prev ack because wrong sn");
					continue;
				}
				System.out.println("sn correct: " + snCorrect);
				System.out.println("Pkt " + sn);
				int tempDataLen = pkt.getLength()-12;
				System.out.println("pkt len " + tempDataLen);
				byte[] tempData = new byte[tempDataLen];
				b.get(tempData);
				//save it to a file
				dos.write(tempData,0,tempDataLen);

				//update ack
				DatagramPacket ack = Ack(sn, pkt.getSocketAddress());
				sk.send(ack);
				++snCorrect;
			}
			else
			{
				DatagramPacket ack = Ack(snCorrect-1, pkt.getSocketAddress());
				sk.send(ack);
				System.out.println("sent prev ack");
			}

		}
		}
		catch (Exception e)
		{
			System.out.println("Exception encountered: "  + e);
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
	
	private static DatagramPacket Ack(int sn, SocketAddress add)
	{
		byte[] ackByte = new byte [12];
		ByteBuffer b = ByteBuffer.wrap(ackByte);
		CRC32 crc = new CRC32();
		b.putLong(0);
		b.putInt(sn);
		
		DatagramPacket ackPkt = new DatagramPacket(ackByte, 0, 12,
						add);
		crc.reset();
		crc.update(ackPkt.getData(), 8, ackPkt.getLength()-8);
		b.rewind();
		b.putLong(crc.getValue());
		return ackPkt;
	}
}