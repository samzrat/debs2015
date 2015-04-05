package samzrat.debs2015;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ApplicationMain implements Runnable {

	public static final int RING_BUFFEER_SIZE = 3600;
	public static final int PILLAR_MAX_SIZE = 500;
	public static final int ROUTE_COUNT_ARRAY_DIMENSION = 300;
	public static final int CELL_PROFIT_ARRAY_DIMENSION = 600;
	public static final int CELL_PROFIT_ARRAY_ROUTE_COUNT_ARRAY_SIZE = 100;
	
	private static Map<String, String> ll2xyConfigMap = new HashMap<String, String>();
	private static Logger LOG = Logger.getLogger(ApplicationMain.class);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss");
	//static int[][][][] routeCountArray = new int[ROUTE_COUNT_ARRAY_DIMENSION][ROUTE_COUNT_ARRAY_DIMENSION][ROUTE_COUNT_ARRAY_DIMENSION][ROUTE_COUNT_ARRAY_DIMENSION];
	static HashMap<String, Integer> routeCountHashMap = new HashMap<>();
	static CellProfitInfo[][] cellProfitArray = new CellProfitInfo[CELL_PROFIT_ARRAY_DIMENSION][CELL_PROFIT_ARRAY_DIMENSION];
	static TripEvent[][] ringBuffer = new TripEvent[RING_BUFFEER_SIZE][PILLAR_MAX_SIZE];
	static int[] pillarRoof = new int[RING_BUFFEER_SIZE];
	
	//INTER THREAD COMMUNICATOR
	private static volatile int combinedHead = 0;

	
	//----------writer thread internals------------------------------- 
	private static int ringBufferHead = 0;
	private static int pillarHead = 0;
	private static Date headTime = null;
    
    //----------------------------------------------------------------
	
	//----------writer thread internals-------------------------------
	//private static int routeQuery_pillar_head = 0;
	private static int routeQuery_PillarHead = 0;
	private static int routeQuery_RingBufferHead = 0;
	//----------------------------------------------------------------
	
	
	public static void main(String[] args) throws Exception {
		
		populateConfigs();
		BasicConfigurator.configure();
		

		int pillar1 = 934;
		int head1 = 356;
		int f = pillar1*100000 + head1;
		
		LOG.info("pillar1 = " + f/100000);
		LOG.info("head1 =" + f%100000);
		
		LOG.info("Starting initialization");
		
/*		for(int i=0; i<ROUTE_COUNT_ARRAY_DIMENSION; i++)
			for(int j=0; j<ROUTE_COUNT_ARRAY_DIMENSION; j++)
				for(int k=0; k<ROUTE_COUNT_ARRAY_DIMENSION; k++)
					for(int l=0; l<ROUTE_COUNT_ARRAY_DIMENSION; l++)
						routeCountArray[i][j][k][l] = 0;
*/		

/*		for(int i=0; i<CELL_PROFIT_ARRAY_DIMENSION; i++)
			for(int j=0; j<CELL_PROFIT_ARRAY_DIMENSION; j++) {
				cellProfitArray[i][j] = new CellProfitInfo();
				for(int k=0; k<CELL_PROFIT_ARRAY_ROUTE_COUNT_ARRAY_SIZE; k++)
					cellProfitArray[i][j].routeCountArray[k] = new RouteProfit();
			}
*/
		
		for(int i=0; i<RING_BUFFEER_SIZE; i++)
			for(int j=0; j<PILLAR_MAX_SIZE; j++)
				ringBuffer[i][j] = new TripEvent();
		
		Arrays.fill(pillarRoof, -1);
		
		
		//Creating the query threads
		ApplicationMain mt = new ApplicationMain();
		Thread popularRoutesQueryThread = new Thread(mt);
		Thread profitableCellsQueryThread = new Thread(mt);
		popularRoutesQueryThread.setName("PopularRoutesQueryThread");
		profitableCellsQueryThread.setName("ProfitableCellsQueryThread");
		popularRoutesQueryThread. start();
		profitableCellsQueryThread. start();
		
	
		LOG.info("Starting file reading");
		LineIterator it = FileUtils.lineIterator(new File(ll2xyConfigMap.get("grid.filename").toString()), "UTF-8");
		Integer lineCount = 0;
	      while (it.hasNext()) {
	    	  try {
	            ++lineCount;
	            extractData(it);
	            
	            
	            //LOG.info(lineCount);
	    	  }
	    	  catch (NumberFormatException e) {
	    		  LOG.info("NumberFormatException encountered");
	    		  continue;
	          }
	    	  catch (ParseException e) {
	        	  LOG.info("ParseException encountered");
	              e.printStackTrace();
	              continue;
	          }
	      }
	      LOG.info("Done!!!");
	}
	
	public void run()
	{
	  
		
	  if(Thread.currentThread().getName().equals("PopularRoutesQueryThread"))
		try {
			executePopularRoutesQuery();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	else if(Thread.currentThread().getName().equals("ProfitableCellsQueryThread"))
		  executeProfitableCellsQuery();
    }
	
	private void executePopularRoutesQuery() throws Exception {
		LOG.info("Thread started: " + Thread.currentThread().getName());
		
		int combinedHeadCopy;
		int writerPillarHead;
		int writerRingBufferHead;

		
		String key = "";
		while(true) {
			combinedHeadCopy = combinedHead;
			writerPillarHead = combinedHeadCopy/100000;
			writerRingBufferHead = combinedHeadCopy%100000;
			
	        if(writerRingBufferHead > routeQuery_RingBufferHead) {
	        	for(int i=routeQuery_PillarHead; i<=pillarRoof[routeQuery_RingBufferHead]; i++) {
	        		key = Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500Y) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			//LOG.info("1, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	for(int i=routeQuery_RingBufferHead + 1; i<writerRingBufferHead; i++) {
	        		if(pillarRoof[i] != -1) {
	        			for(int j=0; j<=pillarRoof[i]; j++) {
	        				
	        				key = Integer.toString(ringBuffer[i][j].beginCell500X) + Integer.toString(ringBuffer[i][j].beginCell500Y) + Integer.toString(ringBuffer[i][j].endCell500X) + Integer.toString(ringBuffer[i][j].endCell500Y);
	        				if(key.equals("0000"))
	        					//LOG.info("2, " + key);
	        				addToRouteCountHashMap(key);
	        				
	        				if(j==0) {
	        					int tail = i-30*60;
	        					if(tail >=0) {
	        						if(pillarRoof[tail] != -1) {
	        							for(int k=0; k<=pillarRoof[tail]; k++) {
	        								key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
	        								if(key.equals("0000"))
	        									//LOG.info("3, " + key);
	        								subtractFromRouteCountHashMap(key);
	        							}
	        						}
	        					}
	        					else {
	        						if(pillarRoof[RING_BUFFEER_SIZE + (tail)] != -1) {
	        							for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
	        								key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
	        								if(key.equals("0000"))
	        									//LOG.info("4, " + key);
	        								subtractFromRouteCountHashMap(key);
	        							}
	        						}
	        					}
	        				}
	        			}
	        		}
	        		else {
	        			int tail = i-30*60;
	        			if(tail >=0) {
    						if(pillarRoof[tail] != -1) {
    							for(int k=0; k<=pillarRoof[tail]; k++) {
    								key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
    								if(key.equals("0000"))
    									//LOG.info("5, " + key);
    								subtractFromRouteCountHashMap(key);
    							}
    						}
    					}
    					else {
    						if(pillarRoof[RING_BUFFEER_SIZE + tail] != -1) {
    							for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
    								key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
    								if(key.equals("0000"))
    									//LOG.info("6, " + key);
    								subtractFromRouteCountHashMap(key);
    							}
    						}
    					}
	        		}
	        	}
	        	for(int i=0; i<writerPillarHead; i++) {
	        		key = Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500Y)  + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			//LOG.info("7, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	routeQuery_PillarHead = writerPillarHead;
	        	routeQuery_RingBufferHead = writerRingBufferHead;
	        	//LOG.info("1 ASSIGN routeQuery_pillar_head = " + routeQuery_pillar_head%100000);
	        	
	        }
	        else if(writerRingBufferHead == routeQuery_RingBufferHead) {
	        	for(int i=routeQuery_PillarHead; i<writerPillarHead; i++) {
	        		key = Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500Y) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			//LOG.info("8, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	//LOG.info("2 ASSIGN routeQuery_pillar_head = " + routeQuery_pillar_head%100000);
	        	routeQuery_PillarHead = writerPillarHead;
	        }
	        else {
	        	for(int i=routeQuery_PillarHead; i<=pillarRoof[routeQuery_RingBufferHead]; i++) {
	        		key = Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500Y) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			//LOG.info("1, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	for(int i=routeQuery_RingBufferHead+1; i<RING_BUFFEER_SIZE-1; i++) {
	        		if(pillarRoof[i] != -1) {
	        			for(int j=0; j<=pillarRoof[i]; j++) {
	        				
	        				key = Integer.toString(ringBuffer[i][j].beginCell500X) + Integer.toString(ringBuffer[i][j].beginCell500Y) + Integer.toString(ringBuffer[i][j].endCell500X) + Integer.toString(ringBuffer[i][j].endCell500Y);
	        				if(key.equals("0000"))
	        					//LOG.info("2, " + key);
	        				addToRouteCountHashMap(key);
	        				
	        				if(j==0) {
	        					int tail = i-30*60;
	        					if(tail >=0) {
	        						if(pillarRoof[tail] != -1) {
	        							for(int k=0; k<=pillarRoof[tail]; k++) {
	        								key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
	        								if(key.equals("0000"))
	        									//LOG.info("3, " + key);
	        								subtractFromRouteCountHashMap(key);
	        							}
	        						}
	        					}
	        					else {
	        						if(pillarRoof[RING_BUFFEER_SIZE + (tail)] != -1) {
	        							for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
	        								key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
	        								if(key.equals("0000"))
	        									//LOG.info("4, " + key);
	        								subtractFromRouteCountHashMap(key);
	        							}
	        						}
	        					}
	        				}
	        			}
	        		}
	        		else {
	        			int tail = i-30*60;
	        			if(tail >=0) {
    						if(pillarRoof[tail] != -1) {
    							for(int k=0; k<=pillarRoof[tail]; k++) {
    								key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
    								if(key.equals("0000"))
    									//LOG.info("5, " + key);
    								subtractFromRouteCountHashMap(key);
    							}
    						}
    					}
    					else {
    						if(pillarRoof[RING_BUFFEER_SIZE + tail] != -1) {
    							for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
    								key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
    								if(key.equals("0000"))
    									//LOG.info("6, " + key);
    								subtractFromRouteCountHashMap(key);
    							}
    						}
    					}
	        		}
	        	}
	        	for(int i=0; i<writerRingBufferHead; i++) {
	        		if(pillarRoof[i] != -1) {
	        			for(int j=0; j<=pillarRoof[i]; j++) {
	        				
	        				key = Integer.toString(ringBuffer[i][j].beginCell500X) + Integer.toString(ringBuffer[i][j].beginCell500Y) + Integer.toString(ringBuffer[i][j].endCell500X) + Integer.toString(ringBuffer[i][j].endCell500Y);
	        				if(key.equals("0000"))
	        					//LOG.info("2, " + key);
	        				addToRouteCountHashMap(key);
	        				
	        				if(j==0) {
	        					int tail = i-30*60;
	        					if(tail >=0) {
	        						if(pillarRoof[tail] != -1) {
	        							for(int k=0; k<=pillarRoof[tail]; k++) {
	        								key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
	        								if(key.equals("0000"))
	        									//LOG.info("3, " + key);
	        								subtractFromRouteCountHashMap(key);
	        							}
	        						}
	        					}
	        					else {
	        						if(pillarRoof[RING_BUFFEER_SIZE + (tail)] != -1) {
	        							for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
	        								key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
	        								if(key.equals("0000"))
	        									//LOG.info("4, " + key);
	        								subtractFromRouteCountHashMap(key);
	        							}
	        						}
	        					}
	        				}
	        			}
	        		}
	        		else {
	        			int tail = i-30*60;
	        			if(tail >=0) {
    						if(pillarRoof[tail] != -1) {
    							for(int k=0; k<=pillarRoof[tail]; k++) {
    								key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
    								if(key.equals("0000"))
    									//LOG.info("5, " + key);
    								subtractFromRouteCountHashMap(key);
    							}
    						}
    					}
    					else {
    						if(pillarRoof[RING_BUFFEER_SIZE + tail] != -1) {
    							for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
    								key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
    								if(key.equals("0000"))
    									//LOG.info("6, " + key);
    								subtractFromRouteCountHashMap(key);
    							}
    						}
    					}
	        		}
	        	}
	        	for(int i=0; i<writerPillarHead; i++) {
	        		key = Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500Y)  + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			//LOG.info("7, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	routeQuery_PillarHead = writerPillarHead;
	        	routeQuery_RingBufferHead = writerRingBufferHead;
	        	//LOG.info("1 ASSIGN routeQuery_pillar_head = " + routeQuery_pillar_head%100000);
	        }
	        //LOG.info(key);
	    }
	}
	
	private void addToRouteCountHashMap(String key)
	{
		if(routeCountHashMap.containsKey(key))
			routeCountHashMap.put(key, routeCountHashMap.get(key)+1);
		else 
			routeCountHashMap.put(key, 1);
	}
	
	private void subtractFromRouteCountHashMap(String key) throws Exception
	{
		if(routeCountHashMap.containsKey(key))
			routeCountHashMap.put(key, routeCountHashMap.get(key)-1);
		else 
			throw new Exception();
	}
	
    private void executeProfitableCellsQuery() {
    	LOG.info("Thread started: " + Thread.currentThread().getName());
    	while(true){
            ;
        }
	}
	
	private static void extractData(LineIterator it) throws Exception {
		String[] pieces = it.nextLine().split(",");
	
		TripEvent selectedTripEvent;
		long startTime = SIMPLE_DATE_FORMAT.parse(pieces[2]).getTime();
		long endTime   = SIMPLE_DATE_FORMAT.parse(pieces[3]).getTime();
		

				
		if(headTime!=null && ((endTime - headTime.getTime())/1000 < 0)) {
			//LOG.info("Event time less that previous one - New: " + SIMPLE_DATE_FORMAT.parse(pieces[3]) + "    Previous: " + headTime);
			return;
		}	
		else if(headTime==null) {
			selectedTripEvent = ringBuffer[ringBufferHead][pillarHead];
		}
		else if((endTime - headTime.getTime())/1000 == 0L) {
			//LOG.info(pillarHead + ", " + endTime);
			selectedTripEvent = ringBuffer[ringBufferHead][pillarHead];
		}
		else {
			//LOG.info("Event time less that previous one - New: " + SIMPLE_DATE_FORMAT.parse(pieces[3]) + "    Previous: " + headTime);
			selectedTripEvent = ringBuffer[(ringBufferHead + (int)((endTime - headTime.getTime())/1000)) % RING_BUFFEER_SIZE][0];
		}

		
		
		//----------SETTING TRIP DATA---------------------------- 
		selectedTripEvent.startTime.setTime(startTime);
		selectedTripEvent.endTime.setTime(endTime);

		selectedTripEvent.beginCell500X = 
				(int) ((Double.parseDouble(pieces[6])+74.916578)/0.005986) + 1;
		selectedTripEvent.beginCell500Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[7]))/0.004491556) + 1;
		
		selectedTripEvent.endCell500X = 
				(int) ((Double.parseDouble(pieces[8])+74.916578)/0.005986) + 1;
		selectedTripEvent.endCell500Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[9]))/0.004491556) + 1;
		//LOG.info("500: (" + toBeFilledTripEvent.beginCell500X + ", " + toBeFilledTripEvent.beginCell500Y + ")  (" + toBeFilledTripEvent.endCell500X + ", " + toBeFilledTripEvent.endCell500Y + ")");

		selectedTripEvent.beginCell250X = 
				(int) ((Double.parseDouble(pieces[6])+74.916578)/0.002993) + 1;
		selectedTripEvent.beginCell250Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[7]))/0.002245778) + 1;
		
		selectedTripEvent.endCell250X = 
				(int) ((Double.parseDouble(pieces[8])+74.916578)/0.002993) + 1;
		selectedTripEvent.endCell250Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[9]))/0.002245778) + 1;
		//LOG.info("250: (" + toBeFilledTripEvent.beginCell250X + ", " + toBeFilledTripEvent.beginCell250Y + ")  (" + toBeFilledTripEvent.endCell250X + ", " + toBeFilledTripEvent.endCell250Y + ")");

		if(selectedTripEvent.beginCell500X < 1 || selectedTripEvent.beginCell500X > 300
				|| selectedTripEvent.beginCell500Y < 1 || selectedTripEvent.beginCell500Y > 300 
				|| selectedTripEvent.beginCell250X < 1 || selectedTripEvent.beginCell250X > 600 
				|| selectedTripEvent.beginCell250Y < 1 || selectedTripEvent.beginCell250Y > 600
				|| selectedTripEvent.endCell500X < 1 || selectedTripEvent.endCell500X > 300
				|| selectedTripEvent.endCell500Y < 1 || selectedTripEvent.endCell500Y > 300 
				|| selectedTripEvent.endCell250X < 1 || selectedTripEvent.endCell250X > 600 
				|| selectedTripEvent.endCell250Y < 1 || selectedTripEvent.endCell250Y > 600)
		{
			return;
		}
		LOG.info(Integer.toString(selectedTripEvent.beginCell500X) + Integer.toString(selectedTripEvent.beginCell500Y) + Integer.toString(selectedTripEvent.beginCell250X) + Integer.toString(selectedTripEvent.beginCell250Y));
		
		selectedTripEvent.fareAmount = Double.parseDouble(pieces[11]);
		selectedTripEvent.tipAmount  = Double.parseDouble(pieces[14]);
		
		selectedTripEvent.medallion0 = pieces[0].charAt(0);
		selectedTripEvent.medallion1 = pieces[0].charAt(1);
		selectedTripEvent.medallion2 = pieces[0].charAt(2);
		selectedTripEvent.medallion3 = pieces[0].charAt(3);
		selectedTripEvent.medallion4 = pieces[0].charAt(4);
		selectedTripEvent.medallion5 = pieces[0].charAt(5);
		selectedTripEvent.medallion6 = pieces[0].charAt(6);
		selectedTripEvent.medallion7 = pieces[0].charAt(7);
		selectedTripEvent.medallion8 = pieces[0].charAt(8);
		selectedTripEvent.medallion9 = pieces[0].charAt(9);
		selectedTripEvent.medallion10 = pieces[0].charAt(10);
		selectedTripEvent.medallion11 = pieces[0].charAt(11);
		selectedTripEvent.medallion12 = pieces[0].charAt(12);
		selectedTripEvent.medallion13 = pieces[0].charAt(13);
		selectedTripEvent.medallion14 = pieces[0].charAt(14);
		selectedTripEvent.medallion15 = pieces[0].charAt(15);
		selectedTripEvent.medallion16 = pieces[0].charAt(16);
		selectedTripEvent.medallion17 = pieces[0].charAt(17);
		selectedTripEvent.medallion18 = pieces[0].charAt(18);
		selectedTripEvent.medallion19 = pieces[0].charAt(19);
		selectedTripEvent.medallion20 = pieces[0].charAt(20);
		selectedTripEvent.medallion21 = pieces[0].charAt(21);
		selectedTripEvent.medallion22 = pieces[0].charAt(22);
		selectedTripEvent.medallion23 = pieces[0].charAt(23);
		selectedTripEvent.medallion24 = pieces[0].charAt(24);
		selectedTripEvent.medallion25 = pieces[0].charAt(25);
		selectedTripEvent.medallion26 = pieces[0].charAt(26);
		selectedTripEvent.medallion27 = pieces[0].charAt(27);
		selectedTripEvent.medallion28 = pieces[0].charAt(28);
		selectedTripEvent.medallion29 = pieces[0].charAt(29);
		selectedTripEvent.medallion30 = pieces[0].charAt(30);
		selectedTripEvent.medallion31 = pieces[0].charAt(31);
		
		
		if(headTime==null) {
			headTime = new Date();
			headTime.setTime(endTime);
			ringBufferHead = 1; 
		}
		else if((endTime - headTime.getTime())/1000 == 0L) {
			pillarHead++;
		}
		else {
			//LOG.info("Event time less that previous one - New: " + SIMPLE_DATE_FORMAT.parse(pieces[3]) + "    Previous: " + headTime);
			
			pillarRoof[ringBufferHead] = pillarHead-1;
			
			int newRingBufferHead =  (ringBufferHead + (int)((endTime - headTime.getTime())/1000)) % RING_BUFFEER_SIZE;
			if(newRingBufferHead>ringBufferHead) {
				for(int i=ringBufferHead+1; i<newRingBufferHead; i++)
					pillarRoof[i] = -1;
			}
			else {
				for(int i=ringBufferHead+1; i<RING_BUFFEER_SIZE; i++)
					pillarRoof[i] = -1;
				for(int i=0; i<newRingBufferHead; i++)
					pillarRoof[i] = -1;
			}
			headTime.setTime(endTime);
			ringBufferHead = newRingBufferHead;
			pillarHead = 0;
		}
		
		//UPDATING INTER THREAD COMMUNIICATOR
		combinedHead = pillarHead*100000 + ringBufferHead;
	}
	
	private static void populateConfigs() {
	      Properties properties = new Properties();
	      FileInputStream input = null;
	      try {
	         input = new FileInputStream("conf//config.properties");
	         properties.load(input);

	         for (String key : properties.stringPropertyNames()) {
	            if (key.startsWith("grid")) {
	               ll2xyConfigMap.put(key, properties.getProperty(key));
	            }
	         }
	      } catch (Exception e) {
	         LOG.error("No config file found, exitting...");
	         e.printStackTrace();
	         System.exit(1);
	      }
	   }
}
