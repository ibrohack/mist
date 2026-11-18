package com.mist.service;

import com.mist.dao.GameDao;
import com.mist.dao.OrderDao;
import com.mist.dao.TransactionRunner;
import com.mist.dao.UserDao;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.exception.ValidationException;
import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.ModelKeys;
import com.mist.model.Order;
import com.mist.model.OrderItem;
import com.mist.model.PaymentCard;
import com.mist.model.Player;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runs the store: browsing and searching the catalog, the cart and wishlist, and checkout with the player's saved card.
 * A purchase writes the order, adds the games to the library, empties the cart, and updates the games' sales statistics
 * and their developers' revenue in one transaction.
 *
 * <p>The store shows only published games. The charge is simulated: placing the order means the card was charged.</p>
 */
public class StoreService {

    /** Longest list the store returns at once. */
    public static final int MAX_LIST_SIZE = 100;

    /** The games collection. */
    private final GameDao games;
    /** The users collection. */
    private final UserDao users;
    /** The orders collection. */
    private final OrderDao orders;
    /** Runs the changes that touch several documents. */
    private final TransactionRunner transactions;
    /** The clock changes are dated with. */
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param games        the game DAO
     * @param users        the user DAO
     * @param orders       the order DAO
     * @param transactions the transaction runner
     * @param clock        the clock that dates orders and checks card expiry
     */
    public StoreService(GameDao games, UserDao users, OrderDao orders, TransactionRunner transactions, Clock clock) {
        this.games = Objects.requireNonNull(games, "games");
        this.users = Objects.requireNonNull(users, "users");
        this.orders = Objects.requireNonNull(orders, "orders");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Lists the latest releases, for "New games".
     *
     * @param limit how many games to show, 1 to {@value #MAX_LIST_SIZE}
     * @return published games, latest release first
     * @throws MistException if the limit is out of range or the database fails
     */
    public List<Game> newReleases(int limit) throws MistException {
        return games.findNewestPublished(checkedLimit(limit));
    }

    /**
     * Lists the free games on sale, for the "Free" tab.
     *
     * @param limit how many games to show, 1 to {@value #MAX_LIST_SIZE}
     * @return free published games, latest release first
     * @throws MistException if the limit is out of range or the database fails
     */
    public List<Game> freeGames(int limit) throws MistException {
        return games.findFreePublished(checkedLimit(limit));
    }

    /**
     * Lists the best sellers, for "Top sellers".
     *
     * @param limit how many games to show, 1 to {@value #MAX_LIST_SIZE}
     * @return published games, most units sold first
     * @throws MistException if the limit is out of range or the database fails
     */
    public List<Game> topSellers(int limit) throws MistException {
        return games.findTopSellers(checkedLimit(limit));
    }

    /**
     * Searches the catalog by name, ignoring case. Blank text lists the latest releases instead.
     *
     * @param text  what the name must contain; matched literally
     * @param limit how many games to show, 1 to {@value #MAX_LIST_SIZE}
     * @return matching published games, by name
     * @throws MistException if the limit is out of range or the database fails
     */
    public List<Game> search(String text, int limit) throws MistException {
        if (text == null || text.isBlank()) {
            return newReleases(limit);
        }
        return games.searchPublished(text, checkedLimit(limit));
    }

    /**
     * Loads a game for its page.
     *
     * @param gameId the game
     * @return the game
     * @throws NotFoundException {@link ServiceKeys#GAME_NOT_FOUND} if there is no such game
     * @throws MistException     if the database fails
     */
    public Game game(ObjectId gameId) throws MistException {
        if (gameId == null) {
            throw new NotFoundException(ServiceKeys.GAME_NOT_FOUND);
        }
        return games.findById(gameId).orElseThrow(() -> new NotFoundException(ServiceKeys.GAME_NOT_FOUND));
    }

    /**
     * Lists the games in a player's cart.
     *
     * @param playerId the logged-in player
     * @return the games, in the order they were added
     * @throws MistException if the account cannot act or the database fails
     */
    public List<Game> cart(ObjectId playerId) throws MistException {
        return games.findByIds(Accounts.activePlayer(users, playerId).getCartGameIds());
    }

    /**
     * Adds a game to a player's cart. Adding a game that is already there changes nothing.
     *
     * @param playerId the logged-in player
     * @param gameId   the game
     * @return the updated player
     * @throws ValidationException if the game is not on sale, is already owned, or the cart is full
     * @throws MistException       if the account cannot act, the game does not exist or the database fails
     */
    public Player addToCart(ObjectId playerId, ObjectId gameId) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            Game game = purchasable(gameId);
            if (player.owns(game.getId())) {
                throw new ValidationException(ModelKeys.PLAYER_ALREADY_OWNED);
            }
            if (!player.getCartGameIds().contains(game.getId()) && player.getCartGameIds().size() >= Order.MAX_ITEMS) {
                throw new ValidationException(ModelKeys.ORDER_ITEMS);
            }
            return player.addToCart(game.getId()) ? users.save(player) : player;
        });
    }

    /**
     * Removes a game from a player's cart.
     *
     * @param playerId the logged-in player
     * @param gameId   the game
     * @return the updated player
     * @throws MistException if the account cannot act or the database fails
     */
    public Player removeFromCart(ObjectId playerId, ObjectId gameId) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            return player.removeFromCart(gameId) ? users.save(player) : player;
        });
    }

    /**
     * Lists the games in a player's wishlist.
     *
     * @param playerId the logged-in player
     * @return the games, in the order they were added
     * @throws MistException if the account cannot act or the database fails
     */
    public List<Game> wishlist(ObjectId playerId) throws MistException {
        return games.findByIds(Accounts.activePlayer(users, playerId).getWishlistGameIds());
    }

    /**
     * Adds a game to a player's wishlist. Adding a game that is already there changes nothing.
     *
     * @param playerId the logged-in player
     * @param gameId   the game
     * @return the updated player
     * @throws ValidationException if the game is not on sale or is already owned
     * @throws MistException       if the account cannot act, the game does not exist or the database fails
     */
    public Player addToWishlist(ObjectId playerId, ObjectId gameId) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            Game game = purchasable(gameId);
            if (player.owns(game.getId())) {
                throw new ValidationException(ModelKeys.PLAYER_ALREADY_OWNED);
            }
            return player.addToWishlist(game.getId()) ? users.save(player) : player;
        });
    }

    /**
     * Removes a game from a player's wishlist.
     *
     * @param playerId the logged-in player
     * @param gameId   the game
     * @return the updated player
     * @throws MistException if the account cannot act or the database fails
     */
    public Player removeFromWishlist(ObjectId playerId, ObjectId gameId) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            return player.removeFromWishlist(gameId) ? users.save(player) : player;
        });
    }

    /**
     * Buys every game in the cart with the player's saved card, in one transaction: the order is written, the games go
     * to the library, the cart is emptied, and each game's sales statistics and each developer's revenue are updated.
     *
     * @param playerId the logged-in player
     * @return the paid order
     * @throws ValidationException {@link ServiceKeys#STORE_EMPTY_CART}, {@link ServiceKeys#GAME_NOT_PURCHASABLE}, or a
     *                             model key if the card is missing or expired or a game is already owned
     * @throws MistException       if the account cannot act, a game no longer exists or the database fails; nothing is
     *                             written then
     */
    public Order checkout(ObjectId playerId) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            List<ObjectId> cart = player.getCartGameIds();
            if (cart.isEmpty()) {
                throw new ValidationException(ServiceKeys.STORE_EMPTY_CART);
            }
            PaymentCard card = player.getCard();
            if (card == null) {
                throw new ValidationException(ModelKeys.PLAYER_CARD);
            }
            if (card.isExpired(YearMonth.now(clock))) {
                throw new ValidationException(ModelKeys.CARD_EXPIRED);
            }
            List<Game> bought = games.findByIds(cart);
            if (bought.size() != cart.size()) {
                throw new NotFoundException(ServiceKeys.GAME_NOT_FOUND);
            }
            for (Game game : bought) {
                if (!game.isPurchasable()) {
                    throw new ValidationException(ServiceKeys.GAME_NOT_PURCHASABLE);
                }
                if (player.owns(game.getId())) {
                    throw new ValidationException(ModelKeys.PLAYER_ALREADY_OWNED);
                }
            }

            Instant now = clock.instant();
            List<OrderItem> items = bought.stream().map(OrderItem::of).toList();
            Order order = orders.save(Rules.check(() -> Order.place(player.getId(), items, card, now)));

            Map<ObjectId, BigDecimal> revenueByDeveloper = new LinkedHashMap<>();
            for (Game game : bought) {
                Rules.apply(() -> player.addToLibrary(game.getId(), order.getId(), now));
                game.recordSale(game.getPrice());
                games.save(game);
                revenueByDeveloper.merge(game.getDeveloperId(), game.getPrice(), BigDecimal::add);
            }
            player.clearCart();
            users.save(player);
            // One query for every developer paid by this order, instead of one per developer.
            Map<ObjectId, Developer> developers = new LinkedHashMap<>();
            for (User user : users.findByIds(revenueByDeveloper.keySet())) {
                if (user instanceof Developer developer) {
                    developers.put(developer.getId(), developer);
                }
            }
            for (Map.Entry<ObjectId, BigDecimal> sale : revenueByDeveloper.entrySet()) {
                Developer developer = developers.get(sale.getKey());
                if (developer == null) {
                    throw new NotFoundException(ServiceKeys.USER_NOT_FOUND);
                }
                developer.recordSale(sale.getValue());
                users.save(developer);
            }
            return order;
        });
    }

    /**
     * Loads a game and checks that it is on sale.
     *
     * @param gameId the game
     * @return the game
     * @throws MistException if it does not exist or is not published
     */
    private Game purchasable(ObjectId gameId) throws MistException {
        Game game = game(gameId);
        if (!game.isPurchasable()) {
            throw new ValidationException(ServiceKeys.GAME_NOT_PURCHASABLE);
        }
        return game;
    }

    /**
     * Checks the size of a requested list.
     *
     * @param limit the requested size
     * @return the same size
     * @throws ValidationException if it is not between 1 and {@link #MAX_LIST_SIZE}
     */
    private static int checkedLimit(int limit) throws ValidationException {
        if (limit < 1 || limit > MAX_LIST_SIZE) {
            throw new ValidationException(ServiceKeys.INVALID_INPUT);
        }
        return limit;
    }
}
