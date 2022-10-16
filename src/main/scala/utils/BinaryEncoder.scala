package utils

trait BinaryEncoder[T] {
  def encode: T => Array[Byte]
}
