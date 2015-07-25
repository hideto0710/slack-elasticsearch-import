
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import slack._
import util._

object SlackElasticsearchImport extends App {

  val logger = Logger(LoggerFactory.getLogger("SlackElasticsearchImportSpec"))
  val conf = ConfigFactory.load()
  val slack = SlackApiClient("xoxp-2545467001-2590832085-6650275460-de9ade")

  val RequestLimit = 3
  val SleepTimeForRetry = 1000

  // チャネル一覧を取得
  val result = SlackApiClient.getWithRetry(slack.listChannelsFunc(1), RequestLimit, SleepTimeForRetry)
  val channelList = result.flatMap(_.channels).getOrElse(Seq())

  val argList = args.toList
  val options = ArgsUtil.nextOption(argList)

  val from = options.getOrElse('from, 0).toString.toLong
  val to = options.getOrElse('to, 1000000).toString.toLong
  val targetChannel = options.getOrElse('channel, "all")

  val targetChannels = targetChannel match {
    case "isMember" => channelList.filter(c => c.is_member)
    case "all" => channelList
    case _ => channelList.filter(c => c.name == targetChannel)
  }

  logger.info(options.toString())
  logger.info(channelList.toString())
  val channelsStr = targetChannels.map(_.name).mkString(", ")
  logger.info(s"Target Channels: [$channelsStr]")
  logger.info(conf.getString("slack.test.timezone"))
  sys.exit()

  /*
  // Create the 'helloakka' actor system
  val system = ActorSystem("helloakka")

  // Create the 'greeter' actor
  val greeter = system.actorOf(Props[Greeter], "greeter")

  // Create an "actor-in-a-box"
  val inbox = Inbox.create(system)

  // Tell the 'greeter' to change its 'greeting' message
  greeter.tell(WhoToGreet("akka"), ActorRef.noSender)

  // Ask the 'greeter for the latest 'greeting'
  // Reply should go to the "actor-in-a-box"
  inbox.send(greeter, Greet)

  // Wait 5 seconds for the reply with the 'greeting' message
  val Greeting(message1) = inbox.receive(5.seconds)
  println(s"Greeting: $message1")

  // Change the greeting and ask for it again
  greeter.tell(WhoToGreet("typesafe"), ActorRef.noSender)
  inbox.send(greeter, Greet)
  val Greeting(message2) = inbox.receive(5.seconds)
  println(s"Greeting: $message2")

  val greetPrinter = system.actorOf(Props[GreetPrinter])
  // after zero seconds, send a Greet message every second to the greeter with a sender of the greetPrinter
  system.scheduler.schedule(0.seconds, 1.second, greeter, Greet)(system.dispatcher, greetPrinter)
  */
}

case object Greet
case class WhoToGreet(who: String)
case class Greeting(message: String)

class Greeter extends Actor {
  var greeting = ""

  def receive = {
    case WhoToGreet(who) => greeting = s"hello, $who"
    case Greet           => sender ! Greeting(greeting) // Send the current greeting back to the sender
  }
}

// prints a greeting
class GreetPrinter extends Actor {
  def receive = {
    case Greeting(message) => println(message)
  }
}