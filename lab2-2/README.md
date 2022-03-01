## Получение исполняемого файла
В каталоге с исходным кодом сервера (`lab2-2`) выполнить команду `mvn package`. Готовый jar-файл будет в каталоге `target`.

## Запуск сервера
Выполнить команду вида `java -jar <путь_к_jar> --path=<path> --port=<port>`. Веб-интерфейс игрового сервера будет доступен по адресу `http://localhost:<port>/<path>`. По умолчанию `port = 8080`.

### Примеры
Предположим, что текущий каталог — каталог с исходным кодом сервера (`lab2-1`), текущая версия сервера — 2.0 и исполняемый файл уже получен.

Тогда:
- `java -jar target/checkers-2.0.jar --path=game-server --port=8082` запустит сервер по адресу `http://localhost:8082/game-server`,
- `java -jar target/checkers-2.0.jar --path --port=808` запустит сервер по адресу `http://localhost:808`,
- `java -jar target/checkers-2.0.jar --path=game/checkers --port` запустит сервер по адресу `http://localhost:8080/game/checkers`.
