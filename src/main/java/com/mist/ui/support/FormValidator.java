package com.mist.ui.support;

import com.mist.exception.DuplicateEntityException;
import com.mist.exception.MistException;
import com.mist.exception.UnauthorizedException;
import com.mist.exception.ValidationException;
import com.mist.service.AuthService;
import com.mist.ui.I18n;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/**
 * Checks a form before its service call and marks the fields in error (behavior C-3 of {@code docs/ui-design.md}).
 *
 * <p>A controller declares the rules of its fields once, when the view loads. {@link #validate()} runs them on the
 * visible fields: a failing field gets the {@code :error} pseudo-class and a tooltip, the first one is focused, and
 * the form message shows its text. When the service still rejects the data, {@link #showServiceError(MistException)}
 * marks the field bound to the exception's key. Editing a field clears its mark.</p>
 */
public final class FormValidator {

    /** The pseudo-class that paints a field in error. */
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    /** The email shape the model accepts. */
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+");
    /** Bundle key of a required field left blank. */
    private static final String REQUIRED = "validation.required";

    /**
     * One check of one field.
     *
     * @param field the field the check marks
     * @param valid whether the field passes
     * @param key   the bundle key of the message when it does not
     */
    private record Rule(Control field, BooleanSupplier valid, String key) {
    }

    /** The label under the form that shows the first error, or a success. */
    private final Label message;
    /** The rules, in the order of the fields. */
    private final List<Rule> rules = new ArrayList<>();
    /** Which field a service error key belongs to. */
    private final Map<String, Control> bindings = new LinkedHashMap<>();
    /** The fields that are marked now. */
    private final List<Control> marked = new ArrayList<>();

    /**
     * Creates the validator of a form.
     *
     * @param message the label that shows the form's messages, or {@code null}
     */
    public FormValidator(Label message) {
        this.message = message;
        if (message != null) {
            message.setText("");
        }
    }

    /**
     * Requires a text field to be filled in.
     *
     * @param field the field
     * @return this validator
     */
    public FormValidator required(TextInputControl field) {
        return rule(field, () -> !text(field).isEmpty(), REQUIRED);
    }

    /**
     * Requires a choice in a combo box or date picker.
     *
     * @param field the field
     * @return this validator
     */
    public FormValidator required(ComboBoxBase<?> field) {
        return rule(field, () -> field.getValue() != null, REQUIRED);
    }

    /**
     * Requires an email address the model accepts.
     *
     * @param field the field
     * @param key   the bundle key of the message
     * @return this validator
     */
    public FormValidator email(TextInputControl field, String key) {
        return rule(field, () -> EMAIL.matcher(text(field)).matches(), key);
    }

    /**
     * Limits the length of a text, as the model does.
     *
     * @param field the field
     * @param max   the most characters allowed
     * @param key   the bundle key of the message
     * @return this validator
     */
    public FormValidator maxLength(TextInputControl field, int max, String key) {
        return rule(field, () -> text(field).length() <= max, key);
    }

    /**
     * Requires a filled-in text to match a pattern; a blank field passes, so combine with {@link #required}.
     *
     * @param field   the field
     * @param pattern the pattern the whole text must match, after removing spaces if {@code compact}
     * @param compact whether spaces are ignored, as in card numbers and IBANs
     * @param key     the bundle key of the message
     * @return this validator
     */
    public FormValidator pattern(TextInputControl field, Pattern pattern, boolean compact, String key) {
        return rule(field, () -> {
            String value = compact ? text(field).replaceAll("[\\s-]", "") : text(field);
            return value.isEmpty() || pattern.matcher(value).matches();
        }, key);
    }

    /**
     * Requires a repeated password to equal the original.
     *
     * @param repeat   the repeated field
     * @param original the original field
     * @return this validator
     */
    public FormValidator sameText(TextInputControl repeat, TextInputControl original) {
        return rule(repeat, () -> Objects.equals(repeat.getText(), original.getText()), "validation.passwordMismatch");
    }

    /**
     * Requires a password the service accepts: 8 to 128 characters with a letter and a digit.
     *
     * @param field the password field
     * @return this validator
     */
    public FormValidator password(TextInputControl field) {
        return rule(field, () -> {
            String value = field.getText() == null ? "" : field.getText();
            return value.length() >= AuthService.MIN_PASSWORD_LENGTH
                    && value.length() <= AuthService.MAX_PASSWORD_LENGTH
                    && value.chars().anyMatch(Character::isLetter) && value.chars().anyMatch(Character::isDigit);
        }, "service.auth.weakPassword");
    }

