package controllers

import akka.event.slf4j.SLF4JLogging
import akka.stream.Materializer
import javax.inject._
import play.api.http.HttpEntity
import play.api.libs.iteratee.Iteratee
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents)(
  implicit
  credentials: TwitterCredentials,
  ws: WSClient,
  mat: Materializer
) extends AbstractController(cc)
    with SLF4JLogging {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def tweetsToScreen: Action[AnyContent] = Action.async {
    val loggingIteratee = Iteratee.foreach[Array[Byte]] { array =>
      log.info(array.map(_.toChar).mkString)
    }
    credentials.getCredentials.map {
      case (consumerKey, requestToken) =>
        ws.url("https://stream.twitter.com/1.1/statuses/filter.json")
          .sign(OAuthCalculator(consumerKey, requestToken))
          .addQueryStringParameters("track" -> "lgbt")
          .addQueryStringParameters("language" -> "pt")
          .withMethod("GET")
          .withRequestTimeout(Duration.create(30, "s"))
          .stream()
          .map { response =>
            // Check that the response was successful
            if (response.status == 200) {

              // Get the content type
              val contentType = response.headers
                .get("Content-Type")
                .flatMap(_.headOption)
                .getOrElse("application/octet-stream")

              // If there's a content length, send that, otherwise return the body chunked
              response.headers.get("Content-Length") match {
                case Some(Seq(length)) =>
                  Ok.sendEntity(
                    HttpEntity
                      .Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType))
                  )
                case _ => {
                  Ok.chunked(response.bodyAsSource).as(contentType)
                }
              }
            } else {
              BadGateway
            }
          }
    } getOrElse {
      Future.failed(new RuntimeException("You did not correctly configure the Twitter credentials"))
    }
  }
}
