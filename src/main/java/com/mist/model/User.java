package com.mist.model;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A MIST account, stored in the {@code users} collection.
 *
 * <p>Players, developers and admins share the collection: the {@code role} field holds the {@link Role} name and tells
 * Hibernate which subclass to load. Fields that belong to another role are stored as {@code null}.</p>
 *
 * <p>The password is stored only as an Argon2id hash, which {@code AuthService} computes; the model refuses any other
 * value, so a plain-text password can never be saved.</p>
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
public abstract class User {

    /** Longest first name accepted. */
    public static final int MAX_FIRST_NAME_LENGTH = 50;
    /** Longest last name accepted. */
    public static final int MAX_LAST_NAME_LENGTH = 80;
    /** Longest email address accepted. */
    public static final int MAX_EMAIL_LENGTH = 254;

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]++@[^@\\s.]++(?:\\.[^@\\s.]++)++");
    private static final String ARGON2ID_PREFIX = "$argon2id$";

    @Id
    @ObjectIdGenerator
    private ObjectId id;

    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private Phone phone;
    private Address address;
    private ObjectId avatarId;
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private Instant deactivatedAt;
    private Preferences preferences;

    /** Used by Hibernate. */
    protected User() {
    }

    /**
     * Creates an active account with the default preferences.
     *
     * @param firstName    first name
     * @param lastName     last name
     * @param email        email address, stored lowercase
     * @param passwordHash Argon2id hash of the password
     * @param phone        phone, or {@code null}
     * @param address      postal address, or {@code null}
     * @param createdAt    when the account is created
     * @throws IllegalArgumentException with a {@link ModelKeys} key if a value is invalid
     */
    protected User(String firstName, String lastName, String email, String passwordHash, Phone phone,
                   Address address, Instant createdAt) {
        this.firstName = Checks.text(firstName, 1, MAX_FIRST_NAME_LENGTH, ModelKeys.USER_FIRST_NAME);
        this.lastName = Checks.text(lastName, 1, MAX_LAST_NAME_LENGTH, ModelKeys.USER_LAST_NAME);
        this.email = normalizeEmail(email);
        this.passwordHash = checkedPasswordHash(passwordHash);
        this.phone = phone;
        this.address = address;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = AccountStatus.ACTIVE;
        this.preferences = Preferences.DEFAULTS;
    }

    /**
     * Normalizes an email address the way MIST stores it, so lookups match: trimmed and lowercase.
     *
     * @param email the address as typed
     * @return the normalized address
     * @throws IllegalArgumentException with {@link ModelKeys#USER_EMAIL} if the address is malformed or too long
     */
    public static String normalizeEmail(String email) {
        String normalized = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        Checks.require(normalized != null && normalized.length() <= MAX_EMAIL_LENGTH
                && EMAIL.matcher(normalized).matches(), ModelKeys.USER_EMAIL);
        return normalized;
    }

    private static String checkedPasswordHash(String passwordHash) {
        Checks.require(passwordHash != null && passwordHash.startsWith(ARGON2ID_PREFIX), ModelKeys.USER_PASSWORD_HASH);
        return passwordHash;
    }

    /**
     * Returns what this account may do.
     *
     * @return the role, which is also the value of the {@code role} field
     */
    public abstract Role role();

    /**
     * Returns the document id.
     *
     * @return the id, or {@code null} before the account is first saved
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Returns the first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the name as shown on screen.
     *
     * @return the first and last names, separated by a space
     */
    public String fullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns the email address, which is also the login.
     *
     * @return the lowercase email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the stored password hash, for {@code AuthService} to verify a login.
     *
     * @return the Argon2id hash in PHC format
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Returns the phone.
     *
     * @return the phone, or {@code null} if none was given
     */
    public Phone getPhone() {
        return phone;
    }

    /**
     * Returns the postal address.
     *
     * @return the address, or {@code null} for an admin without one
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Returns the profile picture.
     *
     * @return the id of the image in {@code media}, or {@code null} if there is none
     */
    public ObjectId getAvatarId() {
        return avatarId;
    }

    /**
     * Returns when the account was created.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns whether the account can still be used.
     *
     * @return the account status
     */
    public AccountStatus getStatus() {
        return status;
    }

    /**
     * Tells whether the account can still be used.
     *
     * @return {@code true} unless the account was deactivated
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Returns when the account was deactivated.
     *
     * @return the deactivation time, or {@code null} while the account is active
     */
    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    /**
     * Returns the interface preferences.
     *
     * @return the language and theme
     */
    public Preferences getPreferences() {
        return preferences;
    }

    /**
     * Changes the personal details shown on the Profile view.
     *
     * @param firstName new first name
     * @param lastName  new last name
     * @param email     new email address, stored lowercase
     * @param phone     new phone, or {@code null}
     * @param address   new postal address, or {@code null}
     * @throws IllegalArgumentException with a {@link ModelKeys} key if a value is invalid
     */
    public void changeProfile(String firstName, String lastName, String email, Phone phone, Address address) {
        String checkedFirstName = Checks.text(firstName, 1, MAX_FIRST_NAME_LENGTH, ModelKeys.USER_FIRST_NAME);
        String checkedLastName = Checks.text(lastName, 1, MAX_LAST_NAME_LENGTH, ModelKeys.USER_LAST_NAME);
        String checkedEmail = normalizeEmail(email);
        checkAddress(address);
        this.firstName = checkedFirstName;
        this.lastName = checkedLastName;
        this.email = checkedEmail;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Replaces the password hash, after the user chose a new password.
     *
     * @param passwordHash Argon2id hash of the new password
     * @throws IllegalArgumentException with {@link ModelKeys#USER_PASSWORD_HASH} if it is not an Argon2id hash
     */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = checkedPasswordHash(passwordHash);
    }

    /**
     * Changes the interface preferences.
     *
     * @param preferences the new language and theme
     */
    public void changePreferences(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    /**
     * Changes the profile picture.
     *
     * @param avatarId id of the new image in {@code media}, or {@code null} to remove it
     */
    public void changeAvatar(ObjectId avatarId) {
        this.avatarId = avatarId;
    }

    /**
     * Deactivates the account ("dar de baja"). The document stays, so orders and reviews keep their references.
     * Deactivating an account twice keeps the first deactivation time.
     *
     * @param now the current time
     */
    public void deactivate(Instant now) {
        if (isActive()) {
            status = AccountStatus.DEACTIVATED;
            deactivatedAt = Objects.requireNonNull(now, "now");
        }
    }

    /**
     * Checks an address against the rules of this kind of account. Players and developers need one; admins don't.
     *
     * @param address the address to check, or {@code null}
     * @throws IllegalArgumentException with {@link ModelKeys#USER_ADDRESS} if this account needs an address
     */
    protected void checkAddress(Address address) {
        // Admins may have no address; Player and Developer require one.
    }
}
