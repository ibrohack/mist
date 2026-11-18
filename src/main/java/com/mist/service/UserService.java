package com.mist.service;

import com.mist.dao.GameDao;
import com.mist.dao.MediaDao;
import com.mist.dao.TransactionRunner;
import com.mist.dao.UserDao;
import com.mist.exception.DuplicateEntityException;
import com.mist.exception.MistException;
import com.mist.exception.NotFoundException;
import com.mist.exception.ValidationException;
import com.mist.model.Address;
import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.GameStatus;
import com.mist.model.Language;
import com.mist.model.Media;
import com.mist.model.Phone;
import com.mist.model.Player;
import com.mist.model.Preferences;
import com.mist.model.Theme;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Manages user profiles, interface preferences and profile pictures, and deactivates accounts. Deactivating a developer
 * also delists the developer's games, in the same transaction.
 *
 * <p>Every method acts for the user whose id it receives, which must be an active account.</p>
 */
public class UserService {

    /** The users collection. */
    private final UserDao users;
    /** The games collection. */
    private final GameDao games;
    /** The media collection, where images are stored. */
    private final MediaDao media;
    /** Runs the changes that touch several documents. */
    private final TransactionRunner transactions;
    /** The clock changes are dated with. */
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param users        the user DAO
     * @param games        the game DAO, to delist a deactivated developer's games
     * @param media        the media DAO, for profile pictures
     * @param transactions the transaction runner
     * @param clock        the clock that dates uploads and deactivations
     */
    public UserService(UserDao users, GameDao games, MediaDao media, TransactionRunner transactions, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.games = Objects.requireNonNull(games, "games");
        this.media = Objects.requireNonNull(media, "media");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Loads an account, whatever its status, for example to show it again after a change.
     *
     * @param userId the account
     * @return the account
     * @throws NotFoundException if there is no such account
     * @throws MistException     if the database fails
     */
    public User get(ObjectId userId) throws MistException {
        if (userId == null) {
            throw new NotFoundException(ServiceKeys.USER_NOT_FOUND);
        }
        return users.findById(userId).orElseThrow(() -> new NotFoundException(ServiceKeys.USER_NOT_FOUND));
    }

    /**
     * Changes the personal details shown on the Profile view.
     *
     * @param userId the logged-in user
     * @param form   the new details
     * @return the updated account
     * @throws ValidationException      if a value is invalid
     * @throws DuplicateEntityException {@link ServiceKeys#USER_EMAIL_TAKEN} if another account uses the new email
     * @throws MistException            if the account cannot act or the database fails
     */
    public User updateProfile(ObjectId userId, ProfileForm form) throws MistException {
        return transactions.call(() -> {
            Objects.requireNonNull(form, "form");
            String email = Rules.check(() -> User.normalizeEmail(form.email()));
            Address address = Rules.check(() -> form.address() == null ? null : form.address().toAddress());
            Phone phone = Rules.check(() -> PhoneForm.toPhone(form.phone()));
            User user = Accounts.active(users, userId);
            if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
                throw new DuplicateEntityException(ServiceKeys.USER_EMAIL_TAKEN);
            }
            Rules.apply(() -> user.changeProfile(form.firstName(), form.lastName(), email, phone, address));
            return users.save(user);
        });
    }

