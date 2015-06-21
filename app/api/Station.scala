package api

import play.api.libs.json._

import scala.xml._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by tomas on 10-06-15.
 */

case class Names(short: String, middle: String, long: String)

object Names {
  def parseNames(el: NodeSeq) = {
    Names((el \ "Kort").text, (el \ "Middel").text, (el \ "Lang").text)
  }

  implicit val writesNames: Writes[Names] = new Writes[Names] {
    override def writes(names: Names): JsValue = {
      Json.obj(
        "short" -> names.short,
        "middle" -> names.middle,
        "long" -> names.long
      )
    }
  }

  implicit val readsNames: Reads[Names] = (
    (JsPath \ "short").read[String] and
      (JsPath \ "middle").read[String] and
      (JsPath \ "long").read[String]
    )(Names.apply _)
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
                   stationType: String,
                   names: Names,
                   land: String,
                   UICCode: String,
                   coords: LatLng,
                   synonyms: Seq[String]) {
  val name = names.long
}

object Station {
  def parseStation(el: Node) = {
    Station(
      (el \ "Code").text,
      (el \ "Type").text,
      Names.parseNames(el \ "Namen"),
      (el \ "Land").text,
      (el \ "UICCode").text,
      LatLng.parseLatLng(el),
      (el \ "Synoniemen" \ "Synoniem").map(_.text)
    )
  }

  implicit val writesStation: Writes[Station] = new Writes[Station] {
    override def writes(station: Station): JsValue = {
      Json.obj(
        "name" -> station.names.long,
        "type" -> station.stationType,
        "names" -> Json.toJson(station.names),
        "uiccode" -> station.UICCode,
        "code" -> station.code,
        "land" -> station.land,
        "coords" -> Json.toJson(station.coords),
        "synonyms" -> Json.toJson(station.synonyms)
      )
    }
  }

  implicit val readsStation: Reads[Station] = (
    (JsPath \ "code").read[String] and
      (JsPath \ "type").read[String] and
      (JsPath \ "names").read[Names] and
      (JsPath \ "land").read[String] and
      (JsPath \ "uiccode").read[String] and
      (JsPath \ "coords").read[LatLng] and
      (JsPath \ "synonyms").read[Seq[String]]
    )(Station.apply _)
}