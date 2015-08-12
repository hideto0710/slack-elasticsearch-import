package com.hideto0710.akka.actor

import scala.concurrent.duration
import scala.util.{Failure, Success}
import akka.actor.{Props, Actor, ActorRef}
import akka.util.Timeout
import com.hideto0710.slack.models.SlackComment
import scala.concurrent.ExecutionContext.Implicits.global

case class Channel(id: String, name: String)
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
