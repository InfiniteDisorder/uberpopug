package accounting

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

  val urc = new UserRepositoryController.PostgresImpl()

  val serverR = for {
    ec <- ecR
    routes = new Routes(urc).r
  } yield {

    val userSignedUpEventConsumer = new UserSignedUpEventConsumer(urc)
    val userStreamingEventConsumer = new UserStreamingEventConsumer(urc)

    val trc = new TaskRepositoryController.PostgresImpl(urc)
    val taskStreamingEventConsumer = new TaskStreamingEventConsumer(trc)
    val taskAssignedEventConsumer = new TaskAssignedEventConsumer(trc)
    val taskCompletedEventConsumer = new TaskCompletedEventConsumer(trc)

    val consumersT = for {
      _ <- userSignedUpEventConsumer.start
      _ <- userStreamingEventConsumer.start
      _ <- taskStreamingEventConsumer.start
      _ <- taskAssignedEventConsumer.start
      _ <- taskCompletedEventConsumer.start
    } yield ()

    consumersT.flatMap { _ =>
      BlazeServerBuilder
        .apply[IO](ec)
        .bindHttp(9093, "localhost")
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
