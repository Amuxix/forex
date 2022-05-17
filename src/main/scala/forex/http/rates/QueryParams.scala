package forex.http.rates

import forex.domain.Currency
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder }

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] = {
    def errorMessage(currency: String) = s"\"$currency\" is not a supported currency!"
    QueryParamDecoder[String].emap { currency =>
      Currency.fromString(currency).toRight(ParseFailure(errorMessage(currency), errorMessage(currency)))
    }
  }

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
