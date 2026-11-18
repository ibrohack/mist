package com.mist.fake;

import com.mist.ui.FilePicker;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Answers the file dialogs of the UI tests with the files the test chose beforehand, or "cancelled".
 */
public final class StubFilePicker implements FilePicker {

    /** The image the next open dialog returns, or {@code null} for cancel. */
    private Path nextImage;
    /** The file the next save dialog returns, or {@code null} for cancel. */
    private Path nextSave;

    /**
     * Sets the answer of the next image dialog.
     *
     * @param image the file to return, or {@code null} to cancel
     */
    public void answerImage(Path image) {
        this.nextImage = image;
    }

    /**
     * Sets the answer of the next save dialog.
     *
     * @param target the file to return, or {@code null} to cancel
     */
    public void answerSave(Path target) {
        this.nextSave = target;
    }

    @Override
    public Optional<Path> openImage(Window owner) {
        Path answer = nextImage;
        nextImage = null;
        return Optional.ofNullable(answer);
    }

    @Override
    public Optional<Path> savePdf(Window owner, String suggestedName) {
        Path answer = nextSave;
        nextSave = null;
        return Optional.ofNullable(answer);
    }
}
