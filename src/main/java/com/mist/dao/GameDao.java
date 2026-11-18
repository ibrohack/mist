package com.mist.dao;

import com.mist.exception.DataAccessException;
import com.mist.model.Game;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the {@code games} collection.
 *
 * <p>The store lists only published games; drafts and delisted games are reached by id or through their developer.</p>
 */
public interface GameDao {

    /**
     * Finds a game by id, whatever its status.
     *
     * @param id the game id
     * @return the game, or empty if there is none
     * @throws DataAccessException if the database fails
     */
    Optional<Game> findById(ObjectId id) throws DataAccessException;

    /**
     * Finds several games at once, for example a cart or a library, whatever their status.
     *
     * @param ids the game ids
     * @return the games found, in the order of {@code ids}; ids with no game are skipped
     * @throws DataAccessException if the database fails
     */
    List<Game> findByIds(Collection<ObjectId> ids) throws DataAccessException;

    /**
     * Lists a developer's games, drafts included, for the developer's dashboard.
     *
     * @param developerId the developer
     * @return the games, newest first
     * @throws DataAccessException if the database fails
     */
    List<Game> findByDeveloper(ObjectId developerId) throws DataAccessException;

    /**
     * Lists the latest published games, for "New games".
     *
     * @param limit how many games to return at most
     * @return published games, latest release date first
     * @throws DataAccessException if the database fails
     */
    List<Game> findNewestPublished(int limit) throws DataAccessException;

    /**
     * Lists the best-selling published games, for "Top sellers".
     *
     * @param limit how many games to return at most
     * @return published games, most units sold first
     * @throws DataAccessException if the database fails
     */
    List<Game> findTopSellers(int limit) throws DataAccessException;

    /**
     * Lists the published games that cost nothing, for the store's "Free" tab.
     *
     * @param limit how many games to return at most
     * @return free published games, latest release date first
     * @throws DataAccessException if the database fails
     */
    List<Game> findFreePublished(int limit) throws DataAccessException;

    /**
     * Searches the published games by name, ignoring case. The text is matched literally, not as a pattern.
     *
     * @param text  the text the name must contain
     * @param limit how many games to return at most
     * @return matching published games, by name; empty for blank text
     * @throws DataAccessException if the database fails
     */
    List<Game> searchPublished(String text, int limit) throws DataAccessException;

    /**
     * Inserts a new game, or writes the changes of an existing one.
     *
     * @param game the game; a new one has no id yet
     * @return the saved game, with its id; use it instead of the argument afterwards
     * @throws DataAccessException if the database fails
     */
    Game save(Game game) throws DataAccessException;
}
