package auth.model

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.generic.JsonCodec

object events {
  case class UserCreated(public_id: String, name: String, role: String)

  object UserCreated {
    implicit val enc: Encoder[UserCreated] = deriveEncoder[UserCreated]
    implicit val dec: Decoder[UserCreated] = deriveDecoder[UserCreated]
  }

  case class RoleChanged(public_id: String, role: String)

  object RoleChanged {
    implicit val enc: Encoder[RoleChanged] = deriveEncoder[RoleChanged]
    implicit val dec: Decoder[RoleChanged] = deriveDecoder[RoleChanged]
  }
}