    /**
     * Adds any other check.
     *
     * @param field the field the check marks
     * @param valid whether the field passes
     * @param key   the bundle key of the message when it does not
     * @return this validator
     */
    public FormValidator rule(Control field, BooleanSupplier valid, String key) {
        rules.add(new Rule(field, valid, key));
        clearOnEdit(field);
        return this;
    }

    /**
     * Binds a service error key to the field it is about, such as {@code service.user.emailTaken} to the email field.
     *
     * @param key   the bundle key the service throws
     * @param field the field to mark
     * @return this validator
     */
    public FormValidator bind(String key, Control field) {
        bindings.put(key, field);
        clearOnEdit(field);
        return this;
    }

    /**
     * Runs the rules of the visible fields and marks those that fail.
     *
     * @return {@code true} if every visible field passes
     */
    public boolean validate() {
        clear();
        Control first = null;
        String firstKey = null;
        for (Rule rule : rules) {
            if (marked.contains(rule.field()) || !isShowing(rule.field()) || rule.valid().getAsBoolean()) {
                continue;
            }
            mark(rule.field(), rule.key());
            if (first == null) {
                first = rule.field();
                firstKey = rule.key();
            }
        }
        if (first == null) {
            return true;
        }
        first.requestFocus();
        showMessage(firstKey, false);
        return false;
    }

    /**
     * Shows a service's rejection on the form: on the bound field if there is one, and in the form message.
     *
     * @param error what the service threw
     * @return {@code true} if the form showed it; {@code false} for errors the form cannot show, such as a database
     *         failure, which the caller hands to the {@link ErrorReporter}
     */
    public boolean showServiceError(MistException error) {
        boolean formError = error instanceof ValidationException || error instanceof DuplicateEntityException
                || (error instanceof UnauthorizedException && bindings.containsKey(error.getMessage()));
        if (!formError) {
            return false;
        }
        clear();
        Control field = bindings.get(error.getMessage());
        if (field != null) {
            mark(field, error.getMessage());
            field.requestFocus();
        }
        showMessage(error.getMessage(), false);
        return true;
    }

    /**
     * Shows a message under the form.
     *
     * @param key     the bundle key of the message
     * @param success {@code true} for a success, {@code false} for an error
     */
    public void showMessage(String key, boolean success) {
        if (message == null) {
            return;
        }
        message.setText(I18n.textOrKey(key));
        message.getStyleClass().removeAll("mist-message-ok", "mist-message-error");
        message.getStyleClass().add(success ? "mist-message-ok" : "mist-message-error");
    }

    /** Removes every mark and the form message. */
    public void clear() {
        for (Control field : List.copyOf(marked)) {
            unmark(field);
        }
        if (message != null) {
            message.setText("");
        }
    }

    /**
     * Marks a field in error.
     *
     * @param field the field
     * @param key   the bundle key of its message
     */
    private void mark(Control field, String key) {
        field.pseudoClassStateChanged(ERROR, true);
        field.setTooltip(new Tooltip(I18n.textOrKey(key)));
        marked.add(field);
    }

    /**
     * Removes a field's mark.
     *
     * @param field the field
     */
    private void unmark(Control field) {
        field.pseudoClassStateChanged(ERROR, false);
        field.setTooltip(null);
        marked.remove(field);
    }

    /**
     * Clears a field's mark as soon as the user edits it.
     *
     * @param field the field
     */
    private void clearOnEdit(Control field) {
        ObservableValue<?> value = switch (field) {
            case TextInputControl text -> text.textProperty();
            case ComboBoxBase<?> box -> box.valueProperty();
            default -> null;
        };
        if (value != null && field.getProperties().putIfAbsent("mist.clearOnEdit", Boolean.TRUE) == null) {
            value.addListener((observable, before, after) -> {
                if (marked.contains(field)) {
                    unmark(field);
                }
            });
        }
    }

    /**
     * Tells whether a field and all its parents are visible, so hidden sections are not checked.
     *
     * @param field the field
     * @return {@code true} if the user can see it
     */
    private static boolean isShowing(Node field) {
        for (Node node = field; node != null; node = node.getParent()) {
            if (!node.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a field's text without surrounding spaces.
     *
     * @param field the field
     * @return the text, never {@code null}
     */
    private static String text(TextInputControl field) {
        return field.getText() == null ? "" : field.getText().strip();
    }
}
