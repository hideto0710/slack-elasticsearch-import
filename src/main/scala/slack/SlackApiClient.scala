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

/**
 * SlackAPIのレスポンス
 */
sealed trait SlackResponse {
  val ok: Boolean
  val error: Option[String]
}

/**
 * channels.historyのレスポンス
 * @param ok リクエストの成功
 * @param error エラー内容
 * @param messages コメント一覧
 * @param has_more 次ページングの有無
 */
case class HistoryChunk(
  ok: Boolean,
  error: Option[String],
  messages: Option[Seq[SlackComment]],
  has_more: Option[Boolean]
) extends SlackResponse

/**
 * channel.listのレスポンス
 * @param ok リクエストの成功
 * @param error エラー内容
 * @param channels チャネルリスト
 */
case class ChannelChunk(
  ok: Boolean,
  error: Option[String],
  channels: Option[Seq[Channel]]
) extends SlackResponse

object SlackJsonProtocol extends DefaultJsonProtocol {
  implicit val slackCommentFmt = jsonFormat4(SlackComment)
  implicit val historyChunkFmt = jsonFormat4(HistoryChunk)
  implicit val channelValueFmt = jsonFormat3(ChannelValue)
  implicit val channelFmt = jsonFormat12(Channel)
  implicit val channelChunkFmt = jsonFormat3(ChannelChunk)
}
import SlackJsonProtocol._

/**
 * SlackApiClientのコンパニオンオブジェクト
 *
 * SlackAPIへのリクエストに関わるUtilityMethodとFactoryMethodを提供する。
 * @version 0.1
 */
object SlackApiClient {

  private type Pipeline[A] = (HttpRequest) => Future[A]
  private val logger = Logger(LoggerFactory.getLogger("SlackApiClient"))
  private val Sleep = 5 * 1000

  /**
   * GETリクエストのFutureを返す。
   * @param p Pipeline
   * @param uri リクエストURI
   * @tparam A レスポンス型
   * @return Future[A]
   * @note 型パラメータでは unmarshal ができないため、引数として受け取る。（implicit value 関連）
   */
  private def get[A](p: Pipeline[A], uri: Uri): Future[A] = {
    logger.debug(uri.toString())
    p(Get(uri))
  }

  /**
   * 同期リクエストのレスポンスを返す。
   * @param getFuture Futureの名前渡し引数
   * @param limit リクエスト試行回数制限
   * @param sleepTime リトライ時のThread.sleep（ms）
   * @param n リクエスト試行回数
   * @tparam A Futureの型
   * @return 同期リクエストのレスポンス
   */
  @tailrec
   private def tryHttpAwait[A](getFuture: => Future[A], limit: Int, sleepTime: Int, n: Int=1): Either[Throwable, A] = {
    if (n > 1) {
      logger.debug(s"tryHttpAwait sleep $sleepTime ms")
      Thread.sleep(sleepTime)
    }
    val f = getFuture
    Await.ready(f, Duration.Inf)
    f.value.get match {
      case Success(r) => Right(r)
      case Failure(e) =>
        if (n < limit) tryHttpAwait(getFuture, limit, sleepTime, n+1) else Left(e)
    }
  }

  /**
   * Noneを排除し、valueをStringに変換したMapを返す。
   * @param map Noneを含んだMap
   * @return Noneが排除されたMap
   */
  private def cleanMap(map: Map[String, Any]): Map[String, String] = {
    map.collect {
      case (k, Some(v)) => (k, v.toString)
    }
  }

  /**
   * SlackApiClientのインスタンスを返す。
   * @param t トークン
   * @return SlackApiClient
   */
  def apply(t: String) = {
    new SlackApiClient(t)
  }

  /**
   * 同期リクエストのレスポンスを返す。
   * @param getFuture Future（非同期リクエスト）
   * @param limit リクエスト試行回数制限
   * @param sleepTime リトライ時のThread.sleep（ms）
   * @tparam A Futureの型
   * @return 同期リクエストのレスポンス
   */
  def syncRequest[A<:SlackResponse](getFuture: => Future[A], limit: Int, sleepTime: Int=Sleep): Either[String, A] = {
    val result = tryHttpAwait(getFuture, limit, sleepTime)
    result match {
      case Right(r) =>
        if (r.ok) Right(r) else Left(r.error.getOrElse("unknown_error"))
      case Left(e) => Left(e.getMessage)
    }
  }
}

/**
 * Slackへのリクエスト用のHTTPClient
 * @constructor SlackApiClientのインスタンスをトークンから作成
 * @param t トークン
 * @version 0.1
 */
class SlackApiClient private (t: String) {
  private val token = t
  private val ApiUrl = "https://slack.com/api/"

  implicit val system = ActorSystem()
  import system.dispatcher

  private def makeUri(resource: String, queryParams: (String, Any)*): Uri = {
    val resourceUri = Uri(ApiUrl + resource)
    val queryMap = SlackApiClient.cleanMap(queryParams.toMap) ++ Map("token" -> token)
    resourceUri withQuery queryMap
  }

  /**
   * channels.listのFutureを返す。
   * @param excludeArchived アーカイブチャネルの排除フラグ
   * @return channel.listのFuture
   */
  def listChannels(excludeArchived: Int = 0): Future[ChannelChunk] = {
    val requestUri = makeUri("channels.list", "exclude_archived" -> excludeArchived)
    SlackApiClient.get[ChannelChunk](
      sendReceive ~> unmarshal[ChannelChunk],
      requestUri
    )
  }

  /**
   * channels.historyのFutureを返す。
   * @param channel チャネル
   * @param latest 取得対象期間の終了日時
   * @param oldest 取得対象期間の開始日時
   * @param inclusive 指定期間のメッセージを含むフラグ
   * @param count メッセージ取得件数（最大1000）
   * @return channels.historyのFuture
   */
  def channelsHistory(
    channel: String,
    latest: Option[Long] = None,
    oldest: Option[Long] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None
  ): Future[HistoryChunk] = {
    val requestUri = makeUri(
      "channels.history",
      "channel" -> channel, "latest" -> latest, "oldest" -> oldest, "inclusive" -> inclusive, "count" -> count
    )
    SlackApiClient.get[HistoryChunk](
      sendReceive ~> unmarshal[HistoryChunk],
      requestUri
    )
  }
}
