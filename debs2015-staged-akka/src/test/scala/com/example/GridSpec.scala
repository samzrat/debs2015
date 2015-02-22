package com.example


import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import scala.concurrent.duration._

class GridSpec(_system: ActorSystem)
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

    new Grid(300, 150, 150, 300) getCell(320, 320) get match {
      case Cell(x, y) => x should be (2)
                         y should be (2)
    }
    
    new Grid(300, 150, 150, 300) getCell(155, 155) get match {
      case Cell(x, y) => x should be (1)
                         y should be (1)
    }
  }
  
  "A grid" should "not return any cell outside boundaries" in {
    
    new Grid(300, 150, 150, 300) getCell(90001, 320) should be (None)
    new Grid(300, -150, -150, 300) getCell(90001, 320) should be (None)
    new Grid(300, -150, -150, 300) getCell(-90001, 320) should be (None)
  }

  
}