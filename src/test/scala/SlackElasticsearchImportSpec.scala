import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import akka.actor.{ Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import scala.concurrent.duration._

class SlackElasticsearchImportSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("SlackElasticsearchImportSpec"))

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  "An HelloAkkaActor" should "be able to set a new greeting" in {
    val greeter = TestActorRef(Props[Greeter])
    greeter ! WhoToGreet("testkit")
    greeter.underlyingActor.asInstanceOf[Greeter].greeting should be("hello, testkit")
  }

  it should "be able to get a new greeting" in {
    val greeter = system.actorOf(Props[Greeter], "greeter")
    greeter ! WhoToGreet("testkit")
    greeter ! Greet
    expectMsgType[Greeting].message.toString should be("hello, testkit")
  }
}