package forex

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.effect.kernel.Concurrent
import cats.syntax.either._
import forex.programs.rates.errors.Error
import forex.programs.rates.errors.Error._
import io.circe.generic.extras.decoding.{ EnumerationDecoder, UnwrappedDecoder }
import io.circe.generic.extras.encoding.{ EnumerationEncoder, UnwrappedEncoder }
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, EntityEncoder, ParseFailure, Response }

package object http {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A] = implicitly
  implicit def enumDecoder[A: EnumerationDecoder]: Decoder[A] = implicitly

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Concurrent]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder, F[_]]: EntityEncoder[F, A]             = jsonEncoderOf[F, A]

  implicit class ParsingErrorOps[A](private val parse: ValidatedNel[ParseFailure, A]) extends AnyVal {
    def validateCurrency: Either[InvalidCurrency, A] =
      parse.toEither.leftMap(errors => InvalidCurrency(errors.head.sanitized))
  }

  implicit class ErrorOps[F[_]: Sync](val error: Error) extends Http4sDsl[F] {
    def toResponse: F[Response[F]] = error match {
      case InvalidCurrency(msg)     => BadRequest(msg.asJson)
      case InvalidCurrencyPair(msg) => BadRequest(msg.asJson)
      case RateLookupFailed(msg)    => BadRequest(msg.asJson)
      case RateNotFound(msg)        => NotFound(msg.asJson)
      case RateRequestFailed(_)     => ServiceUnavailable("External service error".asJson)
      case UriCreationFailed(msg)   => InternalServerError(msg.asJson)
    }
  }
}
