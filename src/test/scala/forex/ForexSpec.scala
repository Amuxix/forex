package forex

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.show._
import forex.ForexSpec._
import forex.config.{ ApplicationConfig, HttpConfig, OneFrameConfig }
import forex.domain.Currency._
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.http.rates.Protocol.GetApiResponse
import forex.services.RatesService
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.{ OneFrameRequestFailed, RateNotFound }
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io.GET
import org.http4s.{ EntityDecoder, HttpApp, Status, Uri }
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Assertion, OptionValues }
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._

class ForexSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with ScalaCheckPropertyChecks
    with Http4sClientDsl[IO] {
  private val config = ApplicationConfig(
    HttpConfig(
      "localhost",
      80,
      40.seconds,
      1.seconds
    ),
    OneFrameConfig(
      "",
      81,
      ""
    )
  )

  private val defaultPrice = Price(1)
  private val okInterpreter: RatesService[IO] =
    (pair: Pair) => Rate(pair, defaultPrice, Timestamp.now).asRight[Error].pure[IO]

  private def okApp: HttpApp[IO] = new Module[IO](okInterpreter, config).httpApp

  def defaultMatcher(from: Currency, to: Currency): Option[GetApiResponse => Boolean] = Some { (rate: GetApiResponse) =>
    rate.from == from && rate.to == to && rate.price == defaultPrice
  }
  def any[A]: Option[A => Boolean] = Some(_ => true)

  def check[A](
      app: HttpApp[IO],
      uriModifier: Uri => Uri,
      expectedStatus: Status,
      expectedBodyMatcher: Option[A => Boolean]
  )(
      implicit ev: EntityDecoder[IO, A]
  ): Assertion = {
    val response = app.run(GET(uriModifier(uri))).unsafeRunSync()

    expectedBodyMatcher.fold {
      // Verify Response's body is empty.
      val bodyIsEmpty = response.body.compile.toVector.unsafeRunSync().isEmpty
      assert(bodyIsEmpty, "Body must be empty")
    } { matcher =>
      matcher(response.as[A].unsafeRunSync()) shouldBe true
    }

    response.status shouldEqual expectedStatus
  }

  private val uri = Uri.unsafeFromString("/rates")

  //Generator for uppercase string of length 3
  private val countryStringGen: Gen[String]                   = Gen.stringOfN(3, Gen.alphaUpperChar)
  private val currencyGen: Gen[Currency]                      = Gen.oneOf(Currency.currencies.keys)
  private implicit val currencyArbitrary: Arbitrary[Currency] = Arbitrary(currencyGen)

  "Forex should" - {
    "Fail when" - {
      "An invalid currency is given in either or both positions" in {
        val currencies = Currency.currencies.values.toSet
        forAll(currencyGen, countryStringGen) { (from: Currency, to: String) =>
          whenever(!currencies.contains(to)) {
            check(okApp, _.addStringQueryParams(from.show, to), Status.BadRequest, any[String])
            check(okApp, _.addStringQueryParams(to, from.show), Status.BadRequest, any[String])
            check(okApp, _.addStringQueryParams(to, to), Status.BadRequest, any[String])
          }
        }
      }
      "Two equal currencies are given" in {
        forAll { currency: Currency =>
          check(okApp, _.addCurrencyQueryParams(currency, currency), Status.BadRequest, any[String])
        }
      }

      "OneFrame returns no results" in {
        def noRateInterpreter: RatesService[IO] = _ => RateNotFound("").asLeft[Rate].pure[IO]
        def noRateApp                           = new Module[IO](noRateInterpreter, config).httpApp
        forAll { (from: Currency, to: Currency) =>
          whenever(from != to) {
            check(noRateApp, _.addCurrencyQueryParams(from, to), Status.NotFound, any[String])
          }
        }
      }
      "OneFrame does not respond" in {
        def requestFailInterpreter: RatesService[IO] = _ => OneFrameRequestFailed("").asLeft[Rate].pure[IO]
        def requestFailApp                           = new Module[IO](requestFailInterpreter, config).httpApp
        forAll { (from: Currency, to: Currency) =>
          whenever(from != to) {
            check(requestFailApp, _.addCurrencyQueryParams(from, to), Status.ServiceUnavailable, any[String])
          }
        }
      }
    }
    "Give cached request if a second request is done before cache expiration time" in {
      val from    = USD
      val to      = JPY
      val request = GET(uri.addCurrencyQueryParams(from, to))
      val app     = okApp
      def getApiResponse: IO[GetApiResponse] =
        for {
          response <- app.run(request)
          apiResponse <- response.as[GetApiResponse]
        } yield apiResponse

      (for {
        firstResponse <- getApiResponse
        _ <- IO.sleep(config.http.cacheDuration / 2)
        secondResponse <- getApiResponse
        _ <- IO.sleep(config.http.cacheDuration)
        thirdResponse <- getApiResponse
      } yield {
        secondResponse shouldEqual firstResponse
        thirdResponse shouldNot equal(firstResponse)
      }).unsafeRunSync()
    }
    "Give rates for the correct rate even if currencies are not uppercase" in {
      check(okApp, _.addStringQueryParams("usd", "jPy"), Status.Ok, defaultMatcher(USD, JPY))
    }
    "Give rates for the correct rate for all possible currency pairs" in {
      forAll { (from: Currency, to: Currency) =>
        whenever(from != to) {
          check(okApp, _.addCurrencyQueryParams(from, to), Status.Ok, defaultMatcher(from, to))
        }
      }
    }
  }
}

object ForexSpec {
  implicit class UriOps(private val uri: Uri) extends AnyVal {
    def addStringQueryParams(from: String, to: String): Uri =
      uri.withQueryParam("from", from).withQueryParam("to", to)
    def addCurrencyQueryParams(from: Currency, to: Currency): Uri =
      uri.withQueryParam("from", from.show).withQueryParam("to", to.show)
  }
}
