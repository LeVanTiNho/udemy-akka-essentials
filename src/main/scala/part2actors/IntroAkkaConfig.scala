package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * 1 - inline configuration
    */
  val configString =
    """
      | akka {
      |   loglevel = "ERROR"
      | }
    """.stripMargin

  // ConfigFactory.parseString method returns a Config object (type-safe config)
  val config = ConfigFactory.parseString(configString)

  // ActorSystem apply method can receive a Config object
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))

  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "A message to remember"

  /**
    * 2 - config file
    */
    // Default config file is resources/application.conf
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"

  /**
    * 3 - separate config in the same file
    */
    // ConfigFactory.load() loads configs from the default file config application.conf
  val config3 = ConfigFactory.load().getConfig("mySpecialConfig")
  val system3 = ActorSystem("system3", config3)
  val actor3 = system3.actorOf(Props[SimpleLoggingActor])
  actor3 ! "separate config in the same file demo"


  /**
    * 4 - separate config in another file
    */

    // load method loads configs by relative path to the config file
    // getConfig, getString loads configs by path to the config
  val config4 = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"separate config log level: ${config4.getString("akka.logLevel")}")
  println(s"separate config secret number: ${config4.getInt("akka.secretNumber")}")


  /**
    * 5 - different file formats
    * JSON, Properties
    */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config: ${jsonConfig.getString("aJsonProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"properties config: ${propsConfig.getString("my.simpleProperty")}")
  println(s"properties config: ${propsConfig.getString("akka.loglevel")}")
}
