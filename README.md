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
| Demo data             | `true`                                      | `MIST_SEED_DEMO`      | `-Dmist.seed.demo`      |

Logs go to `~/.mist/logs`: `mist-app.log` for the application and `mist-db.log` for the database operation log.

## Run

```sh
./mvnw javafx:run
```

The splash connects to MongoDB; if it cannot, MIST shows the offline screen with a Retry button.

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

**Offline.** Stop the database (`docker compose down`) and start MIST: the offline screen appears. Start it again
(`docker compose up -d`) and press *Retry*.

From an IDE, run `com.mist.Launcher`, not `MistApp`: JavaFX refuses to start an `Application` subclass as the main
class of a classpath app. Maven generates the TailwindFX stylesheet during `process-resources`, so let the IDE build
with Maven, or run `./mvnw process-resources` once after changing the style classes in a view.

## Tests

| Command          | Runs                                                            | Needs                 |
| ---------------- | --------------------------------------------------------------- | --------------------- |
| `./mvnw test`    | Architecture, i18n and UI tests: TestFX drives every screen on the real services over in-memory DAOs (windows open briefly; the mouse is not moved) | Nothing extra |
| `./mvnw verify`  | The above, plus `*IT` integration tests on MongoDB 7 in Testcontainers | Docker Desktop running |

Without Docker, `verify` fails instead of skipping the integration tests.

`./mvnw javadoc:javadoc` builds the API documentation in `target/reports/apidocs`, private members included; it fails
on any missing comment.

## Packaging

```sh
./mvnw -Ppackage package
```

The `package` profile builds a Windows app image with `jpackage`: `target/jpackage/out/MIST/MIST.exe` with its own
Java runtime, zipped to `target/MIST-0.1.0-windows.zip`. Unzip it anywhere and run `MIST.exe`; no Java or installer
is needed. It is a classpath app image (`jlink` cannot take Hibernate's automatic modules), and it is not an installer,
so WiX is not required. The app still needs MongoDB as described above.

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
docs/                    UI design, data model, decision log and the brand kit sources
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
