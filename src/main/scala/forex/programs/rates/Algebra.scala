package forex.programs.rates

import cats.data.EitherT
import forex.domain.Rate

import errors._

trait Algebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): EitherT[F, Error, Rate]
}
