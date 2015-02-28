package com.example

import akka.actor.{Actor, ActorLogging, Props}
import scala.io.Source
import akka.actor.actorRef2Scala
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class TopTenRoutesActor extends Actor with ActorLogging {
  import TopTenRoutesActor._
  
  var topRoutesList = ListBuffer[Tuple2[Route, Int]]()
  
  var topRoutesArray:Array[Option[Tuple2[Route, Int]]] = new Array[Option[Tuple2[Route, Int]]](10)
  for(i <- 0 to 10-1)
	  topRoutesArray(i) = None
  
  var bestCount = 0
  var tenthBestCount = 0 
  
  
  
  def receive = {
    case PossibleTopperMsg(potentialRoute: Route, potentialRouteCount: Int) =>
      //println("PossibleTopperMsg " + potentialRouteCount)
      //println("SIZE= " + topRoutesArray.filter(_ != None).size)
      topRoutesArray.filter(_ != None).size match {
        case 0 =>
          topRoutesArray(9) = Some(Tuple2(potentialRoute, potentialRouteCount))
          sender ! RouteCountActor.NewTopRoutesRangeMsg(potentialRouteCount, 0)
          //println("ADDED THE FIRST STAR ROUTE")
        case _ =>
          //println("EVALUATION OF STAR ROUTE")
          breakable { 
            for(i <- 0 to 10-1) {
              topRoutesArray(i) match {
                case None =>
                case Some(starRouteTuple) => 
                  if(starRouteTuple._1 == potentialRoute) {
                    //println("INCOMING ROUTE MATCHED")
                    topRoutesArray(i) = Some(Tuple2(potentialRoute, potentialRouteCount))
                    for(j <- i to 10-2) {
                      if(topRoutesArray(j).get._2 > topRoutesArray(j+1).get._2) {
                        //println("INCREMENT " + topRoutesArray(j).get._2 + "    " + topRoutesArray(j+1).get._2)
                        val temp = topRoutesArray(j)
                        topRoutesArray(j) = topRoutesArray(j+1)
                        topRoutesArray(j+1) = temp
                      } 
                    }
                    for(j <- i to 1 by -1) {
                      if(topRoutesArray(j-1) != None && topRoutesArray(j).get._2 < topRoutesArray(j-1).get._2) {
                        //println("DECREMENT " + topRoutesArray(j).get._2 + "    " + topRoutesArray(j-1).get._2)
                        val temp = topRoutesArray(j)
                        topRoutesArray(j) = topRoutesArray(j-1)
                        topRoutesArray(j-1) = temp
                      } 
                    }
                    val someList = topRoutesArray.filter(_ != None)
                  val someListSize = topRoutesArray.filter(_ != None).size
                  
                  if(someList(0).get._2 != tenthBestCount || someList(someListSize-1).get._2 != bestCount) {
                      sender ! RouteCountActor.NewTopRoutesRangeMsg(someList(someListSize-1).get._2, someList(0).get._2)
                      //println(someList(someListSize-1).get._2 + "    " + someList(0).get._2)
                  }
                  bestCount = someList(someListSize-1).get._2
                  tenthBestCount = someList(0).get._2

                  break
                  }
              }
            
            }
          } 
          breakable {
            for(i <- 0 to 10-1) {
              //println("Inside 1st for loop")
              topRoutesArray(i) match {
                case None =>
                case Some(starRouteTuple: Tuple2[Route, Int]) =>  
                  if(potentialRouteCount  >= starRouteTuple._2) {
                    for(j <- 0 to i-1) {
                      //println("Inside 2nd for loop") 
                      topRoutesArray(j) = topRoutesArray(j+1)
                    }  
                  }
                  topRoutesArray(i) = Some(Tuple2(potentialRoute, potentialRouteCount))
                  //println("New top ten route: count = " + potentialRouteCount)
                
                  val someList = topRoutesArray.filter(_ != None)
                  val someListSize = topRoutesArray.filter(_ != None).size
                  
                  if(someList(0).get._2 != tenthBestCount || someList(someListSize-1).get._2 != bestCount) {
                      sender ! RouteCountActor.NewTopRoutesRangeMsg(someList(someListSize-1).get._2, someList(0).get._2)
                      //println(someList(someListSize-1).get._2 + "    " + someList(0).get._2)
                  }
                  bestCount = someList(someListSize-1).get._2
                  tenthBestCount = someList(0).get._2

                  break
                }  
            }
         }
     }
  }	
  
  
  
}

object TopTenRoutesActor {
  val props = Props[TopTenRoutesActor]
  case class PossibleTopperMsg(route: Route,count: Int)
}