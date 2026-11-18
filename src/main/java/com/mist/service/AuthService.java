package com.mist.service;

import com.mist.dao.UserDao;
import com.mist.exception.DuplicateEntityException;
import com.mist.exception.MistException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.logging.DbOperationLogger;
import com.mist.model.Address;
import com.mist.model.Developer;
import com.mist.model.PaymentCard;
import com.mist.model.Phone;
import com.mist.model.Player;
import com.mist.model.User;
import org.bson.types.ObjectId;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Logs users in and out and registers new player and developer accounts. Passwords are checked against Argon2id hashes
 * and never written to the log.
 *
 * <p>Every method that receives a password wipes its characters before returning, so the caller should pass a copy it
 * no longer needs, such as the array from a {@code PasswordField}.</p>
 */
public class AuthService {

    /** Shortest password accepted. */
    public static final int MIN_PASSWORD_LENGTH = 8;
    /** Longest password accepted. */
    public static final int MAX_PASSWORD_LENGTH = 128;

    /** The users collection. */
    private final UserDao users;
    /** The clock changes are dated with. */
    private final Clock clock;
    /** Hashes and checks passwords with Argon2id. */
    private final PasswordHasher hasher = new PasswordHasher();
    /** Checked when the email is unknown, so a wrong email takes as long as a wrong password. */
    private final String dummyHash = hasher.hash(UUID.randomUUID().toString().toCharArray());

