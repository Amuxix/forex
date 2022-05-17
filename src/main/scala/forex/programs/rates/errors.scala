package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class RateRequestFailed(msg: String) extends Error
    final case class UriCreationFailed(msg: String) extends Error
    final case class InvalidCurrency(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.OneFrameRequestFailed(msg) =>
      println("Transforming OneFrameRequestFailed")
      Error.RateRequestFailed(msg)
    case RatesServiceError.UriCreationFailed(msg) => Error.UriCreationFailed(msg)
  }

}
