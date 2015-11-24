
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

	public class Neighbor {
		private int[] distVector;
		private int id;

		public Neighbor(int[] v, int i) {

			distVector = v;
			id = i;
		}

		public int[] getDist() {
			return distVector;
		}

		public int getID() {
			return id;
		}
	}

	private String serverName;
	private int routerId;
	private int serverPort;
	private int interval;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outStream;
	private Socket tcpSocket;
	private boolean QUIT = false;
	private RtnTable routingTable;
	private int[] linkCost;
	private Neighbor[] neighbors;
	private int[] nextHop;
	private int[][] minCost;
	private int[] minCostVector;
	private Timer timer;
	// private Queue<Neighbor> neighbors;

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
		// to be completed
		this.serverName = serverName;
		this.routerId = routerId;
		this.serverPort = serverPort;
		this.interval = updateInterval;
		this.routingTable = new RtnTable();
		// this.neighbors = new LinkedList<Neighbor>();
		// this.neighbors = new Neighbor[];
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
			timer.scheduleAtFixedRate(new TimeoutHandler(this.minCost, this.linkCost, this), this.interval,
					this.interval);

			// int test = 10;
			// test = inputStream.readByte();
			DvrPacket packet = null;
			while (!QUIT) {
				packet = (DvrPacket) inputStream.readObject();
				processDvr(packet);
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

	private void processDvr(DvrPacket dvr) {

		boolean updateOccured;

		if (dvr.sourceid == DvrPacket.SERVER) {// if dvr.sourceId == SERVER
												// update linkCost vector
			updateOccured = updateLinkCost(dvr);

			if (updateOccured) {
				// update minCost vector
				this.minCostVector = dvr.mincost;
			}
		}

		// update minCost vector
		else {
			int temp = dvr.sourceid;
			this.minCost[temp] = dvr.mincost;
			neighbors[temp] = new Neighbor(dvr.mincost, temp);

		}

	}

	private void initiateServerContact() throws IOException, ClassNotFoundException {

		DvrPacket packet = null;

		// rec
		outStream.writeObject(new DvrPacket(this.routerId, DvrPacket.SERVER, DvrPacket.HELLO));
		packet = (DvrPacket) inputStream.readObject();
		if (packet.type != 1) {
			this.QUIT = true;
			return;
		}
		int len = packet.mincost.length;
		this.neighbors = new Neighbor[len];
		this.linkCost = packet.getMinCost();
		this.minCost = new int[len][len];
		for (int i = 0; i < len; i++) {
			Arrays.fill(this.minCost[i], 999);
		}

		minCost[packet.destid] = Arrays.copyOf(linkCost, len);
		this.neighbors[this.routerId] = new Neighbor(Arrays.copyOf(linkCost, len), this.routerId);

	}

	private boolean updateLinkCost(DvrPacket dvr) {
		boolean updateOccured = false;
		for (int i = 0; i < dvr.mincost.length; i++) {
			if (this.linkCost[i] < dvr.mincost[i]) {
				updateOccured = true;
				linkCost[i] = dvr.mincost[i];
			}
		}

		// updateNeighbors();

		return updateOccured;

	}

	private void updateMinCost(DvrPacket dvr) {

		for (int i = 0; i < dvr.mincost.length; i++) 
		{
			if (this.routerId == i)
			{
				break;
			}
			for (int j = 0; j < dvr.mincost.length; j++) {
				for (Neighbor neighbor : neighbors) 
				{
					int cost = this.linkCost[i] + neighbor.distVector[j];

					if (this.minCost[i][j] > cost) 
					{
						this.minCost[i][j] = cost;
						updateNextHop(i, neighbor.id);
					}
				}
			}
		}
	}

	private void updateNextHop(int index, int node) {

		this.nextHop[index] = node;

	}

	public void notifyNeighbor(int id) throws IOException {

		// DvrPacket pack = new DvrPacket(this.routerId, id, DvrPacket.ROUTE,
		// this.minCost[id]);
		outStream.writeObject(new DvrPacket(this.routerId, id, DvrPacket.ROUTE, this.minCost[this.routerId]));

	}
	/*
	 * private void updateNeighbors(){ for(int i =0; i<this.linkCost.length;
	 * i++){ if ((linkCost[i]!=0)&&(linkCost[i]!=999)) { neighbors.add(new
	 * Neighbor(this.minCost[i], i)); } } }
	 */

	/**
	 * A simple test driver
	 * 
	 * 
	 */
	public static void main(String[] args) {
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 10000;
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