    /**
     * Creates the service.
     *
     * @param users the user DAO
     * @param clock the clock that dates new accounts
     */
    public AuthService(UserDao users, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Logs a user in and writes their email on the following lines of the operation log.
     *
     * @param email    the email address as typed
     * @param password the password as typed; wiped before returning
     * @return the logged-in account: a {@code Player}, {@code Developer} or {@code Admin}
     * @throws UnauthorizedException {@link ServiceKeys#AUTH_BAD_CREDENTIALS} if the email or password is wrong (the
     *                               same for both, so it does not reveal which accounts exist), or
     *                               {@link ServiceKeys#ACCOUNT_DEACTIVATED} if the account was deactivated
     * @throws MistException         if the database fails
     */
    public User login(String email, char[] password) throws MistException {
        try {
            Optional<User> found = Optional.empty();
            try {
                found = users.findByEmail(User.normalizeEmail(email));
            } catch (IllegalArgumentException _) {
                // A malformed email cannot match an account; fall through to the dummy check.
            }
            String storedHash = found.map(User::getPasswordHash).orElse(dummyHash);
            boolean matches = password != null && hasher.verify(password, storedHash);
            if (found.isEmpty() || !matches) {
                throw new UnauthorizedException(ServiceKeys.AUTH_BAD_CREDENTIALS);
            }
            User user = found.get();
            if (!user.isActive()) {
                throw new UnauthorizedException(ServiceKeys.ACCOUNT_DEACTIVATED);
            }
            DbOperationLogger.setUser(user.getEmail());
            return user;
        } finally {
            wipe(password);
        }
    }

    /** Logs the current user out: the operation log goes back to anonymous. */
    public void logout() {
        DbOperationLogger.clearUser();
    }

    /**
     * Registers a new player, who can log in straight away.
     *
     * @param form the registration form; its password is wiped before returning
     * @return the new player, with its id
     * @throws ValidationException      if a value is invalid or the password is weak
     * @throws DuplicateEntityException {@link ServiceKeys#USER_EMAIL_TAKEN} or {@link ServiceKeys#USER_NICKNAME_TAKEN}
     * @throws MistException            if the database fails
     */
    public Player registerPlayer(PlayerSignUp form) throws MistException {
        Objects.requireNonNull(form, "form");
        try {
            checkStrength(form.password());
            String email = Rules.check(() -> User.normalizeEmail(form.email()));
            Address address = Rules.check(() -> form.address() == null ? null : form.address().toAddress());
            Phone phone = Rules.check(() -> PhoneForm.toPhone(form.phone()));
            PaymentCard card = Rules.check(() -> form.card() == null ? null : form.card().toCard(YearMonth.now(clock)));
            String nickname = form.nickname() == null ? null : form.nickname().strip();
            if (users.existsByEmail(email)) {
                throw new DuplicateEntityException(ServiceKeys.USER_EMAIL_TAKEN);
            }
            if (nickname != null && !nickname.isEmpty() && users.existsByNickname(nickname)) {
                throw new DuplicateEntityException(ServiceKeys.USER_NICKNAME_TAKEN);
            }
            String hash = hasher.hash(form.password());
            Player player = Rules.check(() -> Player.register(form.firstName(), form.lastName(), email, hash, phone,
                    address, nickname, card, clock.instant()));
            return users.save(player);
        } finally {
            wipe(form.password());
        }
    }

    /**
     * Registers a new developer, who can log in straight away.
     *
     * @param form the registration form; its password is wiped before returning
     * @return the new developer, with its id
     * @throws ValidationException      if a value is invalid or the password is weak
     * @throws DuplicateEntityException {@link ServiceKeys#USER_EMAIL_TAKEN} or
     *                                  {@link ServiceKeys#USER_STUDIO_NAME_TAKEN}
     * @throws MistException            if the database fails
     */
    public Developer registerDeveloper(DeveloperSignUp form) throws MistException {
        Objects.requireNonNull(form, "form");
        try {
            checkStrength(form.password());
            String email = Rules.check(() -> User.normalizeEmail(form.email()));
            Address address = Rules.check(() -> form.address() == null ? null : form.address().toAddress());
            Phone phone = Rules.check(() -> PhoneForm.toPhone(form.phone()));
            String studioName = form.studioName() == null ? null : form.studioName().strip();
            if (users.existsByEmail(email)) {
                throw new DuplicateEntityException(ServiceKeys.USER_EMAIL_TAKEN);
            }
            if (studioName != null && !studioName.isEmpty() && users.existsByStudioName(studioName)) {
                throw new DuplicateEntityException(ServiceKeys.USER_STUDIO_NAME_TAKEN);
            }
            String hash = hasher.hash(form.password());
            Developer developer = Rules.check(() -> Developer.register(form.firstName(), form.lastName(), email, hash,
                    phone, address, studioName, form.bankAccount(), clock.instant()));
            return users.save(developer);
        } finally {
            wipe(form.password());
        }
    }

    /**
     * Changes a user's password after checking the current one.
     *
     * @param userId          the logged-in user
     * @param currentPassword the current password; wiped before returning
     * @param newPassword     the new password; wiped before returning
     * @throws UnauthorizedException {@link ServiceKeys#AUTH_WRONG_PASSWORD} if the current password is wrong, or if
     *                               the account was deactivated
     * @throws ValidationException   {@link ServiceKeys#AUTH_WEAK_PASSWORD} if the new password is weak
     * @throws MistException         if the account does not exist or the database fails
     */
    public void changePassword(ObjectId userId, char[] currentPassword, char[] newPassword)
            throws MistException {
        try {
            User user = Accounts.active(users, userId);
            if (currentPassword == null || !hasher.verify(currentPassword, user.getPasswordHash())) {
                throw new UnauthorizedException(ServiceKeys.AUTH_WRONG_PASSWORD);
            }
            checkStrength(newPassword);
            String hash = hasher.hash(newPassword);
            Rules.apply(() -> user.changePasswordHash(hash));
            users.save(user);
        } finally {
            wipe(currentPassword);
            wipe(newPassword);
        }
    }

    /**
     * Checks the password policy: 8 to 128 characters, with at least one letter and one digit.
     *
     * @param password the new password
     * @throws ValidationException {@link ServiceKeys#AUTH_WEAK_PASSWORD} if it breaks the policy
     */
    static void checkStrength(char[] password) throws ValidationException {
        boolean letter = false;
        boolean digit = false;
        if (password != null) {
            for (char c : password) {
                letter |= Character.isLetter(c);
                digit |= Character.isDigit(c);
            }
        }
        if (password == null || password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH
                || !letter || !digit) {
            throw new ValidationException(ServiceKeys.AUTH_WEAK_PASSWORD);
        }
    }

    /**
     * Overwrites a password in memory once it is no longer needed.
     *
     * @param password the password, or {@code null}
     */
    private static void wipe(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
