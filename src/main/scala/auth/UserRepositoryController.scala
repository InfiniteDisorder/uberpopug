package auth

import auth.UserRepositoryController.{ChangeRoleError, SignInError, SignupError}
import auth.model.events.{RoleChanged, UserCreated}
import auth.model.{PublicId, User, roles}
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie._
import doobie.implicits._
import cats.syntax.either._
import utils.KafkaEventProducer

import java.util.UUID

trait UserRepositoryController[F[_]] {
  def signIn(
      email: String,
      password: String
  ): EitherT[F, SignInError.type, PublicId]

  def signUp(
      email: String,
      password: String,
      name: String
  ): EitherT[F, SignupError.type, PublicId]

  def changeRole(
      public_id: PublicId,
      role: String
  )(user: User): EitherT[F, ChangeRoleError.type, Unit]

  def get(public_id: PublicId): OptionT[F, User]
}

object UserRepositoryController {

  class PostgresImpl(
      userCreatedEP: KafkaEventProducer[UserCreated],
      roleChangedEP: KafkaEventProducer[RoleChanged]
  ) extends UserRepositoryController[IO] {

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql:$dbName",
      "postgres",
      "pass"
    )

    override def signIn(
        email: String,
        password: String
    ): EitherT[IO, SignInError.type, PublicId] = {
      val u0 =
        sql"""select id, email, name, password, public_id, role from users where email = $email"""
          .query[User]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      val r: EitherT[IO, SignInError.type, PublicId] = EitherT
        .fromOptionF(u0, SignInError)
        .subflatMap {
          case u if u.password == password =>
            u.public_id.asRight
          case _ => SignInError.asLeft
        }

      r
    }

    override def signUp(
        email: String,
        password: String,
        name: String
    ): EitherT[IO, SignupError.type, PublicId] = {

      val id = UUID.randomUUID().toString
      val public_id = UUID.randomUUID().toString

      val dbInsert =
        sql"""insert into users (id, email, name, password, public_id, role) 
              values ($id, $email, $name, $password, $public_id, 'USER')
           """.update.run.transact(xa)

      val emailCheck: EitherT[IO, SignupError.type, Unit] =
        EitherT {
          getByEmail(email).fold(().asRight[SignupError.type])(_ =>
            SignupError.asLeft[Unit]
          )
        }

      emailCheck
        .semiflatMap(_ => dbInsert)
        .semiflatMap(_ =>
          userCreatedEP
            .send(List(UserCreated(public_id, name, "USER")))
            .as(public_id)
        )
    }

    override def changeRole(public_id: PublicId, role: String)(
        user: User
    ): EitherT[IO, ChangeRoleError.type, Unit] = {
      if (
        roles.all.contains(
          role
        ) && (user.role == roles.Manager || user.role == roles.Admin)
      ) {

        val dpUpdate =
          sql"""update users set role = $role where public_id = $public_id""".update.run
            .transact(xa)

        val res = for {
          _ <- dpUpdate
          _ <- roleChangedEP.send(List(RoleChanged(public_id, role)))
        } yield ()

        EitherT.liftF(res)
      } else {
        EitherT.fromEither(ChangeRoleError.asLeft)
      }

    }

    override def get(public_id: PublicId): OptionT[IO, User] = {
      val r =
        sql"""select id, email, name, password, public_id, role from users where public_id = $public_id"""
          .query[User]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

    private def getByEmail(email: String): OptionT[IO, User] = {
      val r =
        sql"""select id, email, name, password, public_id, role from users where email = $email"""
          .query[User]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

  }

  case object SignInError { val message: String = "invalid email or password" }
  case object SignupError {
    val message: String = "user with this email already exists"
  }

  case object ChangeRoleError { val message: String = "change role error " }
}
