package tasks

import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec

import java.util.Date

package object model {

  case class User(
      public_id: String,
      name: String,
      role: String
  )

  case class Task(
      id: String,
      name: String,
      description: String,
      assignee_id: String,
      created_by_id: String,
      created_at: Date,
      completed: Boolean
  )

  case class TaskInputFormat(
      name: String,
      description: String
  )

  object TaskInputFormat {
    implicit val enc: Encoder[TaskInputFormat] = deriveEncoder[TaskInputFormat]
    implicit val dec: Decoder[TaskInputFormat] = deriveDecoder[TaskInputFormat]
  }
}
