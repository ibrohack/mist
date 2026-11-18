package com.mist.dao;

/**
 * Bundle keys of the messages the data access layer reports. Each one exists in {@code messages_es.properties} and
 * {@code messages_en.properties}.
 */
public final class DaoKeys {

    /** The database could not complete an operation, for example because it cannot be reached. */
    public static final String FAILURE = "dao.failure";
    /** A write broke a unique index. */
    public static final String DUPLICATE = "dao.duplicate";

    /** Not instantiable: a holder of bundle keys. */
    private DaoKeys() {
    }
}
