# MIST decision log

Decisions taken while building the project scaffold (week 2, 19 Oct 2026), the model, data access and service layers
(17 Nov 2026), the user interface (18 Nov 2026), the review against the module rubrics (18 Nov 2026), and how they
change the preliminary plan of 12 Oct 2026.

## Scaffold decisions

1. **The first window is the real splash screen**, rebuilt in FXML from `docs/brand/mist-splash.svg`: the MIST mark
   and wordmark, the tagline from the ES/EN bundles, and an acid progress bar.
   - It stays the app's first screen, so `MistApp` never has to change which screen it opens.
   - Week 2 only adds the database check and the move to Login inside `SplashController`.
   - JavaFX has no letter-spacing, so the kit's tracking on the wordmark and tagline cannot be reproduced.
2. **The database configuration is final.**
   - `mist.properties` holds the defaults; `MIST_MONGO_*` environment variables and `-Dmist.mongo.*` system
     properties override them.
   - `AppConfig` loads those settings, and `MongoConnection` creates the single shared `MongoClient`.
   - `HibernateBootstrap` builds the `SessionFactory` on that client, with every Hibernate setting in code. There is
     no `hibernate.properties`.
   - The app does not connect at startup yet; the splash's database check calls these classes in week 2.
3. **Package rename.** `com.mist.dao.mongo` becomes `com.mist.dao.hibernate`.
4. **Checked exceptions.** `MistException extends Exception`, so the path to the UI shows in method signatures
   (IL5.6). Exception messages are i18n bundle keys.
5. **`mist-` prefix.** Brand utility classes are prefixed (`mist-bg-void`, `mist-text-fog`, `mist-fill-acid`) so
   TailwindFX never treats them as its own tokens.
6. **Dark defaults.** `.root` in `mist-theme.css` also sets Modena's base colors (`-fx-base`, `-fx-background`,
   `-fx-accent`, ...), so every control starts dark and readable before it gets a brand rule.
7. **Layer test.** `LayerRulesTest` (ArchUnit, run as plain JUnit tests) enforces the layer rules from day one.
8. **Log location.** Logs go to `~/.mist/logs`, because an installed app cannot write next to itself in Program Files.
9. **JUnit 5.14, not 6.** TestFX 4.0.18 targets JUnit 5.
10. **Docker for `verify`.** `./mvnw test` needs nothing extra. `./mvnw verify` needs Docker Desktop running; the
    MongoDB integration test fails rather than silently skipping.

## Model layer decisions (17 Nov 2026)

The model follows the team's diagrams (`Usuario` → `Cliente`/`Desarrollador`, `Juego`, `Order`, `Valoraciones`) and the
navigation diagram. [data-model.md](data-model.md) describes every collection and field.

1. **One `users` collection** for players, developers and admins, through `SINGLE_TABLE` inheritance with discriminator
   `role`: one login query, one unique index on `email`, and the Admin account the rubric's log viewer needs.
2. **"Cliente" is called Player** in code (`Player`, `Role.PLAYER`, `playerId`).
3. **Images live in MongoDB**, in the `media` collection (binary, up to 2 MiB each), so every machine that uses the same
   database sees the same covers and avatars, and developers can upload them.
4. **Saved card, simulated charge.** Only the holder, brand, last four digits and expiry are stored.
5. **Achievements are display only**, embedded in the game.
6. **Normalized personal data:** first and last name, a structured address, a phone split into country code and
   number, and a unique studio name for developers.
7. **Argon2id password hashes**; the model refuses a value that is not one.
8. **Model invariants carry bundle keys.** Factories and mutators throw `IllegalArgumentException` or
   `IllegalStateException` whose message is a `ModelKeys` key; services wrap it in `ValidationException`. Embedded values
   are immutable records, and entities replace them (copy-on-write) so Hibernate always writes the change.
9. **No automated tests for the model layer**; the project adds UI tests later. A one-off check against a MongoDB 7
   replica set proved the mapping (see Findings).

## Data access layer decisions (17 Nov 2026)

1. **One DAO interface per collection** (`UserDao`, `GameDao`, `OrderDao`, `ReviewDao`, `MediaDao`), implemented in
   `dao.hibernate`. The interfaces speak only `model`, `exception` and `ObjectId`.
