package com.hideto0710.akka.actor

import akka.actor.{Actor, ActorRef}
import com.hideto0710.slack.SlackApiClient
import com.hideto0710.slack.SlackApiClient.Slack
import com.typesafe.config.ConfigFactory

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