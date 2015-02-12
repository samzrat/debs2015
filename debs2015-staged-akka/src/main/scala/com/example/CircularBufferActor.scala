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

class CircularBufferActor extends Actor with ActorLogging {
  import CircularBufferActor._
  
  var counter = 0
  val circularBuffer: Array[Stack[Int]] = new Array[Stack[Int]](1800)

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
      
        circularBuffer(0).push(5)
        circularBuffer(1).push(10)
        circularBuffer(0).pop
        
        newlineCount += 1
    }
    log.info("In CircularBuffer - count is " + newlineCount.toString)
  }
  
  def init() {
    for(i <- 0 to 1799)
      circularBuffer(i) = new Stack[Int]
  }
  
  
}

object CircularBufferActor {
  val props = Props[CircularBufferActor]
  case object BeginProcessing

}