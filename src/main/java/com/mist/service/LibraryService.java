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
import com.mist.model.LibraryEntry;
import com.mist.model.Order;
import com.mist.model.Player;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages a player's library: the owned games, and returning a game for a refund to the card it was paid with. A return
 * updates the order, the library, the game's sales statistics and the developer's revenue in one transaction.
 *
 * <p>A game can be returned within {@link #REFUND_WINDOW} of its purchase. Delisted games stay in the library.</p>
 */
public class LibraryService {

    /** How long after the purchase a game can be returned. */
    public static final Duration REFUND_WINDOW = Duration.ofDays(14);

    /** The users collection. */
    private final UserDao users;
    /** The games collection. */
    private final GameDao games;
    /** The orders collection. */
    private final OrderDao orders;
    /** Runs the changes that touch several documents. */
    private final TransactionRunner transactions;
    /** The clock changes are dated with. */
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param users        the user DAO
     * @param games        the game DAO
     * @param orders       the order DAO
     * @param transactions the transaction runner
     * @param clock        the clock that decides whether the return period is over
     */
    public LibraryService(UserDao users, GameDao games, OrderDao orders, TransactionRunner transactions, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.games = Objects.requireNonNull(games, "games");
        this.orders = Objects.requireNonNull(orders, "orders");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Lists a player's games for the Library view.
     *
     * @param playerId the logged-in player
     * @return the owned games, latest purchase first, each with whether it can still be returned
     * @throws MistException if the account cannot act or the database fails
     */
    public List<OwnedGame> ownedGames(ObjectId playerId) throws MistException {
        Player player = Accounts.activePlayer(users, playerId);
        List<LibraryEntry> entries = player.getLibrary().stream()
                .sorted(Comparator.comparing(LibraryEntry::acquiredAt).reversed())
                .toList();
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<ObjectId, Game> gamesById = games.findByIds(entries.stream().map(LibraryEntry::gameId).toList()).stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
        Map<ObjectId, Order> ordersById = orders.findByPlayer(player.getId()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Instant now = clock.instant();
        return entries.stream()
                .filter(entry -> gamesById.containsKey(entry.gameId()))
                .map(entry -> new OwnedGame(gamesById.get(entry.gameId()), entry.acquiredAt(),
                        isReturnable(ordersById.get(entry.orderId()), entry.gameId(), now)))
                .toList();
    }

    /**
     * Lists a player's purchase history, with the refunds made.
     *
     * @param playerId the logged-in player
     * @return the orders, newest first
     * @throws MistException if the account cannot act or the database fails
     */
    public List<Order> orders(ObjectId playerId) throws MistException {
        return orders.findByPlayer(Accounts.activePlayer(users, playerId).getId());
    }

    /**
     * Tells whether a player can still return a game.
     *
     * @param playerId the logged-in player
     * @param gameId   the game
     * @return {@code true} if the player owns it and its return period is not over
     * @throws MistException if the account cannot act or the database fails
     */
    public boolean canReturn(ObjectId playerId, ObjectId gameId) throws MistException {
        Player player = Accounts.activePlayer(users, playerId);
        Optional<LibraryEntry> entry = entry(player, gameId);
        if (entry.isEmpty()) {
            return false;
        }
        Order order = orders.findById(entry.get().orderId()).orElse(null);
        return isReturnable(order, gameId, clock.instant()) && order.getPlayerId().equals(player.getId());
    }

    /**
     * Returns a game for a refund to the card it was paid with, in one transaction: the order item is marked refunded,
     * the game leaves the library, and the sale is taken off the game's statistics and the developer's revenue.
     *
     * @param playerId the logged-in player
     * @param gameId   the game to return
     * @return the amount refunded
     * @throws ValidationException {@link ServiceKeys#GAME_NOT_OWNED} or {@link ServiceKeys#LIBRARY_REFUND_CLOSED}
     * @throws MistException       if the account cannot act, the order is missing or the database fails; nothing is
     *                             written then
     */
    public BigDecimal returnGame(ObjectId playerId, ObjectId gameId) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            LibraryEntry entry = entry(player, gameId)
                    .orElseThrow(() -> new ValidationException(ServiceKeys.GAME_NOT_OWNED));
            Order order = orders.findById(entry.orderId())
                    .filter(found -> found.getPlayerId().equals(player.getId()))
                    .orElseThrow(() -> new NotFoundException(ServiceKeys.ORDER_NOT_FOUND));
            Instant now = clock.instant();
            if (!order.isRefundable(gameId, now, REFUND_WINDOW)) {
                throw new ValidationException(ServiceKeys.LIBRARY_REFUND_CLOSED);
            }
            BigDecimal amount = Rules.check(() -> order.refund(gameId, now));
            orders.save(order);

            player.removeFromLibrary(gameId);
            users.save(player);

            Game game = games.findById(gameId).orElseThrow(() -> new NotFoundException(ServiceKeys.GAME_NOT_FOUND));
            Rules.apply(() -> game.recordRefund(amount));
            games.save(game);

            Developer developer = Accounts.developer(users, game.getDeveloperId());
            Rules.apply(() -> developer.recordRefund(amount));
            users.save(developer);
            return amount;
        });
    }

    /**
     * Finds a game in a player's library.
     *
     * @param player the player
     * @param gameId the game
     * @return the library entry, or empty if the player does not own it
     */
    private static Optional<LibraryEntry> entry(Player player, ObjectId gameId) {
        return player.getLibrary().stream().filter(entry -> entry.gameId().equals(gameId)).findFirst();
    }

    /**
     * Tells whether a game of an order can still be returned.
     *
     * @param order  the order, or {@code null}
     * @param gameId the game
     * @param now    the current instant
     * @return {@code true} within the refund window and while not refunded
     */
    private static boolean isReturnable(Order order, ObjectId gameId, Instant now) {
        return order != null && order.isRefundable(gameId, now, REFUND_WINDOW);
    }
}
