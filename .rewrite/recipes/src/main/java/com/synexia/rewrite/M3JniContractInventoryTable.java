// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Read-only Java-side JNI contract inventory used to drive native compatibility manifests. */
public final class M3JniContractInventoryTable
        extends DataTable<M3JniContractInventoryTable.Row> {

    public M3JniContractInventoryTable(Recipe recipe) {
        super(
                recipe,
                "M3 JNI contract inventory",
                "Java native declarations, JVM descriptors and mechanically derived JNI linkage "
                        + "evidence. Rows are evidence only and never authorize native symbol changes.");
    }

    public record Row(
            @Column(displayName = "Source path", description = "Java source containing the native declaration.")
                    String sourcePath,
            @Column(displayName = "Declaring type", description = "Fully qualified Java declaring type.")
                    String declaringType,
            @Column(displayName = "Candidate M3 type", description = "Mechanical M3 candidate when the declaring type is MIndex-prefixed.")
                    String candidateM3Type,
            @Column(displayName = "Method", description = "Native Java method name.")
                    String methodName,
            @Column(displayName = "JVM descriptor", description = "Exact attributed JVM method descriptor.")
                    String descriptor,
            @Column(displayName = "Java declaration", description = "Printed Java native method declaration.")
                    String javaDeclaration,
            @Column(displayName = "Static", description = "Whether the native method is static.")
                    boolean staticMethod,
            @Column(displayName = "Native overload count", description = "Number of native methods with this name in the declaring type.")
                    int nativeOverloadCount,
            @Column(displayName = "JNI short symbol", description = "Mechanically derived JNI short-name symbol.")
                    String jniShortSymbol,
            @Column(displayName = "JNI long symbol", description = "Mechanically derived JNI long-name symbol using the argument descriptor.")
                    String jniLongSymbol,
            @Column(displayName = "Required dynamic symbol", description = "Short symbol for unique native names, long symbol for overloaded native names.")
                    String requiredDynamicSymbol,
            @Column(displayName = "RegisterNatives key", description = "Method name plus exact JVM descriptor for JNINativeMethod correlation.")
                    String registerNativesKey,
            @Column(displayName = "Linkage strategy", description = "Expected JNI linkage evidence required before migration.")
                    String linkageStrategy,
            @Column(displayName = "Authority", description = "Always READ_ONLY_EVIDENCE.")
                    String authority) {}
}
