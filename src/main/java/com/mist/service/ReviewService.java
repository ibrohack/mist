package com.mist.service;

import com.mist.dao.GameDao;
import com.mist.dao.ReviewDao;
import com.mist.dao.TransactionRunner;
import com.mist.dao.UserDao;
import com.mist.exception.DuplicateEntityException;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.model.Game;
import com.mist.model.Player;
import com.mist.model.Review;
import org.bson.types.ObjectId;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lets players rate and review the games they own, once per game. Saving a review and updating the game's rating
 * statistics run in one transaction.
 *
 * <p>Reviews never change once written. A game's page reads them a page at a time, newest first, with each author's
 * nickname loaded in one extra query, because the MongoDB extension has no joins or subqueries.</p>
 */
public class ReviewService {

    /** Most reviews returned in one page. */
    public static final int MAX_PAGE_SIZE = 50;

    /** The highest page number accepted, a guard against absurd offsets. */
    private static final int MAX_PAGE = 100_000;

    /** The reviews collection. */
    private final ReviewDao reviews;
    /** The users collection. */
    private final UserDao users;
    /** The games collection. */
    private final GameDao games;
    /** Runs the changes that touch several documents. */
    private final TransactionRunner transactions;
    /** The clock changes are dated with. */
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param reviews      the review DAO
     * @param users        the user DAO
     * @param games        the game DAO
     * @param transactions the transaction runner
     * @param clock        the clock that dates reviews
     */
    public ReviewService(ReviewDao reviews, UserDao users, GameDao games, TransactionRunner transactions,
                         Clock clock) {
        this.reviews = Objects.requireNonNull(reviews, "reviews");
        this.users = Objects.requireNonNull(users, "users");
        this.games = Objects.requireNonNull(games, "games");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Rates and reviews a game the player owns, and adds the rating to the game's statistics, in one transaction.
     *
     * @param playerId the logged-in player
     * @param gameId   the game
     * @param rating   1 to 5
     * @param comment  the review text, or {@code null} for a rating only
     * @return the stored review
     * @throws UnauthorizedException    {@link ServiceKeys#GAME_NOT_OWNED} if the player does not own the game
     * @throws DuplicateEntityException {@link ServiceKeys#REVIEW_ALREADY_EXISTS} if the player already reviewed it
     * @throws ValidationException      if the rating or comment is invalid
     * @throws MistException            if the account cannot act, the game does not exist or the database fails
     */
    public Review rate(ObjectId playerId, ObjectId gameId, int rating, String comment) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            if (gameId == null || !player.owns(gameId)) {
                throw new UnauthorizedException(ServiceKeys.GAME_NOT_OWNED);
            }
            if (reviews.findByGameAndPlayer(gameId, player.getId()).isPresent()) {
                throw new DuplicateEntityException(ServiceKeys.REVIEW_ALREADY_EXISTS);
            }
            Game game = games.findById(gameId).orElseThrow(() -> new NotFoundException(ServiceKeys.GAME_NOT_FOUND));
            Review review = reviews.insert(Rules.check(
                    () -> Review.of(player.getId(), gameId, rating, comment, clock.instant())));
            game.recordRating(review.getRating());
            games.save(game);
            return review;
        });
    }

    /**
     * Finds the review a player wrote for a game, to show it instead of the review form.
     *
     * @param playerId the player
     * @param gameId   the game
     * @return the review, or empty if the player has not reviewed the game
     * @throws MistException if the database fails
     */
    public Optional<Review> reviewOf(ObjectId playerId, ObjectId gameId) throws MistException {
        if (playerId == null || gameId == null) {
            return Optional.empty();
        }
        return reviews.findByGameAndPlayer(gameId, playerId);
    }

    /**
     * Reads one page of a game's reviews, newest first, with their authors' nicknames.
     *
     * @param gameId the game
     * @param page   the page number, from 0
     * @param size   reviews per page, 1 to {@value #MAX_PAGE_SIZE}
     * @return the reviews of the page; empty after the last page
     * @throws ValidationException if the page or size is out of range
     * @throws MistException       if the database fails
     */
    public List<GameReview> reviews(ObjectId gameId, int page, int size) throws MistException {
        if (gameId == null) {
            throw new NotFoundException(ServiceKeys.GAME_NOT_FOUND);
        }
        if (page < 0 || page > MAX_PAGE || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException(ServiceKeys.INVALID_INPUT);
        }
        List<Review> found = reviews.findByGame(gameId, page * size, size);
        Map<ObjectId, String> nicknames = new HashMap<>();
        for (var author : users.findByIds(found.stream().map(Review::getPlayerId).toList())) {
            if (author instanceof Player player) {
                nicknames.put(player.getId(), player.getNickname());
            }
        }
        return found.stream().map(review -> new GameReview(review, nicknames.get(review.getPlayerId()))).toList();
    }
}
