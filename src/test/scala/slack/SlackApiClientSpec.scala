package slack

import org.scalatest.{Matchers, FlatSpec}
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SlackApiClientSpec extends FlatSpec() with Matchers {

  val conf = ConfigFactory.load()
  val slack = SlackApiClient(conf.getString("slack.token"))

  "An SlackApiClient" should "be able to get channel list" in {
    val f = slack.listChannels(0)
    val result = Await.result(f, Duration.Inf)
    result.ok should be (true)
    result.channels.toArray should not be empty
  }

}
