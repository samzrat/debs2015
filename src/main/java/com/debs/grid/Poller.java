package com.debs.grid;

import java.util.Calendar;

/**
 * @author Sameer
 * 
 */
public class Poller implements Runnable {
   private Calendar startTime;
   private Calendar endTime;

   @Override
   public void run() {
      endTime = Frame.tripEvent.endTime;

      startTime = ((Calendar) (endTime.clone()));
      startTime.add(Calendar.HOUR_OF_DAY, -30);

      System.out.println("Invoking poller(), size of current event list = "
               + Frame.currentEvents.size());
      for (TripEvent tripEvent : Frame.currentEvents) {
         if (tripEvent.endTime.compareTo(this.startTime) < 0) {
            Frame.currentEvents.remove(tripEvent);

            String key =
                     tripEvent.startCell.xCell.toString() + tripEvent.startCell.yCell.toString()
                              + tripEvent.endCell.xCell.toString()
                              + tripEvent.endCell.yCell.toString();

            if (tripEvent.distance != 0) {
               if (Frame.getRouteMap().containsKey(key)) {
                  Frame.getRouteMap().put(key, Frame.getRouteMap().get(key) - 1);
                  if (Frame.getRouteMap().get(key) == 0) {
                     System.out.println("Removing key - " + key);
                     Frame.getRouteMap().remove(key);
                  }
               }
            }
         }
      }
      System.out.println("poller() completed");
   }
}
