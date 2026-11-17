package com.mist.model;

/**
 * Bundle keys of the messages the model raises when an invariant is broken.
 *
 * <p>Model factories and mutators throw {@link IllegalArgumentException} for invalid input and
 * {@link IllegalStateException} for an operation the current state does not allow. Either way the exception message is
 * one of these keys, so a service can wrap it in a {@code ValidationException} and the UI shows it in the user's
 * language. Every key exists in {@code i18n/messages_es.properties} and {@code i18n/messages_en.properties}.</p>
 */
public final class ModelKeys {

    /** An amount of money is missing or negative. */
    public static final String AMOUNT = "model.amount";

    /** A first name is blank or too long. */
    public static final String USER_FIRST_NAME = "model.user.firstName";
    /** A last name is blank or too long. */
    public static final String USER_LAST_NAME = "model.user.lastName";
    /** An email address is malformed or too long. */
    public static final String USER_EMAIL = "model.user.email";
    /** A password hash is not an Argon2id hash. */
    public static final String USER_PASSWORD_HASH = "model.user.passwordHash";

    /** A phone country code is not a {@code +} followed by 1 to 3 digits. */
    public static final String PHONE_COUNTRY_CODE = "model.phone.countryCode";
    /** A phone number is not 4 to 14 digits. */
    public static final String PHONE_NUMBER = "model.phone.number";

    /** A street is blank or too long. */
    public static final String ADDRESS_STREET = "model.address.street";
    /** The address details (floor, door) are too long. */
    public static final String ADDRESS_DETAILS = "model.address.details";
    /** A postal code is malformed. */
    public static final String ADDRESS_POSTAL_CODE = "model.address.postalCode";
    /** A city is blank or too long. */
    public static final String ADDRESS_CITY = "model.address.city";
    /** A province is too long. */
    public static final String ADDRESS_PROVINCE = "model.address.province";
    /** A country is not a two-letter ISO 3166-1 code. */
    public static final String ADDRESS_COUNTRY = "model.address.country";

    /** A card holder name is blank or too long. */
    public static final String CARD_HOLDER = "model.card.holder";
    /** A card number is malformed or fails the Luhn check. */
    public static final String CARD_NUMBER = "model.card.number";
    /** A card expiry month or year is out of range. */
    public static final String CARD_EXPIRY = "model.card.expiry";
    /** A card has already expired. */
    public static final String CARD_EXPIRED = "model.card.expired";

    /** A nickname has the wrong length or characters. */
    public static final String PLAYER_NICKNAME = "model.player.nickname";
    /** A player has no payment card. */
    public static final String PLAYER_CARD = "model.player.card";

    /** A studio name is blank or too long. */
    public static final String DEVELOPER_STUDIO_NAME = "model.developer.studioName";
    /** A bank account is not a valid IBAN. */
    public static final String DEVELOPER_BANK_ACCOUNT = "model.developer.bankAccount";

    /** An image is not PNG, JPEG or GIF. */
    public static final String MEDIA_TYPE = "model.media.type";
    /** An image is empty or larger than the limit. */
    public static final String MEDIA_SIZE = "model.media.size";
    /** An image file name is blank or too long. */
    public static final String MEDIA_FILE_NAME = "model.media.fileName";

    /** A game name is blank or too long. */
    public static final String GAME_NAME = "model.game.name";
    /** A game description is too long. */
    public static final String GAME_DESCRIPTION = "model.game.description";
    /** A game price is out of range. */
    public static final String GAME_PRICE = "model.game.price";
    /** A game has too many tags, or a tag is blank or too long. */
    public static final String GAME_TAGS = "model.game.tags";
    /** A game has too many languages, or a language is not a two-letter ISO 639-1 code. */
    public static final String GAME_LANGUAGES = "model.game.languages";
    /** A game has too many screenshots. */
    public static final String GAME_SCREENSHOTS = "model.game.screenshots";
    /** A game has too many achievements. */
    public static final String GAME_ACHIEVEMENTS = "model.game.achievements";
    /** A draft lacks what publishing needs: cover, description, release date, PEGI or requirements. */
    public static final String GAME_INCOMPLETE = "model.game.incomplete";
    /** Only a draft can be published. */
    public static final String GAME_NOT_DRAFT = "model.game.notDraft";
    /** Only a published game can be delisted. */
    public static final String GAME_NOT_PUBLISHED = "model.game.notPublished";
    /** An achievement name or description is blank or too long. */
    public static final String ACHIEVEMENT = "model.achievement";
    /** A system requirement is blank or too long. */
    public static final String REQUIREMENTS = "model.requirements";

    /** An order has no items or too many. */
    public static final String ORDER_ITEMS = "model.order.items";
    /** An order holds the same game twice. */
    public static final String ORDER_DUPLICATE_GAME = "model.order.duplicateGame";
    /** A game is not part of the order. */
    public static final String ORDER_GAME_NOT_IN_ORDER = "model.order.gameNotInOrder";
    /** A game of the order was already refunded. */
    public static final String ORDER_ALREADY_REFUNDED = "model.order.alreadyRefunded";

    /** A rating is not between 1 and 5. */
    public static final String REVIEW_RATING = "model.review.rating";
    /** A review comment is too long. */
    public static final String REVIEW_COMMENT = "model.review.comment";

    private ModelKeys() {
    }
}