2. **Transactions without Hibernate in the services.** `TransactionRunner` (in `dao`) runs a unit of work in one
   transaction; `HibernateTransactions` binds the session to the thread, so DAO calls inside the work join it and
   nested calls join the outer one. Outside a transaction, each DAO call runs in a short transaction of its own.
3. **Writes flush inside the DAO call**, so a broken unique index fails that call, and is logged against it, instead of
   failing the commit at the end.
4. **Error translation.** Hibernate, JPA, JDBC and driver failures become `DataAccessException` (`dao.failure`); a
   duplicate key (MongoDB codes 11000, 11001, 12582) becomes `DuplicateEntityException` (`dao.duplicate`), and a
   document a validator rejects (code 121) becomes `InvalidDocumentException` (`dao.invalidDocument`). Model
   exceptions pass through unchanged, after the rollback, for the services to wrap.
5. **Operation log.** Every DAO call writes `date | user | tx | OPERATION | collection | details | OK/FAILED` to
   `mist-db.log`; the line format is in [data-model.md](data-model.md#operation-log). The user set at login is kept
   app-wide, so background threads log it too. Details never contain passwords or hashes.
6. **`SchemaSetup.apply` runs on every start.** It creates missing collections, updates the validators with `collMod`
   and creates the indexes; all of it is idempotent, and each step is logged. The validators are strict and reject
   invalid writes.
7. **Paging uses `setFirstResult`/`setMaxResults`**, which the extension translates to `$skip`/`$limit`.

## Service layer decisions (17 Nov 2026)

1. **Stateless services that take the acting user's id.** Each method loads that user and checks that the account
   exists, is active and has the right role. The UI keeps the logged-in `User` that `AuthService.login` returns.
2. **Raw input in form records** (`PlayerSignUp`, `ProfileForm`, `GameForm`, `CardForm`, `ImageUpload`, ...). The
   services build the model values from them, so every rule violation reaches the UI as a `ValidationException` with
   the model's bundle key. A few model calls take values the service has already checked (for example
   `Developer.recordSale` in checkout) and are not wrapped; if one of them threw, it would be a bug, and
   `ErrorReporter` shows it as an unexpected error.
3. **Argon2id through Bouncy Castle** (`bcprov-jdk18on`, pure Java). `PasswordHasher` writes the standard PHC string
   with the OWASP minimum cost (`m=19456,t=2,p=1`), a 16-byte salt and a 32-byte hash; checks read the cost from the
   stored string. A one-off check confirmed that `argon2-cffi` verifies MIST's hashes and MIST verifies its hashes.
4. **Login does not reveal which accounts exist.** A wrong email and a wrong password get the same message, and an
   unknown email still runs one hash check, so both take the same time. Passwords arrive as `char[]` and are wiped.
5. **Password policy:** 8 to 128 characters, with at least one letter and one digit.
6. **Uniqueness is checked twice.** Services check the email, nickname, studio name and review first, to show a precise
   message; the unique indexes still stop a write that races past the check (`dao.duplicate`).
7. **Returns are allowed for 14 days** after the purchase (`LibraryService.REFUND_WINDOW`). The model leaves the length
   to the service, and nothing earlier fixed it.
8. **A game joins the developer's catalog (`gameIds`) when it is published**, in the publish transaction, as the
   transaction table in [data-model.md](data-model.md) says. Drafts appear on the dashboard through the
   `developerId` index.
9. **`MediaService`** serves covers, screenshots and avatars to the views, which may not use the DAOs. Replacing an
   image stores the new one and deletes the old one in the same transaction.
10. **`MistContext` wires the app.** It lives in `com.mist`, which may depend on every layer, and gets the DAOs from
    `HibernateDataAccess`, so it never touches Hibernate types (the layer test forbids them outside the data layer).
    `MistApp` does not open it yet; the splash's database check will.

## UI layer decisions (18 Nov 2026)

The interface follows the team's navigation diagram and the brand kit; [ui-design.md](ui-design.md) describes the aspect
and behavior of every screen.

