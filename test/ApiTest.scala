import api.{Advice, Station}
import controllers.routes
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
      val stations = (responseNode \ "stations").as[JsArray].value
      stations.size must greaterThan(1)
      (stations.head \ "name").get.as[String] must equalTo("\'s-Hertogenbosch")
      (stations.head \ "code").get.as[String] must equalTo("HT")
      (stations.head \ "land").get.as[String] must equalTo("NL")
      (stations.head \ "coords" \ "lat").get.as[Double] must equalTo(51.69048)
      (stations.head \ "coords" \ "lon").get.as[Double] must equalTo(5.29362)
      (stations.head \ "synoniemen").get.as[JsArray].head.get.as[String] must equalTo("Hertogenbosch (\'s)")
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
      val req = FakeRequest(GET, controllers.routes.Api.search("den bosch").url)

      val Some(result) = route(req)
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      val content = contentAsString(result)

      content must contain("stations")
      val responseNode = Json.parse(contentAsString(result))

      val stations = (responseNode \ "stations").get.as[Seq[Station]]

      stations must have length(2)
    }
  }
}