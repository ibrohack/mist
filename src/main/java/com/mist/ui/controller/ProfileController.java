package com.mist.ui.controller;

import com.mist.exception.MistException;
import com.mist.model.Address;
import com.mist.model.Developer;
import com.mist.model.ModelKeys;
import com.mist.model.PaymentCard;
import com.mist.model.Phone;
import com.mist.model.Player;
import com.mist.model.User;
import com.mist.service.AddressForm;
import com.mist.service.CardForm;
import com.mist.service.PhoneForm;
import com.mist.service.ProfileForm;
import com.mist.service.ServiceKeys;
import com.mist.ui.I18n;
import com.mist.ui.NavigationAware;
import com.mist.ui.UiContext;
import com.mist.ui.support.Choices;
import com.mist.ui.support.Dialogs;
import com.mist.ui.support.FormValidator;
import com.mist.ui.support.Formats;
import com.mist.ui.support.Patterns;
import com.mist.ui.support.UiTasks;
import com.mist.ui.support.Uploads;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller of the profile: the avatar and one row per editable item, each with its own editor opened in place.
 *
 * <p>Implements the behaviors PROFILE-1 to PROFILE-12 of {@code docs/ui-design.md}.</p>
 */
public class ProfileController implements NavigationAware {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** The pencil drawn on every Edit button. */
    private static final String PENCIL = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 "
            + "0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    /** Radius of the avatar. */
    private static final double AVATAR_RADIUS = 48;
    /** The country the address editor starts with when there is no address yet. */
    private static final String DEFAULT_COUNTRY = "ES";

    /** What the controller works with. */
    private final UiContext context;
    /** Each Edit button and the editor it opens. */
    private final Map<Button, VBox> editors = new LinkedHashMap<>();
    /** Checks the open editor and marks the fields the service rejects. */
    private FormValidator form;
    /** The user as last loaded or saved. */
    private User user;

