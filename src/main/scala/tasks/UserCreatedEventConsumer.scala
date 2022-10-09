package tasks

import auth.model.events.UserCreated
import cats.effect.IO
import utils.KafkaEventConsumer

class UserCreatedEventConsumer(urc: UserRepositoryController[IO])
    extends KafkaEventConsumer[UserCreated]("uberpopug.user.created", "tasks") {

  override def f: UserCreated => IO[Unit] = event =>
    urc.create(event.public_id, event.name, event.role)
}
