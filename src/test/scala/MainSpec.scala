import MockServer.calling
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.io.Source

class MainSpec extends FlatSpecLike with Matchers with BeforeAndAfterAll {

  "This test" should "start a server and check its response" in {

    val routes: Route = path("foo") {
      complete("bar")
    }

    calling(routes) { (host, port) =>
      val load = Source.fromURL(s"http://$host:$port/foo").mkString
      load shouldBe "bar"
    }

  }
}
