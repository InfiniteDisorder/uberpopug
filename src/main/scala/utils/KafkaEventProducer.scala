package utils

import cats.effect.IO
import fs2.kafka.{
  Acks,
  KafkaProducer,
  ProducerRecord,
  ProducerRecords,
  ProducerSettings
}
import io.circe.Encoder
import cats.syntax.traverse._
import io.circe.syntax._

import java.nio.charset.StandardCharsets

class KafkaEventProducer[T: Encoder](topic: String)
    extends EventProducer[IO, T] {

  def send(events: List[T]): IO[Unit] =
    KafkaProducer
      .resource(producerSettings)
      .use { producer =>
        events.traverse { e =>
          producer
            .produce(
              ProducerRecords.one(
                ProducerRecord(
                  topic,
                  (),
                  e.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
                )
              )
            )
            .flatten
        }.void

      }

  private val producerSettings = ProducerSettings[IO, Unit, Array[Byte]]
    .withBootstrapServers("localhost:9092")
    .withRetries(3)
    .withAcks(Acks.One)
}
