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

case class Cell (xCell: Long, yCell: Long)
case class XY (xx: Double, yy: Double)
case class TripEvent (startTime: Date, endTime: Date, startCell: Cell, endCell: Cell, distance: Double)
class CircularBufferPointer(var position: Int, var time: Option[Date]) {}

class CircularBufferActor extends Actor with ActorLogging {
  import CircularBufferActor._
  val routeCountActor = context.actorOf(RouteCountActor.props, "routeCountActor")
  
  val circularBufferSize = 1800
  
  val circularBuffer: Array[Stack[TripEvent]] = new Array[Stack[TripEvent]](circularBufferSize)
  var head: CircularBufferPointer = new CircularBufferPointer(-1, None)
  val tail: CircularBufferPointer = new CircularBufferPointer(-1, None)
  

  def receive = {
  	case BeginProcessing => 
	    log.info("In PingActor - starting ping-pong")
	    process()
   
  	
  }	
  
  def process() {
        
    init()
    
    val fileSource = Source.fromFile("sorted_data.csv")
    var newlineCount = 0L
    for (line <- fileSource.getLines) {
      
        val tripEvent = DataExtractor.extractTripEventData(line)
      
        routeCountActor ! RouteCountActor.IncrementRouteCountMsg(tripEvent)
        head.position match {
          case -1 => 
            head.position = 0
            head.time = Some(tripEvent.endTime)
            tail.position = 0
            tail.time = Some(tripEvent.endTime)
            println("Head= " + head.position + "      Tail= " + tail.position)
          case  _ => 
            ((tripEvent.endTime.getTime() - head.time.get.getTime())/1000) match {
              case 0   => 
                //println("Same data => head_time: " + head.time + ", incoming_trip_time: " + tripEvent.endTime)
                circularBuffer(head.position).push(tripEvent)
                head.time = Some(tripEvent.endTime)
              case x if x > 0 => 
                //println("X=" + x + " incoming_trip_time GREATER => head_time: " + head.time + ", incoming_trip_time: " + tripEvent.endTime)
                
                head.time = Some(tripEvent.endTime)
                head.position = (head.position+x.toInt) % circularBufferSize
                circularBuffer(head.position).push(tripEvent)
                val oldTailPosition = tail.position 
                //println("Date comparision = " + (head.time.get.getTime() - tail.time.get.getTime())/1000 + "            New Event date = " + tripEvent.endTime)
                if((head.time.get.getTime() - tail.time.get.getTime())/1000 > circularBufferSize )
                {
                   val tailJump = ((head.time.get.getTime() - tail.time.get.getTime())/1000) - circularBufferSize
                   tail.time.get.setTime(tail.time.get.getTime + tailJump.toInt*1000) 
                   tail.position = (tail.position+tailJump.toInt) % circularBufferSize
                }
              if(oldTailPosition < tail.position-1) {
                for(y <- oldTailPosition to tail.position-1) {
                  val stackSize = circularBuffer(y).size
                  for(z <- 0 to stackSize-1) {
                    val event = circularBuffer(y).pop()
                    //println("pop")
                    routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                  }
                }
              }
              else if(oldTailPosition > tail.position-1){
                for(y <- oldTailPosition to circularBufferSize-1) {
                  val stackSize = circularBuffer(y).size
                  for(z <- 0 to stackSize-1) {
                    val event = circularBuffer(y).pop()
                    //println("pop")
                    routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                  }
                }
                for(y <- 0 to tail.position-1) {
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
        
        
        newlineCount += 1
    }
    log.info("In CircularBuffer - count is " + newlineCount.toString)
  }
  
  def init() {
    for(i <- 0 to circularBufferSize-1)
      circularBuffer(i) = new Stack[TripEvent]
  }
  
  
}

object CircularBufferActor {
  val props = Props[CircularBufferActor]
  case object BeginProcessing

}