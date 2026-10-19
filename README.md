# MIST

MIST is a desktop PC game store and community, an alternative to Steam, built for Reto 2.3 "MongoQuest".
It is a layered JavaFX application backed by MongoDB, where clients buy and rate games and developers publish them.

## Prerequisites

- [Temurin JDK 25](https://adoptium.net/)
- Docker Desktop, or MongoDB 7 installed locally and configured as replica set `rs0`
- Git

Maven is not needed: the wrapper (`./mvnw`, or `mvnw.cmd` on Windows) downloads Maven 3.9.16 on first use.

## Database

MIST needs MongoDB running as the single-node replica set `rs0`, because multi-document transactions only work on a
replica set.

**With Docker:**

```sh
docker compose up -d     # MongoDB 7.0 on localhost:27017; the first start initiates rs0
docker compose down      # stops it; the data stays in the mist-mongo-data volume
```

If a MongoDB Windows service already listens on port 27017, it hides the container from `localhost`. Stop that service
first (`net stop MongoDB` in an administrator terminal), or use it as a local install as described below.

**With a local install:** add the replica set to `mongod.cfg`, restart the service, then initiate the set once:

```yaml
replication:
  replSetName: rs0
```

```sh
mongosh --eval "rs.initiate()"
```

## Configuration

The defaults live in `src/main/resources/mist.properties`. An environment variable overrides a default, and a system
property overrides both.

| Setting               | Default                                     | Environment variable  | System property         |
| --------------------- | ------------------------------------------- | --------------------- | ----------------------- |
| MongoDB connection    | `mongodb://localhost:27017/?replicaSet=rs0` | `MIST_MONGO_URI`      | `-Dmist.mongo.uri`      |
| Database name         | `mist`                                      | `MIST_MONGO_DATABASE` | `-Dmist.mongo.database` |

Logs go to `~/.mist/logs`: `mist-app.log` for the application and `mist-db.log` for the database operation log.

## Run

```sh
./mvnw javafx:run
```

From an IDE, run `com.mist.Launcher`, not `MistApp`: JavaFX refuses to start an `Application` subclass as the main
class of a classpath app. Maven generates the TailwindFX stylesheet during `process-resources`, so let the IDE build
with Maven, or run `./mvnw process-resources` once after changing the style classes in a view.

## Tests

| Command          | Runs                                                            | Needs                 |
| ---------------- | --------------------------------------------------------------- | --------------------- |
| `./mvnw test`    | Unit, architecture and UI tests (TestFX opens a real window)     | Nothing extra         |
| `./mvnw verify`  | The above, plus `*IT` integration tests on MongoDB 7 in Testcontainers | Docker Desktop running |

Without Docker, `verify` fails instead of skipping the integration tests.

## Layout

```
src/main/java/com/mist/
├─ Launcher, MistApp     entry point and JavaFX application
├─ ui/                   navigation (SceneRouter, View), I18n, Styles (TailwindFX facade), UserSession
│  └─ controller/        FXML controllers
├─ service/              use cases, validation, domain exceptions
├─ dao/                  one interface per collection
│  └─ hibernate/         implementations on Hibernate ORM with the MongoDB extension
├─ model/                entities and enums
├─ config/               AppConfig, MongoConnection, HibernateBootstrap, SchemaSetup
├─ exception/            checked exceptions rooted at MistException
└─ logging/              database operation log
src/main/resources/
├─ com/mist/ui/          views (FXML), mist-theme.css, Space Grotesk fonts, launcher icons
├─ i18n/                 messages_es.properties, messages_en.properties
└─ mist.properties, logback.xml
docs/                    decision log and the brand kit sources
```

Layer rules, enforced by `src/test/java/com/mist/architecture/LayerRulesTest.java`:

- `ui` never touches `dao`; data reaches the UI only through services.
- `service` never depends on `ui` or on `dao.hibernate`.
- The `dao` interfaces depend only on `model` and `exception`.
- Only `dao.hibernate`, `config` and `model` may use Hibernate or the MongoDB driver.
- Only `com.mist` and `ui` may use JavaFX.
- Only `com.mist.ui.Styles` may use TailwindFX.
- Every class in `exception` extends `MistException`.

## Styling

- TailwindFX utilities set spacing, layout, typography and effects. Never use TailwindFX palette colors.
- Brand colors come only from the `mist-*` classes in `mist-theme.css` (`mist-bg-void`, `mist-text-fog`,
  `mist-fill-acid`, ...). The theme is installed last, so it wins over TailwindFX's stylesheets.
- Never put a TailwindFX utility and a `mist-*` class that set the same property on one node: TailwindFX writes some
  utilities as inline styles, and inline styles beat any stylesheet.
- TailwindFX 0.1.2 specifics:
  - Static utilities such as `p-24` or `font-bold` come from the stylesheet Maven generates.
  - Arbitrary values such as `w-[350px]` are applied when the view loads.
  - For `HBox` and `VBox` spacing, use `gap-[24px]`; a static `gap-6` does not reach them.
  - `text-[104px]` sets the text color, not the size. Use a named size (`text-base` ... `text-9xl`), or a `mist-*`
    class when the brand needs an exact size, as `mist-wordmark` does.
- No UI text in Java or FXML. Every string comes from `i18n/messages_{es,en}.properties`; Spanish is the default.

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, ...).
- One feature branch per task, merged through a pull request reviewed by a partner.
- Code, comments and Javadoc are in English, and every public type has a class-level Javadoc.

Space Grotesk is licensed under the SIL Open Font License; see `src/main/resources/com/mist/ui/fonts/OFL.txt`.
