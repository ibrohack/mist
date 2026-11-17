package com.mist.dao;

import com.mist.exception.DataAccessException;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the {@code users} collection, which holds players, developers and admins in one collection.
 *
 * <p>Lookups return the right subclass ({@code Player}, {@code Developer} or {@code Admin}), read from the
 * {@code role} field.</p>
 */
public interface UserDao {

    /**
     * Finds an account by id.
     *
     * @param id the account id
     * @return the account, or empty if there is none
     * @throws DataAccessException if the database fails
     */
    Optional<User> findById(ObjectId id) throws DataAccessException;

    /**
     * Finds an account by its login.
     *
     * @param email the email address, already normalized with {@code User.normalizeEmail}
     * @return the account, or empty if no account uses that address
     * @throws DataAccessException if the database fails
     */
    Optional<User> findByEmail(String email) throws DataAccessException;

    /**
     * Finds several accounts at once, for example the authors of a page of reviews.
     *
     * @param ids the account ids
     * @return the accounts found, in no particular order; ids with no account are skipped
     * @throws DataAccessException if the database fails
     */
    List<User> findByIds(Collection<ObjectId> ids) throws DataAccessException;

    /**
     * Tells whether an account already uses an email address.
     *
     * @param email the normalized email address
     * @return {@code true} if it is taken
     * @throws DataAccessException if the database fails
     */
    boolean existsByEmail(String email) throws DataAccessException;

    /**
     * Tells whether a player already uses a nickname.
     *
     * @param nickname the nickname
     * @return {@code true} if it is taken
     * @throws DataAccessException if the database fails
     */
    boolean existsByNickname(String nickname) throws DataAccessException;

    /**
     * Tells whether a developer already uses a studio name.
     *
     * @param studioName the studio name
     * @return {@code true} if it is taken
     * @throws DataAccessException if the database fails
     */
    boolean existsByStudioName(String studioName) throws DataAccessException;

    /**
     * Inserts a new account, or writes the changes of an existing one.
     *
     * @param user the account; a new one has no id yet
     * @param <T>  the kind of account
     * @return the saved account, with its id; use it instead of the argument afterwards
     * @throws com.mist.exception.DuplicateEntityException if the email, nickname or studio name is taken
     * @throws DataAccessException                        if the database fails
     */
    <T extends User> T save(T user) throws DataAccessException;
}
