package tasks

import auth.model.events.UserSignedUp
import cats.effect.IO
import utils.KafkaEventConsumer

class UserSignedUpEventConsumer(urc: UserRepositoryController[IO])
    extends KafkaEventConsumer[UserSignedUp](
      "uberpopug.user.signed-up",
      "tasks"
    ) {

  override def f: UserSignedUp => IO[Unit] = event =>
    urc.create(event.public_id, event.name, event.role)
}
