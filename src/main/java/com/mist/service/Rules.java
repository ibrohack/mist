package com.mist.service;

import com.mist.exception.ValidationException;

/**
 * Runs model code and turns its rule violations into {@link ValidationException}.
 *
 * <p>Model factories and mutators throw {@code IllegalArgumentException} or {@code IllegalStateException} whose message
 * is a {@code model.*} bundle key. That key becomes the key of the {@code ValidationException}; a message that is not a
 * key becomes {@link ServiceKeys#INVALID_INPUT}.</p>
 */
final class Rules {

    /** The prefix of the model's bundle keys. */
    private static final String MODEL_KEY_PREFIX = "model.";

    /** Not instantiable: a holder of rule helpers. */
    private Rules() {
    }

    /**
     * Runs model code that returns a value.
     *
     * @param call the model code
     * @param <T>  the type of the result
     * @return what the model code returned
     * @throws ValidationException if the model code rejected a value or a state change
     */
    static <T> T check(ModelCall<T> call) throws ValidationException {
        try {
            return call.get();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw invalid(e);
        }
    }

    /**
     * Runs model code without a result.
     *
     * @param action the model code
     * @throws ValidationException if the model code rejected a value or a state change
     */
    static void apply(ModelAction action) throws ValidationException {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw invalid(e);
        }
    }

    /**
     * Wraps a model rule violation.
     *
     * @param violation the exception the model threw
     * @return a validation exception with the model's key, or {@link ServiceKeys#INVALID_INPUT}
     */
    static ValidationException invalid(RuntimeException violation) {
        String message = violation.getMessage();
        String key = message != null && message.startsWith(MODEL_KEY_PREFIX) ? message : ServiceKeys.INVALID_INPUT;
        return new ValidationException(key, violation);
    }

    /**
     * Model code that returns a value.
     *
     * @param <T> the type of the result
     */
    @FunctionalInterface
    interface ModelCall<T> {

        /**
         * Runs the model code.
         *
         * @return the result
         */
        T get();
    }

    /** Model code without a result. */
    @FunctionalInterface
    interface ModelAction {

        /** Runs the model code. */
        void run();
    }
}
