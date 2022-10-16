package utils

import cats.effect.IO
import cats.syntax.traverse._
import fs2.kafka._
import utils.syntax.codec._

class KafkaEventProducer[T: BinaryEncoder](topic: String)
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
                  e.bytes
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
