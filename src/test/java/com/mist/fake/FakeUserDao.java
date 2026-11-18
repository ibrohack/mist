package com.mist.fake;

import com.mist.dao.DaoKeys;
import com.mist.dao.UserDao;
import com.mist.exception.DataAccessException;
import com.mist.exception.DuplicateEntityException;
import com.mist.model.Developer;
import com.mist.model.Player;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link UserDao} on the in-memory database, with the unique email, nickname and studio name of the real indexes.
 */
public final class FakeUserDao implements UserDao {

    /** The shared in-memory database. */
    private final FakeDatabase db;

    /**
     * Creates the DAO.
     *
     * @param db the shared in-memory database
     */
    public FakeUserDao(FakeDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<User> findById(ObjectId id) throws DataAccessException {
        db.check();
        return Optional.ofNullable(db.users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) throws DataAccessException {
        db.check();
        return db.users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
    }

    @Override
    public List<User> findByIds(Collection<ObjectId> ids) throws DataAccessException {
        db.check();
        return ids.stream().distinct().map(db.users::get).filter(Objects::nonNull).toList();
    }

    @Override
    public boolean existsByEmail(String email) throws DataAccessException {
        db.check();
        return db.users.values().stream().anyMatch(user -> user.getEmail().equals(email));
    }

    @Override
    public boolean existsByNickname(String nickname) throws DataAccessException {
        db.check();
        return db.users.values().stream()
                .anyMatch(user -> user instanceof Player player && nickname.equals(player.getNickname()));
    }

    @Override
    public boolean existsByStudioName(String studioName) throws DataAccessException {
        db.check();
        return db.users.values().stream()
                .anyMatch(user -> user instanceof Developer developer && studioName.equals(developer.getStudioName()));
    }

    @Override
    public <T extends User> T save(T user) throws DataAccessException {
        db.check();
        ObjectId id = FakeDatabase.assignId(user);
        boolean duplicate = db.users.values().stream()
                .filter(other -> !other.getId().equals(id))
                .anyMatch(other -> other.getEmail().equals(user.getEmail())
                        || sameNickname(other, user) || sameStudio(other, user));
        if (duplicate) {
            throw new DuplicateEntityException(DaoKeys.DUPLICATE);
        }
        db.users.put(id, user);
        return user;
    }

    /**
     * Tells whether two players share a nickname.
     *
     * @param a one user
     * @param b another user
     * @return {@code true} if both are players with the same nickname
     */
    private static boolean sameNickname(User a, User b) {
        return a instanceof Player p && b instanceof Player q && p.getNickname() != null
                && p.getNickname().equals(q.getNickname());
    }

    /**
     * Tells whether two developers share a studio name.
     *
     * @param a one user
     * @param b another user
     * @return {@code true} if both are developers with the same studio name
     */
    private static boolean sameStudio(User a, User b) {
        return a instanceof Developer d && b instanceof Developer e && d.getStudioName().equals(e.getStudioName());
    }
}
