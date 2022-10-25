package accounting

import accounting.model.User
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s.{HttpRoutes, Request}
import org.http4s.server.AuthMiddleware

class Routes(urc: UserRepositoryController[IO]) {
  val r: HttpRoutes[IO] = HttpRoutes.empty[IO]

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
