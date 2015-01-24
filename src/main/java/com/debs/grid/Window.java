package com.debs.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class Window {

   static ConcurrentHashMap<String, Integer> frequencyCube =
            new ConcurrentHashMap<String, Integer>();

   private Date startTime;
   private Date endTime;

   private List<TripEvent> currentEvents = Collections.synchronizedList(new ArrayList<TripEvent>());

   private static Logger LOG = Logger.getLogger(Window.class);
   private static final String DELIMITER = "~";

   public synchronized void addNewTripEvent(TripEvent newTripEvent) {
      endTime = newTripEvent.endTime;

      startTime = (Date) endTime.clone();
      startTime = new Date(startTime.getTime() - (30 * 60000));

      Iterator<TripEvent> itr = currentEvents.iterator();
      while(itr.hasNext()) {
         TripEvent tripEvent = itr.next();
         // System.out.println("(start, end) -> " + startTime + ", " + endTime);
         if (tripEvent.endTime.before(startTime)) {
            itr.remove();

            String key =
                     tripEvent.startCell.xCell.toString() + DELIMITER
                              + tripEvent.startCell.yCell.toString() + DELIMITER
                              + tripEvent.endCell.xCell.toString() + DELIMITER
                              + tripEvent.endCell.yCell.toString();

            if (tripEvent.distance != 0) {
               if (frequencyCube.containsKey(key)) {
                  frequencyCube.put(key, frequencyCube.get(key) - 1);
                  if (frequencyCube.get(key) == 0) {
                     LOG.info("Removing key - " + key);
                     frequencyCube.remove(key);
                  }
               }
            }
         }
      }

      currentEvents.add(newTripEvent);
      String newKey =
               newTripEvent.startCell.xCell.toString() + DELIMITER
                        + newTripEvent.startCell.yCell.toString() + DELIMITER
                        + newTripEvent.endCell.xCell.toString() + DELIMITER
                        + newTripEvent.endCell.yCell.toString();
      if (frequencyCube.containsKey(newKey))
         frequencyCube.put(newKey, frequencyCube.get(newKey) + 1);
      else
         frequencyCube.put(newKey, 1);

      // Iterator iit = frequencyCube.entrySet().iterator();
      // while (iit.hasNext()) {
      // Map.Entry pairs = (Map.Entry) iit.next();
      // LOG.info(pairs.getKey() + " = " + pairs.getValue());
      // }

   }
}
