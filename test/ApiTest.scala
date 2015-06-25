import api.{Advice, Station}
import controllers.routes
import global.Global
import org.joda.time.DateTime
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._


/**
 * Created by tomas on 17-06-15.
 */
@RunWith(classOf[JUnitRunner])
class ApiTest extends Specification {

  "Api" should {
    "give stations" in new WithApplication {

      val req = FakeRequest(GET, controllers.routes.Api.stations.url)

      val Some(result) = route(req)
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val content = contentAsString(result)

      content must contain("stations")

      val responseNode = Json.parse(contentAsString(result))
      val stations = (responseNode \ "stations").get.as[Seq[Station]]
      stations.size must greaterThan(1)
      val station = stations.head

      station.name must equalTo("\'s-Hertogenbosch")
      station.code must equalTo("HT")
      station.land must equalTo("NL")
      station.coords.lat must equalTo(51.69048)
      station.coords.lon must equalTo(5.29362)
      station.synonyms.head must equalTo("Hertogenbosch (\'s)")
    }

    "should give advice" in new WithApplication {

      val req = FakeRequest(GET, controllers.routes.Api.advices("KBW", "ASD").url)

      val Some(result) = route(req)
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val content = contentAsString(result)

      content must contain("advices")

      val responseNode = Json.parse(contentAsString(result))
      (responseNode \ "count").as[Int] must greaterThan(1)
      val advices = (responseNode \ "advices").get.as[Seq[Advice]]

      advices.map { advice =>
        advice.overstappen must greaterThanOrEqualTo(0)
        advice.vertrek.planned.getMillis must lessThanOrEqualTo(advice.vertrek.actual.getMillis)
        advice.aankomst.planned.getMillis must lessThanOrEqualTo(advice.aankomst.actual.getMillis)
        advice.vertrek.planned.getMillis must lessThan(advice.aankomst.planned.getMillis)
      }
    }
  }

  "Search Api" should {
    "give right stations for queryies" in new WithApplication {
      val req = FakeRequest(GET, controllers.routes.Api.search("'s-Hertogenbosch").url)

      val Some(result) = route(req)
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val content = contentAsString(result)

      content must contain("stations")
      val responseNode = Json.parse(contentAsString(result))

      val stations = (responseNode \ "stations").get.as[Seq[JsValue]].map { obj =>
        (obj \ "station").as[Station]
      }
      stations must have length greaterThan(0)
      stations.head.code must be equalTo "HT"
    }
  }

  "Register Api" should {
    "register user with push" in new WithApplication() {
      val req = FakeRequest(GET, controllers.routes.Api.registerUUID("tomas_TEST", "PUSH", "FAKE_TOKEN").url)

      val Some(result) = route(req)
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val responseNode = Json.parse(contentAsString(result))

      (responseNode \ "success").get.as[Boolean] must be equalTo true
    }

    "not register user with abigous type" in new WithApplication() {
      val req = FakeRequest(GET, controllers.routes.Api.registerUUID("tomas_TEST", "ABC", "FAKE_TOKEN").url)

      val Some(result) = route(req)
      status(result) must equalTo(NOT_FOUND)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val responseNode = Json.parse(contentAsString(result))

      (responseNode \ "success").get.as[Boolean] must be equalTo false
    }

    "register user with existing stations" in new WithApplication() {
      val req = FakeRequest(GET, controllers.routes.Api.registerStation("tomas_TEST", "ASD", "KBW").url)

      val Some(result) = route(req)
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val responseNode = Json.parse(contentAsString(result))

      (responseNode \ "success").get.as[Boolean] must be equalTo true
    }

    "not register user with non-existing stations" in new WithApplication() {
      val req = FakeRequest(GET, controllers.routes.Api.registerStation("tomas_TEST", "ABC", "DEF").url)

      val Some(result) = route(req)
      status(result) must equalTo(NOT_FOUND)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val responseNode = Json.parse(contentAsString(result))

      (responseNode \ "success").get.as[Boolean] must be equalTo false
    }
  }
}