    /** The avatar's background circle. */
    @FXML
    private Circle avatarCircle;
    /** The user's initials, shown while there is no avatar. */
    @FXML
    private Label avatarInitials;
    /** The avatar image, clipped to a circle. */
    @FXML
    private ImageView avatarImage;
    /** The user's full name. */
    @FXML
    private Label nameTitle;
    /** The account type. */
    @FXML
    private Label roleLabel;
    /** When the account was created. */
    @FXML
    private Label memberSinceLabel;
    /** Uploads a new avatar. */
    @FXML
    private Button changeAvatarButton;
    /** Removes the avatar. */
    @FXML
    private Button removeAvatarButton;
    /** The result of the last save, or the first error. */
    @FXML
    private Label formMessage;
    /** The name row's value. */
    @FXML
    private Label nameValue;
    /** Opens the name editor. */
    @FXML
    private Button nameEdit;
    /** The name editor. */
    @FXML
    private VBox nameEditor;
    /** First name. */
    @FXML
    private TextField firstNameField;
    /** Last name. */
    @FXML
    private TextField lastNameField;
    /** The email row's value. */
    @FXML
    private Label emailValue;
    /** Opens the email editor. */
    @FXML
    private Button emailEdit;
    /** The email editor. */
    @FXML
    private VBox emailEditor;
    /** Email. */
    @FXML
    private TextField emailField;
    /** The phone row's value. */
    @FXML
    private Label phoneValue;
    /** Opens the phone editor. */
    @FXML
    private Button phoneEdit;
    /** The phone editor. */
    @FXML
    private VBox phoneEditor;
    /** Phone country code. */
    @FXML
    private TextField phoneCodeField;
    /** Phone number. */
    @FXML
    private TextField phoneNumberField;
    /** The address row's value. */
    @FXML
    private Label addressValue;
    /** Opens the address editor. */
    @FXML
    private Button addressEdit;
    /** The address editor. */
    @FXML
    private VBox addressEditor;
    /** Street and number. */
    @FXML
    private TextField streetField;
    /** Floor and door. */
    @FXML
    private TextField detailsField;
    /** Postal code. */
    @FXML
    private TextField postalCodeField;
    /** City. */
    @FXML
    private TextField cityField;
    /** Province. */
    @FXML
    private TextField provinceField;
    /** Country. */
    @FXML
    private ComboBox<String> countryBox;
    /** Opens the password editor. */
    @FXML
    private Button passwordEdit;
    /** The password editor. */
    @FXML
    private VBox passwordEditor;
    /** The current password, to prove it is the user. */
    @FXML
    private PasswordField currentPasswordField;
    /** The new password. */
    @FXML
    private PasswordField newPasswordField;
    /** The new password again. */
    @FXML
    private PasswordField repeatPasswordField;
    /** Saves the new password. */
    @FXML
    private Button passwordSave;
    /** The player-only nickname row. */
    @FXML
    private VBox nicknameRow;
    /** The nickname row's value. */
    @FXML
    private Label nicknameValue;
    /** Opens the nickname editor. */
    @FXML
    private Button nicknameEdit;
    /** The nickname editor. */
    @FXML
    private VBox nicknameEditor;
    /** Nickname. */
    @FXML
    private TextField nicknameField;
    /** The player-only card row. */
    @FXML
    private VBox cardRow;
    /** The card row's value: brand, last digits and expiry. */
    @FXML
    private Label cardValue;
    /** Opens the card editor. */
    @FXML
    private Button cardEdit;
    /** The card editor. */
    @FXML
    private VBox cardEditor;
    /** Card holder. */
    @FXML
    private TextField cardHolderField;
    /** Card number, typed again since only its last digits are stored. */
    @FXML
    private TextField cardNumberField;
    /** Card expiry month. */
    @FXML
    private ComboBox<Integer> expiryMonthBox;
    /** Card expiry year. */
    @FXML
    private ComboBox<Integer> expiryYearBox;
    /** The developer-only studio row. */
    @FXML
    private VBox studioRow;
    /** The studio row's value. */
    @FXML
    private Label studioValue;
    /** Opens the studio editor. */
    @FXML
    private Button studioEdit;
    /** The studio editor. */
    @FXML
    private VBox studioEditor;
    /** Studio name. */
    @FXML
    private TextField studioNameField;
    /** The developer-only bank account row. */
    @FXML
    private VBox bankRow;
    /** The bank row's value, masked. */
    @FXML
    private Label bankValue;
    /** Opens the bank account editor. */
    @FXML
    private Button bankEdit;
    /** The bank account editor. */
    @FXML
    private VBox bankEditor;
    /** IBAN. */
    @FXML
    private TextField bankAccountField;
    /** Explains what deactivating does for this role. */
    @FXML
    private Label deactivateHint;
    /** Deactivates the account. */
    @FXML
    private Button deactivateButton;

    /**
     * Creates the controller.
     *
     * @param context what the controller works with
     */
    public ProfileController(UiContext context) {
        this.context = context;
    }

