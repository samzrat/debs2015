package samzrat.debs2015

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map
import samzrat.debs2015.TopTenProfitableCellsActor._

class CellProfitActor extends Actor with ActorLogging {
import CellProfitActor._
  
  val topTenProfitableCellsActor = context.actorOf(TopTenProfitableCellsActor.props, "topTenProfitableCellsActor")

  val cellProfitMap: Map[Cell,ProfitData] = Map()
  
  def receive = {
    case IncrementEmptyTaxiMsg(tripEvent: TripEvent) =>
      //println("IncrementEmptyTaxiMsg")
      val cell = tripEvent.grid250Cells.endCell
      //println("IncrementEmptyTaxiMsg endCell (" + cell.xCell + ", " + cell.yCell + ")")
      cellProfitMap.contains(cell) match {
        case true =>
         cellProfitMap += cell -> ProfitData(cellProfitMap(cell).tripProfitList, cellProfitMap(cell).emptyTaxiCount+1)      
       case false =>
         cellProfitMap += cell -> ProfitData(List(), 1)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              
      }
      val profitability = calculateProfitability(cell: Cell, cellProfitMap(cell))
      topTenProfitableCellsActor ! PossibleTopperMsg(cell, profitability)

    case DecrememtEmptyTaxiMsg(tripEvent: TripEvent) =>
      //println("DecrememtEmptyTaxiMsg")
      val cell = tripEvent.grid250Cells.endCell
      cellProfitMap.contains(cell) match {
        case true =>
         if(cellProfitMap(cell).emptyTaxiCount==0) throw new Exception()
         cellProfitMap += cell -> ProfitData(cellProfitMap(cell).tripProfitList, cellProfitMap(cell).emptyTaxiCount-1)      
       case false =>
         throw new Exception()
      }
      val profitability = calculateProfitability(cell: Cell, cellProfitMap(cell))  
      topTenProfitableCellsActor ! PossibleTopperMsg(cell, profitability)
	  case AddTripFareToCellProfitMsg(tripEvent: TripEvent) =>
      val cell = tripEvent.grid250Cells.startCell
      //println("AddTripFareToCellProfitMsg startCell (" + cell.xCell + ", " + cell.yCell + ")")
      cellProfitMap.contains(cell) match {                                                                                                                                                                                                                                                                                                                                                                                                                      
        case true =>
         cellProfitMap += cell -> ProfitData(cellProfitMap(cell).tripProfitList.::(TripProfit(tripEvent.fareAmount + tripEvent.tipAmount, tripEvent.medallion)), cellProfitMap(cell).emptyTaxiCount)      
       case false =>
         cellProfitMap += cell -> ProfitData(List(TripProfit(tripEvent.fareAmount + tripEvent.tipAmount, null)), 0)
      }
      val profitability = calculateProfitability(cell: Cell, cellProfitMap(cell))
      topTenProfitableCellsActor ! PossibleTopperMsg(cell, profitability)
    case RemoveTripFareFromCellProfitMsg(tripEvent: TripEvent)  =>
      //println("RemoveTripFareFromCellProfitMsg")
      val cell = tripEvent.grid250Cells.startCell
      if(cellProfitMap.contains(cell)==false)
        throw new Exception()
      
      cellProfitMap += cell -> ProfitData(cellProfitMap(cell).tripProfitList.filter(_.medallion != tripEvent.medallion), cellProfitMap(cell).emptyTaxiCount)
      val profitability = calculateProfitability(cell: Cell, cellProfitMap(cell))
      topTenProfitableCellsActor ! PossibleTopperMsg(cell, profitability)
    case _ => throw new Exception()  
  }	
  
  def calculateProfitability(cell: Cell, profitData: ProfitData): Double = {
    val sortedProfitData = profitData.tripProfitList.sortBy(_.profit)
    val medianProfit: Double = 
      if(sortedProfitData.size==0) Double.NaN 
      else if(sortedProfitData.size==1) sortedProfitData(0).profit
      else 
        sortedProfitData.size%2 match {
        case 0 => (sortedProfitData(sortedProfitData.size/2-1).profit + sortedProfitData(sortedProfitData.size/2).profit)/2
        case _ => sortedProfitData((sortedProfitData.size-1)/2).profit
      }
    val profitability = if (profitData.emptyTaxiCount!=0 && !medianProfit.isNaN) medianProfit/profitData.emptyTaxiCount else Double.NaN
    //if(!medianProfit.isNaN && profitData.emptyTaxiCount!=0)
      println("Profitability Cell(" + cell.xCell + ", " + cell.yCell + "): " + profitability + "   Median profit=" + medianProfit + "   EmptyTaxiCount=" + profitData.emptyTaxiCount)
    return profitability
  }  
  
}

object CellProfitActor {
  val props = Props[CellProfitActor]
  
  case class AddTripFareToCellProfitMsg(tripEvent: TripEvent)
  case class RemoveTripFareFromCellProfitMsg(tripEvent: TripEvent)
  case class IncrementEmptyTaxiMsg(tripEvent: TripEvent)
  case class DecrememtEmptyTaxiMsg(tripEvent: TripEvent)
  
  case class TripProfit(profit: Double, medallion: String)
  case class ProfitData(tripProfitList: List[TripProfit], emptyTaxiCount: Int)
  
}