import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import slack.models.SlackComment
import scala.collection.mutable
import scala.concurrent.duration._

class SlackElasticsearchImportSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("SlackElasticsearchImportSpec"))

  override def afterAll(): Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  private type ImportMap = mutable.Map[String, List[SlackComment]]

  "An ESActor" should "be able to set a new greeting" in {
    val es = TestActorRef(Props[ESActor])
    val cmt1 = SlackComment(None, Some("cmt_id1"), Some("test"), "test message1", "12345")
    val cmt2 = SlackComment(None, Some("cmt_id2"), Some("test"), "test message2", "12345")
    val cmt3 = SlackComment(None, Some("cmt_id3"), None, "test message3", "12345")
    es ! ESImportRequest(Channel("channel_id1","channel"), Seq(cmt1))
    es ! ESImportRequest(Channel("channel_id1","channel"), Seq(cmt2))
    es ! ESImportRequest(Channel("channel_id2","channel"), Seq(cmt3))

    val data: ImportMap = mutable.Map("channel_id1" -> (List(cmt1) ::: List(cmt2)), "channel_id2" -> List(cmt3))
    es.underlyingActor.asInstanceOf[ESActor].data should be(data)
  }

  /*it should "be able to get a new greeting" in {
    val greeter = system.actorOf(Props[Greeter], "greeter")
    greeter ! WhoToGreet("testkit")
    greeter ! Greet
    expectMsgType[Greeting].message.toString should be("hello, testkit")
  }*/
}