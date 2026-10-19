package com.mist.config;

/**
 * Creates the MongoDB collections with their {@code $jsonSchema} validators, plus the text and multikey indexes.
 *
 * <p>All of this goes through the driver on the shared {@code MongoClient}, because the Hibernate MongoDB extension
 * cannot create validators or these index types.</p>
 */
public class SchemaSetup {
}