1. **The UI never sees `MistContext`.** `MistApp` hands the UI a `Backend` (`MistBackend` in the app); the views get
   `AppServices` from it. `LayerRulesTest` keeps `ui` away from `dao`.
2. **Controllers get a `UiContext` in their constructor**, through the `FXMLLoader` controller factory in
   `SceneRouter`; FXML files keep `fx:controller`, so Scene Builder opens them.
3. **Every service call runs on a worker thread** (`UiTasks`, a JavaFX `Task`), with the control that started it
   disabled. Errors reach one `ErrorReporter`: rule violations are warnings or marks on the form, database failures
   offer a retry, a deactivated account ends the session, anything else is logged and shown as a generic error.
4. **Forms are checked twice**: `FormValidator` before the call, with the model's own limits and formats, and the
   service after it; a service error key is bound to its field, so it is marked in the same place.
5. **Dialogs never block** the JavaFX thread and carry MIST's own button texts, so they follow the UI language.
6. **Language and theme switch at once** and are saved in the user's preferences; a language change rebuilds the
   screen in place. The light theme only redefines the brand tokens.
7. **Login hides which accounts exist**, as the service does: wrong email and wrong password get the same message.
8. **The profile edits one row at a time, in place**, as the navigation diagram draws it, instead of a separate form.
9. **Demo data seeds an empty database** (`DemoData`, `mist.seed.demo=true` by default): an admin, 12 developer
   studios, 15 published games and 1 draft with real cover and screenshot JPGs (art is only drawn when a JPG is
   missing), three players with 4 orders, a cart, a wishlist and 6 reviews. It is the only way an admin account is
   created.
10. **The operation log is read from the files**, `~/.mist/logs/mist-db.log` and its rolled days, by `LogService`, for
    admins only (ADT IL5.1).
11. **Help comes from the bundles**: one topic per screen (`help.<topic>.*`), so it is translated like the rest and a
    screen gets help by adding keys. F1 opens it everywhere.
12. **UI tests run the real services on in-memory DAOs** (`com.mist.fake`) with failure switches, so `./mvnw test`
    covers use cases, rule errors, database failures and file errors without Docker. Interactions fire the controls'
    events instead of moving the mouse.
13. **Packaging is a jpackage app image**, not an installer, so no WiX is needed; see the README.
14. **Javadoc covers private members too** (`show=private`, `doclint=all`) and fails on any warning
    (`./mvnw javadoc:javadoc`). Doclint does not flag every undocumented private field, so the rubric review also
    checked every declaration by script.
15. **Reports are filled from what the screen shows.** The receipt takes the order on screen and the sales report the
    dashboard's figures; labels come from the UI bundles (`$R{...}`) and values arrive formatted by `Formats`, so a PDF
    matches the screen and its language. The DejaVu fonts are embedded (for the euro sign and accents), and the JDT
    compiler builds the templates, so the app image needs no `jdk.compiler`.
16. **Two tables are edited in place**, as the rubric asks: the dashboard's price column saves on commit and puts the
    stored price back when the service refuses it; the editor's achievement cells check their length on commit and
    are saved with the rest of the game.
17. **New game asks only for a name and a price**, what `createDraft` needs, and opens the draft in the editor. The
    service answers an incomplete draft with one key (`model.game.incomplete`), so the editor checks the publishing
    rules itself first and marks each missing field.
18. **Prices are typed in either language**: `12,99`, `12.99` and `12,99 €` are the same price, everywhere a price is
    entered.
19. **Returns are offered in two places**: the library's context menu, for the game at hand, and the orders tree,
    which also shows what was refunded and gives each order's receipt.
20. **The early library view was rebuilt**: its own blue palette and fixed texts gave way to the brand tokens and the
    bundles, and the empty-library art is rebuilt in FXML like the splash and offline screens.

## Rubric review decisions (18 Nov 2026)

The ADT and DIN rubrics and the statement were checked item by item against the code. These decisions close the gaps
found.

1. **No lost updates, without `@Version`.** Every service method that loads a document, changes it and saves it back
   (cart, wishlist, profile, card, studio name, bank account, preferences, password, game update, delist) now does so
   inside one transaction, as the multi-document use cases already did. MongoDB aborts a transaction whose document
   another transaction changed first, so a stale copy is never written back. `@Version` was not used: support for it
   in `mongodb-hibernate` 1.0.0 is not documented, and the transaction approach needs no schema change.
