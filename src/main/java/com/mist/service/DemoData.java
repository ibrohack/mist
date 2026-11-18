package com.mist.service;

import com.mist.dao.GameDao;
import com.mist.dao.MediaDao;
import com.mist.dao.OrderDao;
import com.mist.dao.ReviewDao;
import com.mist.dao.TransactionRunner;
import com.mist.dao.UserDao;
import com.mist.exception.MistException;
import com.mist.logging.DbOperationLogger;
import com.mist.model.Address;
import com.mist.model.Admin;
import com.mist.model.Developer;
import com.mist.model.Game;
import com.mist.model.PaymentCard;
import com.mist.model.Pegi;
import com.mist.model.Phone;
import com.mist.model.Player;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fills an empty database with demo content, so MIST never starts empty: an admin, two developers with a catalog of
 * twelve games (one of them still a draft), and three players with orders, a cart, a wishlist and reviews.
 *
 * <p>It is the only way an {@link Admin} account is created. Games, orders and reviews go through the real services,
 * so they pass the same rules and transactions as the app. One order of each of two players is 20 days old, so the
 * library shows both returnable and no longer returnable games. Covers and screenshots are drawn with Java2D.</p>
 *
 * <p>Every demo account uses the password {@value #PASSWORD}.</p>
 */
public final class DemoData {

    /** Email of the demo admin, who can read the operation log. */
    public static final String ADMIN_EMAIL = "admin@mist.local";
    /** Email of the demo player with the most data. */
    public static final String PLAYER_EMAIL = "lucia@mist.local";
    /** Email of a second demo player. */
    public static final String SECOND_PLAYER_EMAIL = "marc@mist.local";
    /** Email of a third demo player. */
    public static final String THIRD_PLAYER_EMAIL = "nora@mist.local";
    /** Email of the demo developer with the most games. */
    public static final String DEVELOPER_EMAIL = "nebula@mist.local";
    /** Email of a second demo developer. */
    public static final String SECOND_DEVELOPER_EMAIL = "lumen@mist.local";
    /** The password of every demo account. */
    public static final String PASSWORD = "mist2026";
    /** Name of the game left as a draft, for the editor demo. */
    public static final String DRAFT_GAME = "The Quiet Signal";

    /** How old the older demo orders are, beyond the 14-day refund window. */
    private static final Duration OLD_ORDER_AGE = Duration.ofDays(20);
    /** Images already drawn, by name and size, so repeated seeding (as in UI tests) draws each once. */
    private static final Map<String, byte[]> DRAWN = new ConcurrentHashMap<>();

    /**
     * One game of the demo catalog.
     *
     * @param name        the name
     * @param price       the price in euros
     * @param developer   0 for the first developer, 1 for the second
     * @param daysAgo     how many days ago it was released
     * @param pegi        its age rating
     * @param tags        its tags
     * @param languages   its languages, as two-letter codes
     * @param description its description
     * @param colors      the two colors of its art, as RGB
     * @param published   whether it is on sale
     */
    private record Seed(String name, String price, int developer, int daysAgo, Pegi pegi, List<String> tags,
                        List<String> languages, String description, int[] colors, boolean published) {
    }

    /** The demo catalog. */
    private static final List<Seed> CATALOG = List.of(
            new Seed("Ashen Lanterns", "29.99", 0, 400, Pegi.PEGI_16, List.of("rpg", "dark fantasy", "action"),
                    List.of("en", "es", "fr"), "Carry the last lantern through a kingdom that burned down a century "
                    + "ago. Every ember you rekindle opens a path, and wakes something that wanted the dark.",
                    new int[] {0x2B1A12, 0xE07A2F}, true),
            new Seed("Tidebreak Harbor", "24.99", 0, 180, Pegi.PEGI_7, List.of("strategy", "city builder"),
                    List.of("en", "es", "de"), "Build a fishing town on stilts, trade with passing ships and keep the "
                    + "harbor alive through storms that rearrange the coastline every season.",
                    new int[] {0x0F2C3F, 0x4FB3BF}, true),
            new Seed("Neon Courier 2088", "14.99", 0, 45, Pegi.PEGI_12, List.of("racing", "arcade"),
                    List.of("en", "es", "ja"), "Deliver parcels across a vertical city at night. Shortcuts, rival "
                    + "couriers and a soundtrack that speeds up with you.",
                    new int[] {0x16062B, 0xFF2E97}, true),
            new Seed("Moss & Marrow", "0.00", 0, 300, Pegi.PEGI_3, List.of("farming", "cozy", "free to play"),
                    List.of("en", "es"), "Tend a garden that grows on the back of a sleeping giant. Free to play, "
                    + "no timers, no hurry.",
                    new int[] {0x1E2F17, 0x9BD35A}, true),
            new Seed("Gravewind Tactics", "34.99", 0, 20, Pegi.PEGI_16, List.of("tactics", "turn-based"),
                    List.of("en", "es", "it"), "Command a band of deserters across a frozen front in turn-based "
                    + "battles where the wind decides who hits.",
                    new int[] {0x1B1F2A, 0x9AA7C7}, true),
            new Seed("Orbital Gardeners", "19.99", 0, 90, Pegi.PEGI_7, List.of("co-op", "simulation"),
                    List.of("en", "es", "pt"), "Grow food for a station of four hundred people with up to three "
                    + "friends. Light, water and patience are all in short supply.",
                    new int[] {0x0B1E14, 0x5CE1A6}, true),
            new Seed(DRAFT_GAME, "9.99", 0, 0, Pegi.PEGI_12, List.of("narrative", "mystery"),
                    List.of("en", "es"), "A radio operator on a lighthouse hears a message that has not been sent "
                    + "yet. Coming soon.",
                    new int[] {0x101418, 0xC6FF3D}, false),
            new Seed("Paper Kites", "0.00", 1, 600, Pegi.PEGI_3, List.of("puzzle", "platformer", "free to play"),
                    List.of("en", "es", "fr", "de"), "Fold the level to carry a paper kite home. Short, gentle and "
                    + "free.",
                    new int[] {0x2A2140, 0xF6D365}, true),
            new Seed("Ironvale Siege", "39.99", 1, 120, Pegi.PEGI_16, List.of("strategy", "rts"),
                    List.of("en", "es", "de", "pl"), "Real-time sieges with destructible walls, supply lines and "
                    + "a campaign that remembers which towns you burned.",
                    new int[] {0x231A14, 0xB08D57}, true),
            new Seed("Sunken Choir", "17.99", 1, 60, Pegi.PEGI_18, List.of("horror", "exploration"),
                    List.of("en", "es"), "Explore a flooded cathedral where the choir never stopped singing. "
                    + "Play with headphones.",
                    new int[] {0x050B12, 0x3C6E71}, true),
            new Seed("Pixel Pantry", "4.99", 1, 30, Pegi.PEGI_3, List.of("cooking", "casual"),
                    List.of("en", "es", "fr"), "Run a tiny restaurant where every recipe is a sliding puzzle.",
                    new int[] {0x3A1F1D, 0xFF8C61}, true),
            new Seed("Starlit Drift", "59.99", 1, 7, Pegi.PEGI_12, List.of("space", "exploration", "open world"),
                    List.of("en", "es", "fr", "de", "ja"), "Chart a galaxy that drifts while you sleep. Every star "
                    + "system is handmade, and none of them stays where you left it.",
                    new int[] {0x070A1F, 0x7B6CF6}, true));

    /** The users collection. */
    private final UserDao users;
    /** The games collection. */
    private final GameDao games;
    /** The orders collection. */
    private final OrderDao orders;
    /** The reviews collection. */
    private final ReviewDao reviews;
    /** The media collection. */
    private final MediaDao media;
    /** Runs the multi-document changes. */
    private final TransactionRunner transactions;
    /** The clock everything is dated with. */
    private final Clock clock;

    /**
     * Creates the seeder on the data access layer.
     *
     * @param users        the users DAO
     * @param games        the games DAO
     * @param orders       the orders DAO
     * @param reviews      the reviews DAO
     * @param media        the media DAO
     * @param transactions the transaction runner
     * @param clock        the clock everything is dated with
     */
    public DemoData(UserDao users, GameDao games, OrderDao orders, ReviewDao reviews, MediaDao media,
                    TransactionRunner transactions, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.games = Objects.requireNonNull(games, "games");
        this.orders = Objects.requireNonNull(orders, "orders");
        this.reviews = Objects.requireNonNull(reviews, "reviews");
        this.media = Objects.requireNonNull(media, "media");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Seeds the demo content unless the demo admin already exists.
     *
     * @return {@code true} if the content was created now
     * @throws MistException if the database fails or a demo value breaks a rule
     */
    public boolean seedIfEmpty() throws MistException {
        if (users.existsByEmail(ADMIN_EMAIL)) {
            return false;
        }
        DbOperationLogger.setUser("demo-data");
        try {
            seed();
        } finally {
            DbOperationLogger.clearUser();
        }
        return true;
    }

    /**
     * Creates every demo account, game, order and review.
     *
     * @throws MistException if the database fails or a demo value breaks a rule
     */
    private void seed() throws MistException {
        // One hash for every demo account: they share the documented demo password.
        String hash = new PasswordHasher().hash(PASSWORD.toCharArray());
        users.save(Admin.create("Ada", "Admin", ADMIN_EMAIL, hash, clock.instant()));
        List<Developer> developers = List.of(
                users.save(Developer.register("Iris", "Navarro", DEVELOPER_EMAIL, hash, Phone.of("+34", "611222333"),
                        Address.of("Calle de la Luna 7", "2B", "28004", "Madrid", "Madrid", "ES"), "Nebula Forge",
                        "ES9121000418450200051332", clock.instant())),
                users.save(Developer.register("Jonas", "Weber", SECOND_DEVELOPER_EMAIL, hash, Phone.of("+49", "301234567"),
                        Address.of("Torstrasse 12", null, "10119", "Berlin", null, "DE"), "Lumen Works",
                        "DE89370400440532013000", clock.instant())));
        Player lucia = users.save(player("Lucía", "Romero", PLAYER_EMAIL, "lucia", hash));
        Player marc = users.save(player("Marc", "Ferrer", SECOND_PLAYER_EMAIL, "marc_f", hash));
        Player nora = users.save(player("Nora", "Ibarra", THIRD_PLAYER_EMAIL, "nora.plays", hash));

        Map<String, Game> catalog = new HashMap<>();
        PublishingService publishing = new PublishingService(games, users, media, transactions, clock);
        for (Seed seed : CATALOG) {
            catalog.put(seed.name(), publish(publishing, developers.get(seed.developer()), seed));
        }

        StoreService past = new StoreService(games, users, orders, transactions, Clock.offset(clock,
                OLD_ORDER_AGE.negated()));
        StoreService today = new StoreService(games, users, orders, transactions, clock);
        buy(past, lucia, catalog, "Ashen Lanterns", "Paper Kites");
        buy(today, lucia, catalog, "Tidebreak Harbor", "Neon Courier 2088");
        buy(today, marc, catalog, "Ashen Lanterns", "Ironvale Siege");
        buy(past, nora, catalog, "Moss & Marrow", "Sunken Choir");
        today.addToWishlist(lucia.getId(), catalog.get("Starlit Drift").getId());
        today.addToCart(lucia.getId(), catalog.get("Gravewind Tactics").getId());

        ReviewService ratings = new ReviewService(reviews, users, games, transactions, clock);
        ratings.rate(lucia.getId(), catalog.get("Ashen Lanterns").getId(), 5,
                "The lantern mechanic never gets old. Hard, fair and beautiful.");
        ratings.rate(lucia.getId(), catalog.get("Paper Kites").getId(), 4, "Short and sweet. Perfect for an evening.");
        ratings.rate(marc.getId(), catalog.get("Ashen Lanterns").getId(), 4,
                "Great atmosphere; the second act drags a little.");
        ratings.rate(marc.getId(), catalog.get("Ironvale Siege").getId(), 5, "Best siege RTS in years.");
        ratings.rate(nora.getId(), catalog.get("Sunken Choir").getId(), 3, "Scary, but the puzzles are obscure.");
        ratings.rate(nora.getId(), catalog.get("Moss & Marrow").getId(), 5, null);
    }

    /**
     * Builds a demo player with an address in Spain and a test card.
     *
     * @param firstName the first name
     * @param lastName  the last name
     * @param email     the email
     * @param nickname  the nickname
     * @param hash      the password hash
     * @return the player, not saved yet
     */
    private Player player(String firstName, String lastName, String email, String nickname, String hash) {
        PaymentCard card = PaymentCard.of(firstName + " " + lastName, "4111111111111111", 12, 2030,
                YearMonth.now(clock));
        return Player.register(firstName, lastName, email, hash, Phone.of("+34", "600123456"),
                Address.of("Calle Mayor 1", "3A", "28013", "Madrid", "Madrid", "ES"), nickname, card,
                clock.instant());
    }

    /**
     * Creates one demo game with its art, and publishes it if the seed says so.
     *
     * @param publishing the publishing service
     * @param developer  the game's developer
     * @param seed       the game's data
     * @return the game
     * @throws MistException if a value breaks a rule
     */
    private Game publish(PublishingService publishing, Developer developer, Seed seed) throws MistException {
        Game game = publishing.createDraft(developer.getId(), seed.name(), new BigDecimal(seed.price()));
        LocalDate released = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).minusDays(seed.daysAgo());
        List<AchievementForm> achievements = List.of(
                new AchievementForm("First steps", "Finish the opening chapter of " + seed.name() + "."),
                new AchievementForm("Collector", "Find every hidden item in one region."),
                new AchievementForm("No way back", "Finish the game on the hardest difficulty."));
        game = publishing.update(developer.getId(), game.getId(), new GameForm(seed.name(), seed.description(),
                new BigDecimal(seed.price()), released, seed.pegi(), requirements(seed.pegi()), seed.tags(),
                seed.languages(), achievements));
        Color dark = new Color(seed.colors()[0]);
        Color light = new Color(seed.colors()[1]);
        game = publishing.changeCover(developer.getId(), game.getId(),
                new ImageUpload("image/jpeg", art(seed.name(), dark, light, 600, 800, false), "cover.jpg"));
        for (int i = 1; i <= 2; i++) {
            game = publishing.addScreenshot(developer.getId(), game.getId(), new ImageUpload("image/jpeg",
                    art(seed.name() + " #" + i, dark, light, 1280, 720, true), "screenshot-" + i + ".jpg"));
        }
        return seed.published() ? publishing.publish(developer.getId(), game.getId()) : game;
    }

    /**
     * Puts games in a player's cart and checks out.
     *
     * @param store   the store service, dated now or in the past
     * @param player  the buyer
     * @param catalog the games by name
     * @param names   the games to buy
     * @throws MistException if the purchase breaks a rule
     */
    private static void buy(StoreService store, Player player, Map<String, Game> catalog, String... names)
            throws MistException {
        for (String name : names) {
            store.addToCart(player.getId(), catalog.get(name).getId());
        }
        store.checkout(player.getId());
    }

    /**
     * Returns system requirements that grow with the game's age rating, as heavier games tend to.
     *
     * @param pegi the age rating
     * @return the requirements
     */
    private static RequirementsForm requirements(Pegi pegi) {
        boolean heavy = pegi.minimumAge() >= 16;
        return new RequirementsForm("Windows 10 64-bit", heavy ? "Intel Core i5-8400" : "Intel Core i3-6100",
                heavy ? "16 GB RAM" : "8 GB RAM", heavy ? "NVIDIA GeForce GTX 1660" : "Intel UHD 620",
                heavy ? "60 GB" : "4 GB");
    }

    /**
     * Draws a cover or a screenshot: a gradient in the game's colors, brand rings and the title, as a JPEG.
     *
     * @param title     the text on the image
     * @param dark      the background color
     * @param light     the accent color
     * @param width     the width in pixels
     * @param height    the height in pixels
     * @param landscape whether to draw a horizon, for screenshots
     * @return the JPEG bytes
     */
    static byte[] art(String title, Color dark, Color light, int width, int height, boolean landscape) {
        return DRAWN.computeIfAbsent(title + "@" + width + "x" + height, key -> {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, dark, width, height, light.darker()));
                g.fillRect(0, 0, width, height);
                g.setStroke(new BasicStroke(Math.max(2, width / 200f)));
                g.setColor(new Color(light.getRed(), light.getGreen(), light.getBlue(), 90));
                int size = Math.min(width, height);
                for (int ring = 1; ring <= 3; ring++) {
                    int r = size * ring / 4;
                    g.drawOval(width * 2 / 3 - r, height / 3 - r, 2 * r, 2 * r);
                }
                g.setColor(light);
                g.fillOval(width * 2 / 3 - size / 10, height / 3 - size / 10, size / 5, size / 5);
                if (landscape) {
                    g.setColor(new Color(0, 0, 0, 120));
                    g.fillPolygon(new int[] {0, width / 4, width / 2, width * 3 / 4, width, width, 0},
                            new int[] {height * 3 / 4, height / 2, height * 2 / 3, height / 2, height * 3 / 4, height,
                                    height}, 7);
                }
                drawTitle(g, title, light, width, height);
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "jpg", out);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot encode demo art for " + title, e);
            }
            return out.toByteArray();
        }).clone();
    }

    /**
     * Writes the title in the lower part of the image, wrapped to its width.
     *
     * @param g      the graphics
     * @param title  the text
     * @param color  the text color
     * @param width  the image width
     * @param height the image height
     */
    private static void drawTitle(Graphics2D g, String title, Color color, int width, int height) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(24, width / 11)));
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : title.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > width * 0.85 && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        lines.add(line.toString());
        int y = height - height / 10 - (lines.size() - 1) * metrics.getHeight();
        g.setColor(Color.WHITE);
        for (String text : lines) {
            g.drawString(text, width / 14, y);
            y += metrics.getHeight();
        }
        g.setColor(color);
        g.fillRect(width / 14, height - height / 14, width / 6, Math.max(4, height / 120));
    }
}
