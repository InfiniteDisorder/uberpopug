package tasks

import auth.model.events.streaming.UserStreaming
import cats.effect.IO
import utils.KafkaEventConsumer

class UserStreamingEventConsumer(urc: UserRepositoryController[IO])
    extends KafkaEventConsumer[UserStreaming](
      "uberpopug.user-streaming",
      "tasks"
    ) {

  override def f: UserStreaming => IO[Unit] = event =>
    urc
      .update(event.public_id, event.name, event.email, event.role)
      .foldF(
        _ => IO.unit,
        _ => IO.unit
      )
}
