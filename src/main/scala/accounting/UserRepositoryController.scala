package accounting

import accounting.UserRepositoryController.ChangeRoleError
import accounting.model.User
import auth.model.roles
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.Transactor
import cats.syntax.either._
import doobie._
import doobie.implicits._

trait UserRepositoryController[F[_]] {
  def get(public_id: String): OptionT[F, User]
  def create(public_id: String, name: String, role: String): F[Unit]
  def update(
      public_id: String,
      name: String,
      email: String,
      role: String
  ): EitherT[F, ChangeRoleError.type, Unit]

}

object UserRepositoryController {

  class PostgresImpl() extends UserRepositoryController[IO] {
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql:$dbName",
      "postgres",
      "pass"
    )

    override def create(
        public_id: String,
        name: String,
        role: String
    ): IO[Unit] = {

      sql"""insert into users (public_id, name, role, balance)
              values ($public_id, $name, $role::role, 0)
           """.update.run.transact(xa).void
    }

    override def get(public_id: String): OptionT[IO, User] = {
      val r =
        sql"""select id, public_id, name, role from users where public_id = $public_id"""
          .query[User]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

    override def update(
        public_id: String,
        name: String,
        email: String,
        role: String
    ): EitherT[IO, ChangeRoleError.type, Unit] = {
      if (roles.all.contains(role)) {

        val u0 =
          sql"""update users set name = $name, role = $role::role where public_id = $public_id""".update.run
            .transact(xa)

        EitherT.liftF(u0.void)
      } else {
        EitherT.fromEither(ChangeRoleError.asLeft)
      }

    }
  }

  case object ChangeRoleError
}
