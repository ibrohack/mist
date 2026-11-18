package com.mist.ui;

/**
 * The content of the help window, which {@link Navigator#showHelp()} opens on the topic of the current screen.
 */
public interface HelpPanel {

    /**
     * Shows the topic of a screen, or the general topic if that screen has none.
     *
     * @param screen the screen the user was on
     */
    void select(View screen);
}
