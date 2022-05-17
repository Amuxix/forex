package forex.services.rates

import cats.Applicative
import cats.effect.Async
import forex.config.OneFrameConfig
import org.http4s.client.Client

import interpreters.oneframe._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Async](httpClient: Client[F], config: OneFrameConfig): Algebra[F] =
    new OneFrame[F](httpClient, config)
}
