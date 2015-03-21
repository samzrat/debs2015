package samzrat.debs2015

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import java.text.NumberFormat
import java.util.Locale
import java.text.ParsePosition
import java.util.Date
import java.text.ParseException
import java.text.SimpleDateFormat
import scala.collection.mutable.Stack
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.json._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import scala.concurrent.duration._
import akka.util.Timeout 
import akka.pattern.ask
import scala.concurrent.Await
import java.util.UUID
import org.apache.commons.codec.digest.DigestUtils
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import scala.collection.mutable.ListBuffer

case class Cell (xCell: Int, yCell: Int)
case class TripCells(startCell: Cell, endCell: Cell)
case class TripEvent (startTime: Date, endTime: Date, grid500Cells: TripCells, grid250Cells: TripCells, fareAmount: Double, tipAmount: Double)
class BufferEntry(val tripEventStack: Stack[TripEvent]) {}
class Pointer(var time: Date, var location: Int) {}

case class GetIterateeAndEnumerator()
case class IterateeAndEnumerator(in: Iteratee[String,Unit], out: Enumerator[String])
case class WebsocketMsg(msg: String)
case class CircularBufferState(state: List[Int])

class CircularBufferActor extends Actor with ActorLogging {
  import CircularBufferActor._
  val routeCountActor = context.actorOf(RouteCountActor.props, "routeCountActor")
  val cellProfitActor = context.actorOf(CellProfitActor.props, "cellProfitActor")
  
  val circularBufferSize = 3600
  val window_15Min = 900
  val window_30Min = 1800
  
  val circularBufferState = new ListBuffer[Int]()
  
  val circularBuffer: Array[BufferEntry] = new Array[BufferEntry](circularBufferSize)
  
  val head: Pointer = new Pointer(new Date(), -1)
  val tail_15Min: Pointer = new Pointer(new Date(), -1)
  val tail_30Min: Pointer = new Pointer(new Date(), -1)
  var tripEndTime: Long = -0
  
  init()

  implicit class PathAdditions(path: JsPath) {

  def readNullableIterable[A <: Iterable[_]](implicit reads: Reads[A]): Reads[A] =
    Reads((json: JsValue) => path.applyTillLast(json).fold(
      error => error,
      result => result.fold(
        invalid = (_) => reads.reads(JsArray()),
        valid = {
          case JsNull => reads.reads(JsArray())
          case js => reads.reads(js).repath(path)
        })
    ))

  def writeNullableIterable[A <: Iterable[_]](implicit writes: Writes[A]): OWrites[A] =
    OWrites[A]{ (a: A) =>
      if (a.isEmpty) Json.obj()
      else JsPath.createObj(path -> writes.writes(a))
    }

  /** When writing it ignores the property when the collection is empty,
    * when reading undefined and empty jsarray becomes an empty collection */
  def formatNullableIterable[A <: Iterable[_]](implicit format: Format[A]): OFormat[A] =
    OFormat[A](r = readNullableIterable(format), w = writeNullableIterable(format))

  }
  
  val (out,channel) = Concurrent.broadcast[String]

  val in = Iteratee.foreach[String] {
      msg => println(msg)
             //the Enumerator returned by Concurrent.broadcast subscribes to the channel and will 
             //receive the pushed messages
             log.info("Entered Iteratee closure")
           context.self ! WebsocketMsg(msg)
  }
  
  def receive = {
  	case BeginProcessing => 
	    log.info("Starting")
	    process()
    case GetIterateeAndEnumerator => 
       sender ! IterateeAndEnumerator(in, out)
    case WebsocketMsg(msg: String) => {
      val msgJson = Json.parse(msg)
      val messageType: String = (msgJson \ "msgType").as[String]
       
      messageType match {
        case "startProcessing" => 
          context.self ! BeginProcessing
          //channel push("Simulation started on server")
        case _ => throw new Exception()  
      }
    }
    case _ => throw new Exception()  
    

  } 
  
  
  
