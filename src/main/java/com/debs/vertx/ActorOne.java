package com.debs.vertx;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

/**
 * @author Sameer
 * 
 */
public class ActorOne implements Handler<Message<String>> {

   static int count = 0;

   @Override
   public void handle(Message<String> event) {
      System.out.println("Received msg -> " + event.body());
      
      // do stuff
      
      // reply if needed
      event.reply("Processed event - " + (++count));
   }

}
