package com.mist.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.Instant;

/**
 * An administrator: moderates users and reviews and reads the database operation log. Stored in {@code users} with
 * role {@code ADMIN}.
 *
 * <p>Admins are not registered from the app; they come from the seed data. They need no address.</p>
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    /** Used by Hibernate. */
    protected Admin() {
    }

    /**
     * Creates an admin.
     *
     * @param firstName    the first name
     * @param lastName     the last name
     * @param email        the email
     * @param passwordHash the Argon2id hash
     * @param createdAt    when the account was created
     */
    private Admin(String firstName, String lastName, String email, String passwordHash, Instant createdAt) {
        super(firstName, lastName, email, passwordHash, null, null, createdAt);
    }

    /**
     * Creates an administrator account.
     *
     * @param firstName    first name
     * @param lastName     last name
     * @param email        email address, which is also the login
     * @param passwordHash Argon2id hash of the password
     * @param now          the current time
     * @return the new, active administrator
     * @throws IllegalArgumentException with a {@link ModelKeys} key if a value is invalid
     */
    public static Admin create(String firstName, String lastName, String email, String passwordHash, Instant now) {
        return new Admin(firstName, lastName, email, passwordHash, now);
    }

    @Override
    public Role role() {
        return Role.ADMIN;
    }
}
