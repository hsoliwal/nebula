/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.algorithms.core;

/** Exception thrown by algorithm execution. Explicit logger replaces only Lombok generation. */
@SuppressWarnings("serial")
public class AlgorithmException extends RuntimeException {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AlgorithmException.class);

    public AlgorithmException(String message) {
        super(message);
        log.error("AlgorithmException created: " + message);
    }

    public AlgorithmException(String message, Throwable cause) {
        super(message, cause);
        log.error("AlgorithmException created: " + message + " - Cause: "
                + (cause == null ? null : cause.getMessage()));
    }
}
