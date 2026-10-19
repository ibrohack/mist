package com.mist.config;

import com.mist.model.GameStatus;
import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Struct;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the production database path (settings, shared {@code MongoClient}, Hibernate {@code SessionFactory})
 * against a real MongoDB replica set, and pins the BSON types that the {@code $jsonSchema} validators will expect.
 *
 * <p>Needs Docker. Without it the test fails instead of being skipped.</p>
 */
@Testcontainers
class HibernateMongoSmokeIT {

    private static final String DATABASE = "mist_smoke";
    private static final String COLLECTION = "smoke_games";

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0").withReplicaSet();

    @Test
    void persistsAndReadsBackInATransaction() {
        try (MongoConnection connection = new MongoConnection(config());
             SessionFactory sessionFactory = buildSessionFactory(connection)) {
            dropSmokeCollection(connection);

            ObjectId id = sessionFactory.fromTransaction(session -> {
                SmokeGame game = sample();
                session.persist(game);
                return game.getId();
            });
            SmokeGame back = sessionFactory.fromTransaction(session -> session
                    .createSelectionQuery("from SmokeGame where title = :t", SmokeGame.class)
                    .setParameter("t", "Hollow Peak")
                    .getSingleResult());

            assertEquals(id, back.getId());
            assertEquals(new BigDecimal("19.99"), back.getPrice());
            assertEquals(GameStatus.PUBLISHED, back.getStatus());
            assertEquals(3, back.getStats().getSales());
        }
    }

    @Test
    void storesTheBsonTypesValidatorsWillExpect() {
        try (MongoConnection connection = new MongoConnection(config());
             SessionFactory sessionFactory = buildSessionFactory(connection)) {
            dropSmokeCollection(connection);

            sessionFactory.inTransaction(session -> session.persist(sample()));
            BsonDocument raw = connection.client().getDatabase(DATABASE)
                    .getCollection(COLLECTION, BsonDocument.class)
                    .find().first();

            assertTrue(raw.get("_id").isObjectId(), () -> "_id: " + raw.get("_id"));
            assertTrue(raw.get("price").isDecimal128(), () -> "price: " + raw.get("price"));
            assertTrue(raw.get("releasedAt").isDateTime(), () -> "releasedAt: " + raw.get("releasedAt"));
            assertEquals("PUBLISHED", raw.getString("status").getValue());
            assertTrue(raw.getArray("developerIds").get(0).isObjectId(), () -> "developerIds: " + raw.get("developerIds"));
            assertTrue(raw.getDocument("stats").get("sales").isInt32(), () -> "stats: " + raw.get("stats"));
            assertTrue(raw.containsKey("subtitle") && raw.get("subtitle").isNull(), () -> "document: " + raw.toJson());
        }
    }

    private static AppConfig config() {
        Properties properties = new Properties();
        properties.setProperty("mist.mongo.uri", MONGO.getReplicaSetUrl());
        properties.setProperty("mist.mongo.database", DATABASE);
        return AppConfig.from(properties);
    }

    private static SessionFactory buildSessionFactory(MongoConnection connection) {
        return HibernateBootstrap.buildSessionFactory(connection, List.of(SmokeGame.class, SmokeStats.class));
    }

    private static void dropSmokeCollection(MongoConnection connection) {
        connection.client().getDatabase(DATABASE).getCollection(COLLECTION).drop();
    }

    private static SmokeGame sample() {
        SmokeGame game = new SmokeGame();
        game.title = "Hollow Peak";
        game.subtitle = null;
        game.price = new BigDecimal("19.99");
        game.releasedAt = Instant.parse("2026-11-02T18:41:07Z");
        game.status = GameStatus.PUBLISHED;
        game.developerIds = List.of(new ObjectId());
        game.stats = new SmokeStats();
        game.stats.sales = 3;
        game.stats.revenue = new BigDecimal("59.97");
        return game;
    }

    /** Test-only entity shaped like a future {@code Game}: every BSON type the real model will use. */
    @Entity(name = "SmokeGame")
    @Table(name = COLLECTION)
    static class SmokeGame {

        @Id
        @ObjectIdGenerator
        ObjectId id;
        String title;
        String subtitle;
        BigDecimal price;
        Instant releasedAt;
        @Enumerated(EnumType.STRING)
        GameStatus status;
        List<ObjectId> developerIds;
        SmokeStats stats;

        ObjectId getId() {
            return id;
        }

        BigDecimal getPrice() {
            return price;
        }

        GameStatus getStatus() {
            return status;
        }

        SmokeStats getStats() {
            return stats;
        }
    }

    /** Test-only embedded document, mapped as a BSON subdocument. */
    @Embeddable
    @Struct(name = "SmokeStats")
    static class SmokeStats {

        int sales;
        BigDecimal revenue;

        int getSales() {
            return sales;
        }
    }
}
