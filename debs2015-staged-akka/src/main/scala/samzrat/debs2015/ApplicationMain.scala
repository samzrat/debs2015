package samzrat.debs2015

import akka.actor.ActorSystem
import samzrat.debs2015.CircularBufferActor;
import scala.io.Source
import akka.actor.ActorLogging

object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val circularBufferActor = system.actorOf(CircularBufferActor.props, "circularBufferActor")
  circularBufferActor ! CircularBufferActor.BeginProcessing
  system.awaitTermination()
  
  
}