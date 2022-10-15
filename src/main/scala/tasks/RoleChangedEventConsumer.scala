package tasks

import auth.model.events.RoleChanged
import cats.effect.IO
import utils.KafkaEventConsumer

class RoleChangedEventConsumer(urc: UserRepositoryController[IO])
    extends KafkaEventConsumer[RoleChanged](
      "uberpopug.users.role-changed",
      "tasks"
    ) {

  override def f: RoleChanged => IO[Unit] = event =>
    urc
      .changeRole(event.public_id, event.role)
      .foldF(
        _ => IO.unit,
        _ => IO.unit
      )
}
