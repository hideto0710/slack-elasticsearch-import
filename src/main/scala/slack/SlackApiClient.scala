package slack

import scala.concurrent.Future
import akka.actor.ActorSystem
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import slack.models._

case class HistoryChunk(ok: Boolean, messages: Option[Seq[SlackComment]], has_more: Option[Boolean])
case class ChannelChunk(ok: Boolean, channels: Option[Seq[Channel]])

object SlackJsonProtocol extends DefaultJsonProtocol {
  implicit val slackCommentFmt = jsonFormat4(SlackComment)
  implicit val historyChunkFmt = jsonFormat3(HistoryChunk)
  implicit val channelValueFmt = jsonFormat3(ChannelValue)
  implicit val channelFmt = jsonFormat12(Channel)
  implicit val channelChunkFmt = jsonFormat2(ChannelChunk)
}
import SlackJsonProtocol._


object SlackApiClient {

  implicit val system = ActorSystem()
  import system.dispatcher

  private val logger = Logger(LoggerFactory.getLogger("HelloAkkaScala"))

  private val apiBase = "https://slack.com/api/"
  private val token = "xoxp-2545467001-2590832085-6650275460-de9ade"

  private def makeUri(resource: String, queryParams: (String, String)*): Uri = {
    val resourceUri = Uri(apiBase + resource)
    val queryMap = queryParams.toMap ++ Map("token" -> token)
    resourceUri withQuery queryMap
  }

  private def get[A](pipeline: (HttpRequest) => Future[A], requestUri: Uri): Future[A] = {
    logger.info(requestUri.toString())
    pipeline(Get(requestUri))
  }

  def listChannels(excludeArchived: Int = 0): Future[ChannelChunk] = {
    val requestUri = makeUri("channels.list", "exclude_archived" -> excludeArchived.toString)
    get[ChannelChunk](
      sendReceive ~> unmarshal[ChannelChunk],
      requestUri
    )
  }
}
