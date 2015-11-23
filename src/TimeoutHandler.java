import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {

	private int[][] minCost;
	private int[] linkCost;
	private Router router;
	public TimeoutHandler(int[][] min, int[] link, Router r){
		
		this.minCost = min;
		this.linkCost = link;
		this.router = r;
		
	}
	
	public void run(){
	
		for(int i = 0; i<linkCost.length; i++){
			if ((linkCost[i]>0)&&(linkCost[i]<999))
			{
				
			}
		}
		
	}
}
