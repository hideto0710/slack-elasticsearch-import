package slack

import org.scalatest.{Matchers, FlatSpec}
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import slack.SlackApiClient.Slack

class SlackApiClientSpec extends FlatSpec() with Matchers {

  val conf = ConfigFactory.load()
  implicit val slack = Slack(conf.getString("slack.token"))

  "An SlackApiClient" should "be able to get channel list" in {
    val f = SlackApiClient.listChannels(0)
    val result = Await.result(f, Duration.Inf)
    result.ok should be (true)
    result.channels.toArray should not be empty
  }

  it should "be able to get channel list sync" in {
    val result = SlackApiClient.syncRequest(SlackApiClient.listChannels(0), 3)
    result match {
      case Right(r) =>
        r.ok should be (true)
        r.channels.get should not be empty
      case Left(e) => println(e)
    }
  }

  it should "be not able to get channel list because of toke" in {
    val f = SlackApiClient.listChannels(0)(Slack(conf.getString("slack.token")+"v"))
    val result = Await.result(f, Duration.Inf)
    result.ok should be (false)
    result.error.get should be ("invalid_auth")
  }
}
