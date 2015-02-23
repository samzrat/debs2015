package com.example

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import java.text.NumberFormat
import java.util.Locale
import java.text.ParsePosition
import java.util.Date
import java.text.ParseException
import java.text.SimpleDateFormat
import scala.collection.mutable.Stack

case class Cell (xCell: Int, yCell: Int)
case class TripCells(startCell: Cell, endCell: Cell)
case class TripEvent (startTime: Date, endTime: Date, grid500Cells: TripCells, grid250Cells: TripCells)
class BufferEntry(var time: Option[Date], var tripEventStack: Stack[TripEvent]) {}

class CircularBufferActor extends Actor with ActorLogging {
  import CircularBufferActor._
  val routeCountActor = context.actorOf(RouteCountActor.props, "routeCountActor")
  
  val circularBufferSize = 1800
  
  val circularBuffer: Array[BufferEntry] = new Array[BufferEntry](circularBufferSize)
  var head_30MinWindow: Int = -1
  var tail_30MinWindow: Int = -1
  
  var head_15MinWindow: Int = -1
  var tail_15MinWindow: Int = -1
  
  init()

  def receive = {
  	case BeginProcessing => 
	    log.info("In PingActor - starting ping-pong")
	    process()
   
  	
  }	
  
  def process() {
        
    
    
    val fileSource = Source.fromFile("sorted_data.csv")
    var newlineCount = 0L
    for (line <- fileSource.getLines) {
      DataExtractor.extractTripEventData(line) match {
        case None =>
        case Some(tripEvent) => 
          routeCountActor ! RouteCountActor.IncrementRouteCountMsg(tripEvent)
          head_30MinWindow match {
            case -1 => 
              assert (tail_30MinWindow== -1 && head_15MinWindow== -1 && tail_15MinWindow== -1, "not -1")
              
              head_30MinWindow = 0
              tail_30MinWindow = 0
              head_15MinWindow = 0
              tail_15MinWindow = 0
              
              circularBuffer(0).time =  Some(tripEvent.endTime)
              
            case _ => 
              ((tripEvent.endTime.getTime() - circularBuffer(head_30MinWindow).time.get.getTime())/1000) match {
                case 0 =>
                  circularBuffer(head_30MinWindow).tripEventStack.push(tripEvent)
                  circularBuffer(head_30MinWindow).time = Some(tripEvent.endTime)
                case timeDiff if timeDiff > 0 => 
                  head_30MinWindow = (head_30MinWindow+timeDiff.toInt) % circularBufferSize
                  circularBuffer(head_30MinWindow).time = Some(tripEvent.endTime)
                  circularBuffer(head_30MinWindow).tripEventStack.push(tripEvent)
                  
                  val oldTailPosition = tail_30MinWindow
                  if((circularBuffer(head_30MinWindow).time.get.getTime() - circularBuffer(tail_30MinWindow).time.get.getTime())/1000 > circularBufferSize ) {
                    val tailJump = ((circularBuffer(head_30MinWindow).time.get.getTime() - circularBuffer(tail_30MinWindow).time.get.getTime())/1000) - circularBufferSize
                    tail_30MinWindow = (tail_30MinWindow+tailJump.toInt) % circularBufferSize
                    circularBuffer(tail_30MinWindow).time.get.setTime(circularBuffer(tail_30MinWindow).time.get.getTime + tailJump.toInt*1000)
                  }
                  
                  if(oldTailPosition < tail_30MinWindow-1) {
                    for(y <- oldTailPosition until tail_30MinWindow) {
                      for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                        val event = circularBuffer(y).tripEventStack.pop()
                        routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                      }
                    }
                  }  
                  else if(oldTailPosition > tail_30MinWindow-1){
                    for(y <- oldTailPosition until circularBufferSize) {
                      for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                        val event = circularBuffer(y).tripEventStack.pop()
                        routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                      }
                    }
                    for(y <- 0 until tail_30MinWindow) {
                      for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                        val event = circularBuffer(y).tripEventStack.pop()
                        routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                      }
                    }
                  }
              }
              case timeDiff if timeDiff < 0 => 
                println("incoming_trip_time LESSER => head_time: " + (circularBuffer(head_30MinWindow)).time + ", incoming_trip_time: " + tripEvent.endTime)
                //ignore
          }    
    
          
      }  
      newlineCount += 1
    }
    println("In CircularBuffer - count is " + newlineCount.toString)
  }
  
  def init() {
    for(i <- 0 until circularBufferSize)
      circularBuffer(i) = new BufferEntry(None, new Stack[TripEvent])
  }
  
  
}

object CircularBufferActor {
  val props = Props[CircularBufferActor]
  case object BeginProcessing

}