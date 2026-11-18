package com.mist.ui.controller;

import com.mist.exception.MistException;
import com.mist.model.Address;
import com.mist.model.Developer;
import com.mist.model.ModelKeys;
import com.mist.model.PaymentCard;
import com.mist.model.User;
import com.mist.service.AddressForm;
import com.mist.service.CardForm;
import com.mist.service.DeveloperSignUp;
import com.mist.service.PhoneForm;
import com.mist.service.PlayerSignUp;
import com.mist.service.ServiceKeys;
import com.mist.ui.LoginNotice;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import com.mist.ui.support.Choices;
import com.mist.ui.support.FormValidator;
import com.mist.ui.support.Patterns;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;

/**
 * Controller of the registration screen: one form for players and developers, whose last group changes with the
 * account type.
 *
 * <p>Implements the behaviors REGISTER-1 to REGISTER-3 of {@code docs/ui-design.md}.</p>
 */
public class RegisterController {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** The country selected when the form opens. */
    private static final String DEFAULT_COUNTRY = "ES";

    /** What the controller works with. */
    private final UiContext context;
    /** Checks the form before the service call and marks the fields the service rejects. */
    private FormValidator form;

    /** The account type toggles. */
    @FXML
    private ToggleGroup roleGroup;
    /** Registers a player. */
    @FXML
    private RadioButton playerRadio;
    /** Registers a developer. */
    @FXML
    private RadioButton developerRadio;
    /** First name. */
    @FXML
    private TextField firstNameField;
    /** Last name. */
    @FXML
    private TextField lastNameField;
    /** Email, the login. */
    @FXML
    private TextField emailField;
    /** Password. */
    @FXML
    private PasswordField passwordField;
    /** The password again. */
    @FXML
    private PasswordField repeatField;
    /** Phone country code, optional. */
    @FXML
    private TextField phoneCodeField;
    /** Phone number, optional. */
    @FXML
    private TextField phoneNumberField;
    /** Street and number. */
    @FXML
    private TextField streetField;
    /** Floor, door and other details, optional. */
    @FXML
    private TextField detailsField;
    /** Postal code. */
    @FXML
    private TextField postalCodeField;
    /** City. */
    @FXML
    private TextField cityField;
    /** Province, optional. */
    @FXML
    private TextField provinceField;
    /** Country, by ISO code. */
    @FXML
    private ComboBox<String> countryBox;
    /** The player-only group. */
    @FXML
    private VBox playerSection;
    /** The player's nickname. */
    @FXML
    private TextField nicknameField;
    /** The name on the player's card. */
    @FXML
    private TextField cardHolderField;
    /** The card number. */
    @FXML
    private TextField cardNumberField;
    /** The card's expiry month. */
    @FXML
    private ComboBox<Integer> expiryMonthBox;
    /** The card's expiry year. */
    @FXML
    private ComboBox<Integer> expiryYearBox;
    /** The developer-only group. */
    @FXML
    private VBox developerSection;
    /** The developer's studio name. */
    @FXML
    private TextField studioNameField;
    /** The developer's IBAN. */
    @FXML
    private TextField bankAccountField;
    /** Shows the first error, or why the service refused. */
    @FXML
    private Label formMessage;
    /** Creates the account; the form's default button. */
    @FXML
    private Button registerButton;
    /** Goes back to the login screen. */
    @FXML
    private Hyperlink backLink;
    /** Opens the help window. */
    @FXML
    private Button helpButton;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public RegisterController(UiContext context) {
        this.context = context;
    }

