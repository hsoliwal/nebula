// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Locale;
import java.util.Objects;

/**
 * Contract-bounded source atom used by recipe-first M3 transformations.
 *
 * <p>The implementation hash may change. The contract hash is the sealed identity that must
 * remain stable unless an explicit migration recipe declares otherwise.</p>
 */
public record M3CodeAtom(
        String atomId,
        String sourcePath,
        String symbol,
        String contractSha256,
        String implementationSha256,
        State state) {

    public M3CodeAtom {
        atomId = required(atomId, "atomId");
        sourcePath = required(sourcePath, "sourcePath");
        symbol = required(symbol, "symbol");
        contractSha256 = sha256(contractSha256, "contractSha256");
        implementationSha256 = sha256(implementationSha256, "implementationSha256");
        state = Objects.requireNonNull(state, "state");
    }

    public boolean replacementCandidate() {
        return state == State.ABSENT || state == State.KNOWN_OLD;
    }

    public boolean blocksMutation() {
        return state == State.DRIFTED
                || state == State.DUPLICATE
                || state == State.AMBIGUOUS
                || state == State.PARSE_ERROR;
    }

    public enum State {
        ABSENT,
        EXACT,
        KNOWN_OLD,
        DRIFTED,
        DUPLICATE,
        AMBIGUOUS,
        PARSE_ERROR
    }

    static String required(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(name + " required");
        }
        return value.strip();
    }

    static String sha256(String value, String name) {
        String normalized = required(value, name).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be a 64-hex SHA-256");
        }
        return normalized;
    }
}
