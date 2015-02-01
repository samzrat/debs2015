package com.debs.vertx;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

/**
 * @author Sameer
 *
 */
public class ActorOne implements Handler<Message<String>>{

   @Override
   public void handle(Message<String> event) {
      // TODO Auto-generated method stub
      event.body();
      //do stuff
   }

}

