package accounting

import accounting.model.{Task, Transaction, User}
import cats.data.{EitherT, OptionT}
import TaskRepositoryController._
import accounting.TransactionController.TransactionError
import cats.effect.IO
import doobie.Transactor
import doobie._
import doobie.implicits._
import cats.syntax.either._

import java.util.Date
import scala.util.Random

trait TaskRepositoryController[F[_]] {

  def create(
      id: String,
      name: String,
      assignee_id: String
  ): EitherT[F, CreateTaskError.type, Unit]

  def assign(
      task_id: String,
      assignee_id: String
  ): EitherT[F, AssignTaskError.type, Unit]

  def complete(task_id: String): EitherT[F, CompleteTaskError.type, Unit]

  protected def get(task_id: String): OptionT[F, Task]
}

object TaskRepositoryController {

  class PostgresImpl(
      urc: UserRepositoryController[IO]
  ) extends TaskRepositoryController[IO] {
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql:$dbName",
      "postgres",
      "pass"
    )

    override def create(
        public_id: String,
        name: String,
        assignee_id: String
    ): EitherT[IO, CreateTaskError.type, Unit] = {

      val rnd = new Random()
      val fee = rnd.nextInt(10) + 11
      val reward = rnd.nextInt(20) + 21

      val tasksT = sql"""
           insert into tasks (public_id, name, assign_fee, complete_reward, assignee_id) values
            ($public_id, $name, $fee, $reward, $assignee_id)
         """.update.run

      val t = Transaction(
        s"assign on create (${name})",
        assignee_id,
        0,
        0,
        fee,
        new Date()
      )

      val usersT =
        sql"""update users set balance = balance - $fee where public_id = $assignee_id""".update.run

      val transactionT =
        sql"""insert into transactions (id, name, user_id, billing_cycle_id, debit, credit, cat)
              values (${t.id}, ${t.name}, ${t.user_id}, ${t.billing_cycle_id},
              ${t.debit}, ${t.credit}, ${t.cat})
           """.update.run

      val res = for {
        _ <- transactionT
        _ <- tasksT
        _ <- usersT
      } yield ()

      EitherT(
        res
          .transact(xa)
          .map(_ => ().asRight)
          .handleError(_ => CreateTaskError.asLeft)
      )
    }

    override def complete(
        task_id: String
    ): EitherT[IO, CompleteTaskError.type, Unit] = {
      for {
        task <- EitherT.fromOptionF(get(task_id).value, CompleteTaskError)
        t = Transaction(
          s"${task.name} completed",
          task.assignee_id,
          0,
          task.complete_reward,
          0,
          new Date()
        )

        transactionT =
          sql"""insert into transactions (id, name, user_id, billing_cycle_id, debit, credit, cat)
              values (${t.id}, ${t.name}, ${t.user_id}, ${t.billing_cycle_id},
              ${t.debit}, ${t.credit}, ${t.cat})
           """.update.run

        usersT =
          sql"""
               update users set balance = balance + ${task.complete_reward} where public_id = ${task.assignee_id}
             """.update.run

        action = for {
          _ <- transactionT
          _ <- usersT
        } yield ()

        _ <- EitherT.liftF(action.transact(xa))
      } yield ()

    }

    override def assign(
        task_id: String,
        assignee_id: String
    ): EitherT[IO, AssignTaskError.type, Unit] = {

      EitherT
        .fromOptionF(get(task_id).value, AssignTaskError)
        .flatMap { task =>
          val tasksT = sql"""
           update tasks assignee_id = assignee_id where public_id = $task_id
         """.update.run

          val t = Transaction(
            s"${task.name} assigned",
            assignee_id,
            0,
            0,
            task.assign_fee,
            new Date()
          )

          val transactionT =
            sql"""insert into transactions (id, name, user_id, billing_cycle_id, debit, credit, cat)
              values (${t.id}, ${t.name}, ${t.user_id}, ${t.billing_cycle_id},
              ${t.debit}, ${t.credit}, ${t.cat})
           """.update.run

          val usersT =
            sql"""
                 update users set balance = balance - ${task.assign_fee} where public_id = ${task.assignee_id}
               """.update.run

          val res = for {
            _ <- tasksT
            _ <- transactionT
            _ <- usersT
          } yield ()

          EitherT(
            res
              .transact(xa)
              .map(_.asRight)
              .handleError(_ => AssignTaskError.asLeft)
          )
        }
    }

    override protected def get(task_id: String): OptionT[IO, Task] = {

      val r =
        sql"""select id, public_id, name, description, assign_fee, complete_reward, assignee_id from tasks where public_id = $task_id"""
          .query[Task]
          .to[List]
          .transact(xa)
          .map(_.headOption)

      OptionT(r)
    }

  }

  case object CreateTaskError
  case object AssignTaskError
  case object CompleteTaskError
}
