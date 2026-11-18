package com.mist;

import com.mist.ui.I18n;
import com.mist.ui.Navigator;
import com.mist.ui.Styles;
import com.mist.ui.SystemFilePicker;
import com.mist.ui.UiContext;
import com.mist.ui.View;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The MIST desktop application. It builds the UI context on the real backend and opens the splash screen, which
 * leads to the rest of the app.
 */
public class MistApp extends Application {

    /** Smallest window width the layouts are designed for. */
    private static final double MIN_WIDTH = 1024;
    /** Smallest window height the layouts are designed for. */
    private static final double MIN_HEIGHT = 640;

    /** The worker threads that run service calls, so the window never waits on MongoDB. */
    private final ExecutorService workers = Executors.newCachedThreadPool(new WorkerThreads());
    /** The context of the main window, closed when the app stops. */
    private UiContext context;

    /** Creates the application; JavaFX calls {@link #start(Stage)} next. */
    public MistApp() {
        // JavaFX creates the application through this constructor.
    }

    @Override
    public void start(Stage stage) {
        I18n.use(I18n.DEFAULT_LOCALE);
        context = new UiContext(new MistBackend(), new SystemFilePicker(), workers);
        Navigator navigator = new Navigator(stage, context);
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> showUnexpected(navigator, error));
        stage.setTitle(I18n.text("app.title"));
        stage.getIcons().setAll(Styles.appIcons());
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        navigator.show(View.SPLASH);
        stage.show();
    }

    @Override
    public void stop() {
        workers.shutdownNow();
        if (context != null) {
            context.close();
        }
    }

    /**
     * Shows an exception nobody caught: it is logged and the user sees a generic error (behavior C-2).
     *
     * @param navigator the navigator of the main window
     * @param error     the uncaught exception
     */
    private void showUnexpected(Navigator navigator, Throwable error) {
        if (Platform.isFxApplicationThread()) {
            context.errors().unexpected(navigator.window(), error);
        } else {
            Platform.runLater(() -> context.errors().unexpected(navigator.window(), error));
        }
    }

    /** Creates the daemon worker threads, named {@code mist-worker-n}, so they never keep the app alive. */
    private static final class WorkerThreads implements ThreadFactory {

        /** Number of the next thread. */
        private final AtomicInteger next = new AtomicInteger(1);

        /** Creates the factory; threads are numbered from 1. */
        WorkerThreads() {
            // Nothing else to set up.
        }

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "mist-worker-" + next.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
