package samzrat.debs2015;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
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

public class ApplicationMain {

	private static Map<String, String> ll2xyConfigMap = new HashMap<String, String>();
	private static Logger LOG = Logger.getLogger(ApplicationMain.class);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss");
	static int[][] routeCountArray = new int[300][300];
	static CellProfitInfo[][] cellProfitArray = new CellProfitInfo[600][600];
	static TripEvent[][] ringBuffer = new TripEvent[3600][500];
	
	public static void main(String[] args) throws Exception {
		
		populateConfigs();
		BasicConfigurator.configure();
		
		LOG.info("Starting initialization");
		
		for(int i=0; i<300; i++)
			for(int j=0; j<300; j++)
				routeCountArray[i][j] = 1;
		
		for(int i=0; i<600; i++)
			for(int j=0; j<600; j++) {
				cellProfitArray[i][j] = new CellProfitInfo();
				for(int k=0; k<100; k++)
					cellProfitArray[i][j].routeCountArray[k] = new RouteProfit();
			}

		for(int i=0; i<3600; i++)
			for(int j=0; j<500; j++)
				ringBuffer[i][j] = new TripEvent();
		
		
		LOG.info("Starting file reading");
		LineIterator it = FileUtils.lineIterator(new File(ll2xyConfigMap.get("grid.filename").toString()), "UTF-8");
		Integer lineCount = 0;
	      while (it.hasNext()) {
	    	  try {
	            ++lineCount;
	            extractData(new TripEvent(), it);
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
	
	private static void extractData(TripEvent toBeFilledTripEvent, LineIterator it) throws Exception {
		String[] pieces = it.nextLine().split(",");
	
		
		toBeFilledTripEvent.beginCell500X = 
				(int) ((Double.parseDouble(pieces[6])+74.916578)/0.005986) + 1;
		toBeFilledTripEvent.beginCell500Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[7]))/0.004491556) + 1;
		
		toBeFilledTripEvent.endCell500X = 
				(int) ((Double.parseDouble(pieces[8])+74.916578)/0.005986) + 1;
		toBeFilledTripEvent.endCell500Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[9]))/0.004491556) + 1;
		//LOG.info("500: (" + toBeFilledTripEvent.beginCell500X + ", " + toBeFilledTripEvent.beginCell500Y + ")  (" + toBeFilledTripEvent.endCell500X + ", " + toBeFilledTripEvent.endCell500Y + ")");

		toBeFilledTripEvent.beginCell250X = 
				(int) ((Double.parseDouble(pieces[6])+74.916578)/0.002993) + 1;
		toBeFilledTripEvent.beginCell250Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[7]))/0.002245778) + 1;
		
		toBeFilledTripEvent.endCell250X = 
				(int) ((Double.parseDouble(pieces[8])+74.916578)/0.002993) + 1;
		toBeFilledTripEvent.endCell250Y = 
				(int) ((41.477182778-Double.parseDouble(pieces[9]))/0.002245778) + 1;
		//LOG.info("250: (" + toBeFilledTripEvent.beginCell250X + ", " + toBeFilledTripEvent.beginCell250Y + ")  (" + toBeFilledTripEvent.endCell250X + ", " + toBeFilledTripEvent.endCell250Y + ")");

		
		toBeFilledTripEvent.fareAmount = Double.parseDouble(pieces[11]);
		toBeFilledTripEvent.tipAmount  = Double.parseDouble(pieces[14]);
		
		toBeFilledTripEvent.startTime.setTime(SIMPLE_DATE_FORMAT.parse(pieces[2]).getTime());
		toBeFilledTripEvent.endTime.setTime(SIMPLE_DATE_FORMAT.parse(pieces[3]).getTime());

		toBeFilledTripEvent.medallion0 = pieces[0].charAt(0);
		toBeFilledTripEvent.medallion1 = pieces[0].charAt(1);
		toBeFilledTripEvent.medallion2 = pieces[0].charAt(2);
		toBeFilledTripEvent.medallion3 = pieces[0].charAt(3);
		toBeFilledTripEvent.medallion4 = pieces[0].charAt(4);
		toBeFilledTripEvent.medallion5 = pieces[0].charAt(5);
		toBeFilledTripEvent.medallion6 = pieces[0].charAt(6);
		toBeFilledTripEvent.medallion7 = pieces[0].charAt(7);
		toBeFilledTripEvent.medallion8 = pieces[0].charAt(8);
		toBeFilledTripEvent.medallion9 = pieces[0].charAt(9);
		toBeFilledTripEvent.medallion10 = pieces[0].charAt(10);
		toBeFilledTripEvent.medallion11 = pieces[0].charAt(11);
		toBeFilledTripEvent.medallion12 = pieces[0].charAt(12);
		toBeFilledTripEvent.medallion13 = pieces[0].charAt(13);
		toBeFilledTripEvent.medallion14 = pieces[0].charAt(14);
		toBeFilledTripEvent.medallion15 = pieces[0].charAt(15);
		toBeFilledTripEvent.medallion16 = pieces[0].charAt(16);
		toBeFilledTripEvent.medallion17 = pieces[0].charAt(17);
		toBeFilledTripEvent.medallion18 = pieces[0].charAt(18);
		toBeFilledTripEvent.medallion19 = pieces[0].charAt(19);
		toBeFilledTripEvent.medallion20 = pieces[0].charAt(20);
		toBeFilledTripEvent.medallion21 = pieces[0].charAt(21);
		toBeFilledTripEvent.medallion22 = pieces[0].charAt(22);
		toBeFilledTripEvent.medallion23 = pieces[0].charAt(23);
		toBeFilledTripEvent.medallion24 = pieces[0].charAt(24);
		toBeFilledTripEvent.medallion25 = pieces[0].charAt(25);
		toBeFilledTripEvent.medallion26 = pieces[0].charAt(26);
		toBeFilledTripEvent.medallion27 = pieces[0].charAt(27);
		toBeFilledTripEvent.medallion28 = pieces[0].charAt(28);
		toBeFilledTripEvent.medallion29 = pieces[0].charAt(29);
		toBeFilledTripEvent.medallion30 = pieces[0].charAt(30);
		toBeFilledTripEvent.medallion31 = pieces[0].charAt(31);
		
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