  def process() {
        
    channel push Json.toJson(List(145, 7, 123, 78, 156, 234)).toString
        
    val fileSource = Source.fromFile("sorted_data.csv")
    var newlineCount = 0L
    for (line <- fileSource.getLines) {
      DataExtractor.extractTripEventData(line) match {
        case None =>
        case Some(tripEvent) => 
          tripEndTime = tripEvent.endTime.getTime()
          routeCountActor ! RouteCountActor.IncrementRouteCountMsg(tripEvent)
          head.location match {
            case -1 => 
              assert (tail_15Min.location== -1 && tail_15Min.location== -1, "not -1")
              
              head.location = 0
              head.time.setTime(tripEvent.endTime.getTime)
              tail_15Min.location = 0
              tail_15Min.time.setTime(tripEvent.endTime.getTime)
              tail_30Min.location = 0
              tail_30Min.time.setTime(tripEvent.endTime.getTime)
              
              circularBuffer(0).tripEventStack.push(tripEvent)
              
            case _ => 
              //println(tripEvent.endTime +  "         " +  circularBuffer(head).time.get)
              ((tripEndTime - head.time.getTime())/1000) match {
                case timeDiff if timeDiff < 0 => 
                //println("incoming_trip_time LESSER => head_time: " + (circularBuffer(head)).time + ", incoming_trip_time: " + tripEvent.endTime)
                //ignore
                case 0L =>
                  circularBuffer(head.location).tripEventStack.push(tripEvent)
                  //circularBuffer(head).time = Some(tripEvent.endTime)
                case timeDiff if timeDiff > 0 => 
                  
                
                  
                  // ------------------------tail 15Min computation -----------------------------------------------------------------
                  
                  
                  
                  if((tripEndTime - tail_15Min.time.getTime)/1000 > window_15Min) {
                    val tailJump = ((tripEndTime - tail_15Min.time.getTime)/1000).toInt - window_15Min
                    if(tail_15Min.location+tailJump < (circularBufferSize) ) {
                      for(y <- tail_15Min.location until tail_15Min.location+tailJump) {
                        val clonedStack = circularBuffer(y).tripEventStack.clone()
                        for(z <- 0 until clonedStack.size) {
                          val event = clonedStack.pop()
                          cellProfitActor ! None
                        }
                      }
                    } 
                    else {
                      for(y <- tail_15Min.location until (circularBufferSize)) {
                        val clonedStack = circularBuffer(y).tripEventStack.clone()
                        for(z <- 0 until clonedStack.size) {
                          val event = clonedStack.pop()
                          cellProfitActor ! None
                        }
                      } 
                      for(y <- 0 until ((tail_15Min.location+tailJump)%(circularBufferSize))) {
                        val clonedStack = circularBuffer(y).tripEventStack.clone()
                        for(z <- 0 until clonedStack.size) {
                          val event = clonedStack.pop()
                          cellProfitActor ! None
                        }
                      } 
                    }
                    tail_15Min.location = (tail_15Min.location+tailJump)%(circularBufferSize)
                    tail_15Min.time.setTime(tail_15Min.time.getTime+tailJump*1000)
                  }
                  
                  // ------------------------tail 30Min computation -----------------------------------------------------------------
                  
                  if((tripEndTime - tail_30Min.time.getTime)/1000 > window_30Min) {
                    val tailJump = ((tripEndTime - tail_30Min.time.getTime)/1000).toInt - window_30Min
                    if(tail_30Min.location+tailJump < (circularBufferSize) ) {
                      for(y <- tail_30Min.location until tail_30Min.location+tailJump) {
                       
                        for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                          val event = circularBuffer(y).tripEventStack.pop()
                          routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                        }
                      }
                    } 
                    else {
                      for(y <- tail_30Min.location until (circularBufferSize)) {
                        
                        for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                          val event = circularBuffer(y).tripEventStack.pop()
                          routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                        }
                      } 
                      for(y <- 0 until ((tail_30Min.location+tailJump)%(circularBufferSize))) {
                        
                        for(z <- 0 until circularBuffer(y).tripEventStack.size) {
                          val event = circularBuffer(y).tripEventStack.pop()
                          routeCountActor ! RouteCountActor.DecrementRouteCountMsg(event)
                        }
                      } 
                    }
                    tail_30Min.location = (tail_30Min.location+tailJump)%(circularBufferSize)
                    tail_30Min.time.setTime(tail_30Min.time.getTime+tailJump*1000)
                  }
                  
                  
                  
                  // ------------------------head computation -----------------------------------------------------------------
                  //val oldHeadLocation = head.location
                  head.location = (head.location+timeDiff.toInt) % circularBufferSize
                  head.time.setTime(tripEndTime)
                  circularBuffer(head.location).tripEventStack.push(tripEvent)
                  
                  //println("head = " + head.location + "              tail_15 = " + tail_15Min.location + "              tail_30 = " + tail_30Min.location)
                  var sum = 0
                  var j = 0
                  for(i <- 0 until circularBufferSize) {
                    sum += circularBuffer(i).tripEventStack.size
                    j += 1
                    if(j==60) {
                      circularBufferState += sum
                      j = 0
                      sum = 0
                    }  
                  }  
                  channel push Json.toJson(circularBufferState).toString
                  circularBufferState.clear()
                  
              }
              newlineCount += 1
              //println("head = " + head.location + "              tail_15 = " + tail_15Min.location + "              tail_30 = " + tail_30Min.location)

          }
      }
    }
    println("In CircularBuffer - count is " + newlineCount.toString)
  }
  
  def init() {
    for(i <- 0 until circularBufferSize)
      circularBuffer(i) = new BufferEntry(new Stack[TripEvent])
  }
  
  
}

object CircularBufferActor {
  val props = Props[CircularBufferActor]
  case object BeginProcessing

}