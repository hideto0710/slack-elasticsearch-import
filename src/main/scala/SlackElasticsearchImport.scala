
import scala.util.{Success, Failure}
import scala.concurrent.duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.util.Timeout
import akka.actor._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import slack._
import slack.models.SlackComment
import slack.SlackApiClient.Slack
import util._

case class Channel(id: String, name: String)

object SlackElasticsearchImport extends App {

  val logger = Logger(LoggerFactory.getLogger("SlackElasticsearchImport"))
  val conf = ConfigFactory.load()
  val ErrorExitCode = 1

  implicit val slack = Slack(conf.getString("slack.token"))

  // チャネル一覧を取得
  val RequestLimit = conf.getInt("slack.requestLimit")
  val channelResult = SlackApiClient.syncRequest(SlackApiClient.listChannels(1), RequestLimit)
  val channelList = channelResult match {
    case Right(r) => r.channels.getOrElse(Seq())
    case Left(e) => // MARK: channel.list取得エラー
      logger.error(s"SlackApiClient [$e] ERROR")
      sys.exit(ErrorExitCode)
  }
  logger.debug(channelList.toString())

  // オプションを設定
  val options = ArgsUtil.nextOption(args.toList)
  val from = options.getOrElse('from, 0.toString).toLong
  val to = options.getOrElse('to, 1000000.toString).toLong
  logger.info(s"Options: [from: $from, to: $to]")

  // インポート対象チャネルを設定
  val targetChannel = options.getOrElse('channel, "all")
  val targetChannels = targetChannel match {
    case "all" => channelList
    case "isMember" => channelList.filter(c => c.is_member)
    case _ => channelList.filter(c => c.name == targetChannel)
  }
  if (targetChannels.isEmpty) { // MARK: インポート対象チャネル設定エラー
    logger.error("No Channels ERROR")
    sys.exit(ErrorExitCode)
  }
  logger.info("Target Channels: [%s]".format(targetChannels.map(_.name).mkString(", ")))

  val system = ActorSystem()
  val esActor = system.actorOf(Props[ESActor], "es")
  val slackActor = system.actorOf(Props(classOf[MainActor], esActor), "slack")

  targetChannels.foreach( t => slackActor ! Channel(t.id, t.name) )
}


case class SlackFetchRequest(channel: Channel, latest: Option[Long], oldest: Option[Long])
case class SlackFetchResponse(channel: Channel, messages: Seq[SlackComment])

class MainActor(out:ActorRef) extends Actor {
  implicit val timeout = Timeout(5, duration.SECONDS)

  // TODO: 並行処理数を制限
  def receive = {
    case Channel(id, name) =>
      context.actorSelection(name).resolveOne().onComplete {
        case Success(actor) =>
          actor ! SlackFetchRequest(Channel(id, name), None, None)
        case Failure(e) =>
          val actor = context.actorOf(Props(classOf[SlackActor], out), name)
          actor ! SlackFetchRequest(Channel(id, name), None, None)
      }
  }
}

class SlackActor(out:ActorRef) extends Actor {
  val conf = ConfigFactory.load()
  implicit val slack = Slack(conf.getString("slack.token"))

  def receive = {
    case SlackFetchRequest(c, latest, oldest) =>
      val result = SlackApiClient.syncRequest(SlackApiClient.channelsHistory(c.id, latest=latest, oldest=oldest), 1)
      result match {
        case Right(r) =>
          val ms = r.messages.getOrElse(Seq()) // TODO:結果0件のときのエラー処理
          out ! SlackFetchResponse(c, ms)
          if(r.has_more.getOrElse(false)) {
            val latest = ms.last.ts.toLong
            val oldest = None // TODO:オプションを参照
            self ! SlackFetchRequest(c, Some(latest), oldest)
          }
        case Left(e) => // TODO:コメント取得時のエラー処理
      }
  }
}

class ESActor extends Actor with ActorLogging  {
  import scala.collection.mutable
  val data = mutable.Map[String, List[SlackComment]]()
  // TODO: Elasticsearchへのインポート処理
  def receive = {
    case SlackFetchResponse(channel, messages) =>
      data.get(channel.id) match {
        case Some(v) => data(channel.id) = v ::: messages.toList
        case _ => data(channel.id) = messages.toList
      }
      log.info(data.toString())
  }
}