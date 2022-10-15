package tasks

import auth.model.roles
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import tasks.model.User
import doobie._
import doobie.implicits._
import cats.syntax.either._
import tasks.UserRepositoryController.ChangeRoleError

trait UserRepositoryController[F[_]] {
  def get(public_id: String): OptionT[F, User]

  def getRandom(): OptionT[F, User]

  def create(public_id: String, name: String, role: String): F[Unit]

  def changeRole(
      public_id: String,
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

      sql"""insert into users (public_id, name, role) 
              values ($public_id, $name, $role::role)
           """.update.run.transact(xa).void
    }

    override def getRandom(): OptionT[IO, User] = {
      val r =
        sql"""select public_id, name, role from users where role = 'USER' order by random() limit 1"""
          .query[User]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

    override def get(public_id: String): OptionT[IO, User] = {
      val r =
        sql"""select public_id, name, role from users where public_id = $public_id"""
          .query[User]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

    override def changeRole(
        public_id: String,
        role: String
    ): EitherT[IO, ChangeRoleError.type, Unit] = {
      if (roles.all.contains(role)) {

        val u0 =
          sql"""update users set role = $role::role where public_id = $public_id""".update.run
            .transact(xa)

        EitherT.liftF(u0.void)
      } else {
        EitherT.fromEither(ChangeRoleError.asLeft)
      }

    }
  }

  case object ChangeRoleError
}
