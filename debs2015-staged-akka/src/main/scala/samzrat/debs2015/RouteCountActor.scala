package samzrat.debs2015

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map

case class Route(startCell: Cell, endCell: Cell)

class RouteCountActor extends Actor with ActorLogging {
import RouteCountActor._
  
  val topTenRoutesActor = context.actorOf(TopTenRoutesActor.props, "topTenRoutesActor")
  
  val routeCountMap: Map[Route,Int] = Map()

  var bestCount = 0
  var tenthBestCount = 0 
  
  def receive = {
  	case IncrementRouteCountMsg(tripEvent: TripEvent) => 
	   //log.info("Increment")
	   val route = Route(tripEvent.grid500Cells.startCell, tripEvent.grid500Cells.endCell)
	   routeCountMap.contains(route) match {
	     case true => 
	       routeCountMap += route -> (routeCountMap(route)+1)
	     case false =>
	       routeCountMap += route -> 1
	   }
	   //println(routeCountMap(route))
	   if(routeCountMap(route)>= tenthBestCount-5) 
	     topTenRoutesActor ! TopTenRoutesActor.PossibleTopperMsg(route, routeCountMap(route))
	     
	case DecrementRouteCountMsg(tripEvent: TripEvent) => 
	   //log.info("Decrement")
	   val route = Route(tripEvent.grid500Cells.startCell, tripEvent.grid500Cells.endCell)
	   routeCountMap.contains(route) match {
	     case true => 
	       routeCountMap += route -> (routeCountMap(route)-1)
	       if(routeCountMap(route)>= tenthBestCount-5) 
	         topTenRoutesActor ! TopTenRoutesActor.PossibleTopperMsg(route, routeCountMap(route))
	     case false =>
	       throw new Exception()
	   }
	case NewTopRoutesRangeMsg(bestCount: Int, tenthBestCount: Int) =>
	  this.bestCount = bestCount
	  this.tenthBestCount = tenthBestCount
  }	
  
  
  
}

object RouteCountActor {
  val props = Props[RouteCountActor]
  case class IncrementRouteCountMsg(tripEvent: TripEvent)
  case class DecrementRouteCountMsg(tripEvent: TripEvent)
  case class NewTopRoutesRangeMsg(bestCount: Int, tenthBestCount: Int)
}