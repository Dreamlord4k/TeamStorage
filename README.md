# TeamStorage 26.2

TeamStorage is a Paper/Purpur 26.2 plugin that adds city-based shared storage, optional city tags in the player list, and local city chat.

## Requirements

- Java 25+
- Paper or Purpur 26.2
- Maven

## Build

```bash
mvn clean package
```

The compiled plugin jar will be created in:

```text
target/TeamStorage-26.2-1.0.0-26.2.jar
```

If your system Java is older than 25, use the bundled portable JDK in this copy:

```powershell
$env:JAVA_HOME = "$PWD\.jdk\jdk-25.0.3+9"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean package
```

## Features

- Personal vanilla ender chest remains unchanged.
- City members can open a shared city storage through an ender chest GUI.
- Admins manage cities with `/city`.
- Each city has a 2-4 character tag.
- Optional player-list prefix: `[TAG] Player`.
- TAB compatibility: no scoreboard teams are used; `tab.enabled` explicitly controls whether TeamStorage changes player-list names.
- City chat through the `@` prefix.
- Persistent city-chat mode through `/citychat` or `/cc`.
- Russian and English messages in one `config.yml`.
- SQLite storage.
- Storage and city-chat logs.

## Commands

- `/city create <name> <tag>`
- `/city delete <name>`
- `/city add <player> <city>`
- `/city remove <player>`
- `/city info <city>`
- `/city list`
- `/city tag <city> <tag>`
- `/city tagcolor <city> <color|#hex>`
- `/teamstorage toggle`
- `/teamstorage open [city]`
- `/citychat`
- `/cc`

`/citystorage` remains available as a legacy alias for `/teamstorage`.

When upgrading from CityStorage, the plugin copies the old `plugins/CityStorage` data into `plugins/TeamStorage` once. The original folder is kept as a backup.

## City Chat

Write `@message` to send a message only to members of your current city.

If a player is not in a city and writes `@message`, the `@` is removed and the message goes to global chat.

`/citychat` toggles persistent city chat mode. When enabled, all normal messages from that player are sent to city chat until toggled off.

## Configuration

Use `language: "ru"` or `language: "en"` in `config.yml`.

Player-list tags are controlled only by this option:

```yaml
tab:
  enabled: true
```

Set it to `false` when TAB or another plugin should have full control over player-list names. TeamStorage does not automatically detect or disable itself for other tab plugins.

Tag colors can use configured aliases:

```yaml
/city tagcolor Novograd green
```

or HEX colors:

```yaml
/city tagcolor Novograd #55ff99
```

---

# TeamStorage 26.2

TeamStorage - плагин для Paper/Purpur 26.2, который добавляет общее городское хранилище, опциональные теги города в табе и локальный чат города.

## Требования

- Java 25+
- Paper или Purpur 26.2
- Maven

## Сборка

```bash
mvn clean package
```

Готовый jar будет создан здесь:

```text
target/TeamStorage-26.2-1.0.0-26.2.jar
```

Если системная Java ниже 25, можно использовать портативный JDK внутри этой копии:

```powershell
$env:JAVA_HOME = "$PWD\.jdk\jdk-25.0.3+9"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean package
```

## Возможности

- Личный ванильный эндер-сундук остается без изменений.
- Участники города могут открывать общее городское хранилище через GUI эндер-сундука.
- Администраторы управляют городами через `/city`.
- У каждого города есть короткий тег из 2-4 символов.
- Опциональная приписка в табе: `[TAG] Player`.
- Совместимость с TAB: scoreboard teams не используются, а изменение имён в табе вручную управляется параметром `tab.enabled`.
- Локальный чат города через префикс `@`.
- Постоянный режим городского чата через `/citychat` или `/cc`.
- Русские и английские сообщения в одном `config.yml`.
- Хранение данных в SQLite.
- Логи хранилища и городского чата.

## Команды

- `/city create <name> <tag>`
- `/city delete <name>`
- `/city add <player> <city>`
- `/city remove <player>`
- `/city info <city>`
- `/city list`
- `/city tag <city> <tag>`
- `/city tagcolor <city> <color|#hex>`
- `/teamstorage toggle`
- `/teamstorage open [city]`
- `/citychat`
- `/cc`

`/citystorage` остается совместимым алиасом команды `/teamstorage`.

При обновлении с CityStorage плагин один раз копирует данные из `plugins/CityStorage` в `plugins/TeamStorage`. Старая папка остается резервной копией.

## Городской чат

Напиши `@сообщение`, чтобы отправить сообщение только участникам своего города.

Если игрок не состоит в городе и пишет `@сообщение`, символ `@` убирается, а сообщение уходит в глобальный чат.

`/citychat` включает или выключает постоянный режим городского чата. Когда режим включен, обычные сообщения игрока уходят в чат города, пока он не выключит режим.

## Настройка

Язык меняется в `config.yml`:

```yaml
language: "ru"
```

или:

```yaml
language: "en"
```

Изменение имён игроков в табе управляется только этим параметром:

```yaml
tab:
  enabled: true
```

Установи `false`, если TAB или другой плагин должен полностью управлять именами игроков. TeamStorage не обнаруживает другие плагины таба и не отключает функцию автоматически.

Цвет тега можно задавать алиасом:

```yaml
/city tagcolor Novograd green
```

или HEX-цветом:

```yaml
/city tagcolor Novograd #55ff99
```
