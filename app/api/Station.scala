package api

import play.api.libs.json.{JsValue, Writes, JsArray, Json}

import scala.xml._

/**
 * Created by tomas on 10-06-15.
 */

case class Namen(kort: String, middel: String, lang: String)

object Namen {
  def parseNamen(el: NodeSeq) = {
    Namen((el \ "Kort").text, (el \ "Middel").text, (el \ "Lang").text)
  }
}

case class LatLng(lat: Double, lon: Double)

object LatLng {
  def parseLatLng(el: Node) = {
    LatLng((el \ "Lat").text.toDouble, (el \ "Lon").text.toDouble)
  }

  implicit val writesLatLng: Writes[LatLng] = new Writes[LatLng] {
    override def writes(ll: LatLng): JsValue = {
      Json.obj("lat" -> ll.lat, "lon" -> ll.lon)
    }
  }
}

case class Station(code: String,
                   name: Namen,
                   land: String,
                   UICCode: String,
                   coords: LatLng,
                   synoniemen: Seq[String])

object Station {
  def parseStation(el: Node) = {
    Station(
      (el \ "Code").text,
      Namen.parseNamen(el \ "Namen"),
      (el \ "Land").text,
      (el \ "UICCode").text,
      LatLng.parseLatLng(el),
      (el \ "Synoniemen" \ "Synoniem").map(_.text)
    )
  }

  implicit val writesStation: Writes[Station] = new Writes[Station] {
    override def writes(station: Station): JsValue = {
      Json.obj(
        "name" -> station.name.lang,
        "code" -> station.code,
        "land" -> station.land,
        "coords" -> Json.toJson(station.coords),
        "synoniemen" -> Json.toJson(station.synoniemen)
      )
    }
  }
}