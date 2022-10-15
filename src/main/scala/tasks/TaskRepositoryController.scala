package tasks

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.Transactor
import tasks.TaskRepositoryController.{
  CompleteTaskError,
  CreateTaskError,
  ReassignTasksError
}
import tasks.model.{Task, User}
import doobie.implicits._
import cats.syntax.traverse._
import cats.syntax.either._

import java.util.{Date, UUID}

trait TaskRepositoryController[F[_]] {

  def create(name: String, description: String)(
      user: User
  ): EitherT[F, CreateTaskError.type, Task]

  def reassign()(auth: User): EitherT[F, ReassignTasksError.type, Unit]

  def complete(task_id: String)(
      auth: User
  ): EitherT[F, CompleteTaskError.type, Unit]

  protected def get(task_id: String): OptionT[F, Task]
}

object TaskRepositoryController {

  class PostgresImpl(urc: UserRepositoryController[IO])
      extends TaskRepositoryController[IO] {

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql:$dbName",
      "postgres",
      "pass"
    )

    override def create(name: String, description: String)(
        user: User
    ): EitherT[IO, CreateTaskError.type, Task] = {

      def assignedTask(assignee_id: String): Task = Task(
        id = UUID.randomUUID().toString,
        name = name,
        description = description,
        assignee_id = assignee_id,
        created_by_id = user.public_id,
        created_at = new Date(),
        completed = false
      )

      for {
        user <- EitherT.fromOptionF(urc.getRandom().value, CreateTaskError)
        task = assignedTask(user.public_id)
        insert =
          sql"""insert into tasks (id, name, description, assignee_id, created_by_id, created_at, completed) 
              values (${task.id}, ${task.name}, ${task.description}, ${task.assignee_id}, 
              ${task.created_by_id}, ${task.created_at}, ${task.completed})
           """.update.run.transact(xa)
        _ <- EitherT.liftF(insert)
      } yield task
    }

    override def complete(task_id: String)(
        auth: User
    ): EitherT[IO, CompleteTaskError.type, Unit] = {
      for {
        task <- EitherT.fromOptionF(get(task_id).value, CompleteTaskError)
        ee = {
          if (task.assignee_id == auth.public_id) ().asRight
          else CompleteTaskError.asLeft
        }
        _ <- EitherT.fromEither[IO].apply(ee)
        _ <-
          EitherT.liftF {
            sql"""update tasks set completed = true where id = $task_id""".update.run
              .transact(xa)
              .void
          }
      } yield ()
    }

    override def reassign()(
        user: User
    ): EitherT[IO, ReassignTasksError.type, Unit] = {

      if (
        user.role == auth.model.roles.Admin ||
        user.role == auth.model.roles.Manager
      ) {
        val r = sql"""select id from tasks where completed = false"""
          .query[String]
          .to[List]
          .transact(xa)
          .flatMap {
            _.traverse { task_id =>
              for {
                user <- urc.getRandom().value.map(_.get)
                _ <- assign(task_id, user.public_id)
              } yield ()
            }
          }

        EitherT.liftF(r.void)
      } else {
        EitherT.fromEither(ReassignTasksError.asLeft)
      }
    }

    override protected def get(task_id: String): OptionT[IO, Task] = {
      val r =
        sql"""select id, name, description, assignee_id, created_by_id, created_at, completed from tasks where id = $task_id"""
          .query[Task]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

    private def assign(task_id: String, user_id: String): IO[Unit] = {
      sql"""update tasks set assignee_id = $user_id where id = $task_id""".update.run
        .transact(xa)
        .void
    }
  }

  case object CreateTaskError
  case object ReassignTasksError
  case object CompleteTaskError
}
