package com.mist.ui;

/**
 * Implemented by controllers that load their data when their screen is shown.
 *
 * <p>{@link Navigator} calls it after the view is on screen, so the controller can start its service calls and use
 * the parameter of the navigation, such as the game a game page shows.</p>
 */
public interface NavigationAware {

    /**
     * Called once each time the screen is shown.
     *
     * @param parameter what the navigation passed to the screen, or {@code null}
     */
    void onNavigatedTo(Object parameter);
}
