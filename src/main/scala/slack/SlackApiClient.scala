package slack

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import akka.actor.ActorSystem
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import slack.models._

import scala.util.{Failure, Success}

sealed trait SlackResponse {
  val ok: Boolean
}
case class HistoryChunk(ok: Boolean, messages: Option[Seq[SlackComment]], has_more: Option[Boolean]) extends SlackResponse
case class ChannelChunk(ok: Boolean, channels: Option[Seq[Channel]]) extends SlackResponse

object SlackJsonProtocol extends DefaultJsonProtocol {
  implicit val slackCommentFmt = jsonFormat4(SlackComment)
  implicit val historyChunkFmt = jsonFormat3(HistoryChunk)
  implicit val channelValueFmt = jsonFormat3(ChannelValue)
  implicit val channelFmt = jsonFormat12(Channel)
  implicit val channelChunkFmt = jsonFormat2(ChannelChunk)
}
import SlackJsonProtocol._

object SlackApiClient {
  private type Pipeline[A] = (HttpRequest) => Future[A]
  private type FutureFunc[A] = () => Future[A]
  private val logger = Logger(LoggerFactory.getLogger("SlackApiClient"))
  private val ThreadSleep = 5 * 1000

  private def get[A](p: Pipeline[A], uri: Uri): Future[A] = {
    logger.debug(uri.toString())
    p(Get(uri))
  }

  def apply(t: String) = {
    new SlackApiClient(t)
  }

  @tailrec
  def getWithRetry[A <: SlackResponse](getFuture: FutureFunc[A], limit: Int, time: Int = ThreadSleep, n: Int = 1): Option[A] = {
    if (n > limit) return None
    if (n > 1) Thread.sleep(time)
    val f = getFuture()
    Await.ready(f, Duration.Inf)
    f.value.get match {
      case Success(result) =>
        if (result.ok) Some(result) else getWithRetry(getFuture, limit, time, n+1)
      case Failure(ex) => getWithRetry(getFuture, limit, time, n+1)
    }
  }
}

class SlackApiClient private (t: String) {
  private val token = t
  private val ApiUrl = "https://slack.com/api/"

  implicit val system = ActorSystem()
  import system.dispatcher
  import slack.SlackApiClient.FutureFunc

  private def makeUri(resource: String, queryParams: (String, String)*): Uri = {
    val resourceUri = Uri(ApiUrl + resource)
    val queryMap = queryParams.toMap ++ Map("token" -> token)
    resourceUri withQuery queryMap
  }

  def listChannels(excludeArchived: Int = 0): Future[ChannelChunk] = {
    val requestUri = makeUri("channels.list", "exclude_archived" -> excludeArchived.toString)
    SlackApiClient.get[ChannelChunk](
      sendReceive ~> unmarshal[ChannelChunk],
      requestUri
    )
  }

  def listChannelsFunc(excludeArchived: Int = 0): FutureFunc[ChannelChunk] = {
    () => listChannels(excludeArchived)
  }
}
