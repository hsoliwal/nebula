// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import com.synexia.job.IProgressMonitor;

/** Provider boundary for Java or optional native precomputed byte-search implementations. */
public interface SearchEngine {
    String name();
    int priority();
    boolean available();
    CompiledSearch compile(byte[] needle);

    /** Progress-aware compatibility entrypoint. */
    default CompiledSearch compile(byte[] needle, IProgressMonitor monitor) {
        IProgressMonitor checked = monitor == null ? IProgressMonitor.noop() : monitor;
        checked.checkCanceled();
        CompiledSearch result = compile(needle);
        checked.checkCanceled();
        return result;
    }
}
