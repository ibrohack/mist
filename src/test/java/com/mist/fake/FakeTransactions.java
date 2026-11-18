package com.mist.fake;

import com.mist.dao.TransactionRunner;
import com.mist.exception.MistException;

/**
 * {@link TransactionRunner} for the in-memory database: it runs the work directly, failing first if the database is
 * offline or a failure was planned. It does not roll back.
 */
public final class FakeTransactions implements TransactionRunner {

    /** The shared in-memory database. */
    private final FakeDatabase db;

    /**
     * Creates the runner.
     *
     * @param db the shared in-memory database
     */
    public FakeTransactions(FakeDatabase db) {
        this.db = db;
    }

    @Override
    public <T> T call(Work<T> work) throws MistException {
        db.check();
        return work.run();
    }
}
