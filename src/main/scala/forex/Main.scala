package forex

import cats.effect._
import forex.config._
import forex.services.RatesServices
import fs2.Stream
import org.http4s.Headers
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.middleware.Logger
import org.typelevel.ci.CIString

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream.compile.drain.as(ExitCode.Success)

}

class Application[F[_]: Async] {

  private def defaultRedactHeadersWhen(name: CIString): Boolean =
    Headers.SensitiveHeaders.contains(name) || name.toString.toLowerCase.contains("token")

  def stream: Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- Stream.resource(BlazeClientBuilder[F].resource)
      loggingClient = Logger(logHeaders = true, logBody = true, redactHeadersWhen = defaultRedactHeadersWhen)(client)
      rateService   = RatesServices.live[F](loggingClient, config.oneFrame)
      module        = new Module[F](rateService, config)
      _ <- BlazeServerBuilder[F]
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
