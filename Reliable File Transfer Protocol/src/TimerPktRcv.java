import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

public class TimerPktRcv extends TimerTask {
	private DatagramSocket sk;	
	public DatagramPacket ackpkt;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] data = new byte[4]; 
		ackpkt = new DatagramPacket(data, data.length);;
		try {
			sk.receive(ackpkt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public TimerPktRcv(DatagramSocket sk)
	{
		this.sk = sk;
	}
}
