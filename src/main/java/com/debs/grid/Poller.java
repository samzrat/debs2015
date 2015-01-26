package com.debs.grid;

import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * @author Sameer
 * 
 */
public class Poller implements Runnable {
   private Date startTime;
   private Date endTime;
   private static final Logger LOG = Logger.getLogger(Poller.class);

   @Override
   public void run() {
      try {
         endTime = Frame.tripEvent.endTime;

         startTime = new Date(endTime.getTime() - 30 * 60000);

         LOG.info("Invoking poller(), size of current event list = " + Frame.currentEvents.size());
         Iterator<TripEvent> itr = Frame.currentEvents.iterator();
         synchronized (Frame.currentEvents) {

            while (itr.hasNext()) {

               TripEvent tripEvent = itr.next();
               if (tripEvent.endTime.before(startTime)) {
                  itr.remove();

                  String key = Frame.formKey(tripEvent);

                  if (Frame.getRouteMap().containsKey(key)) {

                     Frame.getRouteMap().put(key, Frame.getRouteMap().get(key) - 1);
                     if (Frame.getRouteMap().get(key) == 0) {
                        LOG.info("Removing key - " + key);
                        Frame.getRouteMap().remove(key);
                     }
                  }
               }
            }
         }
         LOG.info("poller() completed");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
