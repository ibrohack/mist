package com.mist.dao.hibernate;

import com.mist.config.MongoConnection;
import com.mist.dao.GameDao;
import com.mist.exception.DataAccessException;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.bson.types.ObjectId;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link GameDao} implemented with Hibernate ORM on MongoDB.
 *
 * <p>Catalog search finds the matching ids with the driver, because the extension only accepts {@code LIKE} with a
 * literal pattern, and then loads the games through Hibernate.</p>
 */
public class HibernateGameDao extends HibernateDao implements GameDao {

    /** The collection the catalog search queries directly. */
    private static final String COLLECTION = "games";
    /** The status field. */
    private static final String STATUS = "status";

    /** The client the catalog search uses. */
    private final MongoConnection connection;

    /**
     * Creates the DAO.
     *
     * @param transactions the transaction runner whose sessions it uses
     * @param connection   the shared client, for the catalog search
     */
    public HibernateGameDao(HibernateTransactions transactions, MongoConnection connection) {
        super(transactions, COLLECTION);
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public Optional<Game> findById(ObjectId id) throws DataAccessException {
        Objects.requireNonNull(id, "id");
        return execute("FIND", "_id=" + id, session -> Optional.ofNullable(session.find(Game.class, id)));
    }

    @Override
    public List<Game> findByIds(Collection<ObjectId> ids) throws DataAccessException {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ObjectId> distinct = ids.stream().distinct().toList();
        return execute("FIND", "_id in " + ids(distinct), session -> inOrder(distinct, loadAll(session, distinct)));
    }

    @Override
    public List<Game> findByDeveloper(ObjectId developerId) throws DataAccessException {
        Objects.requireNonNull(developerId, "developerId");
        return execute("FIND", "developerId=" + developerId, session -> session
                .createSelectionQuery("from Game where developerId = :developerId order by createdAt desc", Game.class)
                .setParameter("developerId", developerId)
                .getResultList());
    }

    @Override
    public List<Game> findNewestPublished(int limit) throws DataAccessException {
        return execute("FIND", "status=PUBLISHED, newest, limit=" + limit, session -> session
                .createSelectionQuery("from Game where status = :status order by releaseDate desc", Game.class)
                .setParameter(STATUS, GameStatus.PUBLISHED)
                .setMaxResults(positive(limit))
                .getResultList());
    }

    @Override
    public List<Game> findTopSellers(int limit) throws DataAccessException {
        return execute("FIND", "status=PUBLISHED, top sellers, limit=" + limit, session -> session
                .createSelectionQuery("from Game where status = :status order by stats.unitsSold desc", Game.class)
                .setParameter(STATUS, GameStatus.PUBLISHED)
                .setMaxResults(positive(limit))
                .getResultList());
    }

    @Override
    public List<Game> findFreePublished(int limit) throws DataAccessException {
        // MongoDB compares decimals by value, so the stored 0.00 matches zero.
        return execute("FIND", "status=PUBLISHED, price=0, limit=" + limit, session -> session
                .createSelectionQuery("from Game where status = :status and price = :free order by releaseDate desc",
                        Game.class)
                .setParameter(STATUS, GameStatus.PUBLISHED)
                .setParameter("free", BigDecimal.ZERO)
                .setMaxResults(positive(limit))
                .getResultList());
    }

    @Override
    public List<Game> searchPublished(String text, int limit) throws DataAccessException {
        String query = text == null ? "" : text.strip();
        if (query.isEmpty()) {
            return List.of();
        }
        return execute("SEARCH", "name~\"" + query + "\", limit=" + limit, session -> {
            List<ObjectId> ids = connection.client().getDatabase(connection.databaseName())
                    .getCollection(COLLECTION)
                    .find(Filters.and(
                            Filters.eq(STATUS, GameStatus.PUBLISHED.name()),
                            Filters.regex("name", Pattern.quote(query), "i")))
                    .projection(Projections.include("_id"))
                    .sort(Sorts.ascending("name"))
                    .limit(positive(limit))
                    .map(document -> document.getObjectId("_id"))
                    .into(new ArrayList<>());
            return ids.isEmpty() ? List.<Game>of() : inOrder(ids, loadAll(session, ids));
        });
    }

    @Override
    public Game save(Game game) throws DataAccessException {
        Objects.requireNonNull(game, "game");
        boolean isNew = game.getId() == null;
        String details = isNew ? "developerId=" + game.getDeveloperId() + ", name=" + game.getName()
                : "_id=" + game.getId() + ", status=" + game.getStatus();
        return execute(isNew ? "INSERT" : "UPDATE", details, session -> persistOrMerge(session, game, isNew));
    }

    /**
     * Loads games by id.
     *
     * @param session the session
     * @param ids     the ids
     * @return the games, in any order
     */
    private static List<Game> loadAll(Session session, List<ObjectId> ids) {
        return session.createSelectionQuery("from Game where id in :ids", Game.class)
                .setParameterList("ids", ids)
                .getResultList();
    }

    /**
     * Puts loaded games in the order of their ids.
     *
     * @param ids   the ids, in the wanted order
     * @param games the games
     * @return the games in the order of {@code ids}
     */
    private static List<Game> inOrder(List<ObjectId> ids, List<Game> games) {
        Map<ObjectId, Game> byId = games.stream().collect(Collectors.toMap(Game::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /**
     * Checks a list size.
     *
     * @param limit the size
     * @return the same size
     * @throws IllegalArgumentException if it is not positive
     */
    private static int positive(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        return limit;
    }
}
