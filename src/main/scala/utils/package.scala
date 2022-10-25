import auth.model.roles
import io.scalaland.chimney.Transformer
import uberpopug.proto.model.{Role => ProtoRole}

import java.util.Date

package object utils {

  object syntax {
    object codec {
      implicit class DecoderSyntax(val v: Array[Byte]) extends AnyVal {
        def as[T](implicit binaryDecoder: BinaryDecoder[T]) =
          binaryDecoder.decode(v)
      }

      implicit class EncoderSyntax[T](val v: T) extends AnyVal {
        def bytes(implicit binaryEncoder: BinaryEncoder[T]) =
          binaryEncoder.encode(v)
      }
    }
  }

  implicit val dateToLongTransformer: Transformer[Date, Long] =
    new Transformer[Date, Long] {
      override def transform(src: Date): Long = src.getTime
    }

  implicit val longToDateTransformer: Transformer[Long, Date] =
    new Transformer[Long, Date] {
      override def transform(src: Long): Date = new Date(src)
    }

  implicit val stringToRoleTransformer: Transformer[String, ProtoRole] =
    new Transformer[String, ProtoRole] {
      override def transform(src: String): ProtoRole = src match {
        case roles.Admin   => ProtoRole.Admin
        case roles.Manager => ProtoRole.Manager
        case roles.User    => ProtoRole.User
        case _             => throw new Exception()
      }
    }

  implicit val roleToStringTransformer: Transformer[ProtoRole, String] =
    new Transformer[ProtoRole, String] {
      override def transform(src: ProtoRole): String = src match {
        case ProtoRole.Admin   => roles.Admin
        case ProtoRole.Manager => roles.Manager
        case ProtoRole.User    => roles.User
        case _                 => throw new Exception()
      }
    }
}
