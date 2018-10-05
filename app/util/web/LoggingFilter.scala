package util.web

import javax.inject.Inject
import akka.stream.Materializer
import util.FutureUtils.defaultContext
import play.api.mvc._
import util.Logging

import scala.concurrent.Future

class LoggingFilter @Inject() (override implicit val mat: Materializer) extends Filter with Logging {
  val metricsName = util.Config.metricsId + "_http_requests"

  def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val startNanos = System.nanoTime

    nextFilter(request).transform(
      result => {
        if (request.path.startsWith("/assets")) {
          result
        } else {
          val requestTime = System.nanoTime - startNanos
          log.info(s"${result.header.status} (${requestTime / 1000000000.0}s): ${request.method} ${request.uri}")
          result.withHeaders("X-Request-Time-Ms" -> (requestTime * 1000000).toInt.toString)
        }
      },
      exception => {
        exception
      }
    )
  }
}