    /** Fills the choices, shows the player group, and declares the checks and the service keys of each field. */
    @FXML
    private void initialize() {
        Choices.countries(countryBox, DEFAULT_COUNTRY);
        Choices.months(expiryMonthBox);
        Choices.expiryYears(expiryYearBox);
        roleGroup.selectedToggleProperty().addListener((observable, before, after) -> onRoleChanged());
        onRoleChanged();
        form = new FormValidator(formMessage)
                .required(firstNameField).maxLength(firstNameField, User.MAX_FIRST_NAME_LENGTH, ModelKeys.USER_FIRST_NAME)
                .required(lastNameField).maxLength(lastNameField, User.MAX_LAST_NAME_LENGTH, ModelKeys.USER_LAST_NAME)
                .required(emailField).email(emailField, ModelKeys.USER_EMAIL)
                .maxLength(emailField, User.MAX_EMAIL_LENGTH, ModelKeys.USER_EMAIL)
                .password(passwordField)
                .sameText(repeatField, passwordField)
                .pattern(phoneCodeField, Patterns.PHONE_CODE, true, ModelKeys.PHONE_COUNTRY_CODE)
                .pattern(phoneNumberField, Patterns.PHONE_NUMBER, true, ModelKeys.PHONE_NUMBER)
                .rule(phoneNumberField, this::phoneComplete, ModelKeys.PHONE_NUMBER)
                .required(streetField).maxLength(streetField, Address.MAX_STREET_LENGTH, ModelKeys.ADDRESS_STREET)
                .maxLength(detailsField, Address.MAX_DETAILS_LENGTH, ModelKeys.ADDRESS_DETAILS)
                .required(postalCodeField).pattern(postalCodeField, Patterns.POSTAL_CODE, false,
                        ModelKeys.ADDRESS_POSTAL_CODE)
                .required(cityField).maxLength(cityField, Address.MAX_CITY_LENGTH, ModelKeys.ADDRESS_CITY)
                .maxLength(provinceField, Address.MAX_PROVINCE_LENGTH, ModelKeys.ADDRESS_PROVINCE)
                .required(countryBox)
                .required(nicknameField).pattern(nicknameField, Patterns.NICKNAME, false, ModelKeys.PLAYER_NICKNAME)
                .required(cardHolderField)
                .maxLength(cardHolderField, PaymentCard.MAX_HOLDER_LENGTH, ModelKeys.CARD_HOLDER)
                .required(cardNumberField).pattern(cardNumberField, Patterns.CARD_NUMBER, true, ModelKeys.CARD_NUMBER)
                .required(expiryMonthBox).required(expiryYearBox)
                .rule(expiryMonthBox, this::cardNotExpired, ModelKeys.CARD_EXPIRED)
                .required(studioNameField)
                .maxLength(studioNameField, Developer.MAX_STUDIO_NAME_LENGTH, ModelKeys.DEVELOPER_STUDIO_NAME)
                .required(bankAccountField)
                .pattern(bankAccountField, Patterns.IBAN, true, ModelKeys.DEVELOPER_BANK_ACCOUNT)
                .bind(ModelKeys.USER_FIRST_NAME, firstNameField).bind(ModelKeys.USER_LAST_NAME, lastNameField)
                .bind(ModelKeys.USER_EMAIL, emailField).bind(ServiceKeys.USER_EMAIL_TAKEN, emailField)
                .bind(ServiceKeys.AUTH_WEAK_PASSWORD, passwordField)
                .bind(ModelKeys.PHONE_COUNTRY_CODE, phoneCodeField).bind(ModelKeys.PHONE_NUMBER, phoneNumberField)
                .bind(ModelKeys.USER_ADDRESS, streetField).bind(ModelKeys.ADDRESS_STREET, streetField)
                .bind(ModelKeys.ADDRESS_DETAILS, detailsField).bind(ModelKeys.ADDRESS_POSTAL_CODE, postalCodeField)
                .bind(ModelKeys.ADDRESS_CITY, cityField).bind(ModelKeys.ADDRESS_PROVINCE, provinceField)
                .bind(ModelKeys.ADDRESS_COUNTRY, countryBox)
                .bind(ModelKeys.PLAYER_NICKNAME, nicknameField).bind(ServiceKeys.USER_NICKNAME_TAKEN, nicknameField)
                .bind(ModelKeys.CARD_HOLDER, cardHolderField).bind(ModelKeys.CARD_NUMBER, cardNumberField)
                .bind(ModelKeys.PLAYER_CARD, cardNumberField).bind(ModelKeys.CARD_EXPIRY, expiryMonthBox)
                .bind(ModelKeys.CARD_EXPIRED, expiryMonthBox)
                .bind(ModelKeys.DEVELOPER_STUDIO_NAME, studioNameField)
                .bind(ServiceKeys.USER_STUDIO_NAME_TAKEN, studioNameField)
                .bind(ModelKeys.DEVELOPER_BANK_ACCOUNT, bankAccountField);
    }

