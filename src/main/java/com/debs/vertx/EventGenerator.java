package com.debs.vertx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.debs.grid.LL2XY;
import com.debs.grid.LL2XY.Cell;
import com.debs.grid.LL2XY.XY;
import com.debs.grid.TripEvent;

/**
 * @author Sameer
 * 
 */
public class EventGenerator extends Verticle {
   ConcurrentMap<String, String> vertxConfigMap = new ConcurrentHashMap<String, String>();
   static Logger log = null;
   private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(
            "M/dd/yyyy hh:mm");

   @Override
   public void start() {
      EventBus eventBus = vertx.eventBus();
      eventBus.registerHandler("actorone", new ActorOne());
      log = container.logger();
      log.info("Starting server");
      vertxConfigMap = vertx.sharedData().getMap("vertxMap");
      populateConfigs();
      LL2XY ll2xy = new LL2XY();
      try {
         log.info("File name - "
                  + vertx.sharedData().getMap("vertxConfigMap").get("filename").toString());
         LineIterator it =
                  FileUtils.lineIterator(
                           new File(vertx.sharedData().getMap("vertxConfigMap").get("filename")
                                    .toString()), "UTF-8");
         while (it.hasNext()) {
            String line = it.next();
            /*
             * form the TripEvent obj here
             */
            Double sLat1 = 0.0;
            Double sLong1 = 0.0;

            Double sLat2 = 0.0;
            Double sLong2 = 0.0;
            String[] pieces = line.split(",");
            sLat1 = Double.parseDouble(pieces[11]);
            sLong1 = Double.parseDouble(pieces[10]);
            sLat2 = Double.parseDouble(pieces[13]);
            sLong2 = Double.parseDouble(pieces[12]);

            XY xy1 = ll2xy.computeLL2XY(sLat1, sLong1);
            XY xy2 = ll2xy.computeLL2XY(sLat2, sLong2);
            Double distance = (ll2xy.getEuclideanDistance(xy1, xy2)) / 1000;
            Cell cell1 = ll2xy.getCellID(xy1);
            Cell cell2 = ll2xy.getCellID(xy2);

            String start = pieces[5];
            String end = pieces[6];
            TripEvent tripEvent =
                     new TripEvent(extractDateTime(start), extractDateTime(end), cell1, cell2,
                              distance);
            /*
             * Send msgs to ActorOne
             */

            eventBus.send("actorone", tripEvent.toString(), new Handler<Message<String>>() {

               @Override
               public void handle(Message<String> reply) {
                  log.info("Reply from ActorOne -> " + reply.body());
               }
            });
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      log.info("Stopping server");
   }

   private void populateConfigs() {
      log.info("Populating configs");
      Properties properties = new Properties();
      FileInputStream input = null;
      try {
         input = new FileInputStream("conf//config.properties");
         properties.load(input);

         for (String key : properties.stringPropertyNames()) {

            if (key.startsWith("grid")) {
               vertx.sharedData().getMap("vertxConfigMap")
                        .put(key.split("\\.")[1], properties.getProperty(key));
               log.info("Result - "
                        + vertx.sharedData().getMap("vertxConfigMap").get("filename").toString());
            }
         }
      } catch (Exception e) {
         log.error("No config file found, exitting...");
         e.printStackTrace();
         System.exit(1);
      }
      log.info("Populating configs done!");
   }

   private static Date extractDateTime(String dateTimeStr) {
      try {
         return SIMPLE_DATE_FORMAT.parse(dateTimeStr);
      } catch (ParseException e) {
         log.error("Invalid date - " + dateTimeStr);
         e.printStackTrace();
         return null;
      }
   }
}
