package forex.services.rates

import cats.effect.Async
import forex.config.OneFrameConfig
import org.http4s.client.Client

import interpreters._

object Interpreters {
  def live[F[_]: Async](httpClient: Client[F], config: OneFrameConfig): Algebra[F] = new OneFrame[F](httpClient, config)
}
