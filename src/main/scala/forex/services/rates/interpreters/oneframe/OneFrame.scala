package forex.services.rates.interpreters.oneframe

import cats.effect.Concurrent
import cats.syntax.bifunctor._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.traverse._
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.http.rates.Protocol.GetApiResponse
import forex.services.rates.Algebra
import forex.services.rates.errors.Error._
import forex.services.rates.errors._
import org.http4s.Uri
import org.http4s.Uri.Path
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.dsl._
import org.http4s.dsl.io.GET
import org.http4s.implicits.http4sLiteralsSyntax

class OneFrame[F[_]: Concurrent](client: Client[F], config: OneFrameConfig) extends Algebra[F] with Http4sClientDsl[F] {

  private def withUri[R](path: Path)(f: Uri => F[Either[Error, R]]): F[Either[Error, R]] =
    Uri
      .fromString(s"http://${config.host}:${config.port}")
      .leftMap(error => UriCreationFailed(error.details))
      .leftWiden[Error]
      .map(_.withPath(path))
      .flatTraverse(f)

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    withUri(path"/rates") { uri =>
      client
        .expect[List[GetApiResponse]](
          GET.apply(
            uri.withQueryParam("pair", s"${pair.from}${pair.to}"),
            "token" -> config.token,
          )
        )
        .map(_.headOption.toRight(OneFrameLookupFailed(s"Could not find rate for $pair")).map { rate =>
          Rate(pair, rate.price, rate.timeStamp)
        })
    }
}
