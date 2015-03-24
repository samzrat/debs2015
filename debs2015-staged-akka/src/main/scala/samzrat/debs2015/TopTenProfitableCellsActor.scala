package samzrat.debs2015

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class TopTenProfitableCellsActor extends Actor with ActorLogging {
import TopTenProfitableCellsActor._
  
   
  var bestProfitability = 0
  var tenthBestProfitability = 0 
  
  var topCellsList = ListBuffer[Tuple2[Cell, Double]]()
  
  def receive = {
    case PossibleTopperMsg(potentialCell: Cell, potentialCellProfitability: Double) =>
      topCellsList += Tuple2(potentialCell, potentialCellProfitability)
      topCellsList.sortBy(_._2).take(10)
        
  } 
  
}

object TopTenProfitableCellsActor {
  val props = Props[TopTenProfitableCellsActor]
  case class PossibleTopperMsg(potentialCell: Cell, potentialCellProfitability: Double)
}