package com.mist.fake;

import com.mist.dao.DaoKeys;
import com.mist.exception.DataAccessException;
import com.mist.model.Game;
import com.mist.model.Media;
import com.mist.model.Order;
import com.mist.model.Review;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The in-memory database behind the fake DAOs of the UI tests.
 *
 * <p>It holds the five collections as maps and lets a test break the backend: {@link #failNextCall()} makes the next
 * DAO call throw a {@link DataAccessException}, as a dropped connection would, and {@link #goOffline()} makes every
 * call and every connection attempt fail, as a stopped MongoDB would. Entities are stored as they are, so a service
 * that changes one and fails before saving leaves the change behind: these fakes do not roll back.</p>
 */
public final class FakeDatabase {

    /** The users collection. */
    final Map<ObjectId, User> users = new LinkedHashMap<>();
    /** The games collection. */
    final Map<ObjectId, Game> games = new LinkedHashMap<>();
    /** The orders collection. */
    final Map<ObjectId, Order> orders = new LinkedHashMap<>();
    /** The reviews collection. */
    final Map<ObjectId, Review> reviews = new LinkedHashMap<>();
    /** The media collection. */
    final Map<ObjectId, Media> media = new LinkedHashMap<>();

    /** The failure the next call throws, or {@code null}. */
    private DataAccessException nextFailure;
    /** Whether every call fails. */
    private boolean offline;
    /** How many DAO calls reached the database. */
    private int calls;

    /** Makes the next DAO call fail with {@code dao.failure}, as a dropped connection would. */
    public void failNextCall() {
        failNextCall(new DataAccessException(DaoKeys.FAILURE));
    }

    /**
     * Makes the next DAO call fail with the given exception.
     *
     * @param failure what the call throws
     */
    public void failNextCall(DataAccessException failure) {
        this.nextFailure = failure;
    }

    /** Makes every DAO call and connection attempt fail, as a stopped MongoDB would. */
    public void goOffline() {
        offline = true;
    }

    /** Lets calls succeed again. */
    public void goOnline() {
        offline = false;
    }

    /**
     * Tells whether the database is reachable.
     *
     * @return {@code false} after {@link #goOffline()}
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * Returns how many DAO calls reached the database.
     *
     * @return the number of calls
     */
    public int calls() {
        return calls;
    }

    /**
     * Returns a user by email, for test assertions.
     *
     * @param email the normalized email
     * @return the user, or {@code null}
     */
    public User userByEmail(String email) {
        return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst().orElse(null);
    }

    /**
     * Returns a game by name, for test assertions.
     *
     * @param name the game's name
     * @return the game, or {@code null}
     */
    public Game gameByName(String name) {
        return games.values().stream().filter(game -> game.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Called by every fake DAO method first: counts the call and throws the planned failure.
     *
     * @throws DataAccessException when offline or when a failure was planned
     */
    void check() throws DataAccessException {
        calls++;
        if (offline) {
            throw new DataAccessException(DaoKeys.FAILURE);
        }
        if (nextFailure != null) {
            DataAccessException failure = nextFailure;
            nextFailure = null;
            throw failure;
        }
    }

    /**
     * Gives an entity a new id if it has none, as Hibernate does on persist.
     *
     * @param entity the entity
     * @param <T>    its type
     * @return its id
     */
    static <T> ObjectId assignId(T entity) {
        try {
            Field id = idField(entity.getClass());
            ObjectId current = (ObjectId) id.get(entity);
            if (current == null) {
                current = new ObjectId();
                id.set(entity, current);
            }
            return current;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set the id of " + entity, e);
        }
    }

    /**
     * Finds the {@code id} field of an entity class or its superclasses.
     *
     * @param type the entity class
     * @return the accessible field
     */
    private static Field idField(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField("id");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // Look in the superclass.
            }
        }
        throw new IllegalStateException(type + " has no id field");
    }
}
