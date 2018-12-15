import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object MockServer {
  def calling(host: String = "localhost", port: Int = 2019, routes: Route)(f: (String, String) => Unit): Unit = {
    println("starting server")
    implicit val theSystem: ActorSystem = ActorSystem(Logging.simpleName(this).replaceAll("\\$", ""))
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = theSystem.dispatcher

    val bindingFuture = Http().bindAndHandle(handler = routes, interface = host, port = port, settings = ServerSettings(theSystem))

    val testRun = bindingFuture.map { binding =>
      println(s"server started: $binding, running test…")
      try {
        f(binding.localAddress.getHostName, binding.localAddress.getPort.toString)
      } finally {
        println("Test finished, unbinding…")
        binding.unbind()
        theSystem.terminate()
      }
    }

    bindingFuture.onComplete {
      case Success(binding) ⇒
        println("server started")
      case Failure(cause) ⇒
        throw new Exception("Server could not be started", cause)
    }

    Await.result(testRun, 20.seconds)

  }
}
