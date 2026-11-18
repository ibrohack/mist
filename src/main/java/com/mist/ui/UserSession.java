package com.mist.ui;

import com.mist.model.Role;
import com.mist.model.User;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import org.bson.types.ObjectId;

import java.util.Objects;
import java.util.Optional;

/**
 * Holds who is using MIST: the user who logged in, a guest who browses the store, or nobody (before login).
 *
 * <p>The services are stateless and take the acting user's id, so every controller reads it from here. Views that
 * show user data, such as the user menu, listen to {@link #versionProperty()}, which changes on every login, update
 * and logout.</p>
 */
public final class UserSession {

    /** The logged-in user, or {@code null} for a guest or nobody. */
    private User user;
    /** Whether a guest is browsing without an account. */
    private boolean guest;
    /** Counts the changes of the session, so views can refresh on any of them. */
    private final ReadOnlyLongWrapper version = new ReadOnlyLongWrapper(this, "version");

    /**
     * Starts the session of a user who just logged in.
     *
     * @param user the user returned by the login
     */
    public void start(User user) {
        this.user = Objects.requireNonNull(user, "user");
        this.guest = false;
        changed();
    }

    /** Starts a guest session, which can browse the store but not buy. */
    public void startGuest() {
        this.user = null;
        this.guest = true;
        changed();
    }

    /**
     * Replaces the logged-in user with a fresher copy, after a change saved by a service.
     *
     * @param updated the user as the service returned it
     * @throws IllegalStateException if nobody is logged in, or {@code updated} is another account
     */
    public void update(User updated) {
        Objects.requireNonNull(updated, "updated");
        if (user == null || !Objects.equals(user.getId(), updated.getId())) {
            throw new IllegalStateException("The updated user is not the logged-in user");
        }
        this.user = updated;
        changed();
    }

    /** Ends the session, after logout or deactivation. */
    public void end() {
        this.user = null;
        this.guest = false;
        changed();
    }

    /**
     * Returns the logged-in user.
     *
     * @return the user, or empty for a guest or nobody
     */
    public Optional<User> user() {
        return Optional.ofNullable(user);
    }

    /**
     * Returns the logged-in user, for screens that only logged-in users reach.
     *
     * @return the user
     * @throws IllegalStateException if nobody is logged in
     */
    public User requireUser() {
        if (user == null) {
            throw new IllegalStateException("Nobody is logged in");
        }
        return user;
    }

    /**
     * Returns the id the services need as the acting user.
     *
     * @return the logged-in user's id
     * @throws IllegalStateException if nobody is logged in
     */
    public ObjectId userId() {
        return requireUser().getId();
    }

    /**
     * Tells whether a user is logged in.
     *
     * @return {@code true} after a login, until logout
     */
    public boolean isLoggedIn() {
        return user != null;
    }

    /**
     * Tells whether a guest is browsing.
     *
     * @return {@code true} in a guest session
     */
    public boolean isGuest() {
        return guest;
    }

    /**
     * Tells whether the logged-in user has a role.
     *
     * @param role the role to check
     * @return {@code true} if a user with that role is logged in
     */
    public boolean hasRole(Role role) {
        return user != null && user.role() == role;
    }

    /**
     * Returns a property that changes on every login, update and logout.
     *
     * @return the session version
     */
    public ReadOnlyLongProperty versionProperty() {
        return version.getReadOnlyProperty();
    }

    /** Signals a change to the views that listen. */
    private void changed() {
        version.set(version.get() + 1);
    }
}
