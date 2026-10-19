package com.mist.config;

import com.mongodb.hibernate.cfg.spi.MongoConfigurationContributor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import java.util.Collection;

/**
 * Builds the Hibernate {@link SessionFactory} on MIST's shared {@code MongoClient}, through the MongoDB extension for
 * Hibernate ORM.
 *
 * <p>Every Hibernate setting lives here, in code. Hibernate never creates collections or indexes
 * ({@code hbm2ddl.auto=none}); {@link SchemaSetup} does that through the driver.</p>
 */
public final class HibernateBootstrap {

    private static final String MONGO_DIALECT = "MongoDB";
    /** Required by the extension; {@code MQL} is its only supported value. */
    private static final String NULL_SEMANTICS = "com.mongodb.hibernate.semantics.nulls";

    private HibernateBootstrap() {
    }

    /**
     * Builds a session factory that maps the given classes and runs on the connection's client and database.
     *
     * <p>Close the session factory before the connection: the extension uses the client but does not own it.</p>
     *
     * @param connection        the shared client and the database name
     * @param annotatedClasses  the entity and embeddable classes to map
     * @return a session factory ready for transactions
     */
    public static SessionFactory buildSessionFactory(MongoConnection connection,
                                                     Collection<Class<?>> annotatedClasses) {
        MongoConfigurationContributor mongoConfiguration = configurator -> configurator
                .mongoClient(connection.client())
                .databaseName(connection.databaseName());
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, MONGO_DIALECT)
                .applySetting(NULL_SEMANTICS, "MQL")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "none")
                .addService(MongoConfigurationContributor.class, mongoConfiguration)
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            annotatedClasses.forEach(sources::addAnnotatedClass);
            return sources.buildMetadata().buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }
}
