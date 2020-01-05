package playground.part2actors.child_actors

import akka.actor.Actor

class Child extends Actor {
  override def receive: Receive = {
    case message => println(s"${self.path} I got: $message")
  }
}
