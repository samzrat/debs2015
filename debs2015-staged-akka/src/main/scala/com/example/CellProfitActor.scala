package com.example

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map

class CellProfitActor extends Actor with ActorLogging {
  import CellProfitActor._
  
  val topTenProfitableCellsActor = context.actorOf(TopTenProfitableCellsActor.props, "topTenProfitableCellsActor")

  
  def receive = {
  	case None => 
	  
  }	
  
  
  
}

object CellProfitActor {
  val props = Props[CellProfitActor]
  
}