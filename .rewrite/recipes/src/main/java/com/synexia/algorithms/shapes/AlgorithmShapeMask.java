// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Allocation-free two-word capability mask for the canonical {@link AlgorithmShape} vocabulary.
 *
 * <p>The vocabulary currently exceeds one 64-bit word, so callers must not truncate it to a
 * single long. The enum declaration order defines the in-process bit layout; {@link #schemaRoot()}
 * fingerprints that layout so persisted evidence can detect vocabulary drift.</p>
 */
public record AlgorithmShapeMask(long lowBits, long highBits) {
    public static final int CAPACITY = Long.SIZE * 2;

    private static final AlgorithmShape[] SHAPES = AlgorithmShape.values();
    private static final long KNOWN_HIGH_BITS = knownHighBits();
    private static final String SCHEMA_ROOT = schemaRoot(SHAPES);
    private static final AlgorithmShapeMask EMPTY = new AlgorithmShapeMask(0L, 0L);

    static {
        if (SHAPES.length > CAPACITY) {
            throw new ExceptionInInitializerError(
                    "AlgorithmShape exceeds the 128-bit capability-mask contract: "
                            + SHAPES.length);
        }
    }

    public AlgorithmShapeMask {
        if ((highBits & ~KNOWN_HIGH_BITS) != 0L) {
            throw new IllegalArgumentException("highBits contains unknown AlgorithmShape bits");
        }
    }

    public static AlgorithmShapeMask empty() {
        return EMPTY;
    }

    public static AlgorithmShapeMask of(Collection<AlgorithmShape> shapes) {
        Objects.requireNonNull(shapes, "shapes");
        long low = 0L;
        long high = 0L;
        for (AlgorithmShape shape : shapes) {
            AlgorithmShape checked = Objects.requireNonNull(shape, "shape");
            int ordinal = checked.ordinal();
            if (ordinal < Long.SIZE) {
                low |= 1L << ordinal;
            } else {
                high |= 1L << (ordinal - Long.SIZE);
            }
        }
        return low == 0L && high == 0L ? EMPTY : new AlgorithmShapeMask(low, high);
    }

    public static AlgorithmShapeMask of(AlgorithmShape... shapes) {
        Objects.requireNonNull(shapes, "shapes");
        return of(List.of(shapes));
    }

    public boolean isEmpty() {
        return lowBits == 0L && highBits == 0L;
    }

    public int bitCount() {
        return Long.bitCount(lowBits) + Long.bitCount(highBits);
    }

    public boolean contains(AlgorithmShape shape) {
        AlgorithmShape checked = Objects.requireNonNull(shape, "shape");
        int ordinal = checked.ordinal();
        return ordinal < Long.SIZE
                ? (lowBits & (1L << ordinal)) != 0L
                : (highBits & (1L << (ordinal - Long.SIZE))) != 0L;
    }

    public boolean containsAll(AlgorithmShapeMask other) {
        AlgorithmShapeMask checked = Objects.requireNonNull(other, "other");
        return (lowBits & checked.lowBits) == checked.lowBits
                && (highBits & checked.highBits) == checked.highBits;
    }

    public boolean intersects(AlgorithmShapeMask other) {
        AlgorithmShapeMask checked = Objects.requireNonNull(other, "other");
        return (lowBits & checked.lowBits) != 0L || (highBits & checked.highBits) != 0L;
    }

    public int intersectionCount(AlgorithmShapeMask other) {
        AlgorithmShapeMask checked = Objects.requireNonNull(other, "other");
        return Long.bitCount(lowBits & checked.lowBits)
                + Long.bitCount(highBits & checked.highBits);
    }

    public AlgorithmShapeMask union(AlgorithmShapeMask other) {
        AlgorithmShapeMask checked = Objects.requireNonNull(other, "other");
        return new AlgorithmShapeMask(lowBits | checked.lowBits, highBits | checked.highBits);
    }

    public AlgorithmShapeMask intersection(AlgorithmShapeMask other) {
        AlgorithmShapeMask checked = Objects.requireNonNull(other, "other");
        long low = lowBits & checked.lowBits;
        long high = highBits & checked.highBits;
        return low == 0L && high == 0L ? EMPTY : new AlgorithmShapeMask(low, high);
    }

    public List<AlgorithmShape> shapes() {
        if (isEmpty()) return List.of();
        ArrayList<AlgorithmShape> result = new ArrayList<>(bitCount());
        long low = lowBits;
        while (low != 0L) {
            int bit = Long.numberOfTrailingZeros(low);
            result.add(SHAPES[bit]);
            low &= low - 1L;
        }
        long high = highBits;
        while (high != 0L) {
            int bit = Long.numberOfTrailingZeros(high);
            result.add(SHAPES[Long.SIZE + bit]);
            high &= high - 1L;
        }
        return List.copyOf(result);
    }

    /** 32 lowercase hex digits, high word first, suitable for deterministic TSV evidence. */
    public String hex() {
        return HexFormat.of().toHexDigits(highBits) + HexFormat.of().toHexDigits(lowBits);
    }

    public static int shapeCount() {
        return SHAPES.length;
    }

    public static String schemaRoot() {
        return SCHEMA_ROOT;
    }

    private static long knownHighBits() {
        int highCount = Math.max(0, SHAPES.length - Long.SIZE);
        if (highCount > Long.SIZE) {
            throw new ExceptionInInitializerError(
                    "AlgorithmShape exceeds the 128-bit capability-mask contract: "
                            + SHAPES.length);
        }
        if (highCount == 0) return 0L;
        if (highCount == Long.SIZE) return -1L;
        return (1L << highCount) - 1L;
    }

    private static String schemaRoot(AlgorithmShape[] shapes) {
        MessageDigest digest = sha256();
        frame(digest, "ALGORITHM-SHAPE-MASK/1");
        frame(digest, Integer.toString(CAPACITY));
        frame(digest, Integer.toString(shapes.length));
        for (int ordinal = 0; ordinal < shapes.length; ordinal++) {
            frame(digest, Integer.toString(ordinal));
            frame(digest, shapes[ordinal].name());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
