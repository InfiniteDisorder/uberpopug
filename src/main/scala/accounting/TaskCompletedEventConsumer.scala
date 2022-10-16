package accounting

import cats.effect.IO
import tasks.model.events.TaskCompleted
import utils.KafkaEventConsumer

class TaskCompletedEventConsumer(trc: TaskRepositoryController[IO])
    extends KafkaEventConsumer[TaskCompleted](
      "uberpopug.task-streaming",
      "accounting"
    ) {

  override def f: TaskCompleted => IO[Unit] = event =>
    trc
      .complete(event.public_id)
      .fold(
        _ => IO.unit,
        _ => IO.unit
      )
}
