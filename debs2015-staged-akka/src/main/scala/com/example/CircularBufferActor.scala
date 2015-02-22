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
case class BufferEntry(time: Option[Date], tripEventStack: Stack[TripEvent])

class CircularBufferActor extends Actor with ActorLogging {
  import CircularBufferActor._
  val routeCountActor = context.actorOf(RouteCountActor.props, "routeCountActor")
  
  val circularBufferSize = 1800
  
  val circularBuffer: Array[BufferEntry] = new Array[BufferEntry](circularBufferSize)
  var head_30MinWindow: Int = -1
  val tail_30MinWindow: Int = -1
  
  var head_15MinWindow: Int = -1
  val tail_15MinWindow: Int = -1
  
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
/*      DataExtractor.extractTripEventData(line) match {
        case None =>
        case Some(tripEvent) => {
          routeCountActor ! RouteCountActor.IncrementRouteCountMsg(tripEvent)
        }  
      }
  */    
 /*     

        routeCountActor ! RouteCountActor.IncrementRouteCountMsg(tripEvent.get)
        head.position match {
          case -1 => 
            head.position = 0
            head.time = Some(tripEvent.get.endTime)
            tail.position = 0
            tail.time = Some(tripEvent.get.endTime)
            println("Head= " + head.position + "      Tail= " + tail.position)
          case  _ => 
            ((tripEvent.get.endTime.getTime() - head.time.get.getTime())/1000) match {
              case 0   => 
                //println("Same data => head_time: " + head.time + ", incoming_trip_time: " + tripEvent.endTime)
                circularBuffer(head.position).push(tripEvent.get)
                head.time = Some(tripEvent.get.endTime)
              case x if x > 0 => 
                //println("X=" + x + " incoming_trip_time GREATER => head_time: " + head.time + ", incoming_trip_time: " + tripEvent.endTime)
                
                head.time = Some(tripEvent.get.endTime)
                head.position = (head.position+x.toInt) % circularBufferSize
                circularBuffer(head.position).push(tripEvent.get)
                val oldTailPosition = tail.position 
                //println("Date comparision = " + (head.time.get.getTime() - tail.time.get.getTime())/1000 + "            New Event date = " + tripEvent.endTime)
                if((head.time.get.getTime() - tail.time.get.getTime())/1000 > circularBufferSize )
                {
                   val tailJump = ((head.time.get.getTime() - tail.time.get.getTime())/1000) - circularBufferSize
                   tail.time.get.setTime(tail.time.get.getTime + tailJump.toInt*1000) 
                   tail.position = (tail.position+tailJump.toInt) % circularBufferSize
                }
              if(oldTailPosition < tail.position-1) {
                for(y <- oldTailPosition until tail.position) {
                  val stackSize = circularBuffer(y).size
                  for(z <- 0 to stackSize-1) {
                    val event = circularBuffer(y).pop()
                    //println("pop")
                    routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                  }
                }
              }
              else if(oldTailPosition > tail.position-1){
                for(y <- oldTailPosition until circularBufferSize) {
                  val stackSize = circularBuffer(y).size
                  for(z <- 0 to stackSize-1) {
                    val event = circularBuffer(y).pop()
                    //println("pop")
                    routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                  }
                }
                for(y <- 0 until tail.position) {
                  val stackSize = circularBuffer(y).size
                  for(z <- 0 to stackSize-1) {
                    val event = circularBuffer(y).pop()
                    //println("pop")
                    routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                  }
                }
              }
              //println("Head=" + head.position + "      Tail=" + tail.position + "      Head_time= " + head.time.get + "      Tail_time= " + tail.time.get + "      Time_diff= " + ((head.time.get.getTime() - tail.time.get.getTime())/1000))
                
            
              case x if x < 0 => 
                //println("incoming_trip_time LESSER => head_time: " + head.time + ", incoming_trip_time: " + tripEvent.endTime)
                //ignore
                
            }
            
            
            
            
        }
     */
        
        newlineCount += 1
    }
    log.info("In CircularBuffer - count is " + newlineCount.toString)
  }
  
  def init() {
    for(i <- 0 until circularBufferSize)
      circularBuffer(i) = BufferEntry(None, new Stack[TripEvent])
  }
  
  
}

object CircularBufferActor {
  val props = Props[CircularBufferActor]
  case object BeginProcessing

}