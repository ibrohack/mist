package com.mist;

import com.mist.config.AppConfig;
import com.mist.exception.DataAccessException;
import com.mist.ui.AppServices;
import com.mist.ui.Backend;

/**
 * The real backend of the UI: it opens {@link MistContext} on the configured MongoDB and hands its services to the
 * views.
 *
 * <p>A failed connection leaves nothing open, so the offline screen can simply try again.</p>
 */
final class MistBackend implements Backend {

    /** The open context, or {@code null} while disconnected. */
    private MistContext context;

    @Override
    public synchronized AppServices connect() throws DataAccessException {
        if (context == null) {
            context = MistContext.open(AppConfig.load());
        }
        return new AppServices(context.auth(), context.users(), context.store(), context.library(),
                context.reviews(), context.publishing(), context.media());
    }

    @Override
    public synchronized void close() {
        if (context != null) {
            context.close();
            context = null;
        }
    }
}
