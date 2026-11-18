package com.mist.ui;

import javafx.scene.Node;

/**
 * The shell around the screens of a session: the header with the user menu, and the area where screens are shown.
 * {@link Navigator} puts each in-shell screen there.
 */
public interface ShellHost {

    /**
     * Shows a screen's content under the header.
     *
     * @param view    the screen
     * @param content the root of the screen's view
     */
    void showContent(View view, Node content);
}
