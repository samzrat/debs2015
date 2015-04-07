package samzrat.debs2015;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

	public static final int RING_BUFFEER_SIZE = 33600;
	public static final int PILLAR_MAX_SIZE = 500;
	public static final int ROUTE_COUNT_ARRAY_DIMENSION = 300;
	public static final int CELL_PROFIT_ARRAY_DIMENSION = 600;
	public static final int CELL_PROFIT_ARRAY_ROUTE_COUNT_ARRAY_SIZE = 100;
	
	public static final int OUTPUT_RING_BUFFER_SIZE = 500000;
	public static final int OUTPUT_LIST_SIZE = 10;
	
	
	private static Map<String, String> ll2xyConfigMap = new HashMap<String, String>();
	private static Logger LOG = Logger.getLogger(ApplicationMain.class);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static HashMap<String, Integer> routeCountHashMap = new HashMap<>();
	//static CellProfitInfo[][] cellProfitArray = new CellProfitInfo[CELL_PROFIT_ARRAY_DIMENSION][CELL_PROFIT_ARRAY_DIMENSION];
	static HashMap<String, CellProfitInfo> cellProfitabilityHashMap = new HashMap<>();
	static TripEvent[][] ringBuffer = new TripEvent[RING_BUFFEER_SIZE][PILLAR_MAX_SIZE];
	static int[] pillarRoof = new int[RING_BUFFEER_SIZE];
	
	
	static TopRoute[][] outputRingBuffer = new TopRoute[OUTPUT_RING_BUFFER_SIZE][OUTPUT_LIST_SIZE];
	static String[] outputStringBuffer = new String[OUTPUT_RING_BUFFER_SIZE];
	
	//INTER THREAD COMMUNICATOR
	private static volatile int combinedHead = 0;
	private static volatile int outputHead = -1;
	private static volatile int outputStringHead = 0;
	
	
	//----------writer thread internals------------------------------- 
	private static int ringBufferHead = 0;
	private static int pillarHead = 0;
	private static Date headTime = null;
    
    //----------------------------------------------------------------
	
	//----------routeQuery thread internals-------------------------------
	//private static int routeQuery_pillar_head = 0;
	private static int routeQuery_PillarHead = 0;
	private static int routeQuery_RingBufferHead = 0;
	
	private static int topRouteCount_rank1 = 0;
	private static int topRouteCount_rank10 = 0;
	private static List<TopRoute> topRoutesArray = new ArrayList<TopRoute>(11);
	
	private static int internalOutputHead = 0;
	
	//----------------------------------------------------------------
	
	//----------cellQuery thread internals-------------------------------
		//private static int routeQuery_pillar_head = 0;
		private static int cellQuery_PillarHead = 0;
		private static int cellQuery_RingBufferHead = 0;
		
		private static double topCellProfitability_rank1 = 0;
		private static double topCellProfitability_rank10 = 0;
		private static List<TopCell> topCellsArray = new ArrayList<TopCell>(11);
		
		private static int internalOutputHead2 = 0;
		
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
		



		
		for(int i=0; i<RING_BUFFEER_SIZE; i++)
			for(int j=0; j<PILLAR_MAX_SIZE; j++)
				ringBuffer[i][j] = new TripEvent();
		
		Arrays.fill(pillarRoof, -1);
		for(TopRoute[] item: outputRingBuffer){
			Arrays.fill(item, null);
		}
		
		//Creating the query threads
		ApplicationMain mt = new ApplicationMain();
		Thread popularRoutesQueryThread = new Thread(mt);
		Thread profitableCellsQueryThread = new Thread(mt);
		Thread outputThread = new Thread(mt);
		popularRoutesQueryThread.setName("PopularRoutesQueryThread");
		profitableCellsQueryThread.setName("ProfitableCellsQueryThread");
		outputThread.setName("OutputThread");
		popularRoutesQueryThread. start();
		profitableCellsQueryThread. start();
		outputThread.start();
	
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


		if(Thread.currentThread().getName().equals("PopularRoutesQueryThread")) {
			try {
				executePopularRoutesQuery();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		else if(Thread.currentThread().getName().equals("ProfitableCellsQueryThread")) {
			executeProfitableCellsQuery();
		}
		else if(Thread.currentThread().getName().equals("OutputThread"))
			try {
				outputTopRoutes();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private void traverseExpiredItemForCells(String key, int i) {
		
	}
	
	private void addToCellProfitabilityHashMap(TripEvent tripEvent) {

		if(tripEvent.fareAmount<0 || tripEvent.tipAmount<0)
			return;

		boolean topTenChanged = false;
		CellProfitInfo cellProfitInfo;
		String key = Integer.toString(tripEvent.beginCell250X) + Integer.toString(tripEvent.beginCell250Y);
		TripFare tripFare = new TripFare(tripEvent.medallion, tripEvent.fareAmount+tripEvent.tipAmount);

		if(cellProfitabilityHashMap.containsKey(key)) {
			cellProfitInfo = cellProfitabilityHashMap.get(key);
			cellProfitInfo.tripFareList.add(tripFare);

		}
		else {
			cellProfitInfo = new CellProfitInfo();
			cellProfitInfo.tripFareList.add(tripFare);
			cellProfitabilityHashMap.put(key, cellProfitInfo);
		}

		//outputStringBuffer[outputStringHead] = Integer.toString(count);
		//outputStringHead = (outputStringHead + 1) % OUTPUT_RING_BUFFER_SIZE;



		if(cellProfitInfo.taxiCount != 0) {		
			
			Collections.sort(cellProfitInfo.tripFareList, new Comparator<TripFare>(){
				public int compare(TripFare p1, TripFare p2) {
					return (int)(p2.fare- p1.fare);
				}
			});
			
			double median;
			if(cellProfitInfo.tripFareList.size()%2 == 0)
				median = cellProfitInfo.tripFareList.get(cellProfitInfo.tripFareList.size()/2).fare + cellProfitInfo.tripFareList.get(cellProfitInfo.tripFareList.size()/2 - 1).fare;
			else 
				median = cellProfitInfo.tripFareList.get(cellProfitInfo.tripFareList.size()/2).fare;
			
			double profitability = median/cellProfitInfo.taxiCount;
				
			
			if(topCellsArray.size()<10) {
				topCellsArray.add(new TopCell(key, profitability));
				topTenChanged = true;
			}
			else {

				if(profitability>topCellProfitability_rank10) {

					boolean found = false;
					for(TopCell item: topCellsArray){
						if(item.cell.equals(key)) {
							item.profitability = profitability;
							found = true;
							topTenChanged = true;
						}
					}
					if(found==false) {
						topCellsArray.get(topCellsArray.size()-1).cell = key;
						topCellsArray.get(topCellsArray.size()-1).profitability = profitability;
						topTenChanged = true;
					}

				}	
			}
			
			Collections.sort(topCellsArray, new Comparator<TopCell>(){
				public int compare(TopCell p1, TopCell p2) {
					return (int)(p2.profitability- p1.profitability);
				}
			});

			topCellProfitability_rank10 = topCellsArray.get(topCellsArray.size()-1).profitability;
			topCellProfitability_rank1 = topCellsArray.get(0).profitability;
	/*		if (topTenChanged == true) {
				for(int i=0; i<10; i++) {
					outputRingBuffer[internalOutputHead][i] = null;
				}
				for(int i=0; i<topRoutesArray.size(); i++) {
					outputRingBuffer[internalOutputHead][i] = new TopRoute(topRoutesArray.get(i).route, topRoutesArray.get(i).count);
				}
				String output = "";
				for(int i=0; i<10; i++) {
					if(outputRingBuffer[internalOutputHead][i] == null)
						output = output + ", null";
					else
						output = output + ", " + outputRingBuffer[internalOutputHead][i].count;
				}
				//LOG.info(output);
				outputHead = internalOutputHead;
				internalOutputHead = (internalOutputHead + 1) % OUTPUT_RING_BUFFER_SIZE;

				//LOG.info("outputHead = " + outputHead);

			}
	*/		//LOG.info(outputHead);

			/*		String topRoutes = "";
			for(TopRoute item: topRoutesArray){
				topRoutes = topRoutes + ", " + item.count;
			}
			LOG.info(topRoutes);
			 */
		}	
		

	}
	
	private void subtractFromCellProfitabilityHashMap(TripEvent tripEvent) throws Exception {
		//LOG.info("subtractFromRouteCountHashMap called");

		boolean topTenChanged = false;
		CellProfitInfo cellProfitInfo;
		String key = Integer.toString(tripEvent.beginCell250X) + Integer.toString(tripEvent.beginCell250Y);
		if(cellProfitabilityHashMap.containsKey(key)) {
			cellProfitInfo = cellProfitabilityHashMap.get(key);
			for(TripFare item: cellProfitInfo.tripFareList){
				if(item.medallion.equals(tripEvent.medallion)) {
					cellProfitInfo.tripFareList.remove(item);
				}
			}
			Collections.sort(cellProfitInfo.tripFareList, new Comparator<TripFare>(){
				public int compare(TripFare p1, TripFare p2) {
					return (int)(p2.fare- p1.fare);
				}
			});


		}
		else {
			LOG.info("DOES NOT CONTAIN KEY: " + key);
			throw new Exception();
		}

		if(cellProfitInfo.taxiCount != 0) {		

			Collections.sort(cellProfitInfo.tripFareList, new Comparator<TripFare>(){
				public int compare(TripFare p1, TripFare p2) {
					return (int)(p2.fare- p1.fare);
				}
			});

			double median;
			if(cellProfitInfo.tripFareList.size()%2 == 0)
				median = cellProfitInfo.tripFareList.get(cellProfitInfo.tripFareList.size()/2).fare + cellProfitInfo.tripFareList.get(cellProfitInfo.tripFareList.size()/2 - 1).fare;
			else 
				median = cellProfitInfo.tripFareList.get(cellProfitInfo.tripFareList.size()/2).fare;

			double profitability = median/cellProfitInfo.taxiCount;


			if(topCellsArray.size()<10) {
				topCellsArray.add(new TopCell(key, profitability));
				topTenChanged = true;
			}
			else {

				if(profitability>topCellProfitability_rank10) {

					boolean found = false;
					for(TopCell item: topCellsArray){
						if(item.cell.equals(key)) {
							item.profitability = profitability;
							found = true;
							topTenChanged = true;
						}
					}
					if(found==false) {
						topCellsArray.get(topCellsArray.size()-1).cell = key;
						topCellsArray.get(topCellsArray.size()-1).profitability = profitability;
						topTenChanged = true;
					}

				}	
			}

			Collections.sort(topCellsArray, new Comparator<TopCell>(){
				public int compare(TopCell p1, TopCell p2) {
					return (int)(p2.profitability- p1.profitability);
				}
			});

			topCellProfitability_rank10 = topCellsArray.get(topCellsArray.size()-1).profitability;
			topCellProfitability_rank1 = topCellsArray.get(0).profitability;
			/*		if (topTenChanged == true) {
				for(int i=0; i<10; i++) {
					outputRingBuffer[internalOutputHead][i] = null;
				}
				for(int i=0; i<topRoutesArray.size(); i++) {
					outputRingBuffer[internalOutputHead][i] = new TopRoute(topRoutesArray.get(i).route, topRoutesArray.get(i).count);
				}
				String output = "";
				for(int i=0; i<10; i++) {
					if(outputRingBuffer[internalOutputHead][i] == null)
						output = output + ", null";
					else
						output = output + ", " + outputRingBuffer[internalOutputHead][i].count;
				}
				//LOG.info(output);
				outputHead = internalOutputHead;
				internalOutputHead = (internalOutputHead + 1) % OUTPUT_RING_BUFFER_SIZE;

				//LOG.info("outputHead = " + outputHead);

			}
			 */		//LOG.info(outputHead);

			/*		String topRoutes = "";
			for(TopRoute item: topRoutesArray){
				topRoutes = topRoutes + ", " + item.count;
			}
			LOG.info(topRoutes);
			 */
		}	
	}

	private void executeProfitableCellsQuery() {
		LOG.info("Thread started: " + Thread.currentThread().getName());

		int combinedHeadCopy;
		int writerPillarHead;
		int writerRingBufferHead;
		String key = "";
		while(true){
			combinedHeadCopy = combinedHead;
			writerPillarHead = combinedHeadCopy/100000;
			writerRingBufferHead = combinedHeadCopy%100000;

			if(writerRingBufferHead > cellQuery_RingBufferHead) {
				for(int i=cellQuery_PillarHead; i<=pillarRoof[cellQuery_RingBufferHead]; i++) {
					addToCellProfitabilityHashMap(ringBuffer[cellQuery_RingBufferHead][i]);
				}
				for(int i=cellQuery_RingBufferHead + 1; i<writerRingBufferHead; i++) {
					if(pillarRoof[i] != -1) {
						for(int j=0; j<=pillarRoof[i]; j++) {
							addToCellProfitabilityHashMap(ringBuffer[i][j]);

							if(j==0) {
								traverseExpiredItemForCells(key, i);
							}
						}
					}
					else {
						traverseExpiredItemForCells(key, i);
					}
				}
				for(int i=0; i<writerPillarHead; i++) {
					addToCellProfitabilityHashMap(ringBuffer[writerRingBufferHead][i]);

					if(i==0) {
						traverseExpiredItemForCells(key, writerRingBufferHead);
					}
				}
				cellQuery_PillarHead = writerPillarHead;
				cellQuery_RingBufferHead = writerRingBufferHead;
				//LOG.info("1 ASSIGN cellQuery_pillar_head = " + cellQuery_pillar_head%100000);

			}
			else if(writerRingBufferHead == cellQuery_RingBufferHead) {
				for(int i=cellQuery_PillarHead; i<writerPillarHead; i++) {
					addToCellProfitabilityHashMap(ringBuffer[cellQuery_RingBufferHead][i]);
				}
				//LOG.info("2 ASSIGN cellQuery_pillar_head = " + cellQuery_pillar_head%100000);
				cellQuery_PillarHead = writerPillarHead;
			}
			else {
				for(int i=cellQuery_PillarHead; i<=pillarRoof[cellQuery_RingBufferHead]; i++) {
					addToCellProfitabilityHashMap(ringBuffer[cellQuery_RingBufferHead][i]);
				}
				for(int i=cellQuery_RingBufferHead+1; i<RING_BUFFEER_SIZE; i++) {
					if(pillarRoof[i] != -1) {
						for(int j=0; j<=pillarRoof[i]; j++) {
							addToCellProfitabilityHashMap(ringBuffer[i][j]);

							if(j==0) {
								traverseExpiredItemForCells(key, i);
							}
						}
					}
					else {
						traverseExpiredItemForCells(key, i);
					}
				}
				for(int i=0; i<writerRingBufferHead; i++) {
					if(pillarRoof[i] != -1) {
						for(int j=0; j<=pillarRoof[i]; j++) {
							addToCellProfitabilityHashMap(ringBuffer[i][j]);

							if(j==0) {
								traverseExpiredItemForCells(key, i);
							}
						}
					}
					else {
						traverseExpiredItemForCells(key, i);
					}
				}
				for(int i=0; i<writerPillarHead; i++) {
					addToCellProfitabilityHashMap(ringBuffer[writerRingBufferHead][i]);

					if(i==0) {
						traverseExpiredItemForCells(key, writerRingBufferHead);
					}
				}
				cellQuery_PillarHead = writerPillarHead;
				cellQuery_RingBufferHead = writerRingBufferHead;
				//LOG.info("1 ASSIGN cellQuery_pillar_head = " + cellQuery_pillar_head%100000);
			}
			//LOG.info(key + ", " + routeCountHashMap.get(key));
		}

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
	        			LOG.info("1, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	for(int i=routeQuery_RingBufferHead + 1; i<writerRingBufferHead; i++) {
	        		if(pillarRoof[i] != -1) {
	        			for(int j=0; j<=pillarRoof[i]; j++) {
	        				
	        				key = Integer.toString(ringBuffer[i][j].beginCell500X) + Integer.toString(ringBuffer[i][j].beginCell500Y) + Integer.toString(ringBuffer[i][j].endCell500X) + Integer.toString(ringBuffer[i][j].endCell500Y);
	        				if(key.equals("0000"))
	        					LOG.info("2, " + key);
	        				addToRouteCountHashMap(key);
	        				
	        				if(j==0) {
	        					traverseExpiredItems(key, i);
	        				}
	        			}
	        		}
	        		else {
	        			traverseExpiredItems(key, i);
	        		}
	        	}
	        	for(int i=0; i<writerPillarHead; i++) {
	        		key = Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500Y)  + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			LOG.info("7, " + key);
	        		addToRouteCountHashMap(key);
	        		
	        		if(i==0) {
	        			traverseExpiredItems(key, writerRingBufferHead);
    				}
	        	}
	        	routeQuery_PillarHead = writerPillarHead;
	        	routeQuery_RingBufferHead = writerRingBufferHead;
	        	//LOG.info("1 ASSIGN routeQuery_pillar_head = " + routeQuery_pillar_head%100000);
	        	
	        }
	        else if(writerRingBufferHead == routeQuery_RingBufferHead) {
	        	for(int i=routeQuery_PillarHead; i<writerPillarHead; i++) {
	        		key = Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500Y) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			LOG.info("8, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	//LOG.info("2 ASSIGN routeQuery_pillar_head = " + routeQuery_pillar_head%100000);
	        	routeQuery_PillarHead = writerPillarHead;
	        }
	        else {
	        	for(int i=routeQuery_PillarHead; i<=pillarRoof[routeQuery_RingBufferHead]; i++) {
	        		key = Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].beginCell500Y) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[routeQuery_RingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			LOG.info("1, " + key);
	        		addToRouteCountHashMap(key);
	        	}
	        	for(int i=routeQuery_RingBufferHead+1; i<RING_BUFFEER_SIZE; i++) {
	        		if(pillarRoof[i] != -1) {
	        			for(int j=0; j<=pillarRoof[i]; j++) {
	        				
	        				key = Integer.toString(ringBuffer[i][j].beginCell500X) + Integer.toString(ringBuffer[i][j].beginCell500Y) + Integer.toString(ringBuffer[i][j].endCell500X) + Integer.toString(ringBuffer[i][j].endCell500Y);
	        				if(key.equals("0000"))
	        					LOG.info("2, " + key);
	        				addToRouteCountHashMap(key);
	        				
	        				if(j==0) {
	        					traverseExpiredItems(key, i);
	        				}
	        			}
	        		}
	        		else {
	        			traverseExpiredItems(key, i);
	        		}
	        	}
	        	for(int i=0; i<writerRingBufferHead; i++) {
	        		if(pillarRoof[i] != -1) {
	        			for(int j=0; j<=pillarRoof[i]; j++) {
	        				
	        				key = Integer.toString(ringBuffer[i][j].beginCell500X) + Integer.toString(ringBuffer[i][j].beginCell500Y) + Integer.toString(ringBuffer[i][j].endCell500X) + Integer.toString(ringBuffer[i][j].endCell500Y);
	        				if(key.equals("0000"))
	        					LOG.info("2, " + key);
	        				addToRouteCountHashMap(key);
	        				
	        				if(j==0) {
	        					traverseExpiredItems(key, i);
	        				}
	        			}
	        		}
	        		else {
	        			traverseExpiredItems(key, i);
	        		}
	        	}
	        	for(int i=0; i<writerPillarHead; i++) {
	        		key = Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].beginCell500Y)  + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500X) + Integer.toString(ringBuffer[writerRingBufferHead][i].endCell500Y);
	        		if(key.equals("0000"))
	        			LOG.info("7, " + key);
	        		addToRouteCountHashMap(key);
	        		
	        		if(i==0) {
	        			traverseExpiredItems(key, writerRingBufferHead);
    				}
	        	}
	        	routeQuery_PillarHead = writerPillarHead;
	        	routeQuery_RingBufferHead = writerRingBufferHead;
	        	//LOG.info("1 ASSIGN routeQuery_pillar_head = " + routeQuery_pillar_head%100000);
	        }
	        //LOG.info(key + ", " + routeCountHashMap.get(key));
	    }
	}
	
	private void traverseExpiredItems(String key, int ringBufferHead) throws Exception {

		int tail = ringBufferHead-30*60;
		if(tail >=0) {
			if(pillarRoof[tail] != -1) {
				for(int k=0; k<=pillarRoof[tail]; k++) {
					key = Integer.toString(ringBuffer[tail][k].beginCell500X) + Integer.toString(ringBuffer[tail][k].beginCell500Y) + Integer.toString(ringBuffer[tail][k].endCell500X) + Integer.toString(ringBuffer[tail][k].endCell500Y);
					if(key.equals("0000"))
						LOG.info("3, " + key);
					subtractFromRouteCountHashMap(key);
				}
			}
		}
		else {
			if(pillarRoof[RING_BUFFEER_SIZE + (tail)] != -1) {
				for(int k=0; k<=pillarRoof[RING_BUFFEER_SIZE + tail]; k++) {
					key = Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].beginCell500Y) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500X) + Integer.toString(ringBuffer[RING_BUFFEER_SIZE + tail][k].endCell500Y);
					if(key.equals("0000"))
						LOG.info("4, " + key);
					subtractFromRouteCountHashMap(key);
				}
			}
		}

	}
	private void addToRouteCountHashMap(String key)
	{
		int count = 1;
		boolean topTenChanged = false;
		if(routeCountHashMap.containsKey(key)) {
			count = routeCountHashMap.get(key) + 1;
			routeCountHashMap.put(key, count);
		}
		else 
			routeCountHashMap.put(key, 1);
		
		outputStringBuffer[outputStringHead] = Integer.toString(count);
		outputStringHead = (outputStringHead + 1) % OUTPUT_RING_BUFFER_SIZE;
		
		
		
		
		if(topRoutesArray.size()<10) {
			topRoutesArray.add(new TopRoute(key, count));
			topTenChanged = true;
		}
		else {
		
			if(count>topRouteCount_rank10) {
				
				boolean found = false;
				for(TopRoute item: topRoutesArray){
					if(item.route.equals(key)) {
						item.count = count;
						found = true;
						topTenChanged = true;
					}
				}
				if(found==false) {
					topRoutesArray.get(topRoutesArray.size()-1).route = key;
					topRoutesArray.get(topRoutesArray.size()-1).count = count;
					topTenChanged = true;
				}

			}	
		}
		
		Collections.sort(topRoutesArray, new Comparator<TopRoute>(){
			public int compare(TopRoute p1, TopRoute p2) {
				return p2.count- p1.count;
			}
		});
		
		topRouteCount_rank10 = topRoutesArray.get(topRoutesArray.size()-1).count;
		topRouteCount_rank1 = topRoutesArray.get(0).count;
		if (topTenChanged == true) {
			for(int i=0; i<10; i++) {
				outputRingBuffer[internalOutputHead][i] = null;
			}
			for(int i=0; i<topRoutesArray.size(); i++) {
				outputRingBuffer[internalOutputHead][i] = new TopRoute(topRoutesArray.get(i).route, topRoutesArray.get(i).count);
			}
			String output = "";
			for(int i=0; i<10; i++) {
				if(outputRingBuffer[internalOutputHead][i] == null)
					output = output + ", null";
				else
					output = output + ", " + outputRingBuffer[internalOutputHead][i].count;
			}
			//LOG.info(output);
			outputHead = internalOutputHead;
			internalOutputHead = (internalOutputHead + 1) % OUTPUT_RING_BUFFER_SIZE;
			
			//LOG.info("outputHead = " + outputHead);
			
		}
		//LOG.info(outputHead);
		
/*		String topRoutes = "";
		for(TopRoute item: topRoutesArray){
			topRoutes = topRoutes + ", " + item.count;
		}
		LOG.info(topRoutes);
		*/
	}

	private void outputTopRoutes() throws Exception {
		int copyOfOutPutHead = -1;
		int previousCopyOfOutPutHead = -1;

		int copyOfOutputStringHead = 0;
		int previouscopyOfOutputStringHead = 0;
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("filename.txt"), "utf-8"));
			for(;;) {

				previouscopyOfOutputStringHead = copyOfOutputStringHead;
				copyOfOutputStringHead = outputStringHead;

				for(int i=previouscopyOfOutputStringHead; i< copyOfOutputStringHead; i++)
					//writer.write(outputStringBuffer[i] + '\n');

								
				
				copyOfOutPutHead = outputHead;
				//LOG.info("copyOfOutPutHead = " + copyOfOutPutHead);
				//LOG.info("outputHead = " + outputHead);
				if(copyOfOutPutHead!= -1 && copyOfOutPutHead>previousCopyOfOutPutHead) {
					for(int l=previousCopyOfOutPutHead+1; l<=copyOfOutPutHead; l++) {
						String output = "";
						//TopRoute[] topRoutes = outputRingBuffer[l];
						for(int i=0; i<10; i++) { 
							if(outputRingBuffer[l][i] == null)
								output += ", null";
							else 
								output += ", " + outputRingBuffer[l][i].count;
						}
						//LOG.info(output);
						writer.write(output + '\n');
					}
					previousCopyOfOutPutHead = copyOfOutPutHead;
				}
			}
		} catch (IOException ex) {
			throw new Exception();
			// report
		} finally {
			try {writer.close();} catch (Exception ex) {}
		}
	}

	private void subtractFromRouteCountHashMap(String key) throws Exception
	{
		//LOG.info("subtractFromRouteCountHashMap called");
		int count = 1;
		if(routeCountHashMap.containsKey(key)) {
			count = routeCountHashMap.get(key) - 1;
			routeCountHashMap.put(key, count);
		}
		else 
		{
			LOG.info("DOES NOT CONTAIN KEY: " + key);
			throw new Exception();

		}



		boolean found = false;
		for(TopRoute item: topRoutesArray){
			if(item.route.equals(key)) {
				item.count = count;
				found = true;
				//LOG.info("found in top ten = " + key + "  " + count);
			}
		}
		if(found==true) {
			Collections.sort(topRoutesArray, new Comparator<TopRoute>(){
				public int compare(TopRoute p1, TopRoute p2) {
					return p2.count- p1.count;
				}
			});

			topRouteCount_rank10 = topRoutesArray.get(topRoutesArray.size()-1).count;
			topRouteCount_rank1 = topRoutesArray.get(0).count;

			for(int i=0; i<10; i++) {
				outputRingBuffer[internalOutputHead][i] = null;
			}
			for(int i=0; i<topRoutesArray.size(); i++) {
				outputRingBuffer[internalOutputHead][i] = new TopRoute(topRoutesArray.get(i).route, topRoutesArray.get(i).count);
			}
			String output = "";
			for(int i=0; i<10; i++) {
				if(outputRingBuffer[internalOutputHead][i] == null)
					output = output + ", null";
				else
					output = output + ", " + outputRingBuffer[internalOutputHead][i].count;
			}
			//LOG.info(output);
			outputHead = internalOutputHead;
			internalOutputHead = (internalOutputHead + 1) % OUTPUT_RING_BUFFER_SIZE;

			//LOG.info("outputHead = " + outputHead);
		}

	}

    
	
	private static void extractData(LineIterator it) throws Exception {
		//LOG.info("Before");
		String[] pieces = it.nextLine().split(",");
		//LOG.info("After");
		
		TripEvent selectedTripEvent;
		long startTime = SIMPLE_DATE_FORMAT.parse(pieces[2]).getTime();
		long endTime   = SIMPLE_DATE_FORMAT.parse(pieces[3]).getTime();
		

				
		if(headTime!=null && ((endTime - headTime.getTime())/1000 < 0)) {
			LOG.info("Event time less that previous one - New: (" + pieces[3] + ") " + SIMPLE_DATE_FORMAT.parse(pieces[3]) + "    Previous: " + headTime);
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
			//LOG.info("Out of grid bounds");
			return;
		}
		//LOG.info(Integer.toString(selectedTripEvent.beginCell500X) + Integer.toString(selectedTripEvent.beginCell500Y) + Integer.toString(selectedTripEvent.beginCell250X) + Integer.toString(selectedTripEvent.beginCell250Y));
		
		selectedTripEvent.fareAmount = Double.parseDouble(pieces[11]);
		selectedTripEvent.tipAmount  = Double.parseDouble(pieces[14]);
		
		selectedTripEvent.medallion = pieces[0];
				
		
		if(headTime==null) {
			headTime = new Date();
			headTime.setTime(endTime);
			ringBufferHead = 0; 
			pillarHead = 1;
			//LOG.info("headTime==null");
		}
		else if((endTime - headTime.getTime())/1000 == 0L) {
			pillarHead++;
			//LOG.info("same" + endTime);
		}
		else {
			//LOG.info("Event time less that previous one - New: " + SIMPLE_DATE_FORMAT.parse(pieces[3]) + "    Previous: " + headTime);
			
			pillarRoof[ringBufferHead] = pillarHead-1;
			
			int newRingBufferHead =  (ringBufferHead + (int)((endTime - headTime.getTime())/1000)) % RING_BUFFEER_SIZE;
			//LOG.info((int)((endTime - headTime.getTime())/1000));
			//LOG.info(newRingBufferHead);
			//LOG.info("different" + endTime);
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
			pillarHead = 1;
		}
		
		//UPDATING INTER THREAD COMMUNIICATOR
		combinedHead = pillarHead*100000 + ringBufferHead;
		//LOG.info(ringBufferHead);
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
