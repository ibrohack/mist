package com.mist.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A player ("cliente"): buys games, keeps them in a library, and rates them. Stored in {@code users} with role
 * {@code PLAYER}.
 *
 * <p>The library, cart and wishlist are embedded. The library holds one {@link LibraryEntry} per owned game; the cart
 * and the wishlist hold game ids. Each change replaces the whole list, so Hibernate always writes it.</p>
 */
@Entity
@DiscriminatorValue("PLAYER")
public class Player extends User {

    /** The shape of a nickname: 3 to 24 letters, digits, dots, dashes or underscores; the validator uses it too. */
    public static final String NICKNAME_REGEX = "[A-Za-z0-9_.-]{3,24}";

    /** {@link #NICKNAME_REGEX}, compiled. */
    private static final Pattern NICKNAME = Pattern.compile(NICKNAME_REGEX);

    /** The nickname, unique among players. */
    private String nickname;
    /** The saved card, masked. */
    private PaymentCard card;
    /** The games owned. */
    private List<LibraryEntry> library;
    /** The games in the cart. */
    private List<ObjectId> cartGameIds;
    /** The games on the wishlist. */
    private List<ObjectId> wishlistGameIds;

    /** Used by Hibernate. */
    protected Player() {
    }

    /**
     * Creates a player.
     *
     * @param firstName    the first name
     * @param lastName     the last name
     * @param email        the email
     * @param passwordHash the Argon2id hash
     * @param phone        the phone, or {@code null}
     * @param address      the address
     * @param nickname     the nickname
     * @param card         the saved card
     * @param createdAt    when the account was created
     */
    private Player(String firstName, String lastName, String email, String passwordHash, Phone phone,
                   Address address, String nickname, PaymentCard card, Instant createdAt) {
        super(firstName, lastName, email, passwordHash, phone, Checks.present(address, ModelKeys.USER_ADDRESS),
                createdAt);
        this.nickname = checkedNickname(nickname);
        this.card = Checks.present(card, ModelKeys.PLAYER_CARD);
        this.library = List.of();
        this.cartGameIds = List.of();
        this.wishlistGameIds = List.of();
    }

    /**
     * Registers a new player.
     *
     * @param firstName    first name
     * @param lastName     last name
     * @param email        email address, which is also the login
     * @param passwordHash Argon2id hash of the password
     * @param phone        phone, or {@code null}
     * @param address      postal address
     * @param nickname     public nickname, 3 to 24 letters, digits, dots, dashes or underscores
     * @param card         saved payment card
     * @param now          the current time
     * @return the new, active player with an empty library, cart and wishlist
     * @throws IllegalArgumentException with a {@link ModelKeys} key if a value is invalid
     */
    public static Player register(String firstName, String lastName, String email, String passwordHash, Phone phone,
                                  Address address, String nickname, PaymentCard card, Instant now) {
        return new Player(firstName, lastName, email, passwordHash, phone, address, nickname, card, now);
    }

    /**
     * Checks a nickname.
     *
     * @param nickname the nickname
     * @return the nickname without surrounding spaces
     */
    private static String checkedNickname(String nickname) {
        String stripped = nickname == null ? null : nickname.strip();
        return Checks.matching(stripped, NICKNAME, ModelKeys.PLAYER_NICKNAME);
    }

    @Override
    public Role role() {
        return Role.PLAYER;
    }

    @Override
    protected void checkAddress(Address address) {
        Checks.present(address, ModelKeys.USER_ADDRESS);
    }

    /**
     * Returns the public nickname, shown on reviews.
     *
     * @return the nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the saved payment card.
     *
     * @return the card, without its number
     */
    public PaymentCard getCard() {
        return card;
    }

    /**
     * Returns the owned games.
     *
     * @return an unmodifiable view of the library, oldest purchase first
     */
    public List<LibraryEntry> getLibrary() {
        return view(library);
    }

    /**
     * Returns the games in the cart.
     *
     * @return an unmodifiable view of the cart's game ids, in the order they were added
     */
    public List<ObjectId> getCartGameIds() {
        return view(cartGameIds);
    }

    /**
     * Returns the games in the wishlist.
     *
     * @return an unmodifiable view of the wishlist's game ids, in the order they were added
     */
    public List<ObjectId> getWishlistGameIds() {
        return view(wishlistGameIds);
    }

