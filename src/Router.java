
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;


/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 * A separate process broadcasts routing update messages
 * to directly connected neighbors at regular intervals.
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	2.0, Oct 11, 2015
 *
 */
public class Router {
	
	
	private String serverName;
	private int routerId;
	private int serverPort;
	private int interval;	
	private ObjectInputStream inputStream;
	private ObjectOutputStream outStream;
	private Socket tcpSocket;
	private boolean QUIT = false;
	private RtnTable routingTable;
    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		// to be completed
		this.serverName = serverName;
		this.routerId = routerId;
		this.serverPort = serverPort;
		this.interval = updateInterval;
		this.routingTable = new RtnTable();
	}
	

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     */
	public RtnTable start() {


		try {
			tcpSocket = new Socket(serverName, serverPort);
			outStream = new ObjectOutputStream(tcpSocket.getOutputStream());
			inputStream = new ObjectInputStream(tcpSocket.getInputStream());
			initiateServerContact();
			//int test = 10;
			//test = inputStream.readByte();
			DvrPacket packet = null;
			while(!QUIT){
				 packet = (DvrPacket)inputStream.readObject();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new RtnTable();
	}
	
	private void processDvr(DvrPacket dvr){
		
	}
	
	private void initiateServerContact() throws IOException, ClassNotFoundException{
		DvrPacket packet = null;
		outStream.writeObject(new DvrPacket(this.routerId, DvrPacket.SERVER, DvrPacket.HELLO));
		packet = (DvrPacket)inputStream.readObject();
	}

	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000;
		int routerId = 0;
		
		
		if (args.length == 1) {
			routerId = Integer.parseInt(args[0]);
		}
		else if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		}
		else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
		
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