    /**
     * Changes a player's public nickname.
     *
     * @param playerId the logged-in player
     * @param nickname the new nickname
     * @return the updated player
     * @throws ValidationException      if the nickname is invalid
     * @throws DuplicateEntityException {@link ServiceKeys#USER_NICKNAME_TAKEN} if another player uses it
     * @throws MistException            if the account cannot act or the database fails
     */
    public Player changeNickname(ObjectId playerId, String nickname) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            String wanted = nickname == null ? null : nickname.strip();
            if (wanted != null && !wanted.equals(player.getNickname()) && users.existsByNickname(wanted)) {
                throw new DuplicateEntityException(ServiceKeys.USER_NICKNAME_TAKEN);
            }
            Rules.apply(() -> player.changeNickname(wanted));
            return users.save(player);
        });
    }

    /**
     * Replaces a player's saved payment card.
     *
     * @param playerId the logged-in player
     * @param card     the new card
     * @return the updated player
     * @throws ValidationException if the card is invalid or has expired
     * @throws MistException       if the account cannot act or the database fails
     */
    public Player changeCard(ObjectId playerId, CardForm card) throws MistException {
        return transactions.call(() -> {
            Player player = Accounts.activePlayer(users, playerId);
            Rules.apply(() -> player.changeCard(card == null ? null : card.toCard(YearMonth.now(clock))));
            return users.save(player);
        });
    }

    /**
     * Changes the name shown on a developer's games.
     *
     * @param developerId the logged-in developer
     * @param studioName  the new studio name
     * @return the updated developer
     * @throws ValidationException      if the name is blank or too long
     * @throws DuplicateEntityException {@link ServiceKeys#USER_STUDIO_NAME_TAKEN} if another developer uses it
     * @throws MistException            if the account cannot act or the database fails
     */
    public Developer changeStudioName(ObjectId developerId, String studioName) throws MistException {
        return transactions.call(() -> {
            Developer developer = Accounts.activeDeveloper(users, developerId);
            String wanted = studioName == null ? null : studioName.strip();
            if (wanted != null && !wanted.equals(developer.getStudioName()) && users.existsByStudioName(wanted)) {
                throw new DuplicateEntityException(ServiceKeys.USER_STUDIO_NAME_TAKEN);
            }
            Rules.apply(() -> developer.changeStudioName(wanted));
            return users.save(developer);
        });
    }

    /**
     * Changes the account a developer's revenue is paid into.
     *
     * @param developerId the logged-in developer
     * @param bankAccount the new IBAN
     * @return the updated developer
     * @throws ValidationException if it is not a valid IBAN
     * @throws MistException       if the account cannot act or the database fails
     */
    public Developer changeBankAccount(ObjectId developerId, String bankAccount) throws MistException {
        return transactions.call(() -> {
            Developer developer = Accounts.activeDeveloper(users, developerId);
            Rules.apply(() -> developer.changeBankAccount(bankAccount));
            return users.save(developer);
        });
    }

    /**
     * Changes the interface language and theme.
     *
     * @param userId   the logged-in user
     * @param language the new language
     * @param theme    the new theme
     * @return the updated account
     * @throws ValidationException if either is missing
     * @throws MistException       if the account cannot act or the database fails
     */
    public User changePreferences(ObjectId userId, Language language, Theme theme) throws MistException {
        if (language == null || theme == null) {
            throw new ValidationException(ServiceKeys.INVALID_INPUT);
        }
        return transactions.call(() -> {
            User user = Accounts.active(users, userId);
            user.changePreferences(new Preferences(language, theme));
            return users.save(user);
        });
    }

    /**
     * Replaces the profile picture. The new image is stored, the account points to it and the old image is deleted, in
     * one transaction.
     *
     * @param userId the logged-in user
     * @param image  the new picture
     * @return the updated account
     * @throws ValidationException if the image type, size or file name is invalid
     * @throws MistException       if the account cannot act or the database fails
     */
    public User changeAvatar(ObjectId userId, ImageUpload image) throws MistException {
        Objects.requireNonNull(image, "image");
        return transactions.call(() -> {
            User user = Accounts.active(users, userId);
            Media stored = media.insert(Rules.check(() -> image.toMedia(user.getId(), clock.instant())));
            ObjectId previous = user.getAvatarId();
            user.changeAvatar(stored.getId());
            User saved = users.save(user);
            if (previous != null) {
                media.delete(previous);
            }
            return saved;
        });
    }

    /**
     * Removes the profile picture and deletes its image.
     *
     * @param userId the logged-in user
     * @return the updated account
     * @throws MistException if the account cannot act or the database fails
     */
    public User removeAvatar(ObjectId userId) throws MistException {
        return transactions.call(() -> {
            User user = Accounts.active(users, userId);
            ObjectId previous = user.getAvatarId();
            if (previous == null) {
                return user;
            }
            user.changeAvatar(null);
            User saved = users.save(user);
            media.delete(previous);
            return saved;
        });
    }

    /**
     * Deactivates an account ("dar de baja"). The account can no longer log in; its orders and reviews stay. For a
     * developer, every published game is delisted in the same transaction. The UI should log the user out afterwards.
     *
     * @param userId the logged-in user
     * @throws MistException if the account cannot act or the database fails
     */
    public void deactivate(ObjectId userId) throws MistException {
        transactions.run(() -> {
            User user = Accounts.active(users, userId);
            user.deactivate(clock.instant());
            if (user instanceof Developer developer) {
                for (Game game : games.findByDeveloper(developer.getId())) {
                    if (game.getStatus() == GameStatus.PUBLISHED) {
                        game.delist();
                        games.save(game);
                    }
                }
            }
            users.save(user);
        });
    }
}
