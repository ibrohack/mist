/**
 * Domain model: the documents stored in MongoDB, the values embedded in them, and their enums.
 *
 * <p>Five collections hold the entities: {@code users} ({@link com.mist.model.Player}, {@link com.mist.model.Developer}
 * and {@link com.mist.model.Admin} in one collection), {@code games}, {@code orders}, {@code reviews} and {@code media}.
 * Documents reference each other by {@code ObjectId}; embedded values are immutable records annotated with
 * {@code @Embeddable @Struct}, and entities replace them (or their lists) instead of changing them in place.</p>
 *
 * <p>Factories and mutators check every invariant. A broken invariant throws an {@link IllegalArgumentException} or
 * {@link IllegalStateException} whose message is a {@link com.mist.model.ModelKeys} bundle key, which services turn into
 * a {@code ValidationException}.</p>
 *
 * <p>Entities may carry Jakarta Persistence and Hibernate mapping annotations. The model depends on no other MIST
 * layer.</p>
 */
package com.mist.model;
