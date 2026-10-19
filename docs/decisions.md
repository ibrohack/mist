# MIST decision log

Decisions taken while building the project scaffold (week 2, 19 Oct 2026), and how they change the preliminary plan
of 12 Oct 2026.

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

## Changes to the preliminary plan

| Topic | Preliminary plan | Now |
| --- | --- | --- |
| Java | 21 + JavaFX 21 | 25 + JavaFX 25.0.4 |
| Mapping | `mongodb-driver-sync` + `PojoCodecProvider` | Hibernate ORM 7.4.7 + `mongodb-hibernate` 1.0.0, on one shared `MongoClient`. The driver is used directly only for validators, special indexes and the connection check |
| Studios | `studios` collection, `Studio` | `developers` collection, `Developer` entity, `DeveloperDao`; references become `developerId` |
| DAO implementations | `com.mist.dao.mongo` | `com.mist.dao.hibernate` |
| Transactions | `ClientSession.withTransaction` | Hibernate transactions, which the extension maps to MongoDB transactions |
| Dates / embedded data | not fixed | `java.time.Instant` only; embedded documents are `@Embeddable @Struct` classes |
| Catalog search | text index | HQL `like` (becomes `$regex`); a text index only if a native MQL query needs it |
| Config | `mist.properties` read by `AppConfig` | same, plus system-property and environment-variable overrides |
| Brand images | splash, empty and offline SVGs converted to PNG | splash rebuilt in FXML (empty and offline later, the same way); only the launcher icon becomes PNG |
| Styling | `mist-theme.css` | TailwindFX utilities + `mist-theme.css` brand layer |
| Packaging | `javafx-maven-plugin` jlink + jpackage | jpackage on a classpath app, because jlink cannot take Hibernate's automatic modules |
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

### Local MongoDB service and the Docker port

On a machine where MongoDB is also installed as a Windows service, that service listens on `127.0.0.1:27017` and
hides the `docker compose` container from `localhost`. Either stop the service before `docker compose up -d`, or
configure the service itself as replica set `rs0` (see the README).
