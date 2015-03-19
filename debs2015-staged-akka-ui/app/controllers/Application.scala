package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.actorRef2Scala
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Concurrent
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.libs.Akka
import samzrat.debs2015._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Visualizations:"))
  }

  def websocket = WebSocket.async[String] { request =>
  Logger.info("Inside of websocket controller")
    implicit val timeout = Timeout(10 second)
    
    val CircularBufferActor = Akka.system.actorOf(Props[CircularBufferActor]/*, name = "someActor"*/)
    
    //val future = (nodeManager ?  StartSimulation(1, 1)).mapTo
    
  (CircularBufferActor ?  GetIterateeAndEnumerator) map {
    case IterateeAndEnumerator(in, out) =>
    Logger.info("Got a websocket response to initialize websocket: " + in.toString + " " + out.toString)
    (in, out)
  }
  }
}