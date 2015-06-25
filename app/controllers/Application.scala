package controllers

import play.api.mvc._

class Application extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.index("Your new application is ready."))
  }

  def search = Action { implicit request =>
    Ok(views.html.search(""))
  }
}
