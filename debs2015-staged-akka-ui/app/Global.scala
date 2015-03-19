import play.api._
import play.libs.Akka
import akka.actor.Props
import samzrat.debs2015._

object Global extends GlobalSettings {

  
  
  override def onStart(app: Application) {
     
    Logger.info("Application has started")
  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
    
}

package object globals {
  lazy val CircularBufferActor = Akka.system.actorOf(Props[CircularBufferActor], name = "someActor")
}