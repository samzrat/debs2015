package samzrat.debs2015

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map

class CellProfitActor extends Actor with ActorLogging {
import CellProfitActor._
  
  val topTenProfitableCellsActor = context.actorOf(TopTenProfitableCellsActor.props, "topTenProfitableCellsActor")

  val cellProfitMap: Map[Cell,ProfitData] = Map()
  
  def receive = {
	  case AddTripFareToCellProfitMsg(tripEvent: TripEvent) =>
      val cell = tripEvent.grid250Cells.startCell
      cellProfitMap.contains(cell) match {
        case true =>
         cellProfitMap += cell -> ProfitData(cellProfitMap(cell).tripProfit.::(TripProfit(tripEvent.fareAmount + tripEvent.tipAmount, tripEvent.medallion)), cellProfitMap(cell).emptyTaxiCount)      
       case false =>
         cellProfitMap += cell -> ProfitData(List(TripProfit(tripEvent.fareAmount + tripEvent.tipAmount, null)), 0)
      }
      val profitability = calculateProfitability(cell: Cell, cellProfitMap(cell))
    case RemoveTripFareFromCellProfitMsg(tripEvent: TripEvent)  =>
      val cell = tripEvent.grid250Cells.startCell
      if(cellProfitMap.contains(cell)==false)
        throw new Exception()
     
      cellProfitMap += cell -> ProfitData(cellProfitMap(cell).tripProfit.filter(_.medallion != tripEvent.medallion), cellProfitMap(cell).emptyTaxiCount)
      val profitability = calculateProfitability(cell: Cell, cellProfitMap(cell))
    case _ => throw new Exception()  
  }	
  
  def calculateProfitability(cell: Cell, profitData: ProfitData): Double = {
    val sortedProfitData = profitData.tripProfit.sortBy(_.profit)
    val medianProfit: Double = sortedProfitData.size%2 match {
      case 0 => (sortedProfitData(sortedProfitData.size/2-1).profit + sortedProfitData(sortedProfitData.size/2).profit)/2
      case _ => sortedProfitData((sortedProfitData.size-1)/2).profit
    }
    val profitability = if (profitData.emptyTaxiCount!=0) medianProfit/profitData.emptyTaxiCount else Double.NaN
    println("Profitability Cell(" + cell.xCell + ", " + cell.yCell + "): " + profitability + "   Median profit=" + medianProfit)
    return profitability
  }  
  
}

object CellProfitActor {
  val props = Props[CellProfitActor]
  
  case class AddTripFareToCellProfitMsg(tripEvent: TripEvent)
  case class RemoveTripFareFromCellProfitMsg(tripEvent: TripEvent)
  
  case class TripProfit(profit: Double, medallion: String)
  case class ProfitData(tripProfit: List[TripProfit], emptyTaxiCount: Int)
  
}