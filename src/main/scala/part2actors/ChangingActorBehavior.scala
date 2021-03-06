package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehavior.Mom.MomStart

// Lesson 3
/**
  * Actors can change their behaviors depend on their state
  */
object ChangingActorBehavior extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    // internal state of the kid
    private var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        // Check the state of FussyKid to decide the next behavior
        // It's bad because when the logic of messages handling becomes more big, we must check the more state
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    // At beginning, the Receive object returned from receive method will be the first message handler object of the stack
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      // case Food(VEGETABLE) => context.become(sadReceive) // push the Receive object (returned from sadReceive) the stack and discard the current Receive

      // push the Receive object (returned from sadReceive) the stack and do not discard the current Receive
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome() // remove the current Receive object from the stack
      case Ask(_) => sender() ! KidReject
    }
  }

  /**
    * The advantage of using message handler object stack to change behavior of actor when state changes is
    * we can easily manage and understand the flow of logic
    */

  /**
    * The rules:
    *   - Akka always use the latest handler on the top of the stack
    *   - When the stack is empty, it calls receive method to get the message handler object
    */

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // do you want to play?
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("do you want to play?")
      case KidAccept => println("Yay, my kid is happy!")
      case KidReject => println("My kid is sad, but as he's healthy!")
    }
  }

  val system = ActorSystem("changingActorBehaviorDemo")
  val fussyKid = system.actorOf(Props[FussyKid])
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom])

  mom ! MomStart(statelessFussyKid)

  /*
    mom receives MomStart
      kid receives Food(veg) -> kid will change the handler to sadReceive
      kid receives Ask(play?) -> kid replies with the sadReceive handler =>
    mom receives KidReject
   */

  /*

  context.become

    Food(veg) -> stack.push(sadReceive)
    Food(chocolate) -> stack.push(happyReceive)

    Stack:
    1. happyReceive
    2. sadReceive
    3. happyReceive
   */

  /*
    new behavior
    Food(veg)
    Food(veg)
    Food(chocolate)
    Food(chocolate)

    Stack:

    1. happyReceive
   */

  /**
    * Exercises
    * 1 - recreate the Counter Actor with context.become and NO MUTABLE STATE
    */

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    override def receive: Receive = countReceive(0)

    // This exercise show us that we can use parameters of receive method to save the state of actor
    def countReceive(currentCount: Int): Receive = {
      // By somehow, Receive object return from this method can access to currentCount
      case Increment =>
        println(s"[countReceive($currentCount)] incrementing")
        context.become(countReceive(currentCount + 1))
      case Decrement =>
        println(s"[countReceive($currentCount)] decrementing")
        context.become(countReceive(currentCount - 1))
      case Print => println(s"[countReceive($currentCount)] my current count is $currentCount")
    }
  }

  import Counter._
  val counter = system.actorOf(Props[Counter], "myCounter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /**
    * Exercise 2 - a simplified voting system
    */

  // The messages Citizen Actor and Vote Aggregator communicate on
  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])


  class Citizen extends Actor {

    // At the beginning the actor system calls the receive method to get the first Receive object to stack
    override def receive: Receive = {
      // To make clear, we can add a new context that is un-voted
      case Vote(c) => context.become(voted(c))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))
    }

    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive = {

      case VoteStatusReply(None) =>

        // a citizen hasn't voted yet
        sender() ! VoteStatusRequest // this might end up in an infinite loop

        // Another way to handle this case to avoid an infinite loop
        val newStillWaiting = stillWaiting - sender()
        context.become(awaitingStatuses(newStillWaiting, currentStats))

      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $newStats")
        } else {
          // still need to process some statuses
          context.become(awaitingStatuses(newStillWaiting, newStats))
        }
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /*
    Print the status of the votes

    Martin -> 1
    Jonas -> 1
    Roland -> 2
   */
}
