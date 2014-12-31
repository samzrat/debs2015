package com.debs.grid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * @author Sameer
 * 
 */
public class LL2XY {

   // private Double sLat = 40.757977;
   // private Double sLong = -73.978165;
   private Double oLat = 41.474937;
   private Double oLong = -74.913585;
   private Double RADIANS = 57.2957795;
   private Double angle = 0.0;
   private static Logger LOG = Logger.getLogger(LL2XY.class);
   private static Map<String, String> ll2xyConfigMap = new HashMap<String, String>();

   public static void main(String[] args) throws IOException {
      Double sLat1 = 40.756775;
      Double sLong1 = -73.989937;

      Double sLat2 = 40.77063;
      Double sLong2 = -73.86525;
      populateConfigs();
      BasicConfigurator.configure();
      LOG.info("Starting");

      LL2XY ll2xy = new LL2XY();
//      ArrayList<String> entries = ll2xy.readFile(ll2xyConfigMap.get("grid.filename").toString());
//      for (int i = 1; i < entries.size(); i++) {
//         String entry = entries.get(i);
//         String[] pieces = entry.split(",");
//         sLat1 = Double.parseDouble(pieces[11]);
//         sLong1 = Double.parseDouble(pieces[10]);
//         sLat2 = Double.parseDouble(pieces[13]);
//         sLong2 = Double.parseDouble(pieces[12]);
//
//         XY xy1 = ll2xy.computeLL2XY(sLat1, sLong1);
//         XY xy2 = ll2xy.computeLL2XY(sLat2, sLong2);
//
//         Double distance = (ll2xy.getEuclideanDistance(xy1, xy2)) / 1000;
//         Cell cell1 = ll2xy.getCellID(xy1);
//         Cell cell2 = ll2xy.getCellID(xy2);
//         LOG.info(distance + "\t" + cell1.xCell + "," + cell1.yCell + "\t" + cell2.xCell + ","
//                  + cell2.yCell);
//      }
       XY xy1 = ll2xy.computeLL2XY(sLat1, sLong1);
       XY xy2 = ll2xy.computeLL2XY(sLat2, sLong2);
      
       Double distance = ll2xy.getEuclideanDistance(xy1, xy2);
       LOG.info(xy1);
       LOG.info(xy2);
       LOG.info(distance);
       LOG.info("Done!!!");
   }

   public XY computeLL2XY(Double srcLat, Double srcLong) {
      Double xx = (srcLong - oLong) * metersDegLong(oLat);
      Double yy = (srcLat - oLat) * metersDegLat(oLat);

      Double r = Math.sqrt(xx * xx + yy * yy);

      Double ct = xx / r;
      Double st = yy / r;

      xx = r * ((ct * Math.cos(angle)) + (st * Math.sin(angle)));
      yy = r * ((st * Math.cos(angle)) - (ct * Math.sin(angle)));

      return new XY(xx, yy);
   }

   private Double metersDegLong(Double x) {
      Double d2r = degToRadians(x);
      return ((111415.13 * Math.cos(d2r)) - (94.55 * Math.cos(3.0 * d2r)) + (0.12 * Math
               .cos(5.0 * d2r)));
   }

   private Double metersDegLat(Double x) {
      Double d2r = degToRadians(x);
      return (111132.09 - (566.05 * Math.cos(2.0 * d2r)) + (1.20 * Math.cos(4.0 * d2r)) - (0.002 * Math
               .cos(6.0 * d2r)));
   }

   private Double degToRadians(Double x) {
      return x / RADIANS;
   }

   public Double getEuclideanDistance(XY xy1, XY xy2) {
      /*
       * returns distance in meters
       */
      return Math.sqrt(((xy2.xx - xy1.xx) * (xy2.xx - xy1.xx))
               + ((xy2.yy - xy1.yy) * (xy2.yy - xy1.yy)));
   }

   private ArrayList<String> readFile(String fname) throws IOException {
      ArrayList<String> list = new ArrayList<String>();
      BufferedReader br = new BufferedReader(new FileReader(new File(fname)));
      String line = "";
      while ((line = br.readLine()) != null) {
         list.add(line);
      }

      LOG.info("Read " + list.size() + " entries");
      return list;
   }

   private Cell getCellID(XY xy) {
      Long x = Math.round(xy.xx / 500);
      Long y = Math.round(xy.yy / 500);
      return new Cell(x, y);
   }

   class XY {
      Double xx;
      Double yy;

      public XY(Double x, Double y) {
         xx = x;
         yy = y;
      }

      @Override
      public String toString() {
         StringBuilder sBuilder = new StringBuilder();
         sBuilder.append("(xx,yy) -> (").append(xx).append(", ").append(yy).append(")");
         return sBuilder.toString();
      }
   }

   class Cell {
      Long xCell;
      Long yCell;

      public Cell(Long x, Long y) {
         xCell = x;
         yCell = y;
      }

      @Override
      public String toString() {
         StringBuilder sBuilder = new StringBuilder();
         sBuilder.append("(xCell,yCell) -> (").append(xCell).append(", ").append(yCell).append(")");
         return sBuilder.toString();
      }
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
