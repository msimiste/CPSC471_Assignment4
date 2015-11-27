
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
	private ArrayList<Neighbor> neighbors;
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

			for (int i = 0; i < neighbors.size(); i++) {
				if (neighbors.get(i).id == temp) {
					neighbors.set(i, new Neighbor(dvr.mincost, temp));
				}
			}
			updateMinCost();

		}

	}

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
		this.neighbors = new ArrayList<Neighbor>(len);
		initializeNeighbors(packet);
		//
		for (int i = 0; i < len; i++) {
			Arrays.fill(this.minCost[i], 999);
		}

		// initializeNeighbors();
		minCost[packet.destid] = Arrays.copyOf(linkCost, len);

		// this is a neighbor of itself, however the array value is not used
		// this.neighbors[this.routerId] = new Neighbor(Arrays.copyOf(linkCost,
		// len), this.routerId);

	}

	private void handleUpdate(DvrPacket dvr) {

		int len = dvr.getMinCost().length;
		// cancel timer,
		this.timer.cancel();
		
		// re init all data structures
		this.linkCost = dvr.getMinCost();
		this.nextHop = new int[len];
		initializeNextHop();
		this.neighbors = new ArrayList<Neighbor>(len);
		initializeNeighbors(dvr);
		
		
		this.minCost = new int[len][len];	
		for (int i = 0; i < len; i++) {
			Arrays.fill(this.minCost[i], 999);
		}
		
		minCost[dvr.destid] = Arrays.copyOf(linkCost, len);
		this.timer = new Timer();
		
		// restart timer;
		timer.scheduleAtFixedRate(new TimeoutHandler(this.minCost, this.linkCost, this), this.interval,
				this.interval);
		
		// re init all data structures
		

	}
	
	private void initializeNextHop(){
		
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

	private void updateMinCost() {

		for (int i = 0; i < this.linkCost.length; i++) {
			for (int j = 0; j < this.linkCost.length; j++) {
				if (this.routerId != j) {
					int cost = this.minCost[i][j];					
					for (Neighbor neighbor : neighbors) {
						
						//int Ncost = neighbor.distVector[j] + neighbor.distVector[i];
						int Ncost1 = minCost[i][neighbor.id] + minCost[neighbor.id][j];

						if (Ncost1 < cost) {
							
							if((neighbor.id==2)&&(i==0)&&(j==1))
							{
								int test =0;
								int test1 = test;
							}
							cost = Ncost1;							
							this.nextHop[j] = neighbor.id;	
							
						}
					
					}
					minCost[i][j] = cost;
				}

			}

		}
		this.minCostVector = this.minCost[this.routerId];
	}

	private void updateNextHop(int index, int node) {

		this.nextHop[index] = node;

	}

	private void initializeNeighbors(DvrPacket dvr) {
		// neighbors = new
		int[] temp = new int[this.linkCost.length];
		Arrays.fill(temp, 999);

		for (int i = 0; i < dvr.mincost.length; i++) {

			if ((dvr.mincost[i] > 0) && (dvr.mincost[i] < 999)) {
				this.neighbors.add(new Neighbor(temp, i));
			}
		}
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
