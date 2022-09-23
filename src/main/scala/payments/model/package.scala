package payments

import java.util.Date

package object model {

  case class AccountBalance(
      userId: UserId,
      currentBalance: Int,
      lastPaidAt: Date
  )

  case class PaymentAction(
      taskId: TaskId,
      userId: UserId,
      change: Int,
      action: PaymentActionType,
      createdAt: Date
  )

  sealed trait PaymentActionType

  object PaymentActionType {
    case object Assignment extends PaymentActionType
    case object Completion extends PaymentActionType
  }

  type TaskId = String
  type UserId = String
}
