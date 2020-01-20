package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

// Lesson 1: Basic Spec

// Name of testing classes should have suffix Spec
class BasicSpec extends TestKit(ActorSystem("BasicSpec")) // TestKit requires an ActorSystem
  with ImplicitSender // Send-reply scenarios
  with WordSpecLike // Allow descriptions of tests in a very natural language style
  with BeforeAndAfterAll // Supply a set of hooks, when we run test suit, the set of hooks will be called
{

  // Define a hook, afterAll hook used to destroy the test suit when it's done
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._

  "A simple actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      // A expectation has 3s duration by default, we can config it
      expectMsg(message) // akka.test.single-expect-default
    }
  }

  "A blackhole actor" should {
    "not reply" in {
      val blackhole = system.actorOf(Props[Blackhole])
      val message = "hello, test"
      blackhole ! message

      expectNoMessage(1 second)
    }
  }

  // message assertions
  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I love Akka"

      // Assert the received message conforms to the type and returns it
      val reply = expectMsgType[String]

      // assert method of Assertions trait of scalatest package
      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello") // test if receive hi or hello
    }

    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"
      expectMsgAllOf("Scala", "Akka") // test must receive "Scala" vaf "Akka"
    }

    "reply with cool tech in a different way" in {
      labTestActor ! "favoriteTech"
      val messages = receiveN(2) // Seq[Any], test must receive 2 two messages in the default duration

      // free to do more complicated assertions
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favoriteTech"

      // expectMsgPF receives a PF
      // in PF we can add more assertion or return a value of some Type
      expectMsgPF() {
        case "Scala" => expectMsg("Scala")
        case "Akka" =>
      }
    }
  }
}


// We should define all the information of a test in the companion object of that test
object BasicSpec {

  // test kid is used to test an particular actor or a particular group of actor
  // Normally, that is defined outside by developer
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message =>
        Thread.sleep(2000)
        sender() ! message
    }
  }

  class Blackhole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
    // emptyBehavior is a Receive object, that is a PartialFunction
  }

  class LabTestActor extends Actor {
    val random = new Random()

    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase()
    }
  }
}
