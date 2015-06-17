package controllers

import java.io.FileReader

import play.api.mvc.{Action, Controller}

import scala.io.Source

/**
 * Created by tomas on 15-06-15.
 */
class Log extends Controller {
  def log = Action {
    val source = Source.fromFile("notify.log")
    val lines = try source.mkString finally source.close()
    Ok(lines)
  }
}
