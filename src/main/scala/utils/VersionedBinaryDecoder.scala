package utils

trait VersionedBinaryDecoder[T] {

  def decode: (Int, Array[Byte]) => T
}
