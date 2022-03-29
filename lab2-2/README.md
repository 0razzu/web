## Получение исполняемого файла
В&nbsp;каталоге с&nbsp;исходным кодом сервера (`lab2-2`) выполнить команду `mvn package`. Готовый jar-файл будет в&nbsp;каталоге `target`.

## Запуск сервера
Выполнить команду вида `java -jar <путь_к_jar> --path=<path> --port=<port> --moveTime=<moveTime>`. Веб-интерфейс игрового сервера будет доступен по&nbsp;адресу `http://localhost:<port>/<path>`, время для&nbsp;выполнения хода составит moveTime&nbsp;секунд. По&nbsp;умолчанию `port = 8080`, `movetime = 120`.

### Примеры
Предположим, что&nbsp;текущий каталог&nbsp;— каталог с&nbsp;исходным кодом сервера (`lab2-1`), текущая версия сервера&nbsp;— 2.0 и&nbsp;исполняемый файл уже получен.

Тогда:
- `java -jar target/checkers-2.0.jar --path=game-server --port=8082 --moveTime=30` запустит сервер по&nbsp;адресу `http://localhost:8082/game-server`, на&nbsp;ход будет 30&nbsp;секунд,
- `java -jar target/checkers-2.0.jar --path --port=808 --moveTime` запустит сервер по&nbsp;адресу `http://localhost:808`, на&nbsp;ход будет 120&nbsp;секунд,
- `java -jar target/checkers-2.0.jar --path=game/checkers --port --moveTime` запустит сервер по&nbsp;адресу `http://localhost:8080/game/checkers`, на&nbsp;ход будет 120&nbsp;секунд.
