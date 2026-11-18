package com.mist.fake;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that tests move forward, to reach the end of the refund window without waiting 14 days.
 */
public final class MutableClock extends Clock {

    /** The current instant. */
    private Instant now;

    /**
     * Creates a clock stopped at an instant.
     *
     * @param start the first instant
     */
    public MutableClock(Instant start) {
        this.now = start;
    }

    /**
     * Moves the clock forward.
     *
     * @param amount how far
     */
    public void advance(Duration amount) {
        now = now.plus(amount);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
