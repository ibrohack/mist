package com.mist.ui;

import com.mist.dao.DaoKeys;
import com.mist.model.Review;
import com.mist.service.DemoData;
import com.mist.service.GameReview;
import com.mist.service.OwnedGame;
import com.mist.service.ServiceKeys;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The review dialog (REVIEW-1 to REVIEW-4), opened from the library (LIB-4) and the game page (GAME-8).
 */
class ReviewTest extends MistUiTest {

    @Test
    void aPlayerReviewsAGameFromTheLibrary() throws Exception {
        openReview("Metroid Prime");

        click("star4");
        type("commentArea", "Fast and loud.");
        click("dialogConfirm");

        Review review = review("Metroid Prime").orElseThrow();
        assertEquals(4, review.getRating());
        assertEquals("Fast and loud.", review.getComment());
        assertEquals(msg("review.published"), dialogMessage());
        assertEquals(4, db.gameByName("Metroid Prime").getStats().ratingSum());
    }

    @Test
    void aRatingIsRequired() throws Exception {
        openReview("Metroid Prime");

        click("dialogConfirm");

        assertEquals(msg("review.ratingRequired"), text("formMessage"));
        assertTrue(visible("commentArea"));
        assertTrue(review("Metroid Prime").isEmpty());
    }

    @Test
    void theCounterFollowsTheTextAndStopsAtTheLimit() throws Exception {
        openReview("Metroid Prime");

        type("commentArea", "Great");
        assertEquals(I18n.format("review.counter", 5, Review.MAX_COMMENT_LENGTH), text("commentCounter"));

        TextArea comment = node("commentArea");
        robot.interact(() -> comment.setText("x".repeat(Review.MAX_COMMENT_LENGTH + 100)));
        assertEquals("Great", comment.getText());
    }

    @Test
    void aSecondReviewIsRefusedInsideTheDialog() throws Exception {
        openReview("Grand Theft Auto V");

        click("star3");
        click("dialogConfirm");

        assertEquals(msg(ServiceKeys.REVIEW_ALREADY_EXISTS), text("formMessage"));
        assertEquals(5, review("Grand Theft Auto V").orElseThrow().getRating());
    }

    @Test
    void cancellingWritesNothing() throws Exception {
        openReview("Metroid Prime");

        click("star5");
        click("dialogCancel");

        assertFalse(visible("commentArea"));
        assertTrue(review("Metroid Prime").isEmpty());
    }

    @Test
    void aDatabaseFailureWhilePublishingOffersARetry() throws Exception {
        openReview("Metroid Prime");
        click("star2");
        db.failNextCall();

        click("dialogConfirm");
        assertEquals(msg(DaoKeys.FAILURE), dialogMessage());
        click("dialogRetry");

        assertEquals(2, review("Metroid Prime").orElseThrow().getRating());
    }

    @Test
    void theGamePageOffersAReviewToOwnersAndShowsItAfterwards() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        openGame("Metroid Prime");
        assertTrue(visible("reviewButton"));

        click("reviewButton");
        click("star5");
        click("dialogConfirm");

        assertFalse(visible("reviewButton"));
        assertEquals(I18n.format("game.yourReview", "★★★★★"), text("yourReviewLabel"));
        ListView<GameReview> reviews = node("reviewsList");
        assertEquals("lucia", reviews.getItems().getFirst().authorNickname());
    }

    @Test
    void theGamePageShowsAnExistingReviewAndNothingToNonOwners() throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);

        openGame("Grand Theft Auto V");
        assertEquals(I18n.format("game.yourReview", "★★★★★"), text("yourReviewLabel"));
        assertFalse(visible("reviewButton"));

        openGame("Red Dead Redemption 2");
        assertFalse(visible("reviewButton"));
        assertFalse(visible("yourReviewLabel"));
    }

    @Test
    void theQuestionMarkOpensTheLibraryHelp() throws Exception {
        openReview("Metroid Prime");

        click("reviewHelpButton");

        assertTrue(navigator.isHelpShowing());
        assertEquals(msg("help.library.title"), text("topicTitle"));
    }

    /**
     * Logs in as the demo player, opens the library and the review dialog of an owned game.
     *
     * @param name the game's name
     * @throws Exception if the login fails
     */
    private void openReview(String name) throws Exception {
        loginAs(DemoData.PLAYER_EMAIL);
        click("libraryLink");
        ListView<OwnedGame> list = node("gameList");
        OwnedGame item = list.getItems().stream().filter(g -> g.game().getName().equals(name)).findFirst()
                .orElseThrow();
        robot.interact(() -> list.getSelectionModel().select(item));
        MenuItem review = list.getContextMenu().getItems().stream().filter(i -> "reviewItem".equals(i.getId()))
                .findFirst().orElseThrow();
        robot.interact(review::fire);
    }

    /**
     * Opens a game's page.
     *
     * @param name the game's name
     */
    private void openGame(String name) {
        ObjectId id = db.gameByName(name).getId();
        robot.interact(() -> navigator.show(View.GAME, id));
    }

    /**
     * Returns the demo player's review of a game, from the database.
     *
     * @param name the game's name
     * @return the review, or empty
     * @throws Exception if the lookup fails
     */
    private Optional<Review> review(String name) throws Exception {
        return backend.reviews().findByGameAndPlayer(db.gameByName(name).getId(),
                db.userByEmail(DemoData.PLAYER_EMAIL).getId());
    }
}
