package api

import java.security.MessageDigest

import org.joda.time.DateTime
import play.api.libs.json.{Json, JsValue, Writes}

import scala.xml._

/**
 * Created by tomas on 14-06-15.
 */

case class OVTime(planned: DateTime,
                  actual: DateTime)

object OVTime {
  def fromString(plannedString: String, actualString: String) = {
    val planned = DateTime.parse(plannedString)
    val actual = DateTime.parse(actualString)
    OVTime(planned, actual)
  }

  implicit val writesOvTime: Writes[OVTime] = new Writes[OVTime] {
    override def writes(ovTime: OVTime): JsValue = {
      Json.obj(
        "planned" -> ovTime.planned,
        "actual" -> ovTime.actual
      )
    }
  }
}

case class Melding(id: String,
                   ernstig: Boolean,
                   text: String)

object Melding {
  def parseMelding(el: Node) = {
    Melding(
      id = (el \ "Id").text,
      ernstig = (el \ "Ernstig").text.toBoolean,
      text = (el \ "Text").text
    )
  }

  implicit val writesMelding: Writes[Melding] = new Writes[Melding] {
    override def writes(melding: Melding): JsValue = {
      Json.obj(
        "id" -> melding.id,
        "ernstig" -> melding.ernstig,
        "text" -> melding.text
      )
    }
  }
}

case class Stop(time: DateTime,
                spoor:Option[String],
                name: String)
object Stop {
  def parseStop(el: Node) = {
    Stop(
      time = DateTime.parse((el \ "Tijd").text),
      spoor = (el \\ "Spoor").headOption.map(_.text),
      name = (el \ "Naam").text
    )
  }

  implicit val writesStop: Writes[Stop] = new Writes[Stop] {
    override def writes(stop: Stop): JsValue = {
      Json.obj(
        "time" -> stop.time,
        "spoor" -> stop.spoor,
        "name" -> stop.name
      )
    }
  }
}

case class ReisDeel(vervoerder: String,
                    stops: Seq[Stop],
                    vervoerType: String)
object ReisDeel {
  def parseReisdeel(el: Node) = {
    ReisDeel(
      vervoerder = (el \ "Vervoerder").text,
      vervoerType = (el \ "VervoerType").text,
      stops = (el \\ "ReisStop").map(Stop.parseStop)
    )
  }

  implicit val writesReisDeel: Writes[ReisDeel] = new Writes[ReisDeel] {
    override def writes(reisDeel: ReisDeel): JsValue = {
      Json.obj(
        "vervoerder" -> reisDeel.vervoerder,
        "vervoerType" -> reisDeel.vervoerType,
        "name" -> reisDeel.stops
      )
    }
  }
}

case class AdviceRequest(from: String, to: String)

case class Advice(overstappen: Int,
                  vertrek: OVTime,
                  aankomst: OVTime,
                  melding: Option[Melding],
                  reisDeel: Seq[ReisDeel],
                  vertrekVertraging: Option[String],
                  status: String,
                  request: AdviceRequest) {
  val statusString = request.from + " " + request.to + " " + vertrekVertraging.getOrElse("")
}

object Advice {
  def parseAdvice(el: Node, from: String, to: String) = {
    Advice(
      overstappen = (el \ "AantalOverstappen").text.toInt,
      vertrek = OVTime.fromString((el \ "GeplandeVertrekTijd").text, (el \ "ActueleVertrekTijd").text),
      aankomst = OVTime.fromString((el \ "GeplandeAankomstTijd").text, (el \ "ActueleAankomstTijd").text),
      melding = None,
      reisDeel = (el \\ "ReisDeel").map(ReisDeel.parseReisdeel),
      vertrekVertraging = (el \ "AankomstVertraging").headOption.map(_.text),
      status = (el \ "Status").text,
      AdviceRequest(from, to)
    )
  }

  implicit val writesAdivce: Writes[Advice] = new Writes[Advice] {
    override def writes(advice: Advice): JsValue = {
      Json.obj(
        "overstappen" -> advice.overstappen,
        "vertrek" -> Json.toJson(advice.vertrek),
        "aankomst" -> Json.toJson(advice.aankomst),
        "melding" -> Json.toJson(advice.melding),
        "reisDeel" -> Json.toJson(advice.reisDeel),
        "vertrekVertraging" -> advice.vertrekVertraging,
        "status" -> advice.status
      )
    }
  }
}