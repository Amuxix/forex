package forex.http
package rates

import cats.data.EitherT
import cats.effect.Sync
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.InvalidCurrencyPair
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      (for {
        from <- EitherT.fromEither(from.validateCurrency)
        to <- EitherT.fromEither(to.validateCurrency)
        _ <- EitherT.cond(from != to, (), InvalidCurrencyPair(s"Two different rates are required to fetch an exchange rate for!"))
        rate <- EitherT(rates.get(RatesProgramProtocol.GetRatesRequest(from, to)))
      } yield rate.asGetApiResponse)
        .foldF(_.toResponse, Ok(_))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
