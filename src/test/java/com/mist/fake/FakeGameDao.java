package com.mist.fake;

import com.mist.dao.GameDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link GameDao} on the in-memory database, sorting and filtering as the real queries do.
 */
public final class FakeGameDao implements GameDao {

    /** The shared in-memory database. */
    private final FakeDatabase db;

    /**
     * Creates the DAO.
     *
     * @param db the shared in-memory database
     */
    public FakeGameDao(FakeDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<Game> findById(ObjectId id) throws DataAccessException {
        db.check();
        return Optional.ofNullable(db.games.get(id));
    }

    @Override
    public List<Game> findByIds(Collection<ObjectId> ids) throws DataAccessException {
        db.check();
        return ids.stream().distinct().map(db.games::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<Game> findByDeveloper(ObjectId developerId) throws DataAccessException {
        db.check();
        return db.games.values().stream()
                .filter(game -> game.getDeveloperId().equals(developerId))
                .sorted(Comparator.comparing(Game::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<Game> findNewestPublished(int limit) throws DataAccessException {
        db.check();
        return published()
                .sorted(Comparator.comparing(Game::getReleaseDate, Comparator.nullsFirst(Instant::compareTo)).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Game> findTopSellers(int limit) throws DataAccessException {
        db.check();
        return published()
                .sorted(Comparator.comparingInt((Game game) -> game.getStats().unitsSold()).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Game> findFreePublished(int limit) throws DataAccessException {
        db.check();
        return published()
                .filter(Game::isFree)
                .sorted(Comparator.comparing(Game::getReleaseDate, Comparator.nullsFirst(Instant::compareTo)).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Game> searchPublished(String text, int limit) throws DataAccessException {
        String query = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return List.of();
        }
        db.check();
        return published()
                .filter(game -> game.getName().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Game::getName))
                .limit(limit)
                .toList();
    }

    @Override
    public Game save(Game game) throws DataAccessException {
        db.check();
        db.games.put(FakeDatabase.assignId(game), game);
        return game;
    }

    /**
     * Streams the published games.
     *
     * @return the games on sale
     */
    private java.util.stream.Stream<Game> published() {
        return db.games.values().stream().filter(game -> game.getStatus() == GameStatus.PUBLISHED);
    }
}
