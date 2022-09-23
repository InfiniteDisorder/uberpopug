package tasks

import java.util.Date

package object model {

  case class Task(
      id: String,
      assigneeId: String,
      createdById: String,
      createdAt: Date
  )

}
