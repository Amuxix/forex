package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import forex.domain._
import forex.services.RatesService

import errors._

class Program[F[_]: Functor](
    ratesService: RatesService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): EitherT[F, Error, Rate] =
    EitherT(ratesService.get(Rate.Pair(request.from, request.to))).leftMap(toProgramError)

}

object Program {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)

}
