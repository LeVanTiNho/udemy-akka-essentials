package playground.part2actors.child_actors

import akka.actor.{ActorSystem, Props}
import playground.part2actors.child_actors.Parent.{CreateChild, TellChild}

object Experiment extends App{
  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("hey Kid!")
  val childSelection = system.actorSelection("user/parent/child")
  childSelection ! "I found you!"
}
