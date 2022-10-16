package tasks.model

import utils.{BinaryDecoder, BinaryEncoder}

import java.util.Date
import io.scalaland.chimney.dsl._
import uberpopug.proto.task_assigned.TaskAssignedV1
import uberpopug.proto.task_completed.TaskCompletedV1
import uberpopug.proto.task_streaming.TaskStreamingV1
import utils._

object events {

  case class TaskStreaming(
      public_id: String,
      name: String,
      assignee_id: String,
      at: Date
  )

  object TaskStreaming {

    implicit val be: BinaryEncoder[TaskStreaming] =
      new BinaryEncoder[TaskStreaming] {
        override def encode: TaskStreaming => Array[Byte] = ts =>
          ts.into[TaskStreamingV1]
            .withFieldComputed(_.publicId, _.public_id)
            .withFieldComputed(_.assigneeId, _.assignee_id)
            .transform
            .toByteArray
      }

    implicit val bd: BinaryDecoder[TaskStreaming] =
      new BinaryDecoder[TaskStreaming] {
        override def decode: Array[Byte] => TaskStreaming = bytes =>
          TaskStreamingV1
            .parseFrom(bytes)
            .into[TaskStreaming]
            .withFieldComputed(_.public_id, _.publicId)
            .withFieldComputed(_.assignee_id, _.assigneeId)
            .transform
      }
  }

  case class TaskAssigned(public_id: String, assignee_id: String, at: Date) {}

  object TaskAssigned {
    implicit val be: BinaryEncoder[TaskAssigned] =
      new BinaryEncoder[TaskAssigned] {
        override def encode: TaskAssigned => Array[Byte] = ta =>
          ta.into[TaskAssignedV1]
            .withFieldComputed(_.publicId, _.public_id)
            .withFieldComputed(_.assigneeId, _.public_id)
            .transform
            .toByteArray
      }

    implicit val bd: BinaryDecoder[TaskAssigned] =
      new BinaryDecoder[TaskAssigned] {
        override def decode: Array[Byte] => TaskAssigned = ba =>
          TaskAssignedV1
            .parseFrom(ba)
            .into[TaskAssigned]
            .withFieldComputed(_.public_id, _.publicId)
            .withFieldComputed(_.assignee_id, _.assigneeId)
            .transform
      }
  }

  case class TaskCompleted(public_id: String, at: Date)

  object TaskCompleted {

    implicit val be: BinaryEncoder[TaskCompleted] =
      new BinaryEncoder[TaskCompleted] {
        override def encode: TaskCompleted => Array[Byte] = tc =>
          tc.into[TaskCompletedV1]
            .withFieldComputed(_.publicId, _.public_id)
            .transform
            .toByteArray
      }

    implicit val bd: BinaryDecoder[TaskCompleted] =
      new BinaryDecoder[TaskCompleted] {
        override def decode: Array[Byte] => TaskCompleted = ab =>
          TaskCompletedV1
            .parseFrom(ab)
            .into[TaskCompleted]
            .withFieldComputed(_.public_id, _.publicId)
            .transform
      }
  }

}
