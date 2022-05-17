package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameRequestFailed(msg: String) extends Error
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class UriCreationFailed(details: String) extends Error
  }

}
