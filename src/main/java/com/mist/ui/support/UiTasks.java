package com.mist.ui.support;

import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs service calls off the JavaFX thread (behavior C-1 of {@code docs/ui-design.md}).
 *
 * <p>Each call becomes a {@link Task} on a worker thread, so the window stays responsive while MongoDB answers. The
 * control that started the call is disabled and the cursor shows "wait" until it ends; then the success or failure
 * handler runs on the JavaFX thread. Failures without a handler go to the {@link ErrorReporter}, with a retry that
 * runs the same work again.</p>
 */
public final class UiTasks {

    /**
     * A unit of work run on a worker thread, usually one service call.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface Work<T> {

        /**
         * Does the work.
         *
         * @return the result handed to the success handler
         * @throws Exception any failure, handed to the failure handler
         */
        T call() throws Exception;
    }

    /** Runs the tasks: a worker pool in the app, the calling thread in UI tests. */
    private final Executor executor;
    /** Reacts to failures that the caller does not handle. */
    private final ErrorReporter errors;
    /** The main window, owner of the error dialogs of tasks started without a control. */
    private final Supplier<Window> mainWindow;
    /** Number of tasks running, so the wait cursor stays while any of them runs. */
    private int running;

    /**
     * Creates the task runner of the main window.
     *
     * @param executor   where tasks run
     * @param errors     the reaction to unhandled failures
     * @param mainWindow the main window
     */
    public UiTasks(Executor executor, ErrorReporter errors, Supplier<Window> mainWindow) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.errors = Objects.requireNonNull(errors, "errors");
        this.mainWindow = Objects.requireNonNull(mainWindow, "mainWindow");
    }

    /**
     * Runs work in the background; a failure is reported by the {@link ErrorReporter}, which may offer to retry.
     *
     * @param busy      the control to disable while the work runs, or {@code null}
     * @param work      the work, usually one service call
     * @param onSuccess what to do with the result, on the JavaFX thread
     * @param <T>       the result type
     */
    public <T> void run(Node busy, Work<T> work, Consumer<T> onSuccess) {
        run(busy, work, onSuccess, error -> errors.report(ownerOf(busy), error, () -> run(busy, work, onSuccess)));
    }

    /**
     * Runs work in the background with the caller's own failure handler.
     *
     * @param busy      the control to disable while the work runs, or {@code null}
     * @param work      the work, usually one service call
     * @param onSuccess what to do with the result, on the JavaFX thread
     * @param onFailure what to do with a failure, on the JavaFX thread
     * @param <T>       the result type
     */
    public <T> void run(Node busy, Work<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        Runnable release = hold(busy);
        task.setOnSucceeded(event -> {
            release.run();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            release.run();
            onFailure.accept(task.getException());
        });
        executor.execute(task);
    }

    /**
     * Returns the window that owns the dialogs about a control's task.
     *
     * @param busy the control, or {@code null}
     * @return the control's window, or the main window
     */
    private Window ownerOf(Node busy) {
        if (busy != null && busy.getScene() != null && busy.getScene().getWindow() != null) {
            return busy.getScene().getWindow();
        }
        return mainWindow.get();
    }

    /**
     * Disables a control and shows the wait cursor while a task runs.
     *
     * @param busy the control, or {@code null}
     * @return what undoes it when the task ends
     */
    private Runnable hold(Node busy) {
        Scene scene = busy == null ? null : busy.getScene();
        boolean disable = busy != null && !busy.isDisable() && !busy.disableProperty().isBound();
        if (disable) {
            busy.setDisable(true);
        }
        running++;
        if (scene != null) {
            scene.setCursor(Cursor.WAIT);
        }
        return () -> {
            running--;
            if (disable) {
                busy.setDisable(false);
            }
            if (scene != null && running == 0) {
                scene.setCursor(Cursor.DEFAULT);
            }
        };
    }
}
