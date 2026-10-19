package com.mist.ui;

/**
 * The screens of MIST, each backed by one FXML file.
 */
public enum View {

    /** First screen: the brand art, the tagline and a loading bar while MIST starts. */
    SPLASH("view/splash.fxml");

    private final String fxml;

    View(String fxml) {
        this.fxml = fxml;
    }

    /**
     * Returns where the view's FXML file lives.
     *
     * @return the FXML path, relative to {@code com/mist/ui/}
     */
    public String fxml() {
        return fxml;
    }
}
