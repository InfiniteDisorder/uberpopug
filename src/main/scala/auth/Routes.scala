package auth
import auth.model.User
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s.{AuthedRoutes, HttpRoutes, Request}
import org.http4s.dsl.io._
import io.circe.syntax._
import cats.syntax.semigroupk._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.AuthMiddleware

class Routes(urc: UserRepositoryController[IO]) {

  val r: HttpRoutes[IO] = unAuthedR <+> authMiddleware(authedR)

  lazy val authedR: AuthedRoutes[User, IO] = AuthedRoutes.of[User, IO] {
    case POST -> Root / "change_role" :?
        PublicIdMP(publicId) +&
        RoleMP(role) as user =>
      urc
        .changeRole(publicId, role)(user)
        .foldF(
          err => BadRequest(err.message.asJson),
          _ => Ok("role changed")
        )
  }

  lazy val unAuthedR: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "sign_in" :? EmailMP(email) +& PasswordMP(password) =>
      urc
        .signIn(email, password)
        .foldF(
          err => BadRequest(err.message.asJson),
          publicId => Ok.apply(publicId)
        )

    case POST -> Root / "sign_up" :?
        EmailMP(email) +&
        PasswordMP(password) +&
        NameMP(name) =>
      urc
        .signUp(email, password, name)
        .foldF(
          err => BadRequest(err.message.asJson),
          publicId => Ok.apply(publicId)
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

  private object NameMP extends QueryParamDecoderMatcher[String]("name")
  private object RoleMP extends QueryParamDecoderMatcher[String]("role")
  private object PublicIdMP
      extends QueryParamDecoderMatcher[String]("public_id")
  private object EmailMP extends QueryParamDecoderMatcher[String]("email")
  private object PasswordMP extends QueryParamDecoderMatcher[String]("password")

}
