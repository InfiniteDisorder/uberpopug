package accounting

import cats.effect.IO
import tasks.model.events.TaskAssigned
import utils.KafkaEventConsumer

class TaskAssignedEventConsumer(trc: TaskRepositoryController[IO])
    extends KafkaEventConsumer[TaskAssigned](
      "uberpopug.task-streaming",
      "accounting"
    ) {

  override def f: TaskAssigned => IO[Unit] = event =>
    trc
      .assign(event.public_id, event.assignee_id)
      .fold(
        _ => IO.unit,
        _ => IO.unit
      )
}
