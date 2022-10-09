package utils

import cats.effect.IO
import cats.syntax.either._
import fs2.Stream
import fs2.kafka.{
  AutoOffsetReset,
  CommittableConsumerRecord,
  ConsumerSettings,
  KafkaConsumer
}
import io.circe.Decoder
import io.circe.parser.parse
import utils.KafkaEventConsumer._

import java.nio.charset.StandardCharsets
import scala.concurrent.duration._

abstract class KafkaEventConsumer[T: Decoder](topic: String, groupId: String) {

  def f: T => IO[Unit]

  def start: IO[Unit] =
    stream.compile.drain
      .onErrorRestartWithDelay(1.second)
      .start
      .void

  private val stream: Stream[IO, Unit] =
    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo(topic)
      .records
      .map { msg =>
        val kr = msg.asInstanceOf[KafkaRecord]
        parseRecord(msg.record.value)
          .map(action => ConsumerContext(action, kr))
          .leftMap(_ => kr)
      }
      .evalMap[IO, Unit] {
        case Left(kr) =>
          println("FAILED")
          IO.raiseError(new Error("consumer failed"))

        case Right(r) =>
          f(r.event)
            .handleErrorWith(e =>
              IO.delay(println(e.fillInStackTrace())) >> IO.raiseError(e)
            )
            .map(_ => r.asRight) >> r.kr.offset.commit >> IO.delay(
            println("COMMITTED")
          )
      }

  private lazy val consumerSettings = ConsumerSettings[IO, Unit, Array[Byte]]
    .withBootstrapServers("localhost:9092")
    .withGroupId(groupId)
    .withMaxPollRecords(100)
    .withMaxPollInterval(1000.millis)
    .withSessionTimeout(6001.millis)
    .withMaxPrefetchBatches(54428800)
    .withAutoOffsetReset(AutoOffsetReset.Earliest)
    .withEnableAutoCommit(false)

  def parseRecord(bytes: Array[Byte]): Either[Throwable, T] =
    parse(new String(bytes, StandardCharsets.UTF_8))
      .flatMap(_.as[T])
      .leftMap(_.fillInStackTrace())

}

object KafkaEventConsumer {
  case class ConsumerContext[T](event: T, kr: KafkaRecord)
  type KafkaRecord = CommittableConsumerRecord[IO, Any, Array[Byte]]

  implicit class IOExt[T](val io: IO[T]) extends AnyVal {

    def onErrorRestartWithDelay(
        delay: FiniteDuration
    ): IO[T] =
      io.handleErrorWith { _ =>
        IO.sleep(delay) >> io.onErrorRestartWithDelay(delay)
      }
  }
}
