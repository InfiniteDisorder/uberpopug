package accounting

import accounting.TransactionController.TransactionError
import accounting.model.Transaction
import cats.data.EitherT
import cats.effect.IO
import doobie.Transactor
import utils.KafkaEventProducer
import doobie._
import doobie.implicits._
import cats.syntax.either._

trait TransactionController[F[_]] {
  def save(t: Transaction): EitherT[F, TransactionError.type, Unit]
}

object TransactionController {

  class PostgresImpl(
  ) extends TransactionController[IO] {

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql:$dbName",
      "postgres",
      "pass"
    )

    override def save(
        t: Transaction
    ): EitherT[IO, TransactionError.type, Unit] = {

      val i =
        sql"""insert into transactions (id, name, user_id, billing_cycle_id, debit, credit, cat)
              values (${t.id}, ${t.name}, ${t.user_id}, ${t.billing_cycle_id},
              ${t.debit}, ${t.credit}, ${t.cat})
           """.update.run.transact(xa).void

      EitherT(i.map(_.asRight).handleError(_ => TransactionError.asLeft))
    }
  }

  case object TransactionError
}