2. **Transient conflicts are retried.** When MongoDB reports a write conflict (code 112) or labels a failure
   `TransientTransactionError`, `HibernateTransactions` runs the whole work again after a short random pause that
   grows with each attempt, up to 5 attempts, and logs a `RETRY` line. A one-off check (not kept) ran six concurrent
   wishlist changes for one player: with immediate retries three were lost to repeated conflicts; with the pause all
   six were kept, in three runs out of three.
3. **Transactions are numbered in the log.** `BEGIN`, `COMMIT` and `ROLLBACK` lines and every DAO line in between carry
   the transaction number, so the admin can see which operations a rollback undid. The log viewer shows the column
   and filters by it.
4. **Sign-in attempts are logged with the email typed**, and registration with the new email, instead of
   `anonymous`; each login adds a `LOGIN` line that worked or failed, with the reason key.
5. **The validators share the model's patterns.** Email, nickname, phone, postal code, country and card digits are
   public regex constants in the model, used by both the model and `SchemaSetup`. Every document and embedded
   document rejects undeclared fields (`additionalProperties: false`), which works because Hibernate writes only
   mapped fields. `media` also bounds the image size with `$binarySize`, which `$jsonSchema` cannot express. A one-off
   check confirmed MongoDB rejects a bad email, nickname or phone, an extra field (top level or embedded) and an empty
   image with code 121, and accepts valid copies.
6. **The demo seed is one transaction**, and the admin, whose presence means "already seeded", is written last; a
   failure no longer leaves a partial catalog that is never completed.
7. **Packaging stays a zipped jpackage app image.** An installer (`--type exe`) needs the WiX Toolset on the build
   machine and adds nothing the course asks for that the app image lacks; see UI decision 13.

## Changes to the preliminary plan

| Topic | Preliminary plan | Now |
| --- | --- | --- |
| Java | 21 + JavaFX 21 | 25 + JavaFX 25.0.4 |
| Mapping | `mongodb-driver-sync` + `PojoCodecProvider` | Hibernate ORM 7.4.7 + `mongodb-hibernate` 1.0.0, on one shared `MongoClient`. The driver is used directly only for validators, indexes, the catalog search and the connection check |
| Accounts | `users` (CLIENT/DEVELOPER/ADMIN) + `studios` collection | one `users` collection with `SINGLE_TABLE` inheritance: `Player` (was "client"), `Developer` (with a unique `studioName`), `Admin`; no studios collection, references become `developerId` and `playerId` |
| Fifth collection | `studios` | `media`: images stored as binary documents, referenced by games and users |
| Payment | wallet balance, `InsufficientFundsException` | saved card (masked: holder, brand, last 4, expiry), charge simulated; the exception is removed |
| Passwords | BCrypt | Argon2id (PHC string), computed by `AuthService` |
| Personal data | `profile {displayName, …}` | normalized: `firstName`/`lastName`, `address` struct, `phone {countryCode, number}` |
| Achievements | unlocked per user in the library | shown on the game only; the library records acquisitions |
| Returns | not planned | per-game refund: an order item gets `refundedAt`, the order becomes partially or fully refunded |
| DAO implementations | `com.mist.dao.mongo` | `com.mist.dao.hibernate` |
| Transactions | `ClientSession.withTransaction` | Hibernate transactions, which the extension maps to MongoDB transactions |
| Dates / embedded data | not fixed | `java.time.Instant` only; embedded documents are `@Embeddable @Struct` classes |
| Catalog search | text index | the driver finds the ids with `$regex` on a quoted pattern, then Hibernate loads the games; no text index |
| Config | `mist.properties` read by `AppConfig` | same, plus system-property and environment-variable overrides |
| Brand images | splash, empty and offline SVGs converted to PNG | splash, offline and empty library rebuilt in FXML; only the launcher icon becomes PNG |
| Styling | `mist-theme.css` | TailwindFX utilities + `mist-theme.css` brand layer |
| Packaging | `javafx-maven-plugin` jlink + jpackage | jpackage app image of a classpath app (`-Ppackage`), because jlink cannot take Hibernate's automatic modules; no installer, so no WiX |
| Tests | JUnit for services and DAOs, TestFX for flows | TestFX tests of every screen on the real services over in-memory DAOs; no service or DAO unit tests |
| Exceptions | hierarchy (type unspecified) | same names, checked |