    /** Prepares the editors, the choices and the checks of every field. */
    @FXML
    private void initialize() {
        avatarImage.setClip(new Circle(AVATAR_RADIUS, AVATAR_RADIUS, AVATAR_RADIUS));
        Choices.months(expiryMonthBox);
        Choices.expiryYears(expiryYearBox);
        editors.put(nameEdit, nameEditor);
        editors.put(emailEdit, emailEditor);
        editors.put(phoneEdit, phoneEditor);
        editors.put(addressEdit, addressEditor);
        editors.put(passwordEdit, passwordEditor);
        editors.put(nicknameEdit, nicknameEditor);
        editors.put(cardEdit, cardEditor);
        editors.put(studioEdit, studioEditor);
        editors.put(bankEdit, bankEditor);
        editors.forEach((button, editor) -> {
            SVGPath pencil = new SVGPath();
            pencil.setContent(PENCIL);
            pencil.getStyleClass().add("mist-icon");
            button.setGraphic(pencil);
            button.setTooltip(new Tooltip(I18n.text("profile.edit")));
            show(editor, false);
        });
        form = new FormValidator(formMessage)
                .required(firstNameField).maxLength(firstNameField, User.MAX_FIRST_NAME_LENGTH, ModelKeys.USER_FIRST_NAME)
                .required(lastNameField).maxLength(lastNameField, User.MAX_LAST_NAME_LENGTH, ModelKeys.USER_LAST_NAME)
                .required(emailField).email(emailField, ModelKeys.USER_EMAIL)
                .pattern(phoneCodeField, Patterns.PHONE_CODE, true, ModelKeys.PHONE_COUNTRY_CODE)
                .pattern(phoneNumberField, Patterns.PHONE_NUMBER, true, ModelKeys.PHONE_NUMBER)
                .rule(phoneNumberField, () -> isBlank(phoneCodeField.getText()) == isBlank(phoneNumberField.getText()),
                        ModelKeys.PHONE_NUMBER)
                .required(streetField).maxLength(streetField, Address.MAX_STREET_LENGTH, ModelKeys.ADDRESS_STREET)
                .maxLength(detailsField, Address.MAX_DETAILS_LENGTH, ModelKeys.ADDRESS_DETAILS)
                .required(postalCodeField)
                .pattern(postalCodeField, Patterns.POSTAL_CODE, false, ModelKeys.ADDRESS_POSTAL_CODE)
                .required(cityField).maxLength(cityField, Address.MAX_CITY_LENGTH, ModelKeys.ADDRESS_CITY)
                .maxLength(provinceField, Address.MAX_PROVINCE_LENGTH, ModelKeys.ADDRESS_PROVINCE)
                .required(countryBox)
                .required(currentPasswordField).password(newPasswordField)
                .sameText(repeatPasswordField, newPasswordField)
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
                .bind(ModelKeys.PHONE_COUNTRY_CODE, phoneCodeField).bind(ModelKeys.PHONE_NUMBER, phoneNumberField)
                .bind(ModelKeys.USER_ADDRESS, streetField).bind(ModelKeys.ADDRESS_STREET, streetField)
                .bind(ModelKeys.ADDRESS_DETAILS, detailsField).bind(ModelKeys.ADDRESS_POSTAL_CODE, postalCodeField)
                .bind(ModelKeys.ADDRESS_CITY, cityField).bind(ModelKeys.ADDRESS_PROVINCE, provinceField)
                .bind(ModelKeys.ADDRESS_COUNTRY, countryBox)
                .bind(ServiceKeys.AUTH_WRONG_PASSWORD, currentPasswordField)
                .bind(ServiceKeys.AUTH_WEAK_PASSWORD, newPasswordField)
                .bind(ModelKeys.PLAYER_NICKNAME, nicknameField).bind(ServiceKeys.USER_NICKNAME_TAKEN, nicknameField)
                .bind(ModelKeys.CARD_HOLDER, cardHolderField).bind(ModelKeys.CARD_NUMBER, cardNumberField)
                .bind(ModelKeys.CARD_EXPIRY, expiryMonthBox).bind(ModelKeys.CARD_EXPIRED, expiryMonthBox)
                .bind(ModelKeys.DEVELOPER_STUDIO_NAME, studioNameField)
                .bind(ServiceKeys.USER_STUDIO_NAME_TAKEN, studioNameField)
                .bind(ModelKeys.DEVELOPER_BANK_ACCOUNT, bankAccountField);
    }

    @Override
    public void onNavigatedTo(Object parameter) {
        // PROFILE-1: load the stored values.
        context.tasks().run(null, () -> context.services().users().get(context.session().userId()), this::render);
    }

    /**
     * PROFILE-2: opens the editor of a row with its current values, closing any other.
     *
     * @param event the action of an Edit button
     */
    @FXML
    private void onEdit(ActionEvent event) {
        closeEditors();
        VBox editor = editors.get((Button) event.getSource());
        prefill(editor);
        show(editor, true);
    }

