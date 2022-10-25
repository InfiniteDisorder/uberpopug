package utils

trait BinaryDecoder[T] {
  def decode: Array[Byte] => T
}