## Findings

### TailwindFX 0.1.2: how utilities reach the screen

`TwFXML.process` only compiles arbitrary values (`w-[350px]`, `gap-[24px]`) into inline styles. Static utilities
(`p-24`, `pl-24`, `text-base`, `font-bold`) stay as bare style classes, and TailwindFX's runtime stylesheet only
defines variables, so on their own they do nothing. `SplashViewTest` caught this: padding stayed at 0 and the
wordmark stayed Regular.

The fix is the second path TailwindFX offers: `tailwindfx-maven-plugin` scans `src/main` during `process-resources`
and writes `css/tailwindfx-generated.css` with a rule for every static utility in use, and `Styles.install` loads it
with `TwInstall.installGenerated`. Both paths are now in use: the generated stylesheet for static utilities, inline
styles for arbitrary values.

Two more limits found on the way:

- **Spacing.** `hbox-gap-*` and `vbox-gap-*` do not exist, and a static `gap-6` compiles to `-fx-hgap`/`-fx-vgap`,
  which `HBox` and `VBox` ignore. `gap-[24px]` works: TailwindFX applies it with `setSpacing`.
- **Font size.** `text-[104px]` compiles to `-fx-text-fill: 104px`, a color, and named sizes stop at `text-8xl`
  (96 px) and `text-9xl` (128 px). The kit's 104 px wordmark therefore uses the brand class `mist-wordmark`;
  `font-bold` stays a TailwindFX utility.

The build also prints a Maven warning about JavaFX 17 POMs: the TailwindFX plugin depends on JavaFX 17, whose
parent POM has duplicate profiles. It only affects the plugin's own build classpath; the app runs on JavaFX 25.0.4
only, which the enforcer checks.

### Hibernate writes null fields

`HibernateMongoSmokeIT` shows that the MongoDB extension stores a `null` Java field as an explicit BSON `null`, not as
a missing field. The week 2 `$jsonSchema` validators must therefore allow null on every optional field:
`bsonType: ["<type>", "null"]`.

The same test confirms the other types the validators will check: `_id` is an `objectId`, `BigDecimal` a `decimal`,
`Instant` a `date`, enums are strings, `List<ObjectId>` is an array of `objectId`, an `int` in an embedded document is
an `int`, and `@Struct` embeddables are subdocuments.

### The model on MongoDB (17 Nov 2026)

A one-off check (not kept in the repository) booted Hibernate with every model class on a `mongo:7.0` replica set,
stored one document of each kind, and ran the queries the DAOs will need. It confirmed:

- `SINGLE_TABLE` users work, and polymorphic queries return the right subclass.
- Java records work as `@Embeddable @Struct` values, alone and in lists, and enums inside them are stored as strings.
- Copy-on-write lists (replacing the list on every change) are written on update.
- `byte[]` is stored as `binData`.
- A purchase across `orders`, `users` and `games` commits as a whole, and nothing is left behind when it fails halfway.
- **Hibernate must be told to store enums as strings.** Without `@Enumerated(EnumType.STRING)` it stores their ordinal
  number; every enum field and record component in the model carries the annotation.
- **Under `SINGLE_TABLE`, other roles' fields are omitted, not written as `null`**: a developer document has no
  `nickname` key. A field of the document's own role without a value is still written as `null`.
- **`LIKE` needs a literal pattern.** `like :param` is not supported, and `lower(name) like '…'` fails inside the
  extension. `ilike '<literal>'` and the driver's `$regex` both work. Catalog search should get ids with the driver's
  `Filters.regex` on a quoted pattern, then load the games with `from Game where id in :ids`.

### Local MongoDB service and the Docker port

On a machine where MongoDB is also installed as a Windows service, that service listens on `127.0.0.1:27017` and
hides the `docker compose` container from `localhost`. Either stop the service before `docker compose up -d`, or
configure the service itself as replica set `rs0` (see the README).
