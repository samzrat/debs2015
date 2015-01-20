package com.debs.grid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sameer
 * 
 */
public class Frame implements Runnable {

   public static ConcurrentHashMap<String, Integer> routeMap =
            new ConcurrentHashMap<String, Integer>();
   public static List<TripEvent> currentEvents = Collections.synchronizedList(new ArrayList<TripEvent>());
   private Calendar startTime;
   private Calendar endTime;

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

   public void setCurentEvents(List<TripEvent> curentEvents) {
      this.currentEvents = curentEvents;
   }

   public Calendar getStartTime() {
      return startTime;
   }

   public void setStartTime(Calendar startTime) {
      this.startTime = startTime;
   }

   public Calendar getEndTime() {
      return endTime;
   }

   public void setEndTime(Calendar endTime) {
      this.endTime = endTime;
   }

   public TripEvent getNewTripEvent() {
      return tripEvent;
   }

   public void setNewTripEvent(TripEvent tripEvent) {
      this.tripEvent = tripEvent;
   }

   @Override
   public void run() {
      System.out.println("Size of current event list = " + currentEvents.size());
      endTime = tripEvent.endTime;

      startTime = ((Calendar) (endTime.clone()));
      startTime.add(Calendar.HOUR_OF_DAY, -30);

      currentEvents.add(tripEvent);
      String newKey =
               tripEvent.startCell.xCell.toString() + tripEvent.startCell.yCell.toString()
                        + tripEvent.endCell.xCell.toString()
                        + tripEvent.endCell.yCell.toString();

      Integer count = routeMap.get(newKey);
      if (count == null) {
         routeMap.put(newKey, 1);
      } else {
         routeMap.put(newKey, ++count);
      }
      
   }
}
