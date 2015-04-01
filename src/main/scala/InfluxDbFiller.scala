import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model._
import akka.http.model.headers.{BasicHttpCredentials, Authorization}
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object InfluxDbFiller extends App {

  implicit val system = ActorSystem("test")

  // the flow materializer. required for the consume()
  implicit val materializer = ActorFlowMaterializer()

  implicit val ec = system.dispatcher

  implicit val timeout = Timeout(30.seconds)

  val host = "10.61.1.36"
  val httpClient = Http(system).outgoingConnection(host, 8888)

  val printChunksConsumer = Sink.foreach[HttpResponse] { res =>
    if(res.status == StatusCodes.OK) {
      println("Got 200!")
      if(res.entity.isChunked)
        println("Chunky!")
      res.entity.dataBytes.map { chunk =>
        System.out.write(chunk.toArray)
        System.out.flush()
      }.to(Sink.ignore).run()
    } else
      println(res.status)
  }

  val request = HttpRequest(uri = Uri("/AtomicArchiveServer/query.json?minTime=2014-001&maxTime=2014-300&limit=-1"))
    .addHeader(Authorization(BasicHttpCredentials("satmon", "satmon")))

  val requests = Source.apply(() => Iterator.single(request))

  val finishFuture = requests.via(httpClient).runWith(printChunksConsumer)

  System.in.read()
  system.shutdown()
  system.awaitTermination()
}
