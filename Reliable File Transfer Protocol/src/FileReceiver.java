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
		pkt.setLength(data.length);
		b.clear();
		sk.receive(pkt);
		b.rewind();
		b.getLong();
		int sn = b.getInt();
		b.rewind();
		System.out.println("sn received: " + sn);
		int snCorrect = 0;
		while (!isPktUncorrupt(pkt, b) || sn != snCorrect)
		{
			//send wrong ack ack-1
			DatagramPacket nack = Ack(-1, pkt.getSocketAddress());
			sk.send(nack);
			System.out.println("sent nak");
			b.clear();
			sk.receive(pkt);
			b.rewind();
			b.getLong();
			sn = b.getInt();
			System.out.println("sn received: " + sn);
		}
		b.rewind();
		b.getLong();
		sn = b.getInt();
		int nameLen = b.getInt();
		//send ack0
		DatagramPacket ack0 = Ack(sn, pkt.getSocketAddress());
		sk.send(ack0);
		System.out.println("sent ack " + sn);
		++snCorrect;

		//create file and shit
		System.out.println(nameLen);
		byte[] nameBytes = new byte[nameLen*2];
		b.get(nameBytes, 0, nameLen*2);
		System.out.println(Arrays.toString(nameBytes));
		String newFileName = new String (nameBytes, "UTF-16"); 
		File f = new File(newFileName);
		FileOutputStream fos = new FileOutputStream(f);
		DataOutputStream dos = new DataOutputStream (fos);	
		f.createNewFile();
		
		// leave pkt 0 alone!!
		
			int snFront = 1;
			int windowSize = 5;
			Vector<APkt> buffVector = new Vector<APkt>(windowSize);
			
		while(true)
		{
			// get pkts as usual -- actually should open threads to receive multiple pkts at a time... do it if got energy
			
			pkt.setLength(data.length);
			b.clear();
			sk.receive(pkt);
			System.out.println("received new packet");
			// Debug output
			//	System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			
			
			if (isPktUncorrupt(pkt, b))
			{
				b.rewind();
				b.getLong();
				sn = b.getInt();
				System.out.println("sn of the ack received " + sn);
				int actSize = b.getInt();
				if (sn < snFront && sn > snFront + windowSize) //	if(sn != snCorrect)
				{
					DatagramPacket ack = Ack(snFront-1, pkt.getSocketAddress());
					sk.send(ack);
					System.out.println("sent prev ack because wrong sn");
					System.out.println("ack sent " + snFront + "minus one");
					continue;
				}
				
				System.out.println("sn front: " + snFront);
				System.out.println("Pkt " + sn);
				
				//now the packet is confirm correct can start to process it
				//here put into buffer n then continue reading first
				//when buffer ready pass to file as much as possible if sn == snFront then while (sn != 0)	then change snfront
				
				//creating APkt
				APkt pktA = new APkt();
				b.rewind();
				b.getLong(); //dun need crc check anymore
				pktA.sn = b.getInt();
				pktA.size = b.getInt();
				b.get(pktA.data);
				
				//how to put into the correct location? -- use snfront to get the correct index
				int index = pktA.sn - snFront;
				buffVector.set(index, pktA);
				APkt pktB;
				if(pktA.sn == snFront)
				{
					pktB = pktA;
					while (pktB.sn != 0)
					{
						dos.write(pktB.data,0,pktB.size);
						buffVector.remove(0);
						buffVector.add(null);
					}
					snFront = pktB.sn;
				}

				//update ack
				DatagramPacket ack = Ack(pktA.sn, pkt.getSocketAddress());
				sk.send(ack);
				System.out.println("ack sent " + sn + "aka " + pktA.sn);
			}
			else
			{
				
		//need to find way to send acks--send ack of pckt received
				DatagramPacket ack = Ack(snFront-1, pkt.getSocketAddress());
				sk.send(ack);
				System.out.println("sent prev ack. ack " + (snFront-1));
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
		crc.update(pkt.getData(), 8,pkt.getLength()-8);
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