    /** PROFILE-3: closes the open editor, discarding its changes. */
    @FXML
    private void onCancel() {
        closeEditors();
    }

    /**
     * PROFILE-4: saves the name, email, phone or address; the other profile values are sent unchanged.
     *
     * @param event the action of a Save button
     */
    @FXML
    private void onSaveProfile(ActionEvent event) {
        if (!form.validate()) {
            return;
        }
        ProfileForm profile = profileForm();
        save((Node) event.getSource(), () -> context.services().users().updateProfile(user.getId(), profile),
                this::saved);
    }

    /** PROFILE-5: saves the nickname. */
    @FXML
    private void onSaveNickname() {
        if (!form.validate()) {
            return;
        }
        String nickname = nicknameField.getText();
        save(nicknameEditor, () -> context.services().users().changeNickname(user.getId(), nickname), this::saved);
    }

    /** PROFILE-6: saves a new card. */
    @FXML
    private void onSaveCard() {
        if (!form.validate()) {
            return;
        }
        CardForm card = new CardForm(cardHolderField.getText(), cardNumberField.getText(), expiryMonthBox.getValue(),
                expiryYearBox.getValue());
        save(cardEditor, () -> context.services().users().changeCard(user.getId(), card), this::saved);
    }

    /** PROFILE-7: saves the studio name. */
    @FXML
    private void onSaveStudio() {
        if (!form.validate()) {
            return;
        }
        String studio = studioNameField.getText();
        save(studioEditor, () -> context.services().users().changeStudioName(user.getId(), studio), this::saved);
    }

    /** PROFILE-8: saves the IBAN. */
    @FXML
    private void onSaveBank() {
        if (!form.validate()) {
            return;
        }
        String iban = bankAccountField.getText();
        save(bankEditor, () -> context.services().users().changeBankAccount(user.getId(), iban), this::saved);
    }

    /** PROFILE-9: changes the password after checking the current one. */
    @FXML
    private void onSavePassword() {
        if (!form.validate()) {
            return;
        }
        char[] current = currentPasswordField.getText().toCharArray();
        char[] next = newPasswordField.getText().toCharArray();
        save(passwordSave, () -> {
            context.services().auth().changePassword(user.getId(), current, next);
            return user;
        }, unchanged -> {
            // R1: the session stays; only the editor closes.
            closeEditors();
            form.showMessage("profile.passwordChanged", true);
            LOG.info("Password changed");
        });
    }

    /** PROFILE-10: uploads the image the user picks as the new avatar. */
    @FXML
    private void onChangeAvatar() {
        // R4: a cancelled chooser does nothing.
        context.files().openImage(context.navigator().window()).ifPresent(file ->
                context.tasks().run(changeAvatarButton,
                        () -> context.services().users().changeAvatar(user.getId(), Uploads.image(file)),
                        // R1: the new avatar shows here and, through the session, in the user menu.
                        this::saved,
                        // R2 (too big, wrong type) and R3 (unreadable file) are reported as dialogs.
                        error -> context.errors().report(context.navigator().window(), error)));
    }

    /** PROFILE-11: removes the avatar after confirming. */
    @FXML
    private void onRemoveAvatar() {
        Dialogs.confirm(context.navigator().window(), I18n.text("profile.removeAvatar.confirm"),
                "profile.removeAvatar", () -> context.tasks().run(removeAvatarButton,
                        () -> context.services().users().removeAvatar(user.getId()), this::saved));
    }

    /** PROFILE-12: deactivates the account after confirming, and signs out. */
    @FXML
    private void onDeactivate() {
        String question = user instanceof Developer ? "profile.deactivate.confirmDeveloper"
                : "profile.deactivate.confirm";
        Dialogs.confirm(context.navigator().window(), I18n.text(question), "profile.deactivate.button",
                () -> context.tasks().run(deactivateButton, () -> {
                    context.services().users().deactivate(user.getId());
                    return user;
                }, deactivated -> {
                    LOG.info("Account deactivated");
                    context.navigator().signOut("profile.deactivated");
                }));
    }

