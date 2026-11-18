package com.mist.service;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hashes and checks passwords with Argon2id, stored in the PHC string format
 * {@code $argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>} that other Argon2 libraries read too.
 *
 * <p>The cost (19 MiB of memory, 2 passes, 1 lane) follows the OWASP minimum for Argon2id. Each hash gets a random
 * 16-byte salt and is 32 bytes long; salt and hash are Base64 without padding, as the PHC format requires. A check
 * reads the cost from the stored string, so hashes stay valid if the cost is raised later.</p>
 *
 * <p>Passwords arrive as {@code char[]} and are turned into UTF-8 bytes that are wiped after use; the caller wipes the
 * characters.</p>
 */
final class PasswordHasher {

    /** Argon2id memory cost in KiB (OWASP minimum). */
    static final int MEMORY_KIB = 19_456;
    /** Argon2id time cost (OWASP minimum). */
    static final int ITERATIONS = 2;
    /** Argon2id parallelism (OWASP minimum). */
    static final int PARALLELISM = 1;

    /** Length of the random salt. */
    private static final int SALT_BYTES = 16;
    /** Length of the derived hash. */
    private static final int HASH_BYTES = 32;
    /** The PHC string format of a stored Argon2id hash. */
    private static final Pattern PHC = Pattern.compile(
            "\\$argon2id\\$v=19\\$m=(\\d{1,8}),t=(\\d{1,4}),p=(\\d{1,3})\\$([A-Za-z0-9+/]+)\\$([A-Za-z0-9+/]+)");
    /** Encodes salts and hashes as unpadded Base64, as PHC strings do. */
    private static final Base64.Encoder ENCODER = Base64.getEncoder().withoutPadding();
    /** Decodes the Base64 parts of a stored hash. */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /** The source of salts. */
    private final SecureRandom random = new SecureRandom();

    /** Creates a hasher with its own source of salts. */
    PasswordHasher() {
        // The cost parameters are fixed constants.
    }

    /**
     * Hashes a new password.
     *
     * @param password the password
     * @return the PHC string to store
     */
    String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_BYTES);
        return "$argon2id$v=19$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + ENCODER.encodeToString(salt) + "$" + ENCODER.encodeToString(hash);
    }

    /**
     * Checks a password against a stored hash, in time that does not depend on where the bytes differ.
     *
     * @param password the password typed at login
     * @param stored   the stored PHC string
     * @return {@code true} if the password matches
     */
    boolean verify(char[] password, String stored) {
        Matcher phc = stored == null ? null : PHC.matcher(stored);
        if (phc == null || !phc.matches()) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = DECODER.decode(phc.group(4));
            expected = DECODER.decode(phc.group(5));
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = derive(password, salt, Integer.parseInt(phc.group(1)), Integer.parseInt(phc.group(2)),
                Integer.parseInt(phc.group(3)), expected.length);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * Derives an Argon2id hash.
     *
     * @param password    the password
     * @param salt        the salt
     * @param memoryKib   the memory cost in KiB
     * @param iterations  the time cost
     * @param parallelism the parallelism
     * @param length      the hash length in bytes
     * @return the hash
     */
    private static byte[] derive(char[] password, byte[] salt, int memoryKib, int iterations, int parallelism,
                                 int length) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(memoryKib)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        byte[] secret = utf8(password);
        byte[] hash = new byte[length];
        try {
            generator.generateBytes(secret, hash);
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
        return hash;
    }

    /**
     * Encodes a password as UTF-8 without making a {@code String} of it.
     *
     * @param password the password
     * @return its UTF-8 bytes, to wipe after use
     */
    private static byte[] utf8(char[] password) {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }
        return bytes;
    }
}
