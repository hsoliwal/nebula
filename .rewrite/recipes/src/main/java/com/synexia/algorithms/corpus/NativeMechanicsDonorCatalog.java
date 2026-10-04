// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Pinned native C/C++ systems donors for low-level optimization mechanics.
 *
 * <p>This catalogue complements {@link OptimizationDonorCatalog}. Competitive-programming
 * catalogues classify bounded problem shapes; this owner records mature native implementations
 * worth studying for memory layout, pointer/offset arithmetic, mmap/page ownership, SIMD scans,
 * automata, hashing and compressed set algebra.</p>
 *
 * <p>A match is evidence only. It grants no source-copy, dependency, native-execution or source
 * substitution authority.</p>
 */
public final class NativeMechanicsDonorCatalog {
    public enum Mechanic {
        POINTER_OFFSET_ADDRESSING,
        CONTIGUOUS_PRIMITIVE_LAYOUT,
        MMAP_PAGE_OWNERSHIP,
        COMPILED_AUTOMATA,
        SIMD_MULTI_PATTERN_SCAN,
        REGEX_VM_JIT,
        TAGGED_AUTOMATA,
        APPROXIMATE_MATCHING,
        ENCODING_DISPATCH,
        SIMD_STRUCTURAL_SCAN,
        STRUCTURAL_INDEX_TAPE,
        STREAMING_HASH,
        WIDE_UNALIGNED_LOADS,
        PAGER_BTREE,
        MMAP_BTREE,
        VARINT_RECORD_LAYOUT,
        ZERO_COPY_READ,
        ADAPTIVE_BITMAP,
        SIMD_SET_ALGEBRA,
        SPARSE_ADAPTIVE_ADDRESS,
        ARENA_ALLOCATOR,
        SIZE_CLASS_SLAB,
        PAGE_SEGMENT_HEAP,
        INTRUSIVE_HASH,
        INTRUSIVE_LIST,
        EVENT_LOOP_IO,
        HANDLE_LIFECYCLE,
        LIBC_MEMORY_SCAN,
        STREAMING_COMPRESSION,
        DICTIONARY_COMPRESSION,
        FINITE_STATE_ENTROPY,
        BLOCK_CODEC,
        GPU_PARALLEL_NFA_SCAN,
        GPU_PARALLEL_DFA_SCAN,
        GPU_BATCH_TRANSFER,
        GPU_PREFIX_SCAN,
        GPU_STREAM_COMPACTION,
        GPU_RADIX_ORDER,
        GPU_HASH_INDEX,
        GPU_GRAPH_FRONTIER,
        ARRAY_KERNEL_COMPILATION,
        GPU_VECTOR_INDEX_BUILD,
        WEBGPU_NATIVE_BACKEND,
        WGSL_SHADER_COMPILER,
        GPU_COMMAND_SUBMISSION,
        SYSTOLIC_INT8_MATMUL,
        MODEL_RUNTIME_ACCELERATION,
        TENSOR_BUFFER_IO
    }

    public enum Owner {
        INDEX_STRING_QUERY,
        INDEX_STRING_REGEX,
        INDEX_STRING_DICTIONARY,
        PARSER_SUBSTRATE,
        MINDEX_DB,
        MINDEX_AST_COLD_TIER,
        MINDEX_AST,
        MINDEX_ARENA,
        MINDEX_SWAP,
        NATIVE_INTEROP,
        COMPRESSION_CODEC,
        FILE_INDEX,
        HASHING,
        GPU_ACCELERATION,
        TENSOR_ACCELERATION,
        GENERAL_RUNTIME
    }

    public enum NativeUse {
        JAVA_SHAPE_FIRST,
        JNI_BATCH_CANDIDATE,
        OPTIONAL_NATIVE_BACKEND,
        COMPATIBILITY_BACKEND,
        GPU_BATCH_CANDIDATE,
        LICENSE_GATED_REFERENCE
    }

    public enum LicenseGate {
        PERMISSIVE_REVIEW,
        PUBLIC_DOMAIN_CORE,
        ARCHIVED_PERMISSIVE_REVIEW,
        CUSTOM_LICENSE_REVIEW,
        MIXED_SOURCE_SCOPE_REVIEW,
        COPYLEFT_REVIEW
    }

