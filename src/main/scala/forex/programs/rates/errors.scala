package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class RateRequestFailed(msg: String) extends Error
    final case class UriCreationFailed(msg: String) extends Error
    final case class InvalidCurrency(msg: String) extends Error
    final case class InvalidCurrencyPair(msg: String) extends Error
    final case class RateNotFound(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.RateNotFound(msg)          => Error.RateNotFound(msg)
    case RatesServiceError.OneFrameLookupFailed(msg)  => Error.RateLookupFailed(msg)
    case RatesServiceError.OneFrameRequestFailed(msg) => Error.RateRequestFailed(msg)
    case RatesServiceError.UriCreationFailed(msg)     => Error.UriCreationFailed(msg)
  }

}
