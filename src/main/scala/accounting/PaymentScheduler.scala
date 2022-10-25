package accounting

import accounting.model.events.DailyRewardPaid
import accounting.model.{Transaction, User}
import cats.effect.IO
import doobie.Transactor
import doobie._
import doobie.implicits._
import cats.syntax.either._
import utils.KafkaEventProducer
import cats.syntax.traverse._
import org.quartz.CronExpression

import java.util.Date
import scala.concurrent.duration._

class PaymentScheduler(producer: KafkaEventProducer[DailyRewardPaid]) {

  val cron = new CronExpression("0 0 * * *")

  def start: IO[Unit] = {
    val now = new Date()
    val time = cron.getTimeAfter(now)
    IO.sleep((time.getTime - now.getTime).milliseconds) >> action >> start
  }

  def action = {
    val billingCycleT =
      sql"""
             select id from billing_cycles where status = open
           """
        .query[Int]
        .to[List]
        .transact(xa)

    val usersT =
      sql"""
           select id, public_id, name, role from users
         """.query[User].to[List]

    def setProcessing(bcId: Int) =
      sql"""
        update billing_cycles set status = processing where id = $bcId
        """.update.run

    def closeBillingCycleT(bcId: Int) =
      sql"""
           update billing_cycles set status = closed where id = $bcId
         """.update.run

    val createNewBC =
      sql"""
           insert into billing_cycles (status) values (open) 
         """.update.run

    for {
      bcId <- billingCycleT.map(_.headOption.get)
      t = for {
        _ <- setProcessing(bcId)
        _ <- createNewBC
      } yield ()
      _ <- t.transact(xa)
      users <- usersT.transact(xa)
      _ <- users.traverse(makePaymentForUser(bcId, _))
      _ <- closeBillingCycleT(bcId).transact(xa)
    } yield ()

  }

  def makePaymentForUser(processingBcId: Int, user: User): IO[Unit] = {

    val getBalance =
      sql"""
           select sum(debit) as debit, sum(credit) as credit from transactions where
           user_id = ${user.public_id} and billing_cycle_id = $processingBcId
         """.query[DailyBalance].to[List].map(_.headOption.get).transact(xa)

    getBalance.flatMap { balance =>
      val cat = new Date()
      val t = Transaction(
        s"Выплата пользователю ${user.public_id} за ${cat.toString}",
        user.public_id,
        processingBcId,
        debit = Math.max(0, balance.credit - balance.debit),
        credit = Math.max(0, balance.debit - balance.credit),
        cat
      )

      val payT =
        sql"""insert into transactions (id, name, user_id, billing_cycle_id, debit, credit, cat)
              values (${t.id}, ${t.name}, ${t.id}, ${t.billing_cycle_id},
              ${t.debit}, ${t.credit}, $cat)
           """.update.run.transact(xa).void

      for {
        _ <- payT
        _ <- producer.send(
          List(DailyRewardPaid(t.id, user.public_id, t.debit, t.credit, cat))
        )
        // Payment system call
      } yield ()
    }
  }

  private lazy val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    s"jdbc:postgresql:$dbName",
    "postgres",
    "pass"
  )

  case class DailyBalance(debit: Int, credit: Int)
}
