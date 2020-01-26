package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

// Lesson 6
object ActorLoggingDemo extends App {

  class SimpleActorWithExplicitLogger extends Actor {
    // #1 - explicit logging

    // Logging object is the main entry point for akka logging
    // The apply method of this object will return a LoggingAdapter
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      /*
      Log levels:
        1 - DEBUG
        2 - INFO
        3 - WARNING/WARN
        4 - ERROR
       */
      // case message => logger.warning(message.toString)// LOG it
      case message => logger.info(message.toString)// LOG it
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger])

  actor ! "Logging a simple message"

  // #2 - ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      // log method of ActorLogging return a LoggingAdapter
      case (a, b) => log.info("Two things: {} and {}", a, b) // string interpolation
      case message => log.info(message.toString)
    }
  }

  val simplerActor = system.actorOf(Props[ActorWithLogging])
  simplerActor ! "Logging a simple message by extending a trait"

  simplerActor ! (42, 65)

  /**
    * Notes:
    *   - The logging is asynchronously -> we cannot except some order of the log message
    *   - The Logging module doesn't depend on the actor implementation -> so we can use another logging module
    */
}
