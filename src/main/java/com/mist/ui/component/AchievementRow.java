package com.mist.ui.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A row of the game editor's editable achievements table: a name and a description the developer edits in place.
 */
public final class AchievementRow {

    /** The achievement's name; named apart from {@link #nameProperty()} so Javadoc does not document it twice. */
    private final StringProperty nameText;
    /** How the achievement is unlocked; named apart from {@link #descriptionProperty()}, as above. */
    private final StringProperty descriptionText;

    /**
     * Creates a row.
     *
     * @param name        the name
     * @param description the description
     */
    public AchievementRow(String name, String description) {
        this.nameText = new SimpleStringProperty(this, "name", name);
        this.descriptionText = new SimpleStringProperty(this, "description", description);
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return nameText.get();
    }

    /**
     * Changes the name.
     *
     * @param value the new name
     */
    public void setName(String value) {
        nameText.set(value);
    }

    /**
     * Returns the name, observable by the table.
     *
     * @return the name property
     */
    public StringProperty nameProperty() {
        return nameText;
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    public String getDescription() {
        return descriptionText.get();
    }

    /**
     * Changes the description.
     *
     * @param value the new description
     */
    public void setDescription(String value) {
        descriptionText.set(value);
    }

    /**
     * Returns the description, observable by the table.
     *
     * @return the description property
     */
    public StringProperty descriptionProperty() {
        return descriptionText;
    }
}
