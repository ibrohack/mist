package com.mist.service;

/**
 * Bundle keys of the messages the services report. Each one exists in {@code messages_es.properties} and
 * {@code messages_en.properties}.
 *
 * <p>Model rule violations keep their own {@code model.*} key; these keys cover the rules that need the database or
 * the logged-in user.</p>
 */
public final class ServiceKeys {

    /** Input broke a rule that has no more specific message. */
    public static final String INVALID_INPUT = "service.invalidInput";

    /** The email and password do not match an account. */
    public static final String AUTH_BAD_CREDENTIALS = "service.auth.badCredentials";
    /** The current password typed to confirm a change is wrong. */
    public static final String AUTH_WRONG_PASSWORD = "service.auth.wrongPassword";
    /** The new password is too short, too long, or lacks a letter or a digit. */
    public static final String AUTH_WEAK_PASSWORD = "service.auth.weakPassword";

    /** The account was deactivated, so it can no longer log in or act. */
    public static final String ACCOUNT_DEACTIVATED = "service.account.deactivated";
    /** The account does not exist. */
    public static final String USER_NOT_FOUND = "service.user.notFound";
    /** Only a player may do this. */
    public static final String USER_NOT_PLAYER = "service.user.notPlayer";
    /** Only a developer may do this. */
    public static final String USER_NOT_DEVELOPER = "service.user.notDeveloper";
    /** Another account already uses the email address. */
    public static final String USER_EMAIL_TAKEN = "service.user.emailTaken";
    /** Another player already uses the nickname. */
    public static final String USER_NICKNAME_TAKEN = "service.user.nicknameTaken";
    /** Another developer already uses the studio name. */
    public static final String USER_STUDIO_NAME_TAKEN = "service.user.studioNameTaken";

    /** The game does not exist. */
    public static final String GAME_NOT_FOUND = "service.game.notFound";
    /** The game is not on sale. */
    public static final String GAME_NOT_PURCHASABLE = "service.game.notPurchasable";
    /** The developer does not own the game. */
    public static final String GAME_NOT_OWNER = "service.game.notOwner";
    /** The player does not have the game in the library. */
    public static final String GAME_NOT_OWNED = "service.game.notOwned";

    /** The image does not exist. */
    public static final String MEDIA_NOT_FOUND = "service.media.notFound";

    /** The cart is empty. */
    public static final String STORE_EMPTY_CART = "service.store.emptyCart";

    /** The order does not exist or belongs to someone else. */
    public static final String ORDER_NOT_FOUND = "service.order.notFound";
    /** The return period of the game is over, or it was already returned. */
    public static final String LIBRARY_REFUND_CLOSED = "service.library.refundClosed";

    /** The player already reviewed the game. */
    public static final String REVIEW_ALREADY_EXISTS = "service.review.alreadyExists";

    private ServiceKeys() {
    }
}
