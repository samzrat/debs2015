package samzrat.debs2015

import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import samzrat.debs2015.DataExtractor;
import scala.concurrent.duration._

class DataExtractorSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("HelloAkkaSpec"))

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  "A grid" should "return the right cell within boundaries" in {


    var line = "1E5F4C1CAE7AB3D06ABBDDD4D9DE7FA6,E0B2F618053518F24790C7FD0264E302,2013-01-01 00:03:00,2013-01-01 00:04:00,60,0.00,-73.993973,40.751266,0.000000,0.000000,CSH,2.50,0.50,0.50,0.00,0.00,3.50"
    DataExtractor.extractTripEventData(line) should not be (None)
    
    //bad longitude
    line = "1E5F4C1CAE7AB3D06ABBDDD4D9DE7FA6,E0B2F618053518F24790C7FD0264E302,2013-01-01 00:03:00,2013-01-01 00:04:00,60,0.00,B-3A $B073.993973,40.751266,0.000000,0.000000,CSH,2.50,0.50,0.50,0.00,0.00,3.50"
    DataExtractor.extractTripEventData(line) should be (None)
    
    //longitude beyond grid boundaries
    line = "1E5F4C1CAE7AB3D06ABBDDD4D9DE7FA6,E0B2F618053518F24790C7FD0264E302,2013-01-01 00:03:00,2013-01-01 00:04:00,60,0.00,-3073.993973,40.751266,0.000000,0.000000,CSH,2.50,0.50,0.50,0.00,0.00,3.50"
    DataExtractor.extractTripEventData(line) should be (None)
    
    //bad date timr
    line = "1E5F4C1CAE7AB3D06ABBDDD4D9DE7FA6,E0B2F618053518F24790C7FD0264E302,2013---1-01 00:03:00,2013-01-01 00:04:00,60,0.00,-3073.993973,40.751266,0.000000,0.000000,CSH,2.50,0.50,0.50,0.00,0.00,3.50"
    DataExtractor.extractTripEventData(line) should be (None)
  }
  

  
}