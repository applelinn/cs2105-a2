import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class FileSender{

	public static void main(String[] args) 
	{
		try
		{

			if (args.length != 4) {
				System.err.println("Usage: FileSender <host> <port> <file> <new file name>");
				System.exit(-1);
			}
			int port = Integer.parseInt(args[1]);
			InetSocketAddress addr = new InetSocketAddress(args[0], port);
			DatagramSocket sk = new DatagramSocket();
			sk.setSoTimeout(200);
			DatagramPacket pkt;
			DatagramPacket ackpkt;
			CRC32 crc = new CRC32();
			String filename = args[2];
			//open file
			FileInputStream fis = new FileInputStream(filename);
			//stream file into stream
			DataInputStream dis = new DataInputStream(fis);
			int byteArraySize = 1000;
			byte[] byteArray = new byte[byteArraySize];
			ByteBuffer buffData = ByteBuffer.wrap(byteArray);
			// read 1000 bytes from file
			//if the file has ended then stop
			int offset = 8 + 4 + 4 ;
			int len = byteArraySize - offset;
			int sn = 0;
			String newName = args[3];
			char[] nameChar = newName.toCharArray();

			//pkt 0 will be the name of the file to be sent (crc, sn, name len, name)
			buffData.putLong(0); //leave space for crc
			buffData.putInt(sn); //put in the sn
			buffData.putInt(nameChar.length); //put in the len of file name
			//put in file name
			for (int j = 0; j < nameChar.length; ++j)
			{
				buffData.putChar(nameChar[j]);
			}
			//crc time~
			crc.reset();
			crc.update(byteArray, 8, byteArray.length - 8);
			long chksum0 = crc.getValue();
			buffData.rewind();
			buffData.putLong(chksum0);//put in the crc
			//packet time pkt0
			pkt = new DatagramPacket(byteArray, byteArray.length, addr);
			// System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(byteArray));
			sk.send(pkt); //send off via socket
			boolean isWrong = true;
			//check for correct ack
			while (isWrong)
			{
				byte[] data = new byte[12];
				ackpkt = new DatagramPacket(data, data.length);
				ByteBuffer b = ByteBuffer.wrap(data);
				ackpkt.setLength(data.length);
				b.clear();
				//start waiting on thread
				//if too long quit
				//block wait for pkt fr thread
				try
				{
					System.out.println("now gonna start waiting for packets");
					sk.receive(ackpkt);
				}catch (SocketTimeoutException soe)
				{
					System.out.println("timeout! taking too long to receive ack");
					sk.send(pkt);
					System.out.println("pkt " + sn + "sent");
					continue;
				}
				System.out.println("ack " + sn + "received");
				
				//crc check
				long checksum = b.getLong();
				crc.reset();
				crc.update(data, 8, data.length-8);
				if(checksum != crc.getValue())
				{
					//ack pkt corrupt
					System.out.println("ack corrupt");
					sk.send(pkt);
					System.out.println("pkt " + sn + "sent");
					continue;
				}
				b.rewind();
				if (b.getInt() != sn)
				{
					System.out.println("Pkt wrong sn");
					sk.send(pkt);
					System.out.println("pkt " + sn + "sent");
					continue;
				}
				else
					isWrong = false;
			}
			++sn;
			
			//lets  leave pkt 0 out of this shit
			//put data in packets
			
			
			int maxLen = 5; // the amount of pkts to be kept in buffer to send out
			// put in maxLen pkts into a vector
			Vector<APkt> toSend;
			
			for( int i = 0; i <= maxLen; ++i)
			{
				//need to redeclare everything in the loop so that the buffers dont override...
				//put a Apkt in...
			}
			
			int actSize = dis.read(byteArray, offset, len);
			while( actSize != -1)
			{
				//send and wait for ack
				for(int i = 0; i <= maxLen ; ++i)
				{
					//send pkt
					//open thread to wait for pkt
				}
				
				//take sn number of ack from pkt n if corrupt status or time out status (lets call it bool ifAckReceived status
				// look for sn in the vector
				//delete the Apkt of the sn that acks have been received
				//put in more pkts till vector length == maxLen
				//if actSize becomes -1 break out
				
				// after loop send the remaining vector.length pkts till their acks are received (via threads that live for 200ms)
				
				
				buffData.rewind();
				buffData.putLong(0);
				System.out.println("act size " + actSize);
				//put in sequence num
				buffData.putInt(8, sn);
				buffData.putInt(12, actSize);
				//put crc
				crc.reset();
				crc.update(byteArray, 8, byteArray.length - 8);
				long chksum = crc.getValue();
				buffData.rewind();
				buffData.putLong(chksum);
				//packet time~
				pkt = new DatagramPacket(byteArray, byteArray.length , addr);
				//	System.out.println("Sent CRC:" + chksum + " Contents:" + bytesToHex(byteArray));
				//send off via socket
				sk.send(pkt);
				System.out.println("pkt " + sn + "sent");
				//wait for pkt ack to come

				isWrong = true;
				while (isWrong)
				{
					byte[] data = new byte[12];
					ackpkt = new DatagramPacket(data, data.length);
					ByteBuffer b = ByteBuffer.wrap(data);
					b.clear();
					try
					{
						System.out.println("now gonna start waiting for packets");
						sk.receive(ackpkt);
					}catch (SocketTimeoutException soe)
					{
						System.out.println("timeout! taking too long to receive ack");
						sk.send(pkt);
						System.out.println("pkt " + sn + "sent");
						continue;
					}
		//			sk.receive(ackpkt);
					System.out.println("ack " + sn + "received");
					b.getLong();
					int pktsn = b.getInt();
					if (pktsn != sn)
					{
						System.out.println("Pkt wrong sn:" + pktsn);
						sk.send(pkt);
						System.out.println("pkt " + sn + "sent");
					}
					else 
					{
						crc.reset();
						crc.update(ackpkt.getData(), 8, ackpkt.getLength()-8);
						b.rewind();
						long checkSum = crc.getValue();
						if (checkSum != b.getLong())
						{
							//check for corruption
							System.out.println("Pkt corrupt:" + pktsn);
							sk.send(pkt);
							System.out.println("pkt " + sn + "sent");
						}
						else
							isWrong = false;
					}
					
				}
				++sn;
				buffData.clear();
				actSize = dis.read(byteArray, offset, len);
			}
			dis.close();
			sk.close();
		}
		catch (Exception e)
		{
			System.out.println("Exception " + e);
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
