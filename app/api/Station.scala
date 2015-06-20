package api

import play.api.libs.json._

import scala.xml._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by tomas on 10-06-15.
 */

case class Namen(kort: String, middel: String, lang: String)

object Namen {
  def parseNamen(el: NodeSeq) = {
    Namen((el \ "Kort").text, (el \ "Middel").text, (el \ "Lang").text)
  }

  implicit val writesNamen: Writes[Namen] = new Writes[Namen] {
    override def writes(namen: Namen): JsValue = {
      Json.obj(
        "kort" -> namen.kort,
        "middel" -> namen.middel,
        "lang" -> namen.lang
      )
    }
  }

  implicit val readsNamen: Reads[Namen] = (
    (JsPath \ "kort").read[String] and
      (JsPath \ "middel").read[String] and
      (JsPath \ "lang").read[String]
    )(Namen.apply _)
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

  implicit val readsLatLng: Reads[LatLng] = (
    (JsPath \ "lat").read[Double] and
      (JsPath \ "lon").read[Double]
    )(LatLng.apply _)
}

case class Station(code: String,
                   names: Namen,
                   land: String,
                   UICCode: String,
                   coords: LatLng,
                   synoniemen: Seq[String]) {
  val name = names.lang
}

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
        "name" -> station.names.lang,
        "names" -> Json.toJson(station.names),
        "uiccode" -> station.UICCode,
        "code" -> station.code,
        "land" -> station.land,
        "coords" -> Json.toJson(station.coords),
        "synoniemen" -> Json.toJson(station.synoniemen)
      )
    }
  }

  implicit val readsStation: Reads[Station] = (
    (JsPath \ "code").read[String] and
      (JsPath \ "names").read[Namen] and
      (JsPath \ "land").read[String] and
      (JsPath \ "uiccode").read[String] and
      (JsPath \ "coords").read[LatLng] and
      (JsPath \ "synoniemen").read[Seq[String]]
    )(Station.apply _)
}