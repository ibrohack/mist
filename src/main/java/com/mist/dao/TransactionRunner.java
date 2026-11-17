package com.mist.dao;

import com.mist.exception.MistException;

/**
 * Runs a unit of work in one database transaction, so a use case that changes several documents commits all of them or
 * none of them.
 *
 * <p>DAO calls made inside the work join the transaction. A call nested inside another joins the outer one. When the
 * work throws, the transaction is rolled back and the exception reaches the caller unchanged; a failure of the
 * database itself, including the commit, arrives as a {@link com.mist.exception.DataAccessException}.</p>
 *
 * <p>Work that catches a DAO exception must not go on writing: the transaction is already broken, so it should let the
 * exception through.</p>
 */
public interface TransactionRunner {

    /**
     * Runs work that returns a value in one transaction.
     *
     * @param work the work to run
     * @param <T>  the type of the result
     * @return what the work returned, after the commit
     * @throws MistException what the work threw, or a {@code DataAccessException} if the database failed
     */
    <T> T call(Work<T> work) throws MistException;

    /**
     * Runs work without a result in one transaction.
     *
     * @param action the work to run
     * @throws MistException what the work threw, or a {@code DataAccessException} if the database failed
     */
    default void run(Action action) throws MistException {
        call(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Work that returns a value.
     *
     * @param <T> the type of the result
     */
    @FunctionalInterface
    interface Work<T> {

        /**
         * Does the work.
         *
         * @return the result
         * @throws MistException if a rule or a DAO call fails
         */
        T run() throws MistException;
    }

    /** Work without a result. */
    @FunctionalInterface
    interface Action {

        /**
         * Does the work.
         *
         * @throws MistException if a rule or a DAO call fails
         */
        void run() throws MistException;
    }
}
