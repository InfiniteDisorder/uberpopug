package utils

trait EventProducer[F[_], T] {
  def send(event: List[T]): F[Unit]
}
