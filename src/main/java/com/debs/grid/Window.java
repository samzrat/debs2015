//package com.debs.grid;
//
//import java.util.Calendar;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class Window {
//
//   public static ConcurrentHashMap<String, Integer> frequencyCube = new ConcurrentHashMap<String, Integer>();
//
//   private Calendar startTime;
//   private Calendar endTime;
//
//   private List<TripEvent> currentEvents = new ArrayList<TripEvent>();
//
//   public void addNewTripEvent(TripEvent newTripEvent) {
//      endTime = newTripEvent.endTime;
//
//      startTime = ((Calendar) (endTime.clone()));
//      startTime.add(Calendar.HOUR_OF_DAY, -30);
//
//      for (TripEvent tripEvent : currentEvents) {
//         if (tripEvent.endTime.compareTo(this.startTime) < 0) {
//            currentEvents.remove(tripEvent);
//
//            String key =
//                     tripEvent.startCell.xCell.toString() + tripEvent.startCell.yCell.toString()
//                              + tripEvent.endCell.xCell.toString()
//                              + tripEvent.endCell.yCell.toString();
//
//            if (tripEvent.distance != 0) {
//               if (frequencyCube.containsKey(key)) {
//                  frequencyCube.put(key, frequencyCube.get(key) - 1);
//                  if (frequencyCube.get(key) == 0)
//                     frequencyCube.remove(key);
//               }
//            }
//         }
//      }
//
//      currentEvents.add(newTripEvent);
//      String newKey =
//               newTripEvent.startCell.xCell.toString() + newTripEvent.startCell.yCell.toString()
//                        + newTripEvent.endCell.xCell.toString()
//                        + newTripEvent.endCell.yCell.toString();
//      if (frequencyCube.containsKey(newKey))
//         frequencyCube.put(newKey, frequencyCube.get(newKey) + 1);
//      else
//         frequencyCube.put(newKey, 1);
//
//      Iterator iit = frequencyCube.entrySet().iterator();
//      while (iit.hasNext()) {
//         Map.Entry pairs = (Map.Entry) iit.next();
//         System.out.println(pairs.getKey() + " = " + pairs.getValue());
//
//      }
//
//   }
//
//}
