import java.io.IOException;
import java.util.TimerTask;

/**
 * 
 * @author msimiste
 * this class handles a timeout for its parent Router class
 */
 
public class TimeoutHandler extends TimerTask {

	
	private int[] linkCost;
	private Router router;
	
	/**
	 * 
	 * @param link
	 * 			the linkCost vector for the parent router
	 * @param r
	 * 			the Parent router calass
	 * 
	 */
	public TimeoutHandler(int[] link, Router r){		
		
		this.linkCost = link;
		this.router = r;		
	}
	
	public void run(){
	
		for(int i = 0; i<linkCost.length; i++){
			if ((linkCost[i]>0)&&(linkCost[i]<999))
			{
				try {
					router.notifyNeighbor(i);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
}
