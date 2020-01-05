package playground.part2actors.child_actors

import akka.actor.{Actor, ActorRef, Props}

object Parent {
  case class CreateChild(name: String)
  case class TellChild(message: String)
}

class Parent extends Actor {
  import Parent._

  override def receive: Receive = {
    case CreateChild(name) =>
      println(s"${self.path} creating child")
      context.become(withChild(context.actorOf(Props[Child], name)))
  }

  def withChild(child: ActorRef): Receive = {
    case TellChild(message) => child forward message
  }
}
