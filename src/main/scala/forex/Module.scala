package forex

import cats.effect.Async
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.middleware.HttpCaching
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Caching, Timeout}
import org.typelevel.ci.CIString

class Module[F[_]: Async](config: ApplicationConfig, client: Client[F]) {
  def defaultRedactHeadersWhen(name: CIString): Boolean =
    Headers.SensitiveHeaders.contains(name) || name.toString.toLowerCase.contains("token")

  private val loggingClient = Logger(logHeaders = true, logBody = true, redactHeadersWhen = defaultRedactHeadersWhen)(client)

  private val ratesService: RatesService[F] = RatesServices.live[F](loggingClient, config.oneFrame)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    List[HttpApp[F] => HttpApp[F]](
      Timeout(config.http.timeout),
      Caching.publicCache(config.http.cacheDuration, _), //Add caching headers
      HttpCaching(config.http.cacheDuration), //Actually does the caching itself
    )
      .foldLeft(http) {
        case (http, middleware) => middleware(http)
      }
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
