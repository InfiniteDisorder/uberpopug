## Event storming

Ссылка на ES 
https://miro.com/app/board/uXjVPR-UrzA=/?share_link_id=897878199264

## Data model

Ссылка на модель данных
https://drive.google.com/file/d/1P6Tc_FIp3JULfNStSmEFDSRmA7VVQ5-G/view?usp=sharing

## Сервисы

4 основных сервиса - `Auth`, `TraskTracker`, `Accounting`, `Analytics`

`Auth` - сервис авторизации, выпускает токены, порождает CUD события `UserCreated`, `UserModified`,
все остальные сервисы являются консьюмерами этих событий и пишут в локальные базы пользователей.

`TaskTracker` - сервис трекер, отвечает за назначение и завершение задач, является продьюсером бизнес-событий 
`TaskAssigned`, `TaskCompleted`. Консьюмеры этих событий - сервисы `Accounting`, `Analytics`, которые делают
записи в логе при изменении соответствующей задачи.

`Accounting` - отвечает за управление информацией о выплатах и балансе сотрудников, выплаты и уведомления на почту
происходят в этом сервисе.

`Anaylytics` - сервис аналитики. Query сервис для лога изменений заданий.


### Бизнес события:
`TaskAssigned`, `TaskCompleted` - таск трекер продьюсер, аналитика и аккаунтинг консьюмеры.

### CUD события
`UserCreated`, `UserModified` - продьюсер auth, консьюмеры - все остальные пользователи
