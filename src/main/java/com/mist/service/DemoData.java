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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fills an empty database with demo content, so MIST never starts empty: an admin, one developer account per studio
 * behind the fifteen highest rated games of all time on Metacritic, those fifteen games (plus Grand Theft Auto VI,
 * still a draft), and three players with orders, a cart, a wishlist and reviews.
 *
 * <p>It is the only way an {@link Admin} account is created. Games, orders and reviews go through the real services,
 * so they pass the same rules and transactions as the app. One order of each of two players is 20 days old, so the
 * library shows both returnable and no longer returnable games. Covers and screenshots come from
 * {@code /com/mist/demo/<folder>/} on the classpath (see {@code SOURCES.md} there); an image that is missing is drawn
 * with Java2D instead.</p>
 *
 * <p>The studios' contact people, addresses and bank accounts are made up. Every demo account uses the password
 * {@value #PASSWORD}.</p>
 */
public final class DemoData {

    /** Email of the demo admin, who can read the operation log. */
    public static final String ADMIN_EMAIL = "admin@mist.com";
    /** Email of the demo player with the most data. */
    public static final String PLAYER_EMAIL = "lucia@mist.com";
    /** Email of a second demo player. */
    public static final String SECOND_PLAYER_EMAIL = "marc@mist.com";
    /** Email of a third demo player. */
    public static final String THIRD_PLAYER_EMAIL = "nora@mist.com";
    /** Email of the demo developer with the most games: Rockstar North. */
    public static final String DEVELOPER_EMAIL = "rockstarnorth@mist.com";
    /** Email of a second demo developer: Rockstar Studios, behind the newest game of the catalog. */
    public static final String SECOND_DEVELOPER_EMAIL = "rockstar.studios@mist.com";
    /** The password of every demo account. */
    public static final String PASSWORD = "mist2026";
    /** Name of the game left as a draft, for the editor demo. */
    public static final String DRAFT_GAME = "Grand Theft Auto VI";

    /** How old the older demo orders are, beyond the 14-day refund window. */
    private static final Duration OLD_ORDER_AGE = Duration.ofDays(20);
    /** Where the demo covers and screenshots are on the classpath. */
    private static final String IMAGES = "/com/mist/demo/";
    /** Images already drawn or read, by name and size, so repeated seeding (as in UI tests) does it once. */
    private static final Map<String, byte[]> DRAWN = new ConcurrentHashMap<>();

    /**
     * One studio of the demo, with the account that sells its games.
     *
     * @param firstName the contact person's first name
     * @param lastName  the contact person's last name
     * @param email     the account's email
     * @param phone     the studio's phone
     * @param address   the studio's address
     * @param studio    the studio's name
     * @param iban      the IBAN sales are paid into
     */
    private record Studio(String firstName, String lastName, String email, Phone phone, Address address,
                          String studio, String iban) {
    }

    /** Rockstar North, Edinburgh: Grand Theft Auto IV and V, and the draft. */
    private static final int ROCKSTAR_NORTH = 0;
    /** Nintendo EPD, Kyoto: Breath of the Wild and Super Mario Odyssey. */
    private static final int NINTENDO_EPD = 1;
    /** Nintendo EAD, Kyoto: Ocarina of Time. */
    private static final int NINTENDO_EAD = 2;
    /** Nintendo EAD Tokyo: both Super Mario Galaxy games. */
    private static final int NINTENDO_EAD_TOKYO = 3;
    /** Rockstar Studios, New York: Red Dead Redemption 2. */
    private static final int ROCKSTAR_STUDIOS = 4;
    /** Namco, Tokyo: SoulCalibur. */
    private static final int NAMCO = 5;
    /** Neversoft, Woodland Hills: Tony Hawk's Pro Skater 2. */
    private static final int NEVERSOFT = 6;
    /** Rare, Twycross: Perfect Dark. */
    private static final int RARE = 7;
    /** Retro Studios, Austin: Metroid Prime. */
    private static final int RETRO_STUDIOS = 8;
    /** Bungie, Bellevue: Halo: Combat Evolved. */
    private static final int BUNGIE = 9;
    /** Visual Concepts, Novato: NFL 2K1. */
    private static final int VISUAL_CONCEPTS = 10;
    /** DMA Design, Edinburgh: Grand Theft Auto III, before it became Rockstar North. */
    private static final int DMA_DESIGN = 11;

    /** The demo studios, indexed by the constants above. */
    private static final List<Studio> STUDIOS = List.of(
            new Studio("Callum", "Reid", DEVELOPER_EMAIL, Phone.of("+44", "1314960123"),
                    Address.of("14 Holyrood Road", null, "EH8 8AF", "Edinburgh", "Scotland", "GB"),
                    "Rockstar North", "GB25RBOS83000112345678"),
            new Studio("Haruka", "Tanaka", "nintendo.epd@mist.com", Phone.of("+81", "756621234"),
                    Address.of("11-1 Kamitoba Hokotate-cho", null, "601-8501", "Kyoto", "Kyoto", "JP"),
                    "Nintendo EPD", "JP78MUFG0001234567890"),
            new Studio("Kenji", "Mori", "nintendo.ead@mist.com", Phone.of("+81", "756625678"),
                    Address.of("60 Fukuine Kamitakamatsu-cho", null, "605-8660", "Kyoto", "Kyoto", "JP"),
                    "Nintendo EAD", "JP18SMBC0002345678901"),
            new Studio("Aiko", "Sato", "nintendo.ead.tokyo@mist.com", Phone.of("+81", "355421234"),
                    Address.of("2-10-1 Nihonbashi", "8F", "103-0027", "Tokyo", "Tokyo", "JP"),
                    "Nintendo EAD Tokyo", "JP95MIZU0003456789012"),
            new Studio("Dana", "Brooks", SECOND_DEVELOPER_EMAIL, Phone.of("+1", "2125550142"),
                    Address.of("622 Broadway", "Floor 4", "10012", "New York", "NY", "US"),
                    "Rockstar Studios", "US18CHAS0210000211234567"),
            new Studio("Takumi", "Ishida", "namco@mist.com", Phone.of("+81", "357301234"),
                    Address.of("2-8-5 Shinagawa", null, "140-0001", "Tokyo", "Tokyo", "JP"),
                    "Namco", "JP37RESO0004567890123"),
            new Studio("Ryan", "Cole", "neversoft@mist.com", Phone.of("+1", "8185550173"),
                    Address.of("21255 Burbank Blvd", "Suite 300", "91367", "Woodland Hills", "CA", "US"),
                    "Neversoft", "US74BOFA0260095931234"),
            new Studio("Emily", "Clarke", "rare@mist.com", Phone.of("+44", "1827880123"),
                    Address.of("Manor Park", null, "CV9 3QN", "Twycross", "Leicestershire", "GB"),
                    "Rare", "GB60BARC20000055779911"),
            new Studio("Marcus", "Hill", "retro.studios@mist.com", Phone.of("+1", "5125550118"),
                    Address.of("1835 Kramer Lane", "Suite A", "78758", "Austin", "TX", "US"),
                    "Retro Studios", "US23WFBI1210002481234"),
            new Studio("Sara", "Lindqvist", "bungie@mist.com", Phone.of("+1", "4255550190"),
                    Address.of("550 106th Ave NE", null, "98004", "Bellevue", "WA", "US"),
                    "Bungie", "US89CITI0210000891234"),
            new Studio("Leo", "Martins", "visual.concepts@mist.com", Phone.of("+1", "4155550127"),
                    Address.of("10 Hamilton Landing", null, "94949", "Novato", "CA", "US"),
                    "Visual Concepts", "US90FRBK3210813671234"),
            new Studio("Fiona", "Grant", "dma.design@mist.com", Phone.of("+44", "1314960456"),
                    Address.of("2 Brunswick Place", null, "EH7 5HP", "Edinburgh", "Scotland", "GB"),
                    "DMA Design", "GB30BKSC80000210987654"));

    /**
     * One achievement of a demo game.
     *
     * @param name        the name
     * @param description what unlocks it
     */
    private record Goal(String name, String description) {
    }

    /**
     * One game of the demo catalog.
     *
     * @param name         the name
     * @param price        the price in euros
     * @param studio       the index of its studio in {@link #STUDIOS}
     * @param released     its original release date
     * @param pegi         its age rating
     * @param tags         its tags
     * @param languages    its languages, as two-letter codes
     * @param description  its description
     * @param folder       the folder of its cover and screenshots under {@link #IMAGES}
     * @param colors       the two colors of its drawn art, as RGB, used when an image is missing
     * @param achievements its achievements
     * @param published    whether it is on sale
     */
    private record Seed(String name, String price, int studio, LocalDate released, Pegi pegi, List<String> tags,
                        List<String> languages, String description, String folder, int[] colors,
                        List<Goal> achievements, boolean published) {
    }

    /** Every language the big modern releases ship in. */
    private static final List<String> MANY = List.of("en", "es", "fr", "de", "it", "ja", "pt", "pl", "ru", "ko", "zh");
    /** The languages of Nintendo's European releases, plus Japanese. */
    private static final List<String> NINTENDO = List.of("en", "es", "fr", "de", "it", "ja");

    /** The demo catalog: Metacritic's fifteen highest rated games, then the draft. */
    private static final List<Seed> CATALOG = List.of(
            new Seed("The Legend of Zelda: Ocarina of Time", "19.99", NINTENDO_EAD, LocalDate.of(1998, 11, 21),
                    Pegi.PEGI_12, List.of("action-adventure", "fantasy", "classic"), NINTENDO,
                    "Link wakes in Kokiri Forest and sets out to stop Ganondorf from claiming the Triforce. Play the "
                    + "Ocarina to bend time, travel seven years ahead and back, and clear the dungeons of Hyrule "
                    + "with Z-targeting, the lock-on that changed 3D combat.",
                    "ocarina-of-time", new int[] {0x14261A, 0xD8B45A},
                    List.of(new Goal("Spiritual Stone", "Earn the Kokiri Emerald from the Great Deku Tree."),
                            new Goal("Hero of Time", "Draw the Master Sword from its pedestal."),
                            new Goal("Song of Storms", "Learn every melody for the Ocarina.")), true),
            new Seed("SoulCalibur", "9.99", NAMCO, LocalDate.of(1999, 9, 9), Pegi.PEGI_12,
                    List.of("fighting", "arcade", "versus"), List.of("en", "ja"),
                    "Weapon-based fighting in full 3D: eight-way run lets every fighter step around attacks, and "
                    + "Mission Battle mode unlocks new costumes, stages and art as you travel the world.",
                    "soulcalibur", new int[] {0x2A0F0F, 0xE8A33D},
                    List.of(new Goal("A tale of souls", "Finish Arcade mode with any fighter."),
                            new Goal("Guard impact", "Repel ten attacks in a single match."),
                            new Goal("Museum curator", "Unlock every gallery entry in Mission Battle.")), true),
            new Seed("Grand Theft Auto IV", "19.99", ROCKSTAR_NORTH, LocalDate.of(2008, 4, 29), Pegi.PEGI_18,
                    List.of("open world", "action", "crime"), List.of("en", "es", "fr", "de", "it"),
                    "Niko Bellic crosses the ocean chasing the American Dream and finds Liberty City instead. A grim, "
                    + "funny crime story in a city that runs on cell phones, debts and favors.",
                    "gta-iv", new int[] {0x1A1A1A, 0xB5A27A},
                    List.of(new Goal("Welcome to America", "Finish the first mission with Roman."),
                            new Goal("Off the boat", "Cross all three islands of Liberty City."),
                            new Goal("Key to the city", "Reach 100% completion.")), true),
            new Seed("Tony Hawk's Pro Skater 2", "9.99", NEVERSOFT, LocalDate.of(2000, 9, 20), Pegi.PEGI_3,
                    List.of("sports", "skateboarding", "arcade"), List.of("en", "fr", "de"),
                    "Two-minute runs, huge combos and manuals to link them. Skate the Hangar, the School and Venice "
                    + "Beach, build your own park and climb from amateur to sponsored pro.",
                    "thps2", new int[] {0x1F1640, 0xF2C230},
                    List.of(new Goal("Sick score", "Score 100,000 points in a single run."),
                            new Goal("Collect S-K-A-T-E", "Find every letter in the Hangar."),
                            new Goal("Gold medal", "Win gold in every competition.")), true),
            new Seed("Super Mario Galaxy", "19.99", NINTENDO_EAD_TOKYO, LocalDate.of(2007, 11, 1), Pegi.PEGI_3,
                    List.of("platformer", "3d", "space"), NINTENDO,
                    "Bowser has taken Princess Peach to the center of the universe. Run around tiny planets, launch "
                    + "between galaxies and collect Power Stars to bring Rosalina's observatory back to life.",
                    "mario-galaxy", new int[] {0x0B1035, 0xF5D04C},
                    List.of(new Goal("Lift-off", "Collect your first Power Star."),
                            new Goal("Comet chaser", "Clear a Prankster Comet challenge."),
                            new Goal("Star collector", "Collect all 120 Power Stars.")), true),
            new Seed("Super Mario Galaxy 2", "19.99", NINTENDO_EAD_TOKYO, LocalDate.of(2010, 5, 23), Pegi.PEGI_3,
                    List.of("platformer", "3d", "yoshi"), NINTENDO,
                    "Mario returns to space aboard Starship Mario, with Yoshi, the Cloud Flower and the Spin Drill. "
                    + "Shorter, denser levels and some of the hardest stars in the series.",
                    "mario-galaxy-2", new int[] {0x10204A, 0x7ED957},
                    List.of(new Goal("Saddle up", "Ride Yoshi for the first time."),
                            new Goal("Green Star hunter", "Find a hidden Green Star."),
                            new Goal("Champion's Road", "Clear Grandmaster Galaxy.")), true),
            new Seed("The Legend of Zelda: Breath of the Wild", "59.99", NINTENDO_EPD, LocalDate.of(2017, 3, 3),
                    Pegi.PEGI_12, List.of("open world", "action-adventure", "exploration"), NINTENDO,
                    "Wake after a hundred years and explore a ruined Hyrule however you like: climb anything you can "
                    + "see, cook what you hunt, glide from the towers and face Calamity Ganon when you are ready.",
                    "breath-of-the-wild", new int[] {0x16303A, 0x9BD3C8},
                    List.of(new Goal("The Great Plateau", "Earn the paraglider."),
                            new Goal("Divine Beasts", "Free all four Divine Beasts."),
                            new Goal("Hestu's gift", "Find one hundred Korok seeds.")), true),
            new Seed("Red Dead Redemption 2", "59.99", ROCKSTAR_STUDIOS, LocalDate.of(2018, 10, 26), Pegi.PEGI_18,
                    List.of("open world", "western", "action"), MANY,
                    "America, 1899. Arthur Morgan and the Van der Linde gang are on the run as the law closes in "
                    + "on the last outlaws. Hunt, fish, rob and ride across a living frontier.",
                    "rdr2", new int[] {0x2B1A10, 0xC8312B},
                    List.of(new Goal("Outlaws from the West", "Finish the first chapter."),
                            new Goal("Best in the West", "Earn a gold medal in every story mission."),
                            new Goal("Zoologist", "Study every animal species.")), true),
            new Seed("Grand Theft Auto V", "29.99", ROCKSTAR_NORTH, LocalDate.of(2013, 9, 17), Pegi.PEGI_18,
                    List.of("open world", "action", "heist"), MANY,
                    "A retired bank robber, a street hustler and a dangerous psychopath pull off a series of heists "
                    + "across Los Santos and Blaine County. Switch between all three at any moment.",
                    "gta-v", new int[] {0x0F2A1F, 0x77C043},
                    List.of(new Goal("Welcome to Los Santos", "Finish the prologue."),
                            new Goal("The Big One", "Complete the final heist."),
                            new Goal("Career criminal", "Reach 100% completion.")), true),
            new Seed("Perfect Dark", "9.99", RARE, LocalDate.of(2000, 5, 22), Pegi.PEGI_16,
                    List.of("fps", "sci-fi", "stealth"), List.of("en", "fr", "de"),
                    "Agent Joanna Dark of the Carrington Institute uncovers an alien conspiracy inside the dataDyne "
                    + "corporation. Every weapon has a secondary mode, and the Combat Simulator never ends.",
                    "perfect-dark", new int[] {0x14121F, 0xE04B3A},
                    List.of(new Goal("Perfect Agent", "Finish the campaign on Perfect Agent."),
                            new Goal("Firing range", "Earn gold on every weapon at the range."),
                            new Goal("Challenge accepted", "Win every Combat Simulator challenge.")), true),
            new Seed("Metroid Prime", "19.99", RETRO_STUDIOS, LocalDate.of(2002, 11, 17), Pegi.PEGI_12,
                    List.of("action-adventure", "exploration", "sci-fi"), List.of("en", "es", "fr", "de", "ja"),
                    "Samus Aran lands on Tallon IV and sees it through her own visor. Scan everything, recover her "
                    + "lost upgrades and roll through the ruins of the Chozo in first person.",
                    "metroid-prime", new int[] {0x1B2228, 0xF28C28},
                    List.of(new Goal("Morph Ball", "Recover the Morph Ball."),
                            new Goal("Logbook", "Scan every creature on Tallon IV."),
                            new Goal("Impact Crater", "Defeat Metroid Prime.")), true),
            new Seed("Grand Theft Auto III", "9.99", DMA_DESIGN, LocalDate.of(2001, 10, 22), Pegi.PEGI_18,
                    List.of("open world", "action", "crime"), List.of("en", "es", "fr", "de", "it"),
                    "Betrayed and left for dead, Claude climbs through Liberty City's gangs one job at a time in the "
                    + "game that took the series to 3D and turned the city into the main character.",
                    "gta-iii", new int[] {0x1C1C24, 0xE5E5E5},
                    List.of(new Goal("Give me liberty", "Finish the first mission."),
                            new Goal("Hidden packages", "Find all one hundred hidden packages."),
                            new Goal("Rampage", "Complete every rampage.")), true),
            new Seed("Super Mario Odyssey", "49.99", NINTENDO_EPD, LocalDate.of(2017, 10, 27), Pegi.PEGI_7,
                    List.of("platformer", "3d", "sandbox"), NINTENDO,
                    "Mario and his new friend Cappy sail the Odyssey across kingdoms to stop Bowser's wedding. Throw "
                    + "Cappy to capture enemies, objects and even a T-Rex, and collect Power Moons everywhere.",
                    "mario-odyssey", new int[] {0x2A1430, 0xE8352E},
                    List.of(new Goal("Captured", "Capture your first enemy with Cappy."),
                            new Goal("New Donk City", "Play the festival in the Metro Kingdom."),
                            new Goal("Moon hunter", "Collect five hundred Power Moons.")), true),
            new Seed("Halo: Combat Evolved", "9.99", BUNGIE, LocalDate.of(2001, 11, 15), Pegi.PEGI_16,
                    List.of("fps", "sci-fi", "co-op"), List.of("en", "es", "fr", "de", "it", "ja", "ko"),
                    "The Master Chief wakes on the Pillar of Autumn as the Covenant attack. Fight across a ring world "
                    + "in a campaign built for vehicles, co-op and thirty seconds of fun repeated forever.",
                    "halo", new int[] {0x14241C, 0x6FB0D9},
                    List.of(new Goal("Pillar of Autumn", "Escape the ship."),
                            new Goal("The Silent Cartographer", "Find the map room."),
                            new Goal("Legendary", "Finish the campaign on Legendary.")), true),
            new Seed("NFL 2K1", "4.99", VISUAL_CONCEPTS, LocalDate.of(2000, 9, 7), Pegi.PEGI_3,
                    List.of("sports", "football", "multiplayer"), List.of("en"),
                    "American football with broadcast presentation, a full franchise mode and the first online play "
                    + "of any console sports game.",
                    "nfl-2k1", new int[] {0x0E2A12, 0xE2E2E2},
                    List.of(new Goal("Kick-off", "Win your first game."),
                            new Goal("Franchise", "Play a full season in Franchise mode."),
                            new Goal("Champions", "Win the championship.")), true),
            new Seed(DRAFT_GAME, "79.99", ROCKSTAR_NORTH, LocalDate.of(2026, 11, 19), Pegi.PEGI_18,
                    List.of("open world", "crime"), List.of("en", "es"),
                    "Lucia and Jason, two criminals in love, run into trouble across the sun-soaked state of "
                    + "Leonida. Coming soon.",
                    "gta-vi", new int[] {0x2A0E2E, 0xFF7AB6},
                    List.of(new Goal("Welcome to Vice City", "Finish the opening mission."),
                            new Goal("Bonnie and Clyde", "Pull off a robbery as a pair."),
                            new Goal("Leonida tourist", "Visit every region of Leonida.")), false));

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
        List<Developer> developers = new ArrayList<>();
        for (Studio studio : STUDIOS) {
            developers.add(users.save(Developer.register(studio.firstName(), studio.lastName(), studio.email(), hash,
                    studio.phone(), studio.address(), studio.studio(), studio.iban(), clock.instant())));
        }
        Player lucia = users.save(player("Lucía", "Romero", PLAYER_EMAIL, "lucia", hash));
        Player marc = users.save(player("Marc", "Ferrer", SECOND_PLAYER_EMAIL, "marc_f", hash));
        Player nora = users.save(player("Nora", "Ibarra", THIRD_PLAYER_EMAIL, "nora.plays", hash));

        Map<String, Game> catalog = new HashMap<>();
        PublishingService publishing = new PublishingService(games, users, media, transactions, clock);
        for (Seed seed : CATALOG) {
            catalog.put(seed.name(), publish(publishing, developers.get(seed.studio()), seed));
        }

        StoreService past = new StoreService(games, users, orders, transactions, Clock.offset(clock,
                OLD_ORDER_AGE.negated()));
        StoreService today = new StoreService(games, users, orders, transactions, clock);
        buy(past, lucia, catalog, "Grand Theft Auto V", "Super Mario Galaxy");
        buy(today, lucia, catalog, "Super Mario Odyssey", "Metroid Prime");
        buy(today, marc, catalog, "Grand Theft Auto V", "Halo: Combat Evolved");
        buy(past, nora, catalog, "The Legend of Zelda: Ocarina of Time", "Perfect Dark");
        today.addToWishlist(lucia.getId(), catalog.get("Red Dead Redemption 2").getId());
        today.addToCart(lucia.getId(), catalog.get("Grand Theft Auto IV").getId());

        ReviewService ratings = new ReviewService(reviews, users, games, transactions, clock);
        ratings.rate(lucia.getId(), catalog.get("Grand Theft Auto V").getId(), 5,
                "Los Santos still feels alive. Three leads, one great heist story, and a map I never tire of.");
        ratings.rate(lucia.getId(), catalog.get("Super Mario Galaxy").getId(), 4,
                "Every galaxy plays with gravity in a new way. Pure joy, though the camera fights you at times.");
        ratings.rate(marc.getId(), catalog.get("Grand Theft Auto V").getId(), 4,
                "Huge and polished; a few missions are filler, the heists are not.");
        ratings.rate(marc.getId(), catalog.get("Halo: Combat Evolved").getId(), 5,
                "The campaign that defined console shooters. The Silent Cartographer is a masterpiece.");
        ratings.rate(nora.getId(), catalog.get("Perfect Dark").getId(), 3,
                "Brilliant weapons and ideas, but the controls show their age.");
        ratings.rate(nora.getId(), catalog.get("The Legend of Zelda: Ocarina of Time").getId(), 5, null);
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
     * Creates one demo game with its cover and screenshots, and publishes it if the seed says so.
     *
     * @param publishing the publishing service
     * @param developer  the game's developer
     * @param seed       the game's data
     * @return the game
     * @throws MistException if a value breaks a rule
     */
    private Game publish(PublishingService publishing, Developer developer, Seed seed) throws MistException {
        Game game = publishing.createDraft(developer.getId(), seed.name(), new BigDecimal(seed.price()));
        List<AchievementForm> achievements = seed.achievements().stream()
                .map(goal -> new AchievementForm(goal.name(), goal.description())).toList();
        game = publishing.update(developer.getId(), game.getId(), new GameForm(seed.name(), seed.description(),
                new BigDecimal(seed.price()), seed.released(), seed.pegi(), requirements(seed.pegi()), seed.tags(),
                seed.languages(), achievements));
        Color dark = new Color(seed.colors()[0]);
        Color light = new Color(seed.colors()[1]);
        byte[] cover = image(seed.folder() + "/cover.jpg");
        game = publishing.changeCover(developer.getId(), game.getId(), new ImageUpload("image/jpeg",
                cover != null ? cover : art(seed.name(), dark, light, 600, 800, false), "cover.jpg"));
        List<byte[]> screenshots = new ArrayList<>();
        for (int i = 1; i <= Game.MAX_SCREENSHOTS; i++) {
            byte[] screenshot = image(seed.folder() + "/screenshot-" + i + ".jpg");
            if (screenshot == null) {
                break;
            }
            screenshots.add(screenshot);
        }
        if (screenshots.isEmpty()) {
            for (int i = 1; i <= 2; i++) {
                screenshots.add(art(seed.name() + " #" + i, dark, light, 1280, 720, true));
            }
        }
        for (int i = 0; i < screenshots.size(); i++) {
            game = publishing.addScreenshot(developer.getId(), game.getId(), new ImageUpload("image/jpeg",
                    screenshots.get(i), "screenshot-" + (i + 1) + ".jpg"));
        }
        return seed.published() ? publishing.publish(developer.getId(), game.getId()) : game;
    }

    /**
     * Reads a demo image from the classpath.
     *
     * @param path the image's path under {@link #IMAGES}
     * @return a copy of its bytes, or {@code null} if it is not there
     */
    private static byte[] image(String path) {
        byte[] bytes = DRAWN.computeIfAbsent(path, key -> {
            try (InputStream in = DemoData.class.getResourceAsStream(IMAGES + key)) {
                return in == null ? new byte[0] : in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read demo image " + key, e);
            }
        });
        return bytes.length == 0 ? null : bytes.clone();
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
