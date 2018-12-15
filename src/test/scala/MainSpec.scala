import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.io.Source

class MainSpec extends FlatSpecLike with Matchers with BeforeAndAfterAll {

  "This test" should "provide a server" in {

    val routes: Route = path("foo") {
      println("inside route")
      complete("bar")
    }

    MockServer.calling("localhost", 2016, routes) { (host, port) =>
      println("inside test")
      val load = Source.fromURL(s"http://127.0.0.1:$port/foo").mkString
      println(s"load = $load")
      load shouldBe "bar"
    }

  }
}
