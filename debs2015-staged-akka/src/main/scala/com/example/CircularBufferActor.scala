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
  val cellProfitActor = context.actorOf(CellProfitActor.props, "cellProfitActor")
  
  val circularBufferSize = 1800
  
  val circularBuffer: Array[BufferEntry] = new Array[BufferEntry](circularBufferSize)
  var head: Int = -1
  var tail_30MinWindow: Int = -1
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
          head match {
            case -1 => 
              assert (tail_30MinWindow== -1 && tail_15MinWindow== -1, "not -1")
              
              head = 0
              tail_30MinWindow = 0
              tail_15MinWindow = 0
              
              circularBuffer(0).time =  Some(tripEvent.endTime)
              
            case _ => 
              //println(tripEvent.endTime +  "         " +  circularBuffer(head).time.get)
              ((tripEvent.endTime.getTime() - circularBuffer(head).time.get.getTime())/1000) match {
                case timeDiff if timeDiff < 0 => 
                //println("incoming_trip_time LESSER => head_time: " + (circularBuffer(head)).time + ", incoming_trip_time: " + tripEvent.endTime)
                //ignore
                case 0L =>
                  circularBuffer(head).tripEventStack.push(tripEvent)
                  //circularBuffer(head).time = Some(tripEvent.endTime)
                case timeDiff if timeDiff > 0 => 
                  val oldHead = head
                  head = (head+timeDiff.toInt) % circularBufferSize
                  if(oldHead < head) {
                    for(i <- oldHead+1 to head)
                      circularBuffer(i).time = Some(new Date(circularBuffer(i-1).time.get.getTime + 1000))
                  }
                  else
                  {
                    var j = 0
                    for(i <- oldHead+1 until circularBufferSize)
                      circularBuffer(i).time = Some(new Date(circularBuffer(i-1).time.get.getTime + 1000))
                    circularBuffer(0).time = Some(new Date(circularBuffer(circularBufferSize-1).time.get.getTime + 1000))  
                    for(i <- 1 to head)
                      circularBuffer(i).time = Some(new Date(circularBuffer(i-1).time.get.getTime + 1000))
                  }
                  circularBuffer(head).tripEventStack.push(tripEvent)
                  
                  //println(head)
                  
                  val old15TailPosition = tail_15MinWindow
                  if((circularBuffer(head).time.get.getTime() - circularBuffer(tail_15MinWindow).time.get.getTime())/1000 > (circularBufferSize/2) ) {
                    val tailJump = ((circularBuffer(head).time.get.getTime() - circularBuffer(tail_15MinWindow).time.get.getTime())/1000) - (circularBufferSize/2)
                    tail_15MinWindow = (tail_15MinWindow+tailJump.toInt) % circularBufferSize
                    //circularBuffer(tail_15MinWindow).time.get.setTime(circularBuffer(tail_15MinWindow).time.get.getTime + tailJump.toInt*1000)
                  }
                  
                  if(old15TailPosition < tail_15MinWindow-1) {
                    for(y <- old15TailPosition until tail_15MinWindow) {
                      val clonedStack = circularBuffer(y).tripEventStack.clone()
                      for(z <- 0 until clonedStack.size) {
                        val event = clonedStack.pop()
                        cellProfitActor ! None
                      }
                    }
                  }  
                  else if(old15TailPosition > tail_15MinWindow-1){
                    for(y <- old15TailPosition until circularBufferSize) {
                      val clonedStack = circularBuffer(y).tripEventStack.clone()
                      for(z <- 0 until clonedStack.size) {
                        val event = clonedStack.pop()
                        cellProfitActor ! None
                      }
                    }
                    for(y <- 0 until tail_15MinWindow) {
                      val clonedStack = circularBuffer(y).tripEventStack.clone()
                      for(z <- 0 until clonedStack.size) {
                        val event = clonedStack.pop()
                        cellProfitActor ! None
                      }
                    }
                  }
                  //println(tail_15MinWindow)
                  
                  
                  val old30TailPosition = tail_30MinWindow
                  //println((tripEvent.endTime.getTime() - circularBuffer(tail_30MinWindow).time.get.getTime())/1000)
                  //print("    " + circularBuffer(tail_30MinWindow).time.get.getTime())
                  //print("    " + circularBuffer(head).time.get.getTime())
                  if((circularBuffer(head).time.get.getTime() - circularBuffer(tail_30MinWindow).time.get.getTime())/1000 > (circularBufferSize) ) {
                    val tailJump = ((circularBuffer(head).time.get.getTime() - circularBuffer(tail_30MinWindow).time.get.getTime())/1000) - (circularBufferSize)
                    tail_30MinWindow = (tail_30MinWindow+tailJump.toInt) % circularBufferSize
                    //circularBuffer(tail_30MinWindow).time.get.setTime(circularBuffer(tail_30MinWindow).time.get.getTime + tailJump.toInt*1000)
                  }
                  
                  if(old30TailPosition < tail_30MinWindow-1) {
                    for(y <- old30TailPosition until tail_30MinWindow) {
                      for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                        val event = circularBuffer(y).tripEventStack.pop()
                        routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                      }
                    }
                  }  
                  else if(old30TailPosition > tail_30MinWindow-1){
                    for(y <- old30TailPosition until circularBufferSize) {
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
                  println(tail_30MinWindow)
              }
              
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