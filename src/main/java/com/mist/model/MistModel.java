package com.mist.model;

import java.util.List;

/**
 * The classes Hibernate maps: every entity and every embeddable of the model.
 *
 * <p>Pass {@link #ANNOTATED_CLASSES} to {@code HibernateBootstrap.buildSessionFactory}. A new entity or embeddable is
 * added here, or Hibernate does not know it.</p>
 */
public final class MistModel {

    /** Every class annotated with {@code @Entity} or {@code @Embeddable} in this package. */
    public static final List<Class<?>> ANNOTATED_CLASSES = List.of(
            PaymentCard.class,
            Preferences.class,
            Phone.class,
            Address.class);

    private MistModel() {
    }
}
