package com.mist.ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Asks for files with the operating system's file dialogs, titled and filtered in the current language.
 */
public final class SystemFilePicker implements FilePicker {

    /** Creates the picker; each dialog is built when asked for. */
    public SystemFilePicker() {
        // Nothing to set up.
    }

    @Override
    public Optional<Path> openImage(Window owner) {
        ResourceBundle bundle = I18n.bundle();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(bundle.getString("file.openImage"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                bundle.getString("file.images"), "*.png", "*.jpg", "*.jpeg", "*.gif"));
        return toPath(chooser.showOpenDialog(owner));
    }

    @Override
    public Optional<Path> savePdf(Window owner, String suggestedName) {
        ResourceBundle bundle = I18n.bundle();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(bundle.getString("file.savePdf"));
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("file.pdf"), "*.pdf"));
        return toPath(chooser.showSaveDialog(owner));
    }

    /**
     * Converts a dialog result.
     *
     * @param file the file the dialog returned, {@code null} when cancelled
     * @return the path, or empty
     */
    private static Optional<Path> toPath(File file) {
        return Optional.ofNullable(file).map(File::toPath);
    }
}
