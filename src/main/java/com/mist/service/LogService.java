package com.mist.service;

import com.mist.dao.UserDao;
import com.mist.exception.DataAccessException;
import com.mist.exception.MistException;
import com.mist.exception.UnauthorizedException;
import com.mist.model.Admin;
import org.bson.types.ObjectId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the database operation log for admins, so they can see it inside MIST.
 *
 * <p>The data layer writes one line per operation to {@code mist-db.log} in the log folder; at midnight the log rolls
 * into {@code mist-db.yyyy-MM-dd.log}. This service lists the days the folder holds and parses a day's lines into
 * {@link LogEntry} records, from the rolled file of that day and from the current file. Only an active
 * {@link Admin} may read it.</p>
 */
public class LogService {

    /** Name of the file the log is written to. */
    public static final String CURRENT_FILE = "mist-db.log";
    /** Name of a rolled file, with the day it holds. */
    private static final Pattern ROLLED_FILE = Pattern.compile("mist-db\\.(\\d{4}-\\d{2}-\\d{2})\\.log");
    /** Splits a line into its six columns. */
    private static final Pattern COLUMNS = Pattern.compile(" \\| ");
    /** The prefix of the user column. */
    private static final String USER_PREFIX = "user=";
    /** Length of the date at the start of each line. */
    private static final int DATE_LENGTH = 10;

    /** The users collection, to check the reader is an admin. */
    private final UserDao users;
    /** The folder the log files are in. */
    private final Path directory;

    /**
     * Creates the service on the log folder.
     *
     * @param users     the users DAO
     * @param directory the folder of {@code mist-db.log}, usually {@code ~/.mist/logs}
     */
    public LogService(UserDao users, Path directory) {
        this.users = Objects.requireNonNull(users, "users");
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    /**
     * Lists the days the log has entries for.
     *
     * @param adminId the admin who reads the log
     * @return the days, newest first; empty if nothing was logged yet
     * @throws UnauthorizedException if the reader is not an active admin
     * @throws DataAccessException   if the log folder cannot be read ({@code service.log.unreadable})
     * @throws MistException         if the user cannot be loaded
     */
    public List<LocalDate> days(ObjectId adminId) throws MistException {
        admin(adminId);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        TreeSet<LocalDate> days = new TreeSet<>(Comparator.reverseOrder());
        try (Stream<Path> files = Files.list(directory)) {
            files.map(file -> ROLLED_FILE.matcher(file.getFileName().toString()))
                    .filter(Matcher::matches)
                    .forEach(match -> days.add(LocalDate.parse(match.group(1))));
        } catch (IOException e) {
            throw new DataAccessException(ServiceKeys.LOG_UNREADABLE, e);
        }
        Path current = directory.resolve(CURRENT_FILE);
        if (Files.isRegularFile(current)) {
            for (LogEntry entry : read(current, entry -> true)) {
                days.add(entry.time().toLocalDate());
            }
        }
        return List.copyOf(days);
    }

    /**
     * Reads the entries of one day, in the order they were written.
     *
     * @param adminId the admin who reads the log
     * @param day     the day
     * @return the day's entries; empty if the day has none
     * @throws UnauthorizedException if the reader is not an active admin
     * @throws DataAccessException   if a log file cannot be read ({@code service.log.unreadable})
     * @throws MistException         if the user cannot be loaded
     */
    public List<LogEntry> entries(ObjectId adminId, LocalDate day) throws MistException {
        admin(adminId);
        Objects.requireNonNull(day, "day");
        List<LogEntry> entries = new ArrayList<>();
        Path rolled = directory.resolve("mist-db." + day + ".log");
        Predicate<LogEntry> sameDay = entry -> entry.time().toLocalDate().equals(day);
        if (Files.isRegularFile(rolled)) {
            entries.addAll(read(rolled, sameDay));
        }
        Path current = directory.resolve(CURRENT_FILE);
        if (Files.isRegularFile(current)) {
            entries.addAll(read(current, sameDay));
        }
        return List.copyOf(entries);
    }

    /**
     * Checks that the reader is an active admin.
     *
     * @param adminId the reader
     * @throws MistException if the reader is missing, deactivated or not an admin
     */
    private void admin(ObjectId adminId) throws MistException {
        if (!(Accounts.active(users, adminId) instanceof Admin)) {
            throw new UnauthorizedException(ServiceKeys.USER_NOT_ADMIN);
        }
    }

    /**
     * Parses the lines of a log file that a filter keeps. Lines in another format are skipped.
     *
     * @param file   the log file
     * @param filter which entries to keep
     * @return the entries, in file order
     * @throws DataAccessException if the file cannot be read
     */
    private static List<LogEntry> read(Path file, Predicate<LogEntry> filter) throws DataAccessException {
        List<LogEntry> entries = new ArrayList<>();
        // An InputStreamReader replaces bytes that are not UTF-8 instead of failing on them.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                parse(line).filter(filter).ifPresent(entries::add);
            }
        } catch (IOException e) {
            throw new DataAccessException(ServiceKeys.LOG_UNREADABLE, e);
        }
        return entries;
    }

    /**
     * Parses one log line: {@code date | user=email | OPERATION | collection | details | OK/FAILED}.
     *
     * @param line the line
     * @return the entry, or empty if the line is in another format
     */
    static Optional<LogEntry> parse(String line) {
        if (line.length() <= DATE_LENGTH) {
            return Optional.empty();
        }
        String[] columns = COLUMNS.split(line, -1);
        if (columns.length != 6 || !columns[1].startsWith(USER_PREFIX)) {
            return Optional.empty();
        }
        try {
            LocalDateTime time = LocalDateTime.parse(columns[0], LogEntry.TIME_FORMAT);
            return Optional.of(new LogEntry(time, columns[1].substring(USER_PREFIX.length()), columns[2],
                    columns[3], columns[4], "OK".equals(columns[5].strip())));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
