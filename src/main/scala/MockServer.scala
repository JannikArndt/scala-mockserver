import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

object MockServer extends LazyLogging {
  def calling(routes: Route, host: String = "localhost", port: Int = 2020)(f: (String, String) => Unit): Unit = {
    implicit val theSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = theSystem.dispatcher

    val bindingFuture = Http().bindAndHandle(handler = routes, interface = host, port = port, settings = ServerSettings(theSystem))

    val testRun = bindingFuture.map { binding =>
      logger.info(s"server started at $binding, running test…")
      try {
        // run test
        f(binding.localAddress.getHostName, binding.localAddress.getPort.toString)
      } finally {
        // unbind, so the port can be reused by the next test
        logger.info("Test finished, terminating…")
        binding.terminate(1.second)
        theSystem.terminate()
        logger.info("System terminated.")
      }
    }

    Await.result(testRun, 20.seconds)

  }
}