    /**
     * Changes the public nickname.
     *
     * @param nickname the new nickname
     * @throws IllegalArgumentException with {@link ModelKeys#PLAYER_NICKNAME} if it is invalid
     */
    public void changeNickname(String nickname) {
        this.nickname = checkedNickname(nickname);
    }

    /**
     * Replaces the saved payment card.
     *
     * @param card the new card
     * @throws IllegalArgumentException with {@link ModelKeys#PLAYER_CARD} if it is missing
     */
    public void changeCard(PaymentCard card) {
        this.card = Checks.present(card, ModelKeys.PLAYER_CARD);
    }

    /**
     * Tells whether the game is in the library.
     *
     * @param gameId the game
     * @return {@code true} if the player owns it
     */
    public boolean owns(ObjectId gameId) {
        return view(library).stream().anyMatch(entry -> entry.gameId().equals(gameId));
    }

    /**
     * Adds a game to the cart, unless the player owns it or it is already there.
     *
     * @param gameId the game
     * @return {@code true} if the cart changed
     */
    public boolean addToCart(ObjectId gameId) {
        if (owns(gameId) || view(cartGameIds).contains(gameId)) {
            return false;
        }
        cartGameIds = appended(cartGameIds, Objects.requireNonNull(gameId, "gameId"));
        return true;
    }

    /**
     * Removes a game from the cart.
     *
     * @param gameId the game
     * @return {@code true} if it was in the cart
     */
    public boolean removeFromCart(ObjectId gameId) {
        if (!view(cartGameIds).contains(gameId)) {
            return false;
        }
        cartGameIds = without(cartGameIds, gameId);
        return true;
    }

    /** Empties the cart, after checkout. */
    public void clearCart() {
        cartGameIds = List.of();
    }

    /**
     * Adds a game to the wishlist, unless the player owns it or it is already there.
     *
     * @param gameId the game
     * @return {@code true} if the wishlist changed
     */
    public boolean addToWishlist(ObjectId gameId) {
        if (owns(gameId) || view(wishlistGameIds).contains(gameId)) {
            return false;
        }
        wishlistGameIds = appended(wishlistGameIds, Objects.requireNonNull(gameId, "gameId"));
        return true;
    }

    /**
     * Removes a game from the wishlist.
     *
     * @param gameId the game
     * @return {@code true} if it was in the wishlist
     */
    public boolean removeFromWishlist(ObjectId gameId) {
        if (!view(wishlistGameIds).contains(gameId)) {
            return false;
        }
        wishlistGameIds = without(wishlistGameIds, gameId);
        return true;
    }

    /**
     * Adds a bought game to the library and drops it from the cart and the wishlist.
     *
     * @param gameId  the game
     * @param orderId the order it was bought with
     * @param at      when the order was paid
     * @throws IllegalStateException with {@link ModelKeys#PLAYER_ALREADY_OWNED} if the player already owns it
     */
    public void addToLibrary(ObjectId gameId, ObjectId orderId, Instant at) {
        Checks.requireState(!owns(gameId), ModelKeys.PLAYER_ALREADY_OWNED);
        library = appended(library, new LibraryEntry(gameId, orderId, at));
        removeFromCart(gameId);
        removeFromWishlist(gameId);
    }

    /**
     * Removes a returned game from the library.
     *
     * @param gameId the game
     * @return {@code true} if the player owned it
     */
    public boolean removeFromLibrary(ObjectId gameId) {
        if (!owns(gameId)) {
            return false;
        }
        List<LibraryEntry> kept = new ArrayList<>(view(library));
        kept.removeIf(entry -> entry.gameId().equals(gameId));
        library = List.copyOf(kept);
        return true;
    }

    /**
     * Returns a read-only view of a list.
     *
     * @param list the list, possibly {@code null}
     * @param <T>  the element type
     * @return an unmodifiable list, empty for {@code null}
     */
    private static <T> List<T> view(List<T> list) {
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }

    /**
     * Returns a copy of a list with one more element.
     *
     * @param list    the list
     * @param element the element to add
     * @param <T>     the element type
     * @return the new list
     */
    private static <T> List<T> appended(List<T> list, T element) {
        List<T> copy = new ArrayList<>(view(list));
        copy.add(element);
        return List.copyOf(copy);
    }

    /**
     * Returns a copy of a list without an element.
     *
     * @param list    the list
     * @param element the element to remove
     * @param <T>     the element type
     * @return the new list
     */
    private static <T> List<T> without(List<T> list, T element) {
        List<T> copy = new ArrayList<>(view(list));
        copy.remove(element);
        return List.copyOf(copy);
    }
}
