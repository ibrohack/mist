package com.mist.config;

import com.mist.model.AccountStatus;
import com.mist.model.Achievement;
import com.mist.model.Address;
import com.mist.model.CardBrand;
import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.Language;
import com.mist.model.Media;
import com.mist.model.Order;
import com.mist.model.OrderStatus;
import com.mist.model.PaymentCard;
import com.mist.model.Pegi;
import com.mist.model.Review;
import com.mist.model.Role;
import com.mist.model.SystemRequirements;
import com.mist.model.Theme;
import com.mist.model.User;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates the MongoDB collections with their {@code $jsonSchema} validators, plus the indexes the queries and the
 * unique rules need.
 *
 * <p>All of this goes through the driver on the shared {@code MongoClient}, because the Hibernate MongoDB extension
 * cannot create validators or these index types. {@link #apply(MongoConnection)} is safe to run on every start: it
 * creates what is missing and brings existing validators up to date.</p>
 *
 * <p>The validators repeat the model's limits, taken from its constants. Optional fields allow {@code null}, because
 * Hibernate writes a missing value as {@code null}. In {@code users}, fields of other roles are absent, so a
 * {@code oneOf} on {@code role} requires each role's own fields.</p>
 */
public final class SchemaSetup {

    /** Server error code when a collection already exists, for example because another app instance created it. */
    private static final int NAMESPACE_EXISTS = 48;

    private static final String STRING = "string";
    private static final String OBJECT_ID = "objectId";
    private static final String DATE = "date";
    private static final String DECIMAL = "decimal";
    private static final String INT = "int";
    private static final String OBJECT = "object";
    private static final String ARRAY = "array";
    private static final String NULL = "null";

    private SchemaSetup() {
    }

    /**
     * Creates or updates the five collections, their validators and their indexes.
     *
     * @param connection the shared client and the database name
     * @throws com.mongodb.MongoException if the server cannot be reached or rejects a command
     */
    public static void apply(MongoConnection connection) {
        MongoDatabase database = connection.client().getDatabase(connection.databaseName());
        Set<String> existing = database.listCollectionNames().into(new HashSet<>());

        validate(database, existing, "users", users());
        validate(database, existing, "games", games());
        validate(database, existing, "orders", orders());
        validate(database, existing, "reviews", reviews());
        validate(database, existing, "media", media());

        MongoCollection<Document> users = database.getCollection("users");
        users.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        users.createIndex(Indexes.ascending("nickname"), new IndexOptions().unique(true)
                .partialFilterExpression(Filters.type("nickname", BsonType.STRING)));
        users.createIndex(Indexes.ascending("studioName"), new IndexOptions().unique(true)
                .partialFilterExpression(Filters.type("studioName", BsonType.STRING)));

        MongoCollection<Document> games = database.getCollection("games");
        games.createIndex(Indexes.ascending("status", "releaseDate"));
        games.createIndex(Indexes.ascending("status", "stats.unitsSold"));
        games.createIndex(Indexes.ascending("developerId"));

        database.getCollection("orders").createIndex(Indexes.ascending("playerId", "createdAt"));

        MongoCollection<Document> reviews = database.getCollection("reviews");
        reviews.createIndex(Indexes.ascending("gameId", "playerId"), new IndexOptions().unique(true));
        reviews.createIndex(Indexes.ascending("gameId", "createdAt"));

        database.getCollection("media").createIndex(Indexes.ascending("ownerId"));
    }

    private static void validate(MongoDatabase database, Set<String> existing, String name, Document jsonSchema) {
        Document validator = new Document("$jsonSchema", jsonSchema);
        if (!existing.contains(name)) {
            try {
                database.createCollection(name, new CreateCollectionOptions().validationOptions(new ValidationOptions()
                        .validator(validator)
                        .validationLevel(ValidationLevel.STRICT)
                        .validationAction(ValidationAction.ERROR)));
                return;
            } catch (MongoCommandException e) {
                if (e.getErrorCode() != NAMESPACE_EXISTS) {
                    throw e;
                }
            }
        }
        database.runCommand(new Document("collMod", name)
                .append("validator", validator)
                .append("validationLevel", "strict")
                .append("validationAction", "error"));
    }

    // ----------------------------------------------------------------------------------------------------- users

    private static Document users() {
        Document properties = new Document()
                .append("_id", type(OBJECT_ID))
                .append("role", oneOfEnum(Role.class))
                .append("firstName", text(1, User.MAX_FIRST_NAME_LENGTH))
                .append("lastName", text(1, User.MAX_LAST_NAME_LENGTH))
                .append("email", text(3, User.MAX_EMAIL_LENGTH))
                .append("passwordHash", type(STRING).append("pattern", "^\\$argon2id\\$"))
                .append("phone", object(true, new Document()
                        .append("countryCode", type(STRING))
                        .append("number", type(STRING)), "countryCode", "number"))
                .append("address", address(true))
                .append("avatarId", type(OBJECT_ID, NULL))
                .append("createdAt", type(DATE))
                .append("status", oneOfEnum(AccountStatus.class))
                .append("deactivatedAt", type(DATE, NULL))
                .append("preferences", object(false, new Document()
                        .append("language", oneOfEnum(Language.class))
                        .append("theme", oneOfEnum(Theme.class)), "language", "theme"))
                .append("nickname", text(3, 24))
                .append("card", card())
                .append("library", array(object(false, new Document()
                        .append("gameId", type(OBJECT_ID))
                        .append("orderId", type(OBJECT_ID))
                        .append("acquiredAt", type(DATE)), "gameId", "orderId", "acquiredAt"), null))
                .append("cartGameIds", array(type(OBJECT_ID), null))
                .append("wishlistGameIds", array(type(OBJECT_ID), null))
                .append("studioName", text(1, Developer.MAX_STUDIO_NAME_LENGTH))
                .append("bankAccount", type(STRING).append("pattern", "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$"))
                .append("gameIds", array(type(OBJECT_ID), null))
                .append("totalRevenue", type(DECIMAL).append("minimum", decimal(BigDecimal.ZERO)));

        Document player = new Document("properties", new Document()
                .append("role", new Document("enum", List.of(Role.PLAYER.name())))
                .append("address", address(false)))
                .append("required", List.of("address", "nickname", "card", "library", "cartGameIds",
                        "wishlistGameIds"));
        Document developer = new Document("properties", new Document()
                .append("role", new Document("enum", List.of(Role.DEVELOPER.name())))
                .append("address", address(false)))
                .append("required", List.of("address", "studioName", "bankAccount", "gameIds", "totalRevenue"));
        Document admin = new Document("properties", new Document()
                .append("role", new Document("enum", List.of(Role.ADMIN.name()))));

        return new Document("bsonType", OBJECT)
                .append("required", List.of("_id", "role", "firstName", "lastName", "email", "passwordHash",
                        "createdAt", "status", "preferences"))
                .append("properties", properties)
                .append("oneOf", List.of(player, developer, admin));
    }

    private static Document address(boolean nullable) {
        return object(nullable, new Document()
                .append("street", text(1, Address.MAX_STREET_LENGTH))
                .append("details", optionalText(Address.MAX_DETAILS_LENGTH))
                .append("postalCode", type(STRING))
                .append("city", text(1, Address.MAX_CITY_LENGTH))
                .append("province", optionalText(Address.MAX_PROVINCE_LENGTH))
                .append("country", type(STRING).append("pattern", "^[A-Z]{2}$")),
                "street", "postalCode", "city", "country");
    }

    private static Document card() {
        return object(false, new Document()
                .append("holderName", text(1, PaymentCard.MAX_HOLDER_LENGTH))
                .append("brand", oneOfEnum(CardBrand.class))
                .append("last4", type(STRING).append("pattern", "^[0-9]{4}$"))
                .append("expiryMonth", type(INT).append("minimum", 1).append("maximum", 12))
                .append("expiryYear", type(INT)
                        .append("minimum", PaymentCard.MIN_EXPIRY_YEAR)
                        .append("maximum", PaymentCard.MAX_EXPIRY_YEAR)),
                "holderName", "brand", "last4", "expiryMonth", "expiryYear");
    }

    // ----------------------------------------------------------------------------------------------------- games

    private static Document games() {
        List<Object> pegi = new ArrayList<>(enumNames(Pegi.class));
        pegi.add(null);
        Document properties = new Document()
                .append("_id", type(OBJECT_ID))
                .append("name", text(1, Game.MAX_NAME_LENGTH))
                .append("description", optionalText(Game.MAX_DESCRIPTION_LENGTH))
                .append("price", money(Game.MAX_PRICE))
                .append("coverId", type(OBJECT_ID, NULL))
                .append("screenshotIds", array(type(OBJECT_ID), Game.MAX_SCREENSHOTS))
                .append("releaseDate", type(DATE, NULL))
                .append("developerId", type(OBJECT_ID))
                .append("requirements", object(true, new Document()
                        .append("os", text(1, SystemRequirements.MAX_LENGTH))
                        .append("processor", text(1, SystemRequirements.MAX_LENGTH))
                        .append("memory", text(1, SystemRequirements.MAX_LENGTH))
                        .append("graphics", text(1, SystemRequirements.MAX_LENGTH))
                        .append("storage", text(1, SystemRequirements.MAX_LENGTH)),
                        "os", "processor", "memory", "graphics", "storage"))
                .append("pegi", new Document("enum", pegi))
                .append("tags", array(text(1, Game.MAX_TAG_LENGTH), Game.MAX_TAGS))
                .append("languages", array(type(STRING).append("pattern", "^[a-z]{2}$"), Game.MAX_LANGUAGES))
                .append("achievements", array(object(false, new Document()
                        .append("name", text(1, Achievement.MAX_NAME_LENGTH))
                        .append("description", text(1, Achievement.MAX_DESCRIPTION_LENGTH)),
                        "name", "description"), Game.MAX_ACHIEVEMENTS))
                .append("stats", object(false, new Document()
                        .append("unitsSold", type(INT).append("minimum", 0))
                        .append("revenue", money(null))
                        .append("ratingCount", type(INT).append("minimum", 0))
                        .append("ratingSum", type(INT).append("minimum", 0)),
                        "unitsSold", "revenue", "ratingCount", "ratingSum"))
                .append("status", oneOfEnum(GameStatus.class))
                .append("createdAt", type(DATE));
        return new Document("bsonType", OBJECT)
                .append("required", List.of("_id", "name", "price", "developerId", "screenshotIds", "tags",
                        "languages", "achievements", "stats", "status", "createdAt"))
                .append("properties", properties);
    }

    // ---------------------------------------------------------------------------------------------------- orders

    private static Document orders() {
        Document item = object(false, new Document()
                .append("gameId", type(OBJECT_ID))
                .append("name", text(1, Game.MAX_NAME_LENGTH))
                .append("price", money(null))
                .append("refundedAt", type(DATE, NULL)),
                "gameId", "name", "price");
        Document properties = new Document()
                .append("_id", type(OBJECT_ID))
                .append("playerId", type(OBJECT_ID))
                .append("items", array(item, Order.MAX_ITEMS).append("minItems", 1))
                .append("total", money(null))
                .append("status", oneOfEnum(OrderStatus.class))
                .append("paidWith", card())
                .append("createdAt", type(DATE));
        return new Document("bsonType", OBJECT)
                .append("required", List.of("_id", "playerId", "items", "total", "status", "paidWith", "createdAt"))
                .append("properties", properties);
    }

    // --------------------------------------------------------------------------------------------------- reviews

    private static Document reviews() {
        Document properties = new Document()
                .append("_id", type(OBJECT_ID))
                .append("playerId", type(OBJECT_ID))
                .append("gameId", type(OBJECT_ID))
                .append("rating", type(INT).append("minimum", Review.MIN_RATING).append("maximum", Review.MAX_RATING))
                .append("comment", optionalText(Review.MAX_COMMENT_LENGTH))
                .append("createdAt", type(DATE));
        return new Document("bsonType", OBJECT)
                .append("required", List.of("_id", "playerId", "gameId", "rating", "createdAt"))
                .append("properties", properties);
    }

    // ----------------------------------------------------------------------------------------------------- media

    private static Document media() {
        Document properties = new Document()
                .append("_id", type(OBJECT_ID))
                .append("contentType", new Document("enum", List.copyOf(Media.CONTENT_TYPES)))
                .append("data", type("binData"))
                .append("fileName", text(1, Media.MAX_FILE_NAME_LENGTH))
                .append("ownerId", type(OBJECT_ID))
                .append("createdAt", type(DATE));
        return new Document("bsonType", OBJECT)
                .append("required", List.of("_id", "contentType", "data", "fileName", "ownerId", "createdAt"))
                .append("properties", properties);
    }

    // --------------------------------------------------------------------------------------------------- helpers

    private static Document type(String... bsonTypes) {
        return new Document("bsonType", bsonTypes.length == 1 ? bsonTypes[0] : List.of(bsonTypes));
    }

    private static Document text(int minLength, int maxLength) {
        return type(STRING).append("minLength", minLength).append("maxLength", maxLength);
    }

    private static Document optionalText(int maxLength) {
        return type(STRING, NULL).append("maxLength", maxLength);
    }

    private static Document money(BigDecimal maximum) {
        Document money = type(DECIMAL).append("minimum", decimal(BigDecimal.ZERO));
        return maximum == null ? money : money.append("maximum", decimal(maximum));
    }

    private static Decimal128 decimal(BigDecimal value) {
        return new Decimal128(value);
    }

    private static Document object(boolean nullable, Document properties, String... required) {
        Document object = nullable ? type(OBJECT, NULL) : type(OBJECT);
        return object.append("required", List.of(required)).append("properties", properties);
    }

    private static Document array(Document items, Integer maxItems) {
        Document array = type(ARRAY).append("items", items);
        return maxItems == null ? array : array.append("maxItems", maxItems);
    }

    private static <E extends Enum<E>> Document oneOfEnum(Class<E> type) {
        return new Document("enum", enumNames(type));
    }

    private static <E extends Enum<E>> List<String> enumNames(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
    }
}
