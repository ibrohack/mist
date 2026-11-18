package com.mist.ui;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Asks the user for a file. Controllers use it instead of {@code FileChooser}, so tests can answer without a native
 * dialog.
 */
public interface FilePicker {

    /**
     * Asks for a PNG, JPEG or GIF image to upload.
     *
     * @param owner the window the dialog belongs to
     * @return the chosen file, or empty if the user cancelled
     */
    Optional<Path> openImage(Window owner);

    /**
     * Asks where to save a PDF report.
     *
     * @param owner         the window the dialog belongs to
     * @param suggestedName the file name offered by default, such as {@code receipt.pdf}
     * @return the chosen file, or empty if the user cancelled
     */
    Optional<Path> savePdf(Window owner, String suggestedName);
}
