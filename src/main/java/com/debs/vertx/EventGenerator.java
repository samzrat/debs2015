package com.debs.vertx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * @author Sameer
 * 
 */
public class EventGenerator extends Verticle {
   ConcurrentMap<String, String> vertxConfigMap = new ConcurrentHashMap<String, String>();
   Logger log = null;

   @Override
   public void start() {
      log = container.logger();
      log.info("Starting server");
      vertxConfigMap = vertx.sharedData().getMap("vertxMap");
      populateConfigs();
      try {
         log.info("File name - " + vertx.sharedData().getMap("vertxConfigMap").get("filename").toString());
         LineIterator it =
                  FileUtils.lineIterator(
                           new File(vertx.sharedData().getMap("vertxConfigMap").get("filename")
                                    .toString()), "UTF-8");
         while (it.hasNext()) {
            log.info(it.next());
            
            /*
             * form the TripEvent obj here
             */
            
            /*
             * Send msgs to ActorOne
             */
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
}
