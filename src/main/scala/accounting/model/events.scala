package accounting.model

import java.util.Date
import java.util.Date
import io.scalaland.chimney.dsl._
import uberpopug.proto.daily_reward_paid.DailyRewardPaidV1
import utils._

object events {
  case class DailyRewardPaid(
      transaction_id: String,
      user_id: String,
      debit: Long,
      credit: Long,
      cat: Date
  )

  object DailyRewardPaid {
    implicit val be: BinaryEncoder[DailyRewardPaid] =
      new BinaryEncoder[DailyRewardPaid] {
        override def encode: DailyRewardPaid => Array[Byte] = ta =>
          ta.into[DailyRewardPaidV1]
            .withFieldComputed(_.userId, _.user_id)
            .withFieldComputed(_.transactionId, _.transaction_id)
            .transform
            .toByteArray
      }

    implicit val bd: BinaryDecoder[DailyRewardPaid] =
      new BinaryDecoder[DailyRewardPaid] {
        override def decode: Array[Byte] => DailyRewardPaid = ba =>
          DailyRewardPaidV1
            .parseFrom(ba)
            .into[DailyRewardPaid]
            .withFieldComputed(_.user_id, _.userId)
            .withFieldComputed(_.transaction_id, _.transactionId)
            .transform
      }
  }
}
