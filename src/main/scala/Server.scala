import akka.actor.{Props, Actor, ActorSystem}
import akka.http.Http
import akka.http.model.{StatusCodes, HttpResponse, HttpRequest}
import akka.http.server.{RouteResult, RequestContext, Route}
import akka.http.server.Directives._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import spray.json._
import akka.pattern.ask

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

class Timestamper extends Actor {
  import Timestamper._

  def currentNanos() = System.currentTimeMillis * 1000L

  var current = currentNanos()

  def receive = {
    case RequestTimestamp =>
      val value1 = currentNanos()
      current =
        if(value1 > current) value1
        else current + 1
      sender ! Timestamp(current)
  }
}

object Timestamper {

  case object RequestTimestamp

  case class Timestamp(value: Long)
}

class Archive extends Actor {
  import Archive._

  var counter = 0L

  val items = new ArrayBuffer[Seq[(String, JsValue)]]()

  var transposed = TreeMap.empty[String, ArrayBuffer[JsValue]]

  def getBuffer(k: String): ArrayBuffer[JsValue] = {
    transposed.get(k) match {
      case Some(v) => v
      case None =>
        val buffer = new ArrayBuffer[JsValue]()
        transposed = transposed.updated(k, buffer)
        buffer
    }
  }

  def receive = {
    case AddData(data) =>
      items += data
      for((k,v) <- data)
        getBuffer(k) += v
      counter += 1
      sender ! DataAdded
    case QueryHistory(prefix) =>
      val response = transposed.filter { case (k,v) => k startsWith prefix }
      sender ! History(response)
    case RequestCount =>
      sender ! Count(counter)
  }
}

object Archive {

  case class QueryHistory(prefix: String)

  case class History(data:Map[String, Seq[JsValue]])

  case object RequestCount

  case class Count(value: Long)

  case class AddData(data: Seq[(String, JsValue)])

  case object DataAdded

  val props = Props(classOf[Archive])
}

object Server extends App {

  implicit val system = ActorSystem("test")

  val archive = system.actorOf(Archive.props)

  // the flow materializer. required for the consume()
  implicit val materializer = ActorFlowMaterializer()

  implicit val ec = system.dispatcher

  implicit val timeout = Timeout(30.seconds)

  def store(data:Seq[(String, JsValue)]): Future[Any] =
    archive ? Archive.AddData(data)

  def getHandler(context: RequestContext): Future[RouteResult] =
    (archive ? Archive.RequestCount).mapTo[Archive.Count].flatMap { c =>
      context.complete(c.value.toString)
    }

  def getHistoryHandler(context: RequestContext): Future[RouteResult] =
    (archive ? Archive.QueryHistory(context.request.uri.query.getOrElse("prefix", ""))).mapTo[Archive.History].flatMap { case Archive.History(data) =>
      println(data)
      context.complete(data.mkString(","))
    }

  def postHandler(context: RequestContext): Future[RouteResult] =
    context.request.entity.toStrict(30.seconds).flatMap { e =>
      val text = e.data.decodeString("UTF-8")
      val record = JSONTransformer.flatten(text.parseJson)
      store(record).flatMap { _ =>
        context.complete(StatusCodes.Created)
      }
    }

  // start up the web server
  val serverSource = Http(system).bind(interface = "0.0.0.0", port = 8081)

  val route =
    Route {
      path("history") {
        getHistoryHandler
      } ~
      get {
        getHandler
      } ~
      post {
        postHandler
      }
    }

  serverSource.to(Sink.foreach { connection =>
    connection.handleWith(route)
  }).run()

  def requestHandler(request:HttpRequest) : HttpResponse = {
    HttpResponse(entity = request.toString)
  }
}
