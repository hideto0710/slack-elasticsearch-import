package com.hideto0710.main

import akka.actor._
import com.hideto0710.akka.actor.{Channel, ESActor, MainActor}
import com.hideto0710.slack.SlackApiClient.Slack
import com.hideto0710.slack._
import com.hideto0710.util._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

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