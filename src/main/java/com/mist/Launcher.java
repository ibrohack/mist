package com.mist;

import javafx.application.Application;

/**
 * Starts MIST.
 *
 * <p>MIST runs on the classpath, and the JavaFX launcher refuses to start an {@code Application} subclass as the main
 * class in that setup, so this plain class starts {@link MistApp} instead.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments, passed on to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(MistApp.class, args);
    }
}
