package auth

import auth.model.events.{RoleChanged, UserCreated}
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

  val userCreatedEp =
    new KafkaEventProducer[UserCreated]("uberpopug.user.created")
  val roleChangedEp =
    new KafkaEventProducer[RoleChanged]("uberpopug.user.role-changed")
  val userController =
    new UserRepositoryController.PostgresImpl(userCreatedEp, roleChangedEp)

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
