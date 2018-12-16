import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

object MockServer extends LazyLogging {

  def calling(routes: Route, host: String = "localhost", port: Int = 2020)(f: (String, String) => Unit): Unit = {
    implicit val theSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = theSystem.dispatcher

    // Error handling
    val errors: mutable.Buffer[HttpRequest] = mutable.Buffer[HttpRequest]()
    def errorsString = errors.map(req => s"${req.method.value} ${req.uri.toString()}").mkString("\n")

    var mockWasCalledAtLeastOnce = false

    def failOnUnexpectedRequests(throwable: Option[Throwable] = None): Unit = {
      throwable match {
        case Some(t) if errors.isEmpty           => throw t
        case Some(t: UnexpectedRequestException) => throw t
        case Some(t)                             => throw new Exception(s"Error in test: $t and ${errors.size} unexpected request(s) occurred: $errorsString")
        case None if errors.nonEmpty             => throw new UnexpectedRequestException(s"${errors.size} unexpected request(s) occurred: $errorsString")
        case None if !mockWasCalledAtLeastOnce   => throw new Exception("Error in test: MockServer was not called.")
        case None                                =>
      }
    }

    // Extend given routes by error handling
    val routesPlusErrorHandling = extractRequestContext { ctx =>
      mockWasCalledAtLeastOnce = true
      routes
    } ~
      extractRequestContext { ctx =>
        errors.append(ctx.request)
        complete(StatusCodes.BadRequest)
      }

    // Bind route to socket
    val bindingFuture = Http().bindAndHandle(handler = routesPlusErrorHandling, interface = host, port = port, settings = ServerSettings(theSystem))

    // Run tests and aggregate errors
    val testRun = bindingFuture.map { binding =>
      logger.info(s"server started at $binding, running test…")
      try {
        // run test
        f(binding.localAddress.getHostName, binding.localAddress.getPort.toString)
        failOnUnexpectedRequests()
      } catch {
        case throwable: Throwable =>
          failOnUnexpectedRequests(Some(throwable))
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

  class UnexpectedRequestException(message: String) extends Exception(message)
}
