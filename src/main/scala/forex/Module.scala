package forex

import cats.effect.Async
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.middleware.HttpCaching
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Caching, Timeout }

class Module[F[_]: Async](
    ratesService: RatesService[F],
    config: ApplicationConfig
) {
  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private def routesMiddleware(http: HttpRoutes[F]): HttpRoutes[F] = AutoSlash(http)

  private def appMiddleware(http: HttpApp[F]): HttpApp[F] =
    List[HttpApp[F] => HttpApp[F]](
      Timeout(config.http.timeout),
      Caching.publicCache(config.http.cacheDuration, _), //Add caching headers
      HttpCaching(config.http.cacheDuration) //Actually does the caching itself
    ).foldLeft(http) {
      case (http, middleware) => middleware(http)
    }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
