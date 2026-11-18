package com.mist.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One line of the database operation log: who did what, on which collection, and whether it worked.
 *
 * @param time       when it happened, in the machine's time zone
 * @param user       the email of the logged-in user, or {@code anonymous}
 * @param operation  what was done, such as {@code INSERT} or {@code ROLLBACK}
 * @param collection the collection it touched, or {@code -}
 * @param details    the ids or filters involved, and the reason of a failure
 * @param ok         {@code true} for {@code OK}, {@code false} for {@code FAILED}
 */
public record LogEntry(LocalDateTime time, String user, String operation, String collection, String details,
                       boolean ok) {

    /** The date and time format of the log. */
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Rebuilds the line as it is written in the log file.
     *
     * @return the log line
     */
    public String line() {
        return String.join(" | ", TIME_FORMAT.format(time), "user=" + user, operation, collection, details,
                ok ? "OK" : "FAILED");
    }
}
