package forex.http.rates

import forex.domain._

object Converters {
  import Protocol._

  private[rates] implicit class GetApiResponseOps(private val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timeStamp = rate.timestamp
      )
  }

}
