import akka.actor.{Actor, Props, ActorSystem}
import akka.http.Http
import akka.http.model.{HttpResponse, HttpRequest}
import akka.io.IO
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}

class TestServer extends Actor {

  implicit val system = context.system

  // the flow materializer. required for the consume()
  implicit val materializer = FlowMaterializer()

  // start up the web server
  IO(Http) ! Http.Bind(interface = "0.0.0.0", port = 8081)

  def requestHandler(request:HttpRequest) : HttpResponse = {
    HttpResponse(entity = request.toString)
  }

  def receive = {
    case Http.BindFailedException ⇒
      println("bind failed.")
      context.system.shutdown()
    case Http.ServerBinding(localAddress, connectionStream) ⇒
      println(s"bound to $localAddress")
      Source(connectionStream).map {
        case Http.IncomingConnection(remoteAddress, requestProducer, responseConsumer) ⇒
          println("accepted connection from " + remoteAddress)
          Source(requestProducer).map(requestHandler).connect(Sink(responseConsumer)).run()
      }.connect(Sink.ignore).run()
  }
}

object MinimalWebServerTest extends App {

  val system = ActorSystem("test")

  val server = system.actorOf(Props[TestServer])

  val text = io.StdIn.readLine()

  system.shutdown()
}
