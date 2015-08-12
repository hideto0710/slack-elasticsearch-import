package com.hideto0710.akka.actor

import akka.actor.{ActorLogging, Actor}
import com.hideto0710.slack.models.SlackComment

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