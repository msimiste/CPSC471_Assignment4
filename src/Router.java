
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

/**
 * Router Class
 * 
 * This class implements the functionality of a router when running the distance
 * vector routing algorithm.
 * 
 * The operation of the router is as follows: 1. send/receive HELLO message 2.
 * while (!QUIT) receive ROUTE messages update mincost/nexthop/etc 3. Cleanup
 * and return
 * 
 * A separate process broadcasts routing update messages to directly connected
 * neighbors at regular intervals.
 * 
 * 
 * @author Majid Ghaderi
 * @version 2.0, Oct 11, 2015
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
	private int[] linkCost;	
	private int[] nextHop;
	private int[][] minCost;
	private int[] minCostVector;
	private Timer timer;	

	/**
	 * Constructor to initialize the rouer instance
	 * 
	 * @param routerId
	 *            Unique ID of the router starting at 0
	 * @param serverName
	 *            Name of the host running the network server
	 * @param serverPort
	 *            TCP port number of the network server
	 * @param updateInterval
	 *            Time interval for sending routing updates to neighboring
	 *            routers (in milli-seconds)
	 */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		
		this.serverName = serverName;
		this.routerId = routerId;
		this.serverPort = serverPort;
		this.interval = updateInterval;		
	}

	/**
	 * starts the router
	 * 
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {

		try {
			// establish TCP connection
			tcpSocket = new Socket(serverName, serverPort);
			outStream = new ObjectOutputStream(tcpSocket.getOutputStream());
			inputStream = new ObjectInputStream(tcpSocket.getInputStream());

			// make first connection to the server
			initiateServerContact();

			// start the timer & set the timeout interval
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimeoutHandler(this.linkCost, this), this.interval,
					this.interval);
			
			
			DvrPacket packet = null;
			
			while (!QUIT) {
				packet = (DvrPacket) inputStream.readObject();
				processDvr(packet);
			}

			this.timer.cancel();
			this.outStream.flush();
			this.outStream.close();
			this.inputStream.close();
			this.tcpSocket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new RtnTable(this.minCostVector, this.nextHop);
	}

	/**
	 * 
	 * @param dvr
	 *    A DvrPacket containing an update from the network
	 */
	private void processDvr(DvrPacket dvr) {

		if (dvr.type == DvrPacket.QUIT) {
			this.QUIT = true;
			return;
		}

		if (dvr.sourceid == DvrPacket.SERVER) {// if dvr.sourceId == SERVER

			this.linkCost = dvr.mincost;
			this.minCostVector = this.linkCost;
			handleUpdate(dvr);

		}

		// update minCost vector
		else if (dvr.type == DvrPacket.ROUTE) {
			int temp = dvr.sourceid;
			this.minCost[temp] = dvr.mincost;		
			updateMinCost();

		}

	}

	/**
	 * Method for the first interation with the server
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void initiateServerContact() throws IOException, ClassNotFoundException {

		DvrPacket packet = null;

		// contact the server
		outStream.writeObject(new DvrPacket(this.routerId, DvrPacket.SERVER, DvrPacket.HELLO));
		// recieve hello info from server
		packet = (DvrPacket) inputStream.readObject();

		if (packet.type == DvrPacket.QUIT) {
			this.QUIT = true;
			return;
		}

		int len = packet.mincost.length;

		this.nextHop = new int[len];
		this.minCostVector = new int[len];

		this.linkCost = packet.getMinCost();
		initializeNextHop();
		this.minCost = new int[len][len];
		
		for (int i = 0; i < len; i++) {
			Arrays.fill(this.minCost[i], 999);
		}
		
		minCost[packet.destid] = Arrays.copyOf(linkCost, len);	

	}

	/**
	 * 
	 * @param dvr
	 * A DvrPacket containing an update from the Server
	 */
	private void handleUpdate(DvrPacket dvr) {

		int len = dvr.getMinCost().length;
		// cancel timer,
		this.timer.cancel();

		// re init all data structures
		this.linkCost = dvr.getMinCost();
		this.nextHop = new int[len];
		initializeNextHop();		

		this.minCost = new int[len][len];
		for (int i = 0; i < len; i++) {
			Arrays.fill(this.minCost[i], 999);
		}

		minCost[dvr.destid] = Arrays.copyOf(linkCost, len);
		this.timer = new Timer();

		// restart timer;
		timer.scheduleAtFixedRate(new TimeoutHandler(this.linkCost, this), this.interval, this.interval);		

	}

	/**
	 * Helper method which initializes the routers nextHop array
	 */
	private void initializeNextHop() {

		for (int i = 0; i < linkCost.length; i++) {
			if (linkCost[i] == 0) {
				this.nextHop[i] = this.routerId;

			} else if (linkCost[i] == DvrPacket.INFINITY) {
				this.nextHop[i] = -1;
			} else {
				this.nextHop[i] = i;
			}
		}
	}

	/**
	 * Helper method which updates the routers minCost vector
	 */
	private void updateMinCost() {

		for (int i = 0; i < this.linkCost.length; i++) {
			for (int j = 0; j < this.linkCost.length; j++) {
				for (int k = 0; k < this.linkCost.length; k++) {
					
					int Ncost1 = minCost[i][k] + minCost[k][j];	
					
					if (Ncost1 < this.minCost[i][j]) {
						minCost[i][j] = Ncost1;
						if((this.routerId == i)&&(this.linkCost[k]!=DvrPacket.INFINITY)){
							this.nextHop[j] = k;
						}
					}
				}	
			}
		}
		this.minCostVector = this.minCost[this.routerId];
	}



	/**
	 * 
	 * @param id
	 * @throws IOException
	 *    int the router id of the neighbor that we are trying to notify
	 */
	 
	public void notifyNeighbor(int id) throws IOException {		
		outStream.writeObject(new DvrPacket(this.routerId, id, DvrPacket.ROUTE, this.minCost[this.routerId]));
	}

	/**
	 * A simple test driver
	 * 
	 * 
	 */
	public static void main(String[] args) {
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000;
		int routerId = 0;

		if (args.length == 1) {
			routerId = Integer.parseInt(args[0]);
		} else if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		} else {
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