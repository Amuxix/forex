package forex.middleware

import cats.data.{ Kleisli, OptionT }
import cats.effect._
import cats.implicits._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.http4s._
import org.http4s.headers._
import org.http4s.server.Middleware

import scala.concurrent.duration.FiniteDuration

//Base code taken from https://gist.github.com/Slakah/670e932dace0b669c55eae3d6b386a54
class HttpCaching[F[_]: Sync, G[_]] private (duration: FiniteDuration) {

  private val cache: Cache[(Uri, Method), Response[G]] = Scaffeine().expireAfterWrite(duration).build()

  private def getCached(request: Request[G], http: Http[F, G]): F[Response[G]] =
    if (request.method.isSafe) {
      getIfPresent(request).getOrElseF(httpAndCache(request, http))
    } else {
      http(request)
    }

  private def getIfPresent(request: Request[G]): OptionT[F, Response[G]] =
    OptionT(Sync[F].delay(cache.getIfPresent(request.uri -> request.method)))

  private def httpAndCache(request: Request[G], http: Http[F, G]): F[Response[G]] =
    for {
      resp <- http(request)
      shouldCache = isCacheable(resp)
      _ <- if (shouldCache) {
            Sync[F].delay(cache.put(request.uri -> request.method, resp))
          } else {
            Sync[F].unit
          }
    } yield resp

  private def isCacheable(resp: Response[G]): Boolean = {
    import Status._
    !resp.headers
      .get[`Cache-Control`]
      .exists(_.values.exists(_ == CacheDirective.`no-store`)) &&
    (resp.status match {
      case Ok                          => true
      case NonAuthoritativeInformation => true
      case NoContent                   => true
      case PartialContent              => true
      case MultipleChoices             => true
      case MovedPermanently            => true
      case NotFound                    => true
      case MethodNotAllowed            => true
      case Gone                        => true
      case UriTooLong                  => true
      case NotImplemented              => true
      case _                           => false
    })
  }

  def caching: Middleware[F, Request[G], Response[G], Request[G], Response[G]] =
    http =>
      Kleisli { request =>
        getCached(request, http)
    }
}

object HttpCaching {
  def apply[F[_]: Sync, G[_]](duration: FiniteDuration)(http: Http[F, G]): Http[F, G] =
    new HttpCaching(duration).caching(http)
}