    /**
     * Runs a save; a rejection is marked on the form, a database failure offers to retry.
     *
     * @param busy    the control to disable while saving
     * @param work    the service call
     * @param onSaved what to do with the saved user
     * @param <T>     the result type
     */
    private <T> void save(Node busy, UiTasks.Work<T> work, Consumer<T> onSaved) {
        context.tasks().run(busy, work, onSaved, error -> {
            if (error instanceof MistException refused && form.showServiceError(refused)) {
                return;
            }
            context.errors().report(context.navigator().window(), error, () -> save(busy, work, onSaved));
        });
    }

    /**
     * R1 of every save: keeps the saved user in the session, shows the new values and closes the editor.
     *
     * @param saved the user as the service saved it
     */
    private void saved(User saved) {
        context.session().update(saved);
        render(saved);
        closeEditors();
        form.showMessage("profile.saved", true);
    }

    /**
     * Shows a user's values, with only the rows of their role.
     *
     * @param shown the user
     */
    private void render(User shown) {
        this.user = shown;
        nameTitle.setText(shown.fullName());
        roleLabel.setText(Formats.label(shown.role()));
        memberSinceLabel.setText(I18n.format("profile.memberSince", Formats.date(shown.getCreatedAt())));
        nameValue.setText(shown.fullName());
        emailValue.setText(shown.getEmail());
        phoneValue.setText(shown.getPhone() == null ? I18n.text("profile.notSet") : shown.getPhone().formatted());
        addressValue.setText(describe(shown.getAddress()));
        boolean player = shown instanceof Player;
        boolean developer = shown instanceof Developer;
        show(nicknameRow, player);
        show(cardRow, player);
        show(studioRow, developer);
        show(bankRow, developer);
        if (shown instanceof Player p) {
            nicknameValue.setText(p.getNickname());
            cardValue.setText(describe(p.getCard()));
        }
        if (shown instanceof Developer d) {
            studioValue.setText(d.getStudioName());
            bankValue.setText(maskIban(d.getBankAccount()));
        }
        deactivateHint.setText(I18n.text(developer ? "profile.deactivate.hintDeveloper" : "profile.deactivate.hint"));
        showAvatar(shown);
    }

    /**
     * Shows the user's avatar, or their initials while there is none.
     *
     * @param shown the user
     */
    private void showAvatar(User shown) {
        avatarInitials.setText((shown.getFirstName().substring(0, 1) + shown.getLastName().substring(0, 1))
                .toUpperCase(Locale.ROOT));
        avatarInitials.setVisible(true);
        avatarImage.setVisible(false);
        removeAvatarButton.setDisable(shown.getAvatarId() == null);
        context.images().load(shown.getAvatarId(), 2 * AVATAR_RADIUS * 2, 2 * AVATAR_RADIUS * 2, this::avatarLoaded);
    }

    /**
     * Shows a loaded avatar image.
     *
     * @param image the decoded avatar
     */
    private void avatarLoaded(Image image) {
        avatarImage.setImage(image);
        avatarImage.setVisible(true);
        avatarInitials.setVisible(false);
    }

