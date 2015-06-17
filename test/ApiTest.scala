import api.Station
import controllers.routes
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._


/**
 * Created by tomas on 17-06-15.
 */
//@RunWith(classOf[JUnitRunner])
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
    }
  }

}