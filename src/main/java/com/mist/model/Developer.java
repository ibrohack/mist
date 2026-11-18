package com.mist.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A developer ("desarrollador"): publishes games under a studio name and is paid into a bank account. Stored in
 * {@code users} with role {@code DEVELOPER}.
 *
 * <p>{@code gameIds} is the developer's catalog and {@code totalRevenue} the income from every sale minus returns;
 * both are kept in step by the publish, purchase and return transactions.</p>
 */
@Entity
@DiscriminatorValue("DEVELOPER")
public class Developer extends User {

    /** Longest studio name accepted. */
    public static final int MAX_STUDIO_NAME_LENGTH = 80;

    /** The shape of an IBAN, without spaces. */
    private static final Pattern IBAN = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");

    /** The studio name, unique among developers. */
    private String studioName;
    /** The IBAN sales are paid into. */
    private String bankAccount;
    /** The developer's published games. */
    private List<ObjectId> gameIds;
    /** What the developer's games have earned, after refunds. */
    private BigDecimal totalRevenue;

    /** Used by Hibernate. */
    protected Developer() {
    }

    /**
     * Creates a developer.
     *
     * @param firstName    the first name
     * @param lastName     the last name
     * @param email        the email
     * @param passwordHash the Argon2id hash
     * @param phone        the phone, or {@code null}
     * @param address      the address
     * @param studioName   the studio name
     * @param bankAccount  the IBAN
     * @param createdAt    when the account was created
     */
    private Developer(String firstName, String lastName, String email, String passwordHash, Phone phone,
                      Address address, String studioName, String bankAccount, Instant createdAt) {
        super(firstName, lastName, email, passwordHash, phone, Checks.present(address, ModelKeys.USER_ADDRESS),
                createdAt);
        this.studioName = checkedStudioName(studioName);
        this.bankAccount = normalizeIban(bankAccount);
        this.gameIds = List.of();
        this.totalRevenue = BigDecimal.ZERO.setScale(2);
    }

    /**
     * Registers a new developer.
     *
     * @param firstName    first name of the account holder
     * @param lastName     last name of the account holder
     * @param email        email address, which is also the login
     * @param passwordHash Argon2id hash of the password
     * @param phone        phone, or {@code null}
     * @param address      postal address
     * @param studioName   name shown on the developer's games
     * @param bankAccount  IBAN the revenue is paid into; spaces are ignored
     * @param now          the current time
     * @return the new, active developer with an empty catalog and no revenue
     * @throws IllegalArgumentException with a {@link ModelKeys} key if a value is invalid
     */
    public static Developer register(String firstName, String lastName, String email, String passwordHash,
                                     Phone phone, Address address, String studioName, String bankAccount,
                                     Instant now) {
        return new Developer(firstName, lastName, email, passwordHash, phone, address, studioName, bankAccount, now);
    }

    /**
     * Normalizes an IBAN the way MIST stores it, without spaces and in upper case, and checks its mod-97 digits.
     *
     * @param iban the IBAN as typed
     * @return the normalized IBAN
     * @throws IllegalArgumentException with {@link ModelKeys#DEVELOPER_BANK_ACCOUNT} if it is not a valid IBAN
     */
    public static String normalizeIban(String iban) {
        String normalized = iban == null ? "" : iban.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        Checks.require(IBAN.matcher(normalized).matches() && mod97(normalized) == 1,
                ModelKeys.DEVELOPER_BANK_ACCOUNT);
        return normalized;
    }

    /**
     * Computes the IBAN checksum.
     *
     * @param iban the IBAN without spaces
     * @return the remainder of the rearranged IBAN modulo 97, 1 when valid
     */
    private static int mod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int remainder = 0;
        for (char c : rearranged.toCharArray()) {
            int value = Character.isDigit(c) ? c - '0' : c - 'A' + 10;
            remainder = (value < 10 ? remainder * 10 : remainder * 100) + value;
            remainder %= 97;
        }
        return remainder;
    }

    /**
     * Checks a studio name.
     *
     * @param studioName the name
     * @return the name without surrounding spaces
     */
    private static String checkedStudioName(String studioName) {
        return Checks.text(studioName, 1, MAX_STUDIO_NAME_LENGTH, ModelKeys.DEVELOPER_STUDIO_NAME);
    }

    @Override
    public Role role() {
        return Role.DEVELOPER;
    }

    @Override
    protected void checkAddress(Address address) {
        Checks.present(address, ModelKeys.USER_ADDRESS);
    }

    /**
     * Returns the name shown on the developer's games.
     *
     * @return the studio name
     */
    public String getStudioName() {
        return studioName;
    }

    /**
     * Returns the account the revenue is paid into.
     *
     * @return the IBAN, without spaces
     */
    public String getBankAccount() {
        return bankAccount;
    }

    /**
     * Returns the developer's catalog.
     *
     * @return an unmodifiable view of the ids of every game the developer created, in creation order
     */
    public List<ObjectId> getGameIds() {
        return gameIds == null ? List.of() : Collections.unmodifiableList(gameIds);
    }

    /**
     * Returns the income from every sale minus returns.
     *
     * @return the total revenue, at scale 2
     */
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    /**
     * Changes the name shown on the developer's games.
     *
     * @param studioName the new studio name
     * @throws IllegalArgumentException with {@link ModelKeys#DEVELOPER_STUDIO_NAME} if it is blank or too long
     */
    public void changeStudioName(String studioName) {
        this.studioName = checkedStudioName(studioName);
    }

    /**
     * Changes the account the revenue is paid into.
     *
     * @param bankAccount the new IBAN; spaces are ignored
     * @throws IllegalArgumentException with {@link ModelKeys#DEVELOPER_BANK_ACCOUNT} if it is not a valid IBAN
     */
    public void changeBankAccount(String bankAccount) {
        this.bankAccount = normalizeIban(bankAccount);
    }

    /**
     * Adds a newly created game to the catalog.
     *
     * @param gameId the game
     * @return {@code true} if the catalog changed; {@code false} if the game was already in it
     */
    public boolean addGame(ObjectId gameId) {
        Objects.requireNonNull(gameId, "gameId");
        if (getGameIds().contains(gameId)) {
            return false;
        }
        List<ObjectId> catalog = new ArrayList<>(getGameIds());
        catalog.add(gameId);
        gameIds = List.copyOf(catalog);
        return true;
    }

    /**
     * Adds the price of a sold game to the revenue.
     *
     * @param amount the price paid
     * @throws IllegalArgumentException with {@link ModelKeys#AMOUNT} if it is missing or negative
     */
    public void recordSale(BigDecimal amount) {
        totalRevenue = revenue().add(Checks.money(amount, ModelKeys.AMOUNT));
    }

    /**
     * Takes the price of a returned game off the revenue.
     *
     * @param amount the price refunded
     * @throws IllegalArgumentException with {@link ModelKeys#AMOUNT} if it is missing, negative, or larger than the
     *                                  revenue
     */
    public void recordRefund(BigDecimal amount) {
        BigDecimal refund = Checks.money(amount, ModelKeys.AMOUNT);
        Checks.require(refund.compareTo(revenue()) <= 0, ModelKeys.AMOUNT);
        totalRevenue = revenue().subtract(refund);
    }

    /**
     * Returns the revenue, zero if not set yet.
     *
     * @return the total revenue
     */
    private BigDecimal revenue() {
        return totalRevenue == null ? BigDecimal.ZERO.setScale(2) : totalRevenue;
    }
}
