package com.mist.model;

/**
 * Whether a user account can still be used.
 */
public enum AccountStatus {

    /** The user can log in and use MIST. */
    ACTIVE,

    /** The user deactivated the account; it stays stored so orders and reviews keep their references. */
    DEACTIVATED
}
