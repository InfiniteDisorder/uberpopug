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

/*
 * class KafkaScheduledEventConsumer(topic: KafkaTopic, jobsRunner: JobsRunner[IO])(implicit
  ioEC: ExecutionContext @@ IOEC
) extends ScheduledEventConsumer[IO] {

  implicit private val ioL: Logging[IO] =
    Logging.Make.plain[IO].byName(getClass.getCanonicalName)

  implicit private val clock = Clock.system[IO]

  override def start: IO[Unit] = {
    val withRestart =
      stream.compile.drain
        .evalOn(ioEC)
        .onErrorRestartWithDelay(5.seconds, e => errorCause"event consuming failed" (e))
        .start
        .void

    info"scheduled event consumer started" >> withRestart
  }

  private val stream: Stream[IO, Unit] =
    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo(topic.name)
      .records
      .map { msg =>
        val kr = msg.asInstanceOf[KafkaRecord]
        parseRecord[ScheduledEvent](msg.record.value)
          .map(action => ConsumerContext(action, kr))
          .leftMap(_ => kr)
      }
      .mapValidAsync { context =>
        val event = context.event

        event.nextRun
          .map(IO.pure)
          .getOrElse(clock.now)
          .flatMap { date =>
            jobsRunner
              .systemRun(event.projectId, event.spk, date)
              .foldF(e => IO.raiseError(new Throwable(e.toString)), _ => IO.pure(context.kr))
          }
      }
      .map(_.fold(identity, identity) -> ())
      .groupWithin(50, 1.millisecond)
      .map(_.toList)
      .batchedCommit(m => debug"$m", m => warn"$m")

  private lazy val consumerSettings = ConsumerSettings[IO, Unit, Array[Byte]]
    .withBootstrapServers(topic.bootstrapServers.mkString(","))
    .withGroupId("event-consumer")
    .withMaxPollRecords(100)
    .withMaxPollInterval(1000.millis)
    .withSessionTimeout(6001.millis)
    .withMaxPrefetchBatches(54428800)
    .withAutoOffsetReset(AutoOffsetReset.Earliest)
    .withEnableAutoCommit(false)

}
 * */
