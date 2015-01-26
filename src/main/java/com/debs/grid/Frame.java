package com.debs.grid;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * @author Sameer
 * 
 */
public class Frame {

   public static ConcurrentHashMap<String, Integer> routeMap =
            new ConcurrentHashMap<String, Integer>();
   public static List<TripEvent> currentEvents = Collections
            .synchronizedList(new ArrayList<TripEvent>());
   private static final Logger LOG = Logger.getLogger(Frame.class);
   private static final String DELIMITER = "~";
   public static Integer eventId = 0;

   private Date startTime;
   private Date endTime;

   public static TripEvent tripEvent = null;

   public static ConcurrentHashMap<String, Integer> getRouteMap() {
      return routeMap;
   }

   public static void setRouteMap(ConcurrentHashMap<String, Integer> routeMap) {
      Frame.routeMap = routeMap;
   }

   public List<TripEvent> getCurentEvents() {
      return currentEvents;
   }

   public synchronized void addNewTripEvent() {
      LOG.info("EventID - " + eventId + ", Size of current event list = " + currentEvents.size());
      endTime = tripEvent.endTime;

      startTime = new Date(endTime.getTime() - 30 * 60000);

      currentEvents.add(tripEvent);
      String newKey = formKey();

      Integer count = routeMap.get(newKey);
      if (count == null) {
         routeMap.put(newKey, 1);
      } else {
         routeMap.put(newKey, ++count);
      }
      try {
         Thread.sleep(10);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public static String formKey() {
      return new StringBuilder().append(tripEvent.startCell.xCell.toString()).append(DELIMITER)
               .append(tripEvent.startCell.yCell.toString()).append(DELIMITER)
               .append(tripEvent.endCell.xCell.toString()).append(DELIMITER)
               .append(tripEvent.endCell.yCell.toString()).toString();
   }

   public static String formKey(TripEvent tripEvent) {
      return new StringBuilder().append(tripEvent.startCell.xCell.toString()).append(DELIMITER)
               .append(tripEvent.startCell.yCell.toString()).append(DELIMITER)
               .append(tripEvent.endCell.xCell.toString()).append(DELIMITER)
               .append(tripEvent.endCell.yCell.toString()).toString();
   }
}