    /**
     * Fills an editor with the current values.
     *
     * @param editor the editor about to open
     */
    private void prefill(VBox editor) {
        if (editor == nameEditor) {
            firstNameField.setText(user.getFirstName());
            lastNameField.setText(user.getLastName());
        } else if (editor == emailEditor) {
            emailField.setText(user.getEmail());
        } else if (editor == phoneEditor) {
            Phone phone = user.getPhone();
            phoneCodeField.setText(phone == null ? "" : phone.countryCode());
            phoneNumberField.setText(phone == null ? "" : phone.number());
        } else if (editor == addressEditor) {
            Address address = user.getAddress();
            streetField.setText(address == null ? "" : address.street());
            detailsField.setText(address == null || address.details() == null ? "" : address.details());
            postalCodeField.setText(address == null ? "" : address.postalCode());
            cityField.setText(address == null ? "" : address.city());
            provinceField.setText(address == null || address.province() == null ? "" : address.province());
            Choices.countries(countryBox, address == null ? DEFAULT_COUNTRY : address.country());
        } else if (editor == passwordEditor) {
            currentPasswordField.clear();
            newPasswordField.clear();
            repeatPasswordField.clear();
        } else if (editor == nicknameEditor && user instanceof Player p) {
            nicknameField.setText(p.getNickname());
        } else if (editor == cardEditor && user instanceof Player p) {
            PaymentCard card = p.getCard();
            cardHolderField.setText(card == null ? user.fullName() : card.holderName());
            cardNumberField.clear();
            expiryMonthBox.setValue(card == null ? null : card.expiryMonth());
            expiryYearBox.setValue(card == null ? null : card.expiryYear());
        } else if (editor == studioEditor && user instanceof Developer d) {
            studioNameField.setText(d.getStudioName());
        } else if (editor == bankEditor && user instanceof Developer d) {
            bankAccountField.setText(d.getBankAccount());
        }
    }

    /**
     * Builds the whole profile from the stored values and the open editor's fields.
     *
     * @return the profile to save
     */
    private ProfileForm profileForm() {
        boolean name = nameEditor.isVisible();
        Phone phone = user.getPhone();
        Address address = user.getAddress();
        PhoneForm phoneForm = phoneEditor.isVisible()
                ? new PhoneForm(phoneCodeField.getText(), phoneNumberField.getText())
                : new PhoneForm(phone == null ? null : phone.countryCode(), phone == null ? null : phone.number());
        AddressForm addressForm;
        if (addressEditor.isVisible()) {
            addressForm = new AddressForm(streetField.getText(), detailsField.getText(), postalCodeField.getText(),
                    cityField.getText(), provinceField.getText(), countryBox.getValue());
        } else {
            addressForm = address == null ? null : new AddressForm(address.street(), address.details(),
                    address.postalCode(), address.city(), address.province(), address.country());
        }
        return new ProfileForm(name ? firstNameField.getText() : user.getFirstName(),
                name ? lastNameField.getText() : user.getLastName(),
                emailEditor.isVisible() ? emailField.getText() : user.getEmail(), phoneForm, addressForm);
    }

    /** Closes every editor and clears the form's marks. */
    private void closeEditors() {
        editors.values().forEach(editor -> show(editor, false));
        form.clear();
    }

    /**
     * Shows or hides a part of the screen, taking its space only when shown.
     *
     * @param node    the part
     * @param visible whether to show it
     */
    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /**
     * Describes an address on one line.
     *
     * @param address the address, or {@code null}
     * @return the text shown in the address row
     */
    private static String describe(Address address) {
        if (address == null) {
            return I18n.text("profile.notSet");
        }
        List<String> parts = new ArrayList<>();
        parts.add(address.details() == null ? address.street() : address.street() + ", " + address.details());
        parts.add(address.postalCode() + " " + address.city()
                + (address.province() == null ? "" : " (" + address.province() + ")"));
        parts.add(Choices.countryName(address.country()));
        return String.join(" · ", parts);
    }

    /**
     * Describes a saved card without revealing it.
     *
     * @param card the card, or {@code null}
     * @return brand, last digits and expiry
     */
    private static String describe(PaymentCard card) {
        if (card == null) {
            return I18n.text("profile.notSet");
        }
        return Formats.label(card.brand()) + " " + card.masked()
                + " · " + String.format("%02d/%d", card.expiryMonth(), card.expiryYear());
    }

    /**
     * Masks an IBAN, keeping the country, check digits and last four characters.
     *
     * @param iban the normalized IBAN
     * @return the masked IBAN
     */
    private static String maskIban(String iban) {
        return iban.length() <= 8 ? iban : iban.substring(0, 4) + " •••• " + iban.substring(iban.length() - 4);
    }

    /**
     * Checks that the chosen card expiry is not in the past.
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
