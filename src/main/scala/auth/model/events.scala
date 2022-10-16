package auth.model

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import uberpopug.proto.user_signed_up.UserSignedUpV1
import utils.{BinaryDecoder, BinaryEncoder}
import io.scalaland.chimney.dsl._
import uberpopug.proto.user_streaming.UserStreamingV1
import utils._

object events {
  case class UserSignedUp(public_id: String, name: String, role: String)

  object UserSignedUp {
    implicit val be: BinaryEncoder[UserSignedUp] =
      new BinaryEncoder[UserSignedUp] {
        override def encode: UserSignedUp => Array[Byte] = usp =>
          usp
            .into[UserSignedUpV1]
            .withFieldComputed(_.publicId, _.public_id)
            .transform
            .toByteArray
      }

    implicit val bd: BinaryDecoder[UserSignedUp] =
      new BinaryDecoder[UserSignedUp] {
        override def decode: Array[Byte] => UserSignedUp = bytes =>
          UserSignedUpV1
            .parseFrom(bytes)
            .into[UserSignedUp]
            .withFieldComputed(_.public_id, _.publicId)
            .transform
      }
  }

  object streaming {
    case class UserStreaming(
        public_id: String,
        name: String,
        email: String,
        role: String
    )

    object UserStreaming {
      implicit val be: BinaryEncoder[UserStreaming] =
        new BinaryEncoder[UserStreaming] {
          override def encode: UserStreaming => Array[Byte] = usp =>
            usp
              .into[UserStreamingV1]
              .withFieldComputed(_.publicId, _.public_id)
              .transform
              .toByteArray
        }

      implicit val bd: BinaryDecoder[UserStreaming] =
        new BinaryDecoder[UserStreaming] {
          override def decode: Array[Byte] => UserStreaming = bytes =>
            UserStreamingV1
              .parseFrom(bytes)
              .into[UserStreaming]
              .withFieldComputed(_.public_id, _.publicId)
              .transform
        }
    }
  }
}
