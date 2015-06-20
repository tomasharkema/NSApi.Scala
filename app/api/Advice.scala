package api

import org.joda.time.DateTime
import play.api.libs.json._

import scala.xml._
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
        "ernstig" -> melding.ernstig, "text" -> melding.text
      )
    }
  }

  implicit val readsMelding: Reads[Melding] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "ernstig").read[Boolean] and
      (JsPath \ "text").read[String]
    )(Melding.apply _)
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

  implicit val readsStop: Reads[Stop] = new Reads[Stop] {
    override def reads(json: JsValue): JsResult[Stop] = {
      JsSuccess(Stop(
        (json \ "time").get.as[DateTime],
        (json \ "spoor").get.asOpt[String],
        (json \ "name").get.as[String]
      ))
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
        "stops" -> reisDeel.stops
      )
    }
  }

  implicit val readsReisDeel: Reads[ReisDeel] = new Reads[ReisDeel] {
    override def reads(json: JsValue): JsResult[ReisDeel] = {
      JsSuccess(ReisDeel(
        vervoerder = (json \ "vervoerder").get.as[String],
        vervoerType = (json \ "vervoerType").get.as[String],
        stops = (json \ "stops").get.as[Seq[Stop]]
      ))
    }
  }
}

case class AdviceRequest(from: String, to: String)

object AdviceRequest {
  implicit val readsAdviceRequest: Reads[AdviceRequest] = new Reads[AdviceRequest] {
    override def reads(json: JsValue): JsResult[AdviceRequest] = {
      for {
        from <- (json \ "from").validate[String]
        to <- (json \ "to").validate[String]
      } yield AdviceRequest(from, to)
    }
  }
}

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
      melding = (el \ "Melding").headOption.map(Melding.parseMelding),
      reisDeel = (el \\ "ReisDeel").map(ReisDeel.parseReisdeel),
      vertrekVertraging = (el \ "AankomstVertraging").headOption.map(_.text),
      status = (el \ "Status").text,
      AdviceRequest(from, to)
    )
  }

  implicit val writesAvice: Writes[Advice] = new Writes[Advice] {
    override def writes(advice: Advice): JsValue = {
      Json.obj(
        "overstappen" -> advice.overstappen,
        "vertrek" -> Json.toJson(advice.vertrek),
        "aankomst" -> Json.toJson(advice.aankomst),
        "melding" -> Json.toJson(advice.melding),
        "reisDeel" -> Json.toJson(advice.reisDeel),
        "vertrekVertraging" -> advice.vertrekVertraging,
        "status" -> advice.status,
        "request" -> Map("from" -> advice.request.from, "to" -> advice.request.to)
      )
    }
  }

  implicit val readsAdvice: Reads[Advice] = new Reads[Advice] {
    override def reads(json: JsValue) = {
      JsSuccess(Advice(
        overstappen = (json \ "overstappen").get.as[Int],
        vertrek = OVTime(planned = (json \ "vertrek" \ "planned").get.as[DateTime], actual = (json \ "vertrek" \ "actual").get.as[DateTime]),
        aankomst = OVTime(planned = (json \ "aankomst" \ "planned").get.as[DateTime], actual = (json \ "aankomst" \ "actual").get.as[DateTime]),
        melding = (json \ "melding").asOpt[Melding],
        reisDeel = (json \ "reisDeel").get.as[Seq[ReisDeel]],
        vertrekVertraging = (json \ "vertrekVertraging").get.asOpt[String],
        status = (json \ "status").get.as[String],
        request = (json \ "request").get.as[AdviceRequest]
      ))
    }
  }
}