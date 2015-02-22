package com.example

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class TopTenProfitableCellsActor extends Actor with ActorLogging {
  import TopTenProfitableCellsActor._
  
   
  
  
  def receive = {
    case None => 
  
  
  } 
  
}

object TopTenProfitableCellsActor {
  val props = Props[TopTenProfitableCellsActor]
  
}