    public record Donor(
            String repository,
            String revision,
            String license,
            String licenseFile,
            String licenseBlob,
            boolean archived,
            NativeUse nativeUse,
            LicenseGate licenseGate,
            List<Mechanic> mechanics,
            List<Owner> owners,
            String rationale,
            String root) {

        public Donor {
            repository = repositoryName(repository);
            revision = sha1(revision, "revision");
            license = text(license, "license");
            licenseFile = text(licenseFile, "licenseFile");
            licenseBlob = sha1(licenseBlob, "licenseBlob");
            nativeUse = Objects.requireNonNull(nativeUse, "nativeUse");
            licenseGate = Objects.requireNonNull(licenseGate, "licenseGate");

            final EnumSet<Mechanic> stableMechanics = EnumSet.noneOf(Mechanic.class);
            stableMechanics.addAll(Objects.requireNonNull(mechanics, "mechanics"));
            if (stableMechanics.isEmpty()) {
                throw new IllegalArgumentException("NATIVE_DONOR_MECHANICS_REQUIRED");
            }
            mechanics = List.copyOf(stableMechanics);

            final EnumSet<Owner> stableOwners = EnumSet.noneOf(Owner.class);
            stableOwners.addAll(Objects.requireNonNull(owners, "owners"));
            if (stableOwners.isEmpty()) {
                throw new IllegalArgumentException("NATIVE_DONOR_OWNERS_REQUIRED");
            }
            owners = List.copyOf(stableOwners);

            rationale = text(rationale, "rationale");
            final CatalogueDigest digest =
                    new CatalogueDigest("SYNEXIA_NATIVE_MECHANICS_DONOR_V1")
                            .text(repository)
                            .text(revision)
                            .text(license)
                            .text(licenseFile)
                            .text(licenseBlob)
                            .text(Boolean.toString(archived))
                            .text(nativeUse.name())
                            .text(licenseGate.name())
                            .text(rationale);
            mechanics.forEach(value -> digest.text(value.name()));
            owners.forEach(value -> digest.text(value.name()));

            final String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("NATIVE_DONOR_ROOT_MISMATCH");
            }
        }

        public boolean implementationAuthority() {
            return false;
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean dependencyAuthority() {
            return false;
        }

        public boolean nativeExecutionAuthority() {
            return false;
        }

        public boolean benchmarkRequired() {
            return nativeUse != NativeUse.LICENSE_GATED_REFERENCE;
        }

        public String repositoryUrl() {
            return "https://github.com/" + repository;
        }
    }

    public record Match(
            Donor donor,
            List<ProblemOptimizationCatalog.Kernel> matchedKernels,
            List<Mechanic> matchedMechanics,
            String root) {

        public Match {
            donor = Objects.requireNonNull(donor, "donor");

            final EnumSet<ProblemOptimizationCatalog.Kernel> stableKernels =
                    EnumSet.noneOf(ProblemOptimizationCatalog.Kernel.class);
            stableKernels.addAll(Objects.requireNonNull(matchedKernels, "matchedKernels"));
            if (stableKernels.isEmpty()) {
                throw new IllegalArgumentException("NATIVE_DONOR_MATCH_KERNEL_REQUIRED");
            }
            matchedKernels = List.copyOf(stableKernels);

            final EnumSet<Mechanic> stableMechanics = EnumSet.noneOf(Mechanic.class);
            stableMechanics.addAll(Objects.requireNonNull(matchedMechanics, "matchedMechanics"));
            if (stableMechanics.isEmpty()) {
                throw new IllegalArgumentException("NATIVE_DONOR_MATCH_MECHANIC_REQUIRED");
            }
            matchedMechanics = List.copyOf(stableMechanics);

            final CatalogueDigest digest =
                    new CatalogueDigest("SYNEXIA_NATIVE_MECHANICS_MATCH_V1")
                            .text(donor.root());
            matchedKernels.forEach(value -> digest.text(value.name()));
            matchedMechanics.forEach(value -> digest.text(value.name()));
            final String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("NATIVE_DONOR_MATCH_ROOT_MISMATCH");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean nativeExecutionAuthority() {
            return false;
        }
    }

    private static final List<Donor> DONORS = List.of(
            donor(
                    "google/re2",
                    "972a15cedd008d846f1a39b2e88ce48d7f166cbd",
                    "BSD-3-Clause",
                    "LICENSE",
                    "09e5ec1c74c187adc8fde6c74308c3492ef31f77",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.COMPILED_AUTOMATA,
                            Mechanic.POINTER_OFFSET_ADDRESSING,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.INDEX_STRING_REGEX, Owner.GENERAL_RUNTIME),
                    "Compiled non-backtracking automata and compact state-transition mechanics."),
            donor(
                    "intel/hyperscan",
                    "5611d13bb0ba722c8a42e51554cd55f96c01caa9",
                    "BSD-3-Clause-style",
                    "COPYING",
                    "ef9b24fb972778cb970e67de2e62f5c6e1eac5e6",
                    false,
                    NativeUse.JNI_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.SIMD_MULTI_PATTERN_SCAN,
                            Mechanic.COMPILED_AUTOMATA,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.INDEX_STRING_REGEX, Owner.GENERAL_RUNTIME),
                    "Compiled multi-pattern databases and amortized SIMD scan mechanics."),
            donor(
                    "VectorCamp/vectorscan",
                    "f40db5e75f1eb93920f4abbd70f5e6e75d22152e",
                    "BSD-3-Clause-style",
                    "COPYING",
                    "908843a015206536dabd63d95efdf146e173fb30",
                    false,
                    NativeUse.JNI_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.SIMD_MULTI_PATTERN_SCAN,
                            Mechanic.COMPILED_AUTOMATA,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.INDEX_STRING_REGEX, Owner.GENERAL_RUNTIME),
                    "Current Hyperscan-family vector backend and architecture-portable scanning."),
            donor(
                    "diku-dk/futhark",
                    "ad3058ad645c7f6e198f2d236b6f7042dc9aa4e6",
                    "ISC",
                    "LICENSE",
                    "4830c93e90722094b0afbc09fdff9cee5d671d5d",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.ARRAY_KERNEL_COMPILATION,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.MINDEX_AST,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION),
                    "Array-kernel compiler and C-library interoperability reference for packed MIndex lanes."),
            donor(
                    "NVIDIA/cccl",
                    "e9b87f0c329f733058218084408f242ec1545707",
                    "Apache-2.0 with component exceptions",
                    "LICENSE",
                    "30823296ea6f77835bf94dc239fcfac6f8e83e03",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.CUSTOM_LICENSE_REVIEW,
                    List.of(
                            Mechanic.GPU_PREFIX_SCAN,
                            Mechanic.GPU_STREAM_COMPACTION,
                            Mechanic.GPU_RADIX_ORDER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.MINDEX_AST,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION),
                    "Pinned CUB/Thrust/CCCL scan, selection and ordering mechanics; component license scope remains review-gated."),
            donor(
                    "NVIDIA/cuCollections",
                    "029384958b066457f0cf0e7fd816611ecae84262",
                    "Apache-2.0",
                    "LICENSE",
                    "d3a429420d42753a502ee2c0dae49aa4e054bbf3",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.GPU_HASH_INDEX,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.MINDEX_AST,
                            Owner.FILE_INDEX,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION),
                    "GPU concurrent hash/index mechanics for packed symbol and candidate lookup; evidence only."),
            donor(
                    "gunrock/gunrock",
                    "748f79e66cc26762cd3f99422b028e742a737bde",
                    "Apache-2.0",
                    "LICENSE",
                    "171c6823b27849195fe704d32d8c23ec6ed59d05",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.GPU_GRAPH_FRONTIER,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.MINDEX_AST,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION),
                    "GPU frontier scheduling and load-balancing reference for admitted dependency-graph rows."),
            donor(
                    "dmikushin/gpu-grep",
                    "bf0afa55443c38c383bc3297cba51f8d989081a2",
                    "MIT",
                    "LICENSE",
                    "ade23f3f8e1231df7bc28b047f0b8a5653fb598f",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.GPU_PARALLEL_NFA_SCAN,
                            Mechanic.COMPILED_AUTOMATA,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.INDEX_STRING_REGEX,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION,
                            Owner.GENERAL_RUNTIME),
                    "CUDA NFA scan mechanics for large candidate batches; evidence only."),
            donor(
                    "vqd8a/DFAGE",
                    "579e1514db3adc10f3af28765a887987b6561123",
                    "BSD-3-Clause",
                    "LICENSE",
                    "2a587e232ee7a568224dfdb9d242b2459e7c12fa",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.GPU_PARALLEL_DFA_SCAN,
                            Mechanic.COMPILED_AUTOMATA,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.INDEX_STRING_REGEX,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION,
                            Owner.GENERAL_RUNTIME),
                    "CUDA DFA execution and state-table layout mechanics; research reference."),
            donor(
                    "NVIDIA/cuvs",
                    "f633a36793943520f5186591dd84f5acc55032f0",
                    "Apache-2.0",
                    "LICENSE",
                    "1a89b9054d669db9cfb06aa2ce187b37f5d50eeb",
                    false,
                    NativeUse.GPU_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.GPU_VECTOR_INDEX_BUILD,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.FILE_INDEX,
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION,
                            Owner.GENERAL_RUNTIME),
                    "GPU index construction, device batching and memory-layout reference; not a regex engine."),
            donor(
                    "google/dawn",
                    "b672eda13605b4561565441f4fb90b63bcab946d",
                    "BSD-3-Clause",
                    "LICENSE",
                    "08e7ccc4f95d780198170d8fa50aece5826b71c7",
                    false,
                    NativeUse.OPTIONAL_NATIVE_BACKEND,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.WEBGPU_NATIVE_BACKEND,
                            Mechanic.WGSL_SHADER_COMPILER,
                            Mechanic.GPU_COMMAND_SUBMISSION,
                            Mechanic.GPU_BATCH_TRANSFER,
                            Mechanic.HANDLE_LIFECYCLE),
                    List.of(
                            Owner.NATIVE_INTEROP,
                            Owner.GPU_ACCELERATION,
                            Owner.GENERAL_RUNTIME),
                    "Cross-platform WebGPU native backend over D3D12, Metal, Vulkan and OpenGL, "
                            + "plus Tint WGSL compilation and sandbox client/server mechanics; "
                            + "candidate evidence only."),
            donor(
                    "google-ai-edge/LiteRT",
                    "65d1ac438d2c6b78f5ca3878bc6a522a3e317dd9",
                    "Apache-2.0",
                    "LICENSE",
                    "261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64",
                    false,
                    NativeUse.OPTIONAL_NATIVE_BACKEND,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.SYSTOLIC_INT8_MATMUL,
                            Mechanic.MODEL_RUNTIME_ACCELERATION,
                            Mechanic.TENSOR_BUFFER_IO,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.TENSOR_ACCELERATION,
                            Owner.NATIVE_INTEROP,
                            Owner.GENERAL_RUNTIME),
                    "Current Google AI Edge runtime reference for compiled models, "
                            + "tensor buffers and explicit accelerator backends."),
            donor(
                    "google-coral/libedgetpu",
                    "e35aed18fea2e2d25d98352e5a5bd357c170bd4d",
                    "Apache-2.0",
                    "LICENSE",
                    "261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64",
                    true,
                    NativeUse.COMPATIBILITY_BACKEND,
                    LicenseGate.ARCHIVED_PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.SYSTOLIC_INT8_MATMUL,
                            Mechanic.MODEL_RUNTIME_ACCELERATION,
                            Mechanic.TENSOR_BUFFER_IO,
                            Mechanic.HANDLE_LIFECYCLE,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.TENSOR_ACCELERATION,
                            Owner.NATIVE_INTEROP),
                    "Archived Google Coral Edge TPU runtime with explicit device enumeration and delegate lifecycle."),
            donor(
                    "google-coral/libcoral",
                    "6589d0bb49c7fdbc4194ce178d06f8cdc7b5df60",
                    "Apache-2.0",
                    "LICENSE",
                    "f4f87bd4ed6dca2480e257335a4063ef4127624c",
                    true,
                    NativeUse.COMPATIBILITY_BACKEND,
                    LicenseGate.ARCHIVED_PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.MODEL_RUNTIME_ACCELERATION,
                            Mechanic.TENSOR_BUFFER_IO,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.TENSOR_ACCELERATION,
                            Owner.NATIVE_INTEROP),
                    "Google Coral TensorFlow Lite/Edge TPU interpreter helpers and per-device inference mechanics."),
            donor(
                    "PCRE2Project/pcre2",
                    "ffa64a462e1d31b2c8afd15634e1e4f7072e0a71",
                    "BSD-3-Clause WITH PCRE2 exception; JIT BSD-2-Clause",
                    "LICENCE.md",
                    "f6fba35db25ba5a4326fa2f4a86a536a24392b30",
                    false,
                    NativeUse.COMPATIBILITY_BACKEND,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.REGEX_VM_JIT,
                            Mechanic.POINTER_OFFSET_ADDRESSING,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.INDEX_STRING_REGEX),
                    "Compatibility regex VM, start-code/literal optimization and optional JIT."),
            donor(
                    "kkos/oniguruma",
                    "f95747b462de672b6f8dbdeb478245ddf061ca53",
                    "two-condition BSD-style",
                    "COPYING",
                    "ee4d7cf05501ee338de85b1e92df869c1e8d5bbf",
                    true,
                    NativeUse.COMPATIBILITY_BACKEND,
                    LicenseGate.ARCHIVED_PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.ENCODING_DISPATCH,
                            Mechanic.POINTER_OFFSET_ADDRESSING,
                            Mechanic.REGEX_VM_JIT),
                    List.of(Owner.INDEX_STRING_REGEX),
                    "Encoding-aware VM and compact byte/codepoint dispatch mechanics."),
            donor(
                    "laurikari/tre",
                    "71bfcaf0af3994384987c6c2679ed7d078ffe189",
                    "two-condition BSD-style",
                    "LICENSE",
                    "76ea75f409c48158c812477ebd29aec2f9096b13",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.TAGGED_AUTOMATA,
                            Mechanic.APPROXIMATE_MATCHING,
                            Mechanic.COMPILED_AUTOMATA),
                    List.of(Owner.INDEX_STRING_REGEX),
                    "Tagged-state and approximate regular-language mechanics."),
            donor(
                    "simdjson/simdjson",
                    "645e5c87b1ea5777a1f9c84c43f695bb264c240d",
                    "MIT (LICENSE-MIT)",
                    "LICENSE-MIT",
                    "86a11f2f65ac14150dd008ce788dc9c8ffa66f8c",
                    false,
                    NativeUse.JNI_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.SIMD_STRUCTURAL_SCAN,
                            Mechanic.STRUCTURAL_INDEX_TAPE,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.PARSER_SUBSTRATE, Owner.GENERAL_RUNTIME),
                    "Stage-separated structural scan followed by compact token-tape traversal."),
            donor(
                    "ashvardanian/StringZilla",
                    "7fed3fb4201a34962cd456a4033bebc5ea09e74e",
                    "Apache-2.0",
                    "LICENSE",
                    "261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64",
                    false,
                    NativeUse.JNI_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.LIBC_MEMORY_SCAN,
                            Mechanic.WIDE_UNALIGNED_LOADS,
                            Mechanic.SIMD_MULTI_PATTERN_SCAN,
                            Mechanic.APPROXIMATE_MATCHING,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.INDEX_STRING_QUERY,
                            Owner.NATIVE_INTEROP,
                            Owner.GENERAL_RUNTIME),
                    "SIMD/SWAR literal search, memory scans, edit-distance and packed string "
                            + "mechanics; evidence only."),
            donor(
                    "Cyan4973/xxHash",
                    "680bf463fa1ca0461b9a7c2dab7556e1f54cf4cf",
                    "BSD-2-Clause",
                    "LICENSE",
                    "e4c5da7234eca7baad9a46f0b144e49ccad2e500",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.STREAMING_HASH,
                            Mechanic.WIDE_UNALIGNED_LOADS,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.HASHING, Owner.GENERAL_RUNTIME),
                    "Wide-load streaming hash state and avalanche/mixing mechanics."),
            donor(
                    "sqlite/sqlite",
                    "2acb2ea9089c604d5786f56ee3e50a0479a268dd",
                    "Public Domain core (LICENSE.md scope)",
                    "LICENSE.md",
                    "5f59bb8fefd54767e2eb08a17d0dc19007958d90",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PUBLIC_DOMAIN_CORE,
                    List.of(
                            Mechanic.PAGER_BTREE,
                            Mechanic.MMAP_PAGE_OWNERSHIP,
                            Mechanic.VARINT_RECORD_LAYOUT),
                    List.of(Owner.MINDEX_DB, Owner.MINDEX_AST_COLD_TIER),
                    "Pager/B-tree, compact records and transaction/page ownership mechanics."),
            donor(
                    "LMDB/lmdb",
                    "700e10f91a65fae69520301926fb9819f16d292f",
                    "OpenLDAP Public License 2.8",
                    "libraries/liblmdb/LICENSE",
                    "05ad7571e448b9d83ead5d4691274d9484574714",
                    false,
                    NativeUse.OPTIONAL_NATIVE_BACKEND,
                    LicenseGate.CUSTOM_LICENSE_REVIEW,
                    List.of(
                            Mechanic.MMAP_BTREE,
                            Mechanic.MMAP_PAGE_OWNERSHIP,
                            Mechanic.ZERO_COPY_READ),
                    List.of(Owner.MINDEX_DB, Owner.MINDEX_AST_COLD_TIER),
                    "mmap-first B+tree, read snapshots and zero-copy page access."),
            donor(
                    "RoaringBitmap/CRoaring",
                    "736200862589ef3cca72007dbcdaf1537c49dcbc",
                    "Apache-2.0 OR MIT",
                    "LICENSE",
                    "8b0ad80d743d242082d7c6e0bca54be0c26a6507",
                    false,
                    NativeUse.JNI_BATCH_CANDIDATE,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.ADAPTIVE_BITMAP,
                            Mechanic.SIMD_SET_ALGEBRA,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.INDEX_STRING_QUERY, Owner.GENERAL_RUNTIME),
                    "Adaptive bitmap containers and cardinality-aware SIMD set algebra."),
            donor(
                    "microsoft/mimalloc",
                    "31d034d94cdb8e22f7d7ed55967f581a2d6e831d",
                    "MIT",
                    "LICENSE",
                    "53315ebee557ac22f3396ea1781df0440c26bb15",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.ARENA_ALLOCATOR,
                            Mechanic.PAGE_SEGMENT_HEAP,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(
                            Owner.MINDEX_ARENA,
                            Owner.MINDEX_AST_COLD_TIER,
                            Owner.GENERAL_RUNTIME),
                    "Page/segment heaps, local free lists and bounded allocator ownership."),
            donor(
                    "jemalloc/jemalloc",
                    "cf35c74da0b6bf10c4efa8e8c67b026187768b68",
                    "BSD-2-Clause-style",
                    "COPYING",
                    "3b7fd3585d2b7c6997ba499253cbd773e5f2efb6",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.ARENA_ALLOCATOR,
                            Mechanic.SIZE_CLASS_SLAB,
                            Mechanic.PAGE_SEGMENT_HEAP),
                    List.of(Owner.MINDEX_ARENA, Owner.GENERAL_RUNTIME),
                    "Arena, extent, bin and size-class mechanics for bounded primitive stores."),
            donor(
                    "libuv/libuv",
                    "abe835d41317b55b16260990821f89b4ee9e437b",
                    "MIT",
                    "LICENSE",
                    "6566365d4f238005a3358710e20b2acde266b594",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.EVENT_LOOP_IO,
                            Mechanic.HANDLE_LIFECYCLE,
                            Mechanic.INTRUSIVE_LIST),
                    List.of(Owner.NATIVE_INTEROP, Owner.GENERAL_RUNTIME),
                    "Event-loop, handle lifecycle and intrusive queue/list ownership mechanics."),
            donor(
                    "troydhanson/uthash",
                    "a49bed0b4abb7dff16c73906dcdc8a9718d582d2",
                    "BSD-1-Clause-style",
                    "LICENSE",
                    "039b7b4f24cf9bba559f5d2c5816ccf79e8366d4",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.INTRUSIVE_HASH,
                            Mechanic.INTRUSIVE_LIST,
                            Mechanic.POINTER_OFFSET_ADDRESSING),
                    List.of(
                            Owner.INDEX_STRING_DICTIONARY,
                            Owner.GENERAL_RUNTIME),
                    "Embedded-handle hash/list mechanics with explicit caller-owned storage."),
            donor(
                    "ifduyue/musl",
                    "c4e1bb3994c14ed5112c894d15a451bf00f0d501",
                    "MIT with per-file notices in COPYRIGHT",
                    "COPYRIGHT",
                    "2f15edc7a17936b15d563363a1e9b70aa3bcbc2b",
                    false,
                    NativeUse.JAVA_SHAPE_FIRST,
                    LicenseGate.CUSTOM_LICENSE_REVIEW,
                    List.of(
                            Mechanic.LIBC_MEMORY_SCAN,
                            Mechanic.POINTER_OFFSET_ADDRESSING,
                            Mechanic.CONTIGUOUS_PRIMITIVE_LAYOUT),
                    List.of(Owner.NATIVE_INTEROP, Owner.GENERAL_RUNTIME),
                    "Compact libc memory/string scan and syscall-boundary mechanics."),
            donor(
                    "facebook/zstd",
                    "01b7154f1172432f8abe9b3bb9909e14a1176b7d",
                    "BSD-3-Clause",
                    "LICENSE",
                    "75800288cc243f164b9d130f125e2ffae28f8e39",
                    false,
                    NativeUse.OPTIONAL_NATIVE_BACKEND,
                    LicenseGate.PERMISSIVE_REVIEW,
                    List.of(
                            Mechanic.STREAMING_COMPRESSION,
                            Mechanic.DICTIONARY_COMPRESSION,
                            Mechanic.FINITE_STATE_ENTROPY,
                            Mechanic.BLOCK_CODEC),
                    List.of(
                            Owner.COMPRESSION_CODEC,
                            Owner.MINDEX_SWAP,
                            Owner.FILE_INDEX),
                    "Dictionary, block and finite-state entropy mechanics for cold/mapped payloads."),
            donor(
                    "lz4/lz4",
                    "0774d05537f9762f838f7ab541b7765f1a729cb5",
                    "lib BSD-2-Clause; repository tooling GPL-2.0-or-later",
                    "LICENSE",
                    "ba8a2900f8b180fefc0c16f8a35076ef290c9492",
                    false,
                    NativeUse.OPTIONAL_NATIVE_BACKEND,
                    LicenseGate.MIXED_SOURCE_SCOPE_REVIEW,
                    List.of(
                            Mechanic.STREAMING_COMPRESSION,
                            Mechanic.BLOCK_CODEC,
                            Mechanic.WIDE_UNALIGNED_LOADS),
                    List.of(
                            Owner.COMPRESSION_CODEC,
                            Owner.MINDEX_SWAP,
                            Owner.FILE_INDEX),
                    "Fast block/stream codec mechanics; reuse must remain inside BSD lib scope."),
            donor(
                    "netdata/libjudy",
                    "777c9f4a8faf3f524d0afa39fb4577876b6b646d",
                    "LGPL-2.1-or-later",
                    "COPYING",
                    "74da134b55bf3bebd709cd4710d5e5e2f8f3aa86",
                    false,
                    NativeUse.LICENSE_GATED_REFERENCE,
                    LicenseGate.COPYLEFT_REVIEW,
                    List.of(
                            Mechanic.SPARSE_ADAPTIVE_ADDRESS,
                            Mechanic.POINTER_OFFSET_ADDRESSING),
                    List.of(Owner.INDEX_STRING_DICTIONARY),
                    "Sparse adaptive integer-address geometry; concept-only unless separately gated."));

    private static final String ROOT = root(DONORS);

    private NativeMechanicsDonorCatalog() {}

    public static List<Donor> all() {
        return DONORS;
    }

    public static List<Donor> byMechanic(final Mechanic mechanic) {
        Objects.requireNonNull(mechanic, "mechanic");
        return DONORS.stream()
                .filter(donor -> donor.mechanics().contains(mechanic))
                .toList();
    }

    public static List<Donor> byOwner(final Owner owner) {
        Objects.requireNonNull(owner, "owner");
        return DONORS.stream()
                .filter(donor -> donor.owners().contains(owner))
                .toList();
    }

    public static Donor require(final String repository) {
        final String checked = repositoryName(repository);
        return DONORS.stream()
                .filter(donor -> donor.repository().equals(checked))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "UNPINNED_NATIVE_MECHANICS_DONOR:" + checked));
    }

    public static List<Match> matches(
            final List<ProblemOptimizationCatalog.Kernel> kernels) {
        Objects.requireNonNull(kernels, "kernels");
        if (kernels.isEmpty()) {
            return List.of();
        }

        final EnumSet<ProblemOptimizationCatalog.Kernel> requested =
                EnumSet.noneOf(ProblemOptimizationCatalog.Kernel.class);
        requested.addAll(kernels);

        final ArrayList<Match> result = new ArrayList<>();
        for (Donor donor : DONORS) {
            final EnumSet<ProblemOptimizationCatalog.Kernel> matchedKernels =
                    EnumSet.noneOf(ProblemOptimizationCatalog.Kernel.class);
            final EnumSet<Mechanic> matchedMechanics = EnumSet.noneOf(Mechanic.class);

            for (ProblemOptimizationCatalog.Kernel kernel : requested) {
                final EnumSet<Mechanic> kernelMechanics = mechanics(kernel);
                final EnumSet<Mechanic> intersection = EnumSet.copyOf(kernelMechanics);
                intersection.retainAll(donor.mechanics());
                if (!intersection.isEmpty()) {
                    matchedKernels.add(kernel);
                    matchedMechanics.addAll(intersection);
                }
            }

            if (!matchedKernels.isEmpty()) {
                result.add(new Match(
                        donor,
                        List.copyOf(matchedKernels),
                        List.copyOf(matchedMechanics),
                        ""));
            }
        }
        result.sort(Comparator.comparing(match -> match.donor().repository()));
        return List.copyOf(result);
    }

    public static List<Match> matches(final ProblemOptimizationCatalog.Kernel kernel) {
        return matches(List.of(Objects.requireNonNull(kernel, "kernel")));
    }

    public static String root() {
        return ROOT;
    }

    private static EnumSet<Mechanic> mechanics(
            final ProblemOptimizationCatalog.Kernel kernel) {
        return switch (Objects.requireNonNull(kernel, "kernel")) {
            case DIRECT_ADDRESS -> EnumSet.of(
                    Mechanic.SPARSE_ADAPTIVE_ADDRESS);
            case ORDERED_RANK_SEARCH, TREE_INDEX -> EnumSet.of(
                    Mechanic.PAGER_BTREE,
                    Mechanic.MMAP_BTREE,
                    Mechanic.MMAP_PAGE_OWNERSHIP,
                    Mechanic.GPU_RADIX_ORDER);
            case LINEAR_SCAN, SLIDING_WINDOW -> EnumSet.of(
                    Mechanic.SIMD_MULTI_PATTERN_SCAN,
                    Mechanic.SIMD_STRUCTURAL_SCAN,
                    Mechanic.STRUCTURAL_INDEX_TAPE,
                    Mechanic.LIBC_MEMORY_SCAN,
                    Mechanic.GPU_STREAM_COMPACTION);
            case PREFIX_SCAN -> EnumSet.of(
                    Mechanic.SIMD_MULTI_PATTERN_SCAN,
                    Mechanic.SIMD_STRUCTURAL_SCAN,
                    Mechanic.STRUCTURAL_INDEX_TAPE,
                    Mechanic.LIBC_MEMORY_SCAN,
                    Mechanic.GPU_PREFIX_SCAN,
                    Mechanic.GPU_STREAM_COMPACTION);
            case HASH_TABLE, ROLLING_HASH -> EnumSet.of(
                    Mechanic.STREAMING_HASH,
                    Mechanic.WIDE_UNALIGNED_LOADS,
                    Mechanic.INTRUSIVE_HASH,
                    Mechanic.GPU_HASH_INDEX);
            case BITMAP_POSTINGS, BIT_PACKING -> EnumSet.of(
                    Mechanic.ADAPTIVE_BITMAP,
                    Mechanic.SIMD_SET_ALGEBRA,
                    Mechanic.GPU_STREAM_COMPACTION);
            case TRIE, AHO_CORASICK -> EnumSet.of(
                    Mechanic.SPARSE_ADAPTIVE_ADDRESS,
                    Mechanic.COMPILED_AUTOMATA,
                    Mechanic.SIMD_MULTI_PATTERN_SCAN,
                    Mechanic.GPU_PARALLEL_DFA_SCAN);
            case KMP -> EnumSet.of(
                    Mechanic.COMPILED_AUTOMATA,
                    Mechanic.SIMD_MULTI_PATTERN_SCAN,
                    Mechanic.TAGGED_AUTOMATA,
                    Mechanic.GPU_PARALLEL_NFA_SCAN,
                    Mechanic.GPU_PARALLEL_DFA_SCAN);
            case BOUNDED_SEARCH -> EnumSet.of(
                    Mechanic.COMPILED_AUTOMATA,
                    Mechanic.REGEX_VM_JIT,
                    Mechanic.TAGGED_AUTOMATA,
                    Mechanic.APPROXIMATE_MATCHING,
                    Mechanic.GPU_PARALLEL_NFA_SCAN,
                    Mechanic.GPU_PARALLEL_DFA_SCAN);
            case RANGE_SEGMENT, SPARSE_TABLE -> EnumSet.of(
                    Mechanic.PAGER_BTREE,
                    Mechanic.MMAP_PAGE_OWNERSHIP);
            case LINKED_REWRITE -> EnumSet.of(
                    Mechanic.POINTER_OFFSET_ADDRESSING,
                    Mechanic.INTRUSIVE_LIST);
            case SORT_SELECT -> EnumSet.of(
                    Mechanic.GPU_RADIX_ORDER);
            case GRAPH_TRAVERSAL, DAG_TOPOLOGY -> EnumSet.of(
                    Mechanic.GPU_GRAPH_FRONTIER);
            case MATRIX_GRID -> EnumSet.of(
                    Mechanic.SIMD_STRUCTURAL_SCAN);
            default -> EnumSet.noneOf(Mechanic.class);
        };
    }

    private static Donor donor(
            final String repository,
            final String revision,
            final String license,
            final String licenseFile,
            final String licenseBlob,
            final boolean archived,
            final NativeUse nativeUse,
            final LicenseGate licenseGate,
            final List<Mechanic> mechanics,
            final List<Owner> owners,
            final String rationale) {
        return new Donor(
                repository,
                revision,
                license,
                licenseFile,
                licenseBlob,
                archived,
                nativeUse,
                licenseGate,
                mechanics,
                owners,
                rationale,
                "");
    }

    private static String root(final List<Donor> donors) {
        final ArrayList<Donor> sorted = new ArrayList<>(donors);
        sorted.sort(Comparator.comparing(Donor::repository).thenComparing(Donor::revision));
        final CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_NATIVE_MECHANICS_DONOR_CATALOG_V1");
        sorted.forEach(donor -> digest.text(donor.root()));
        return digest.finish();
    }

    private static String repositoryName(final String value) {
        final String checked = text(value, "repository");
        if (!checked.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("INVALID_NATIVE_DONOR_REPOSITORY");
        }
        return checked;
    }

    private static String text(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return checked;
    }

    private static String sha1(final String value, final String field) {
        final String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException(
                    "INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return checked;
    }

    private static String sha256(final String value, final String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return value;
    }
}