    /** REGISTER-1: shows the group of the chosen account type; the hidden one is not checked. */
    private void onRoleChanged() {
        boolean player = playerRadio.isSelected();
        playerSection.setVisible(player);
        playerSection.setManaged(player);
        developerSection.setVisible(!player);
        developerSection.setManaged(!player);
    }

    /** REGISTER-2: checks the visible fields and creates the account. */
    @FXML
    private void onRegister() {
        if (!form.validate()) {
            return;
        }
        String email = emailField.getText().strip();
        boolean player = playerRadio.isSelected();
        LOG.info("Registering a {}", player ? "player" : "developer");
        context.tasks().run(registerButton, () -> player
                        ? context.services().auth().registerPlayer(playerSignUp())
                        : context.services().auth().registerDeveloper(developerSignUp()),
                // R1: back to login with the email filled in.
                created -> context.navigator().show(View.LOGIN, new LoginNotice(email, "register.done", true)),
                this::registerFailed);
    }

    /**
     * REGISTER-2.R2 to R4: marks a taken or invalid value on its field; a database failure offers to retry.
     *
     * @param error why the service refused
     */
    private void registerFailed(Throwable error) {
        if (error instanceof MistException refused && form.showServiceError(refused)) {
            return;
        }
        context.errors().report(context.navigator().window(), error, this::onRegister);
    }

    /** HELP-1: opens the help on this screen's topic. */
    @FXML
    private void onHelp() {
        context.navigator().showHelp();
    }

    /** REGISTER-3: goes back to the login screen. */
    @FXML
    private void onBack() {
        context.navigator().show(View.LOGIN);
    }

    /**
     * Builds the player's sign-up from the form. The password array is wiped by the service.
     *
     * @return the sign-up
     */
    private PlayerSignUp playerSignUp() {
        return new PlayerSignUp(firstNameField.getText(), lastNameField.getText(), emailField.getText(),
                passwordField.getText().toCharArray(), phone(), address(), nicknameField.getText(),
                new CardForm(cardHolderField.getText(), cardNumberField.getText(), expiryMonthBox.getValue(),
                        expiryYearBox.getValue()));
    }

    /**
     * Builds the developer's sign-up from the form. The password array is wiped by the service.
     *
     * @return the sign-up
     */
    private DeveloperSignUp developerSignUp() {
        return new DeveloperSignUp(firstNameField.getText(), lastNameField.getText(), emailField.getText(),
                passwordField.getText().toCharArray(), phone(), address(), studioNameField.getText(),
                bankAccountField.getText());
    }

    /**
     * Builds the phone from its two fields.
     *
     * @return the phone form, both parts blank if the phone was left out
     */
    private PhoneForm phone() {
        return new PhoneForm(phoneCodeField.getText(), phoneNumberField.getText());
    }

    /**
     * Builds the address from its fields.
     *
     * @return the address form
     */
    private AddressForm address() {
        return new AddressForm(streetField.getText(), detailsField.getText(), postalCodeField.getText(),
                cityField.getText(), provinceField.getText(), countryBox.getValue());
    }

    /**
     * Checks that the phone is given whole or not at all.
     *
     * @return {@code true} if both parts are blank or both are filled in
     */
    private boolean phoneComplete() {
        return isBlank(phoneCodeField.getText()) == isBlank(phoneNumberField.getText());
    }

    /**
     * Checks that the chosen expiry is not in the past.
     *
     * @return {@code true} if no expiry is chosen yet or it is this month or later
     */
    private boolean cardNotExpired() {
        Integer month = expiryMonthBox.getValue();
        Integer year = expiryYearBox.getValue();
        return month == null || year == null || !YearMonth.of(year, month).isBefore(YearMonth.now());
    }

    /**
     * Tells whether a text is empty.
     *
     * @param text the text, possibly {@code null}
     * @return {@code true} for {@code null} or blank
     */
    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
