package com.mist.ui.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A row of the game editor's editable achievements table: a name and a description the developer edits in place.
 */
public final class AchievementRow {

    /** The achievement's name. */
    private final StringProperty name;
    /** How the achievement is unlocked. */
    private final StringProperty description;

    /**
     * Creates a row.
     *
     * @param name        the name
     * @param description the description
     */
    public AchievementRow(String name, String description) {
        this.name = new SimpleStringProperty(this, "name", name);
        this.description = new SimpleStringProperty(this, "description", description);
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Changes the name.
     *
     * @param value the new name
     */
    public void setName(String value) {
        name.set(value);
    }

    /**
     * Returns the name, observable by the table.
     *
     * @return the name property
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description.get();
    }

    /**
     * Changes the description.
     *
     * @param value the new description
     */
    public void setDescription(String value) {
        description.set(value);
    }

    /**
     * Returns the description, observable by the table.
     *
     * @return the description property
     */
    public StringProperty descriptionProperty() {
        return description;
    }
}
