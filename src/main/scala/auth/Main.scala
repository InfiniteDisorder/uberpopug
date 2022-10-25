package auth

import auth.model.events.streaming.UserStreaming
import auth.model.events.UserSignedUp
import cats.effect.IO.delay
import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.server.BlazeServerBuilder
import utils.KafkaEventProducer

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

object Main extends IOApp {

  val ecR = Resource
    .make(delay(new ForkJoinPool(4)))(p => delay(p.shutdown()))
    .map { executor =>
      ExecutionContext
        .fromExecutor(executor)
    }

  val userSignedUpEP =
    new KafkaEventProducer[UserSignedUp]("uberpopug.user.signed-up")
  val userStreamingEP =
    new KafkaEventProducer[UserStreaming]("uberpopug.user-streaming")

  val userController =
    new UserRepositoryController.PostgresImpl(userSignedUpEP, userStreamingEP)

  val serverR = for {
    ec <- ecR
    routes = new Routes(userController).r
  } yield BlazeServerBuilder
    .apply[IO](ec)
    .bindHttp(9090, "localhost")
    .withHttpApp(routes.orNotFound)
    .resource
    .use(_ => IO.never)
    .flatMap(_ => IO.pure(ExitCode.Success))
    .handleError { _ => ExitCode.Error }

  override def run(args: List[String]): IO[ExitCode] = {
    serverR.use(identity)
  }
}
