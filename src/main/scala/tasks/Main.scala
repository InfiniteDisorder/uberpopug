package tasks

import cats.effect.IO.delay
import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.server.BlazeServerBuilder

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

object Main extends IOApp {

  val ecR = Resource
    .make(delay(new ForkJoinPool(4)))(p => delay(p.shutdown()))
    .map { executor =>
      ExecutionContext
        .fromExecutor(executor)
    }

  val userController = new UserRepositoryController.PostgresImpl()
  val taskController = new TaskRepositoryController.PostgresImpl(userController)

  val userCreatedEventConsumer = new UserCreatedEventConsumer(userController)
  val roleChangedEventConsumer = new RoleChangedEventConsumer(userController)

  val serverR = for {
    ec <- ecR
    routes = new Routes(userController, taskController).r
  } yield {

    val consumersT = for {
      _ <- userCreatedEventConsumer.start
      _ <- roleChangedEventConsumer.start
    } yield ()

    consumersT.flatMap { _ =>
      BlazeServerBuilder
        .apply[IO](ec)
        .bindHttp(9091, "localhost")
        .withHttpApp(routes.orNotFound)
        .resource
        .use(_ => IO.never)
        .flatMap(_ => IO.pure(ExitCode.Success))
        .handleError { _ => ExitCode.Error }
    }

  }

  override def run(args: List[String]): IO[ExitCode] = {
    serverR.use(identity)
  }
}
