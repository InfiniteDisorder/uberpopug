package tasks

import tasks.model.User
import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.IO
import org.http4s.{AuthedRoutes, Request}
import org.http4s.dsl.io._
import org.http4s.server.AuthMiddleware
import tasks.model.TaskInputFormat
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import EntityDecoder._
import org.http4s.circe.CirceEntityDecoder._

class Routes(
    urc: UserRepositoryController[IO],
    trc: TaskRepositoryController[IO]
) {

  val r: HttpRoutes[IO] = authMiddleware(authedR)

  lazy val authedR: AuthedRoutes[User, IO] = AuthedRoutes.of[User, IO] {

    case ar @ PUT -> Root / "task" as user =>
      val r = for {
        fmt <- EitherT.liftF(ar.req.as[TaskInputFormat])
        _ <- trc.create(fmt.name, fmt.description)(user)
      } yield ()

      r.foldF(
        e => BadRequest(e.toString),
        _ => Ok("task created")
      )

    case POST -> Root / "reassign" as user =>
      trc
        .reassign()(user)
        .foldF(
          e => BadRequest(e.toString),
          _ => Ok("reassigned")
        )

    case POST -> Root / "complete" / task_id as user =>
      trc
        .complete(task_id)(user)
        .foldF(
          e => BadRequest(e.toString),
          _ => Ok("completed")
        )
  }

  type G[A] = OptionT[IO, A]

  private lazy val authLayer: Kleisli[G, Request[IO], User] =
    Kleisli { r =>
      OptionT
        .fromOption[IO](r.multiParams.get("auth_user_id").flatMap(_.headOption))
        .flatMap { authUserId =>
          urc.get(authUserId)
        }
    }

  private lazy val authMiddleware: AuthMiddleware[IO, User] =
    AuthMiddleware[IO, User](authLayer)

}
