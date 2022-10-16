package accounting

import java.util.Date
import com.roundeights.hasher.Implicits._

package object model {
  case class Transaction(
      name: String,
      user_id: String,
      billing_cycle_id: Int,
      debit: Int,
      credit: Int,
      cat: Date
  ) {

    lazy val id: String = (user_id + name + cat.getTime.toString).sha1
  }

  case class Task(
      id: Int,
      public_id: String,
      name: String,
      assign_fee: Int,
      complete_reward: Int,
      assignee_id: String
  )

  case class User(
      id: Int,
      public_id: String,
      name: String,
      role: String,
      balance: Int
  )
}
