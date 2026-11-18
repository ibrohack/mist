# MIST

MIST is a desktop PC game store and community, an alternative to Steam, built for Reto 2.3 "MongoQuest".
It is a layered JavaFX application backed by MongoDB, where clients buy and rate games and developers publish them.

| Document | For |
| --- | --- |
| [docs/user-manual.md](docs/user-manual.md), [docs/manual-usuario.md](docs/manual-usuario.md) | The user manual, in English and Spanish, with a screenshot of every screen |
| [docs/architecture.md](docs/architecture.md) | Installation, the layers and why MIST is built this way |
| [docs/data-model.md](docs/data-model.md) | The MongoDB collections, transactions, validators and the operation log |
| [docs/ui-design.md](docs/ui-design.md) | The aspect and behavior of every screen |
| [docs/decisions.md](docs/decisions.md) | The decision log |

## Prerequisites

The steps below are for Windows 10 or 11 and Windows PowerShell. Run each command on its own, one block at a time,
and wait for it to finish before the next one. Unless a step says *as administrator*, an ordinary PowerShell window
is enough.

- [Temurin JDK 25](https://adoptium.net/). In the installer, enable *Set JAVA_HOME variable*.
- Git.
- One of the databases described in [Database](#database): Docker Desktop, or MongoDB Community Server 7.0 installed
  locally.

Maven is not needed: the wrapper `mvnw.cmd` downloads Maven 3.9.16 on first use.

Check that Java is installed (open a new PowerShell window after installing it). The output names version 25:

```powershell
java -version
```

Move to the project folder; every `.\mvnw.cmd` and `docker compose` command below runs from there:

```powershell
cd C:\path\to\mist
```

## Database

MIST needs MongoDB running as the single-node replica set `rs0`, because multi-document transactions only work on a
replica set. The default connection string, `mongodb://localhost:27017/?replicaSet=rs0`, asks for `rs0`, so MIST does
not connect to a standalone `mongod` at all and shows the offline screen. Choose **one** of the setups below; all of
them listen on `localhost:27017`, so the default configuration needs no change.

| Setup | Needs | Start the database |
| ----- | ----- | ------------------ |
| [A. Docker](#a-docker) | Docker Desktop | `docker compose up -d` each time |
| [B. Local MongoDB as a Windows service](#b-local-mongodb-as-a-windows-service) | Administrator rights, once | Nothing: the service starts with Windows |
| [C. Local MongoDB started by hand](#c-local-mongodb-started-by-hand) | No administrator rights | A PowerShell window with `mongod` open while you use MIST |

### A. Docker

1. Start Docker Desktop and wait until it says the engine is running.
2. Start MongoDB 7.0. The first start also initiates the replica set `rs0`:

   ```powershell
   docker compose up -d
   ```

3. Check it. After a few seconds the `mist-mongo` container shows `(healthy)`:

   ```powershell
   docker compose ps
   ```

4. Run MIST (see [Run](#run)).

To stop the database (the data stays in the `mist-mongo-data` volume):

```powershell
docker compose down
```

If a MongoDB Windows service is also installed, it takes port 27017 and hides the container from `localhost`. Stop
it first, in a PowerShell window opened as administrator:

```powershell
Stop-Service MongoDB
```

### B. Local MongoDB as a Windows service

Done once. After that, MongoDB starts with Windows and MIST connects without any further step.

1. Download and run the MSI of [MongoDB Community Server](https://www.mongodb.com/try/download/community), version
   7.0, platform Windows x64. Choose *Complete* and keep *Install MongoD as a Service* ticked with its defaults
   (service name `MongoDB`, *Run service as Network Service user*). MongoDB Compass is optional.
2. Download and run the MSI of [MongoDB Shell](https://www.mongodb.com/try/download/shell) (`mongosh`); the server
   installer no longer includes it.
3. Close every PowerShell window and open a new one, so it sees the new `PATH`. Check the shell; it prints a version
   number:

   ```powershell
   mongosh --version
   ```

4. Open PowerShell **as administrator**: Start menu, type `PowerShell`, right-click *Windows PowerShell*, *Run as
   administrator*.
5. In that window, open the service's configuration file in Notepad (change `7.0` if you installed another version):

   ```powershell
   notepad "C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg"
   ```

6. Find the line `#replication:` and replace it with these two lines, exactly as shown (two spaces before
   `replSetName`, no tabs):

   ```yaml
   replication:
     replSetName: rs0
   ```

   Save with Ctrl+S and close Notepad.
7. Still in the administrator window, restart the service so it reads the change:

   ```powershell
   Restart-Service MongoDB
   ```

8. Check that it runs. `Status` must be `Running`:

   ```powershell
   Get-Service MongoDB
   ```

   If it is `Stopped`, the file has a typing mistake; see [Troubleshooting](#troubleshooting).
9. Initiate the replica set. This step is easy to forget: `replSetName` alone is not enough, and until the set is
   initiated MIST shows the offline screen. Any PowerShell window works:

   ```powershell
   mongosh --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'localhost:27017'}]})"
   ```

   It prints `ok: 1`. Type the member's host as `localhost:27017`: a bare `rs.initiate()` records the computer's name
   instead, and the connection then fails whenever that name does not resolve (a VPN, another network).
10. Check the replica set. After a few seconds it prints `PRIMARY`:

    ```powershell
    mongosh --quiet --eval "rs.status().members[0].stateStr"
    ```

11. Run MIST (see [Run](#run)).

The replica set is stored with the data, so steps 4 to 10 are never repeated. Running step 9 again only answers
`MongoServerError: already initialized`, which is harmless.

### C. Local MongoDB started by hand

For a computer without administrator rights, or to keep MIST's data apart from the service's. Install the server and
the shell as in steps 1 to 3 of setup B; in the server installer, untick *Install MongoD as a Service*.

1. Create a folder for the data, once:

   ```powershell
   New-Item -ItemType Directory -Path "$env:USERPROFILE\mist-data"
   ```

2. Check that nothing else uses port 27017. It must print nothing:

   ```powershell
   Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue
   ```

   If it prints a line, another server holds the port (the `MongoDB` service, or the Docker container). On Windows a
   second `mongod` does not refuse to start on a busy port: both listen, and MIST may reach the wrong one. Stop the
   other server first (`docker compose down`, or `Stop-Service MongoDB` in an administrator window), or use setup B.
3. Start the server. Leave this window open while you use MIST; Ctrl+C stops it:

   ```powershell
   & "C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe" --replSet rs0 --dbpath "$env:USERPROFILE\mist-data" --bind_ip localhost --port 27017
   ```

   It keeps printing log lines in JSON; that is normal. It is ready when a line contains `Waiting for connections`.
4. Open a **second** PowerShell window and initiate the replica set, once:

   ```powershell
   mongosh --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'localhost:27017'}]})"
   ```

5. In the same second window, check it. After a few seconds it prints `PRIMARY`:

   ```powershell
   mongosh --quiet --eval "rs.status().members[0].stateStr"
   ```

6. Run MIST from the second window (see [Run](#run)).

Next time, only steps 2 and 3 are needed before running MIST.

### MongoDB on another port or computer

Initiate the replica set with that `host:port` in place of `localhost:27017`, then tell MIST where it is. The variable
lasts until the PowerShell window closes, so run MIST from the same window:

```powershell
$env:MIST_MONGO_URI = "mongodb://localhost:27018/?replicaSet=rs0"
```

```powershell
.\mvnw.cmd javafx:run
```

The host and port in the URI must match the ones the replica set was initiated with.

### Troubleshooting

Each command below is a separate check; run the ones that fit the symptom.

**MIST shows the offline screen.** The server is not running, listens on another port, or is not the initiated
replica set `rs0`.

1. See whether anything listens on port 27017. No output means no server is running; start it (setup A step 2,
   `Start-Service MongoDB` as administrator for setup B, or setup C step 3):

   ```powershell
   Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue
   ```

2. See the replica set's state. `PRIMARY` is correct; `no replset config has been received` means it was never
   initiated (run the `rs.initiate(...)` step); `not running with --replSet` means `replSetName` is missing:

   ```powershell
   mongosh --quiet --eval "rs.status().members[0].stateStr"
   ```

3. Read MIST's last log lines. `Cannot reach the database` confirms that the connection failed, with the time of each
   attempt:

   ```powershell
   Get-Content "$env:USERPROFILE\.mist\logs\mist-app.log" -Tail 20
   ```

Once the database is fixed, press *Retry* on the offline screen.

**The service does not start after editing `mongod.cfg`.** The YAML is invalid, usually a tab or a wrong indent.
The last lines of the server's log name the problem:

```powershell
Get-Content "C:\Program Files\MongoDB\Server\7.0\log\mongod.log" -Tail 20
```

**MIST shows old or unexpected data.** Docker and a local server both use port 27017, and MIST reached the other
one. Run only one of them.

## Configuration

The defaults live in `src/main/resources/mist.properties`. An environment variable overrides a default, and a Java
system property overrides both.

| Setting               | Default                                     | Environment variable  | System property         |
| --------------------- | ------------------------------------------- | --------------------- | ----------------------- |
| MongoDB connection    | `mongodb://localhost:27017/?replicaSet=rs0` | `MIST_MONGO_URI`      | `-Dmist.mongo.uri`      |
| Database name         | `mist`                                      | `MIST_MONGO_DATABASE` | `-Dmist.mongo.database` |
| Demo data             | `true`                                      | `MIST_SEED_DEMO`      | `-Dmist.seed.demo`      |

With `.\mvnw.cmd javafx:run`, use the environment variables: MIST runs in its own Java process, which does not
receive `-D` options given to Maven. Set the variable in the PowerShell window, then run MIST from that same window.
For example, to start on a new database without demo data:

```powershell
$env:MIST_MONGO_DATABASE = "mist-empty"
```

```powershell
$env:MIST_SEED_DEMO = "false"
```

```powershell
.\mvnw.cmd javafx:run
```

To go back to the defaults, close the window, or remove a variable:

```powershell
Remove-Item Env:MIST_SEED_DEMO
```

The system properties are for an IDE run configuration (its *VM options*) or a direct `java -D...` command.

Logs go to `%USERPROFILE%\.mist\logs`: `mist-app.log` for the application and `mist-db.log` for the database
operation log.

## Run

With the database running (see [Database](#database)), from the project folder:

```powershell
.\mvnw.cmd javafx:run
```

The first run downloads Maven and the dependencies, which takes a few minutes. The splash connects to MongoDB; if it
cannot, MIST shows the offline screen with a Retry button (see [Troubleshooting](#troubleshooting)).

### Demo data

On the first start against an empty database, MIST seeds demo content (turn it off with `mist.seed.demo=false`).
Every demo account uses the password `mist2026`:

The catalog is Metacritic's fifteen highest rated games of all time, each sold by the studio that made it, with its
real cover and screenshots (sources in `src/main/resources/com/mist/demo/SOURCES.md`). None of them is free, so the
store's Free tab starts empty.

| Account                        | Role      | What it has                                                                 |
| ------------------------------ | --------- | --------------------------------------------------------------------------- |
| `admin@mist.com`               | Admin     | The operation log viewer                                                    |
| `rockstarnorth@mist.com`       | Developer | Rockstar North: GTA IV and GTA V, and Grand Theft Auto VI as a draft        |
| `rockstar.studios@mist.com`    | Developer | Rockstar Studios: Red Dead Redemption 2, the newest release                 |
| `nintendo.epd@mist.com`        | Developer | Nintendo EPD: Breath of the Wild and Super Mario Odyssey                    |
| `nintendo.ead@mist.com`        | Developer | Nintendo EAD: Ocarina of Time                                               |
| `nintendo.ead.tokyo@mist.com`  | Developer | Nintendo EAD Tokyo: Super Mario Galaxy and Super Mario Galaxy 2             |
| `dma.design@mist.com`          | Developer | DMA Design: Grand Theft Auto III                                            |
| `namco@mist.com`               | Developer | Namco: SoulCalibur                                                          |
| `neversoft@mist.com`           | Developer | Neversoft: Tony Hawk's Pro Skater 2                                         |
| `rare@mist.com`                | Developer | Rare: Perfect Dark                                                          |
| `retro.studios@mist.com`       | Developer | Retro Studios: Metroid Prime                                                |
| `bungie@mist.com`              | Developer | Bungie: Halo: Combat Evolved                                                |
| `visual.concepts@mist.com`     | Developer | Visual Concepts: NFL 2K1                                                    |
| `lucia@mist.com`               | Player    | Four games (two still returnable), a cart, a wishlist, two reviews          |
| `marc@mist.com`                | Player    | Two games and two reviews                                                   |
| `nora@mist.com`                | Player    | Two games bought 20 days ago and two reviews                                |

### Demo script

A walk through every use case, one role at a time. F1 opens the help of the current screen at any point, and the user
menu (the avatar, top right) switches the language and the theme.

**Guest.** On the login screen, press *Enter as guest*. Browse the store's carousel and its New, Top sellers and Free
tabs, search for `zelda`, and open a game. *Add to cart* asks you to log in.

**Player** (`lucia@mist.com`).

1. *Cart*: Grand Theft Auto IV is waiting. Tick the terms, check out, confirm, and save the receipt as a PDF.
2. *Wishlist*: move Red Dead Redemption 2 to the cart, or remove it.
3. *Library*: search, sort and filter by tag. Right-click Super Mario Odyssey and write a review; it shows on the game
   page.
4. Right-click Metroid Prime and *Return* it (it was bought today, so it is inside the 14 days).
5. *Orders*: the returned game shows its refund date; the 20-day-old order cannot be returned. Download a receipt.
6. User menu, *Profile*: change the avatar, the card or the password, one row at a time.

**Developer** (`rockstarnorth@mist.com`).

1. The dashboard opens with the revenue, units and games on sale, the revenue chart and the table of games.
2. Double-click Grand Theft Auto IV's price, type `14,99` and press Enter. Type `free` instead and the old price comes
   back with a warning.
3. *New game*: give a name and a price; the editor opens. *Publish* marks in red what the draft lacks. Fill it in,
   change the cover, add tags, languages and achievements (double-click a cell), *Save*, then *Publish*.
4. Back on the dashboard, select Grand Theft Auto VI (complete already) and *Publish* it; select a published game and
   *Take off sale*.
5. *Sales report* saves the dashboard's figures as a PDF.

**Admin** (`admin@mist.com`). The operation log opens on the newest day with database operations. Filter by day,
user, operation or *Failed only*, and right-click a row to copy it. What other users do while you watch shows up after
*Refresh*.

**Offline.** Stop the database (`docker compose down`, `Stop-Service MongoDB` as administrator, or Ctrl+C in the
`mongod` window) and start MIST: the offline screen appears. Start the database again (`docker compose up -d`,
`Start-Service MongoDB`, or the `mongod` command) and press *Retry*.

From an IDE, run `com.mist.Launcher`, not `MistApp`: JavaFX refuses to start an `Application` subclass as the main
class of a classpath app. Maven generates the TailwindFX stylesheet during `process-resources`, so let the IDE build
with Maven, or run `.\mvnw.cmd process-resources` once after changing the style classes in a view.

## Tests

| Command          | Runs                                                            | Needs                 |
| ---------------- | --------------------------------------------------------------- | --------------------- |
| `.\mvnw.cmd test`    | Architecture, i18n and UI tests: TestFX drives every screen on the real services over in-memory DAOs (windows open briefly; the mouse is not moved) | Nothing extra |
| `.\mvnw.cmd verify`  | The above, plus `*IT` integration tests on MongoDB 7 in Testcontainers | Docker Desktop running |

Without Docker, `verify` fails instead of skipping the integration tests.

`.\mvnw.cmd javadoc:javadoc` builds the API documentation in `target/reports/apidocs`, private members included; it fails
on any Javadoc warning.

## Packaging

```powershell
.\mvnw.cmd -Ppackage package
```

The `package` profile builds a Windows app image with `jpackage`: `target/jpackage/out/MIST/MIST.exe` with its own
Java runtime, zipped to `target/MIST-1.0.0-windows.zip`. Unzip it anywhere and run `MIST.exe`; no Java or installer
is needed. It is a classpath app image (`jlink` cannot take Hibernate's automatic modules), and it is not an installer,
so WiX is not required. The app still needs MongoDB as described above.

### Executable JAR

```powershell
.\mvnw.cmd -Pjar package
```

The `jar` profile builds `target/dist/MIST-1.0.0.jar`, a single JAR with every library inside. It needs a Java 25 runtime
on the computer, and runs with:

```powershell
java -jar MIST-1.0.0.jar
```

The settings in [Configuration](#configuration) apply as usual; system properties go before `-jar`, for example
`java -Dmist.seed.demo=false -jar MIST-1.0.0.jar`. The JAR carries JavaFX's native libraries for the platform it was
built on, so a JAR built on Windows runs on Windows. JavaFX prints an `Unsupported JavaFX configuration` warning at
start, as it does for any classpath app; it is harmless.

## Layout

```
src/main/java/com/mist/
├─ Launcher, MistApp     entry point and JavaFX application
├─ MistContext           opens the data layer and builds every service for the UI
├─ ui/                   navigation (Navigator, SceneRouter, View), UiContext, I18n, Styles, UserSession
│  ├─ controller/        FXML controllers, one per view, and the dialogs (review, new game)
│  ├─ component/         custom list cells, and the row models of the achievements table and the orders tree
│  ├─ report/            PDF reports (receipt, sales) with JasperReports
│  └─ support/           background tasks, error reporting, dialogs, form validation, formats, images
├─ service/              use cases (Auth, User, Store, Library, Review, Publishing, Media, Log), forms, DemoData
├─ dao/                  one interface per collection, plus TransactionRunner
│  └─ hibernate/         implementations on Hibernate ORM with the MongoDB extension, HibernateDataAccess
├─ model/                entities and enums
├─ config/               AppConfig, MongoConnection, HibernateBootstrap, SchemaSetup
├─ exception/            checked exceptions rooted at MistException
└─ logging/              database operation log
src/main/resources/
├─ com/mist/ui/          views (FXML), report templates (JRXML), the themes, Space Grotesk fonts, launcher icons
├─ i18n/                 messages_es.properties, messages_en.properties
└─ mist.properties, logback.xml
src/main/packaging/      the Windows icon and the zip descriptor of the app image
docs/                    architecture, UI design, data model, decision log and the brand kit sources
```

Layer rules, enforced by `src/test/java/com/mist/architecture/LayerRulesTest.java`:

- `ui` never touches `dao`; data reaches the UI only through services.
- `service` never depends on `ui` or on `dao.hibernate`.
- The `dao` interfaces depend only on `model` and `exception`.
- Only `dao.hibernate`, `config` and `model` may use Hibernate or the MongoDB driver.
- Only `com.mist` and `ui` may use JavaFX.
- Only `com.mist.ui.Styles` may use TailwindFX.
- Every class in `exception` extends `MistException`.

Using the layers from the UI:

- Open `MistContext.open(AppConfig.load())` once, when the splash checks the database; a `DataAccessException` means
  MongoDB cannot be reached. Close it when the app stops.
- Services are stateless: each method takes the id of the user who acts, and the UI keeps the logged-in `User` that
  `AuthService.login` returns in `UserSession`.
- Every service method throws `MistException` subclasses whose message is a bundle key; controllers show it with
  `I18n` in a themed alert.

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
  - Static paddings set all four sides, so `px-6,py-3` keeps only the last one; use one `p-*` utility, or
    `<padding>` in the FXML.
  - `text-[104px]` sets the text color, not the size. Use a named size (`text-base` ... `text-9xl`), or a `mist-*`
    class when the brand needs an exact size, as `mist-wordmark` does.
- No UI text in Java or FXML. Every string comes from `i18n/messages_{es,en}.properties`; Spanish is the default.

## Conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, ...).
- One feature branch per task, merged through a pull request reviewed by a partner.
- Code, comments and Javadoc are in English, and every public type has a class-level Javadoc.

Space Grotesk is licensed under the SIL Open Font License; see `src/main/resources/com/mist/ui/fonts/OFL.txt`.
