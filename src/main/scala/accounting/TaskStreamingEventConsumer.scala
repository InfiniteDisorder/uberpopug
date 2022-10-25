package accounting

import cats.effect.IO
import tasks.model.events.TaskStreaming
import utils.KafkaEventConsumer

class TaskStreamingEventConsumer(trc: TaskRepositoryController[IO])
    extends KafkaEventConsumer[TaskStreaming](
      "uberpopug.task-streaming",
      "accounting"
    ) {

  override def f: TaskStreaming => IO[Unit] = event =>
    trc
      .create(event.public_id, event.name, event.assignee_id)
      .fold(
        _ => IO.unit,
        _ => IO.unit
      )
}
