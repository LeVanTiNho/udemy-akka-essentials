package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

// Lesson 1: Stopping, Watching Actors
object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {

      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))

      case StopChild(name) =>
        log.info(s"Stopping child with the name $name")
        val childOption = children.get(name)

        childOption.foreach(childRef => context.stop(childRef))
          // stop method does not immediately stop the actor, the processing of the messages will continue

      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
        // stop(self) doesn't stop itself immediately, it stops its children before
        // It should not be received more massages, because stop method send a message that tell it to stop

      case message =>
        log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * method #1 - using context.stop
    */
  import Parent._
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! StartChild("child1")
  val child = system.actorSelection("/user/parent/child1")
  child ! "hi kid!"

  parent ! StopChild("child1")
  for (_ <- 1 to 50) child ! "are you still there?"

  parent ! StartChild("child2")
  val child2 = system.actorSelection("user/parent/child2")
  child2 ! "hi, second child"

  parent ! Stop
  for (_ <- 1 to 10) parent ! "parent, are you still there?"

  for (i <- 1 to 100) child2 ! s"[$i] second kid, are you still alive?"

  /**
    * method #2 - using special messages
    */

  val looseActor = system.actorOf(Props[Child])
  looseActor ! "hello, loose actor"
  looseActor ! PoisonPill // like stop(self) call, PoisonPill will make the actor will not be received any more massage
  looseActor ! "loose actor, are you still there?"

  val abruptlyTerminatedActor = system.actorOf(Props[Child])
  abruptlyTerminatedActor ! "you are about to be terminated"
  abruptlyTerminatedActor ! Kill // Kill messages make the actor stop and throw ActorKilledException
  abruptlyTerminatedActor ! "you have been terminated"

  /**
    *  Death watch
    */
  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the reference that I'm watching $ref has been stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)

  watchedChild ! PoisonPill
}
