// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact pinned donor provenance for source-driven problem-level supersets. */
public final class ChallengeSupersetCatalog {
    public record DonorOrigin(
            String repository,
            String commit,
            String path,
            String blobSha,
            String license) {
        public DonorOrigin {
            repository = require(repository, "repository");
            commit = require(commit, "commit");
            path = require(path, "path");
            blobSha = require(blobSha, "blobSha");
            license = require(license, "license");
        }
    }

    public record ProblemSuperset(
            String stableId,
            String title,
            AlgorithmShape shape,
            String kernel,
            List<DonorOrigin> donors) {
        public ProblemSuperset {
            stableId = require(stableId, "stableId");
            title = require(title, "title");
            Objects.requireNonNull(shape, "shape");
            kernel = require(kernel, "kernel");
            donors = List.copyOf(Objects.requireNonNull(donors, "donors"));
            if (donors.size() < 2) {
                throw new IllegalArgumentException("source-driven superset requires >= 2 donor origins");
            }
        }
    }

    private static final String NEETCODE_COMMIT =
            "3186ede2ea4c4788e87be4b509bf2b66d5eba0e9";
    private static final String FISHER_COMMIT =
            "21574248036d95ed0980916fcb4392987e09a95b";
    private static final String JAVA_AID_COMMIT =
            "0928f53bb32d0aad2d740a3917ecfc2d56018bb1";
    private static final String RODNEY_COMMIT =
            "a98773d3a0c4f11b9cd90d6a1f37b994c170996a";
    private static final String GFG_KISHAN_COMMIT =
            "39d6a47408cd7a25796e314945258da9aa546227";
    private static final String GFG_CVALINGAM_COMMIT =
            "8341993d94a6100415fe2b060f30345b2079ed49";
    private static final String GFG_KISHAN_CURRENT_COMMIT =
            "3e820da454756b5543459c77799a638e1c00a87b";

    private static final List<ProblemSuperset> ALL = List.of(
            leetcode(
                    217,
                    "Contains Duplicate",
                    AlgorithmShape.HASH_MEMBERSHIP,
                    "ChallengeSupersetKernels.containsDuplicate",
                    "java/0217-contains-duplicate.java",
                    "6ad8b8bcb200b42d1cd18046a540e45ceba61a67",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_217.java",
                    "5b9281c9a3bd40d45bd0e07970b0a27b80c16107"),
            leetcode(
                    121,
                    "Best Time to Buy and Sell Stock",
                    AlgorithmShape.LINEAR_SCAN,
                    "ChallengeSupersetKernels.bestSingleStockTrade",
                    "java/0121-best-time-to-buy-and-sell-stock.java",
                    "2bdd93355a22736d1b6cd04b6aa4299fd77b7641",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_121.java",
                    "3d4814a788b9786722dcbaef5dc40d7389904a32"),
            leetcode(
                    242,
                    "Valid Anagram",
                    AlgorithmShape.FREQUENCY_COUNT,
                    "ChallengeSupersetKernels.validAnagramUtf16",
                    "java/0242-valid-anagram.java",
                    "381e5c255e28a9b4f1bf1eef151faa6b5a3edcf0",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_242.java",
                    "a4951e468bc0a0d4c49ea063853b7c22e7bf6d80"),
            leetcode(
                    20,
                    "Valid Parentheses",
                    AlgorithmShape.STACK_MACHINE,
                    "ChallengeSupersetKernels.balancedBrackets",
                    "java/0020-valid-parentheses.java",
                    "106c0006d4ba22af8ea889cc072728708e5bfd2b",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_20.java",
                    "da3f88feb4567efdbc806a2b48372cc66cbffc26"),
            leetcode(
                    3,
                    "Longest Substring Without Repeating Characters",
                    AlgorithmShape.SLIDING_WINDOW,
                    "ChallengeSupersetKernels.longestDistinctSubstringUtf16",
                    "java/0003-longest-substring-without-repeating-characters.java",
                    "a6d9f3f90bf0e9e2febf6279c9ee814d52c83285",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_3.java",
                    "0fe50a228aadbc144a38312d04da7a79b4f19cc1"),
            leetcode(
                    28,
                    "Find the Index of the First Occurrence in a String",
                    AlgorithmShape.KMP,
                    "ChallengeSupersetKernels.firstSubstringUtf16",
                    "java/0028-find-the-index-of-the-first-occurrence-in-a-string.java",
                    "859748e9eef7bfe9a0bbdef7eb673ad3ed535ea0",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_28.java",
                    "f5b5c58cde2e89fb3a0ed8ac584a32f4ecbfc030"),
            leetcode(
                    704,
                    "Binary Search",
                    AlgorithmShape.BINARY_SEARCH,
                    "ChallengeSupersetKernels.binarySearch",
                    "java/0704-binary-search.java",
                    "7cd21ad3e22ac07d09161333600126556b876dca",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_704.java",
                    "72f63111055040a8b0a0d9ed3098c45a520b02cf"),
            leetcode(
                    53,
                    "Maximum Subarray",
                    AlgorithmShape.KADANE,
                    "ChallengeSupersetKernels.maximumSubarray",
                    "java/0053-maximum-subarray.java",
                    "e9d27b8ccff969546341419126166b7e07ad3a6a",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_53.java",
                    "03adcca52670819aba994492b2147097b739d818"),
            leetcode(
                    739,
                    "Daily Temperatures",
                    AlgorithmShape.MONOTONIC_STACK,
                    "ChallengeSupersetKernels.dailyTemperatures",
                    "java/0739-daily-temperatures.java",
                    "d03cbd7aaa56b18f9a9f21300e7ac13e1aa7cda1",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_739.java",
                    "ba4bd47c1378cafbb814de69558e3a80dff22b8c"),
            leetcode(
                    1,
                    "Two Sum",
                    AlgorithmShape.PAIR_SUM_HASH,
                    "ChallengeSupersetKernels.twoSumFirst",
                    "java/0001-two-sum.java",
                    "d6845efa93ae8dc3caba40929e8081be985727f5",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_1.java",
                    "2d2589918188aed99261f31b2640eee6514b5622"),
            leetcode(
                    56,
                    "Merge Intervals",
                    AlgorithmShape.INTERVAL_MERGE,
                    "ChallengeSupersetKernels.mergeIntervals",
                    "java/0056-merge-intervals.java",
                    "3821e119a0ce291e0d60e2cb7d0aa6c21989788e",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_56.java",
                    "42ad3f330fa00787b6ebd25538722dde740f4f08"),
            leetcode(
                    238,
                    "Product of Array Except Self",
                    AlgorithmShape.PREFIX_SCAN,
                    "ChallengeSupersetKernels.productExceptSelfChecked",
                    "java/0238-product-of-array-except-self.java",
                    "9c1558559d76070b3cd07a05fd6f911b42816172",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_238.java",
                    "43f681493df3034f89f21415a1969eb5375d99a8"),
            leetcode(
                    347,
                    "Top K Frequent Elements",
                    AlgorithmShape.HEAP_SELECT,
                    "ChallengeSupersetKernels.topKFrequent",
                    "java/0347-top-k-frequent-elements.java",
                    "eefaf420a9ac5efe130dbfab54ece5f6b43dcaea",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_347.java",
                    "3253ac6563bbc2e111e1b16ed200211aa7f3e21d"),
            leetcode(
                    11,
                    "Container With Most Water",
                    AlgorithmShape.TWO_POINTER,
                    "ChallengeSupersetKernels.maximumContainerArea",
                    "java/0011-container-with-most-water.java",
                    "4d3ba5d75925401da8ce8b2632c21bb0da2effef",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_11.java",
                    "8e5ceb4652e8e0498680c5a4b3e37f7c5094f703"),
            leetcode(
                    70,
                    "Climbing Stairs",
                    AlgorithmShape.DYNAMIC_PROGRAMMING,
                    "ChallengeSupersetKernels.climbingStairWays",
                    "java/0070-climbing-stairs.java",
                    "4df6c202dd0ff98f43eeb643b1ec4c0883d4c110",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_70.java",
                    "276bceb45261d8428ca95ecabe7b9bda5e9d24dc"),
            leetcode(
                    200,
                    "Number of Islands",
                    AlgorithmShape.GRID_CONNECTED_COMPONENTS,
                    "ChallengeSupersetKernels.numberOfIslands4",
                    "java/0200-number-of-islands.java",
                    "1c48f4e8f63c7aaedb62cc2e587e4b5cc29798e5",
                    "src/main/java/com/fishercoder/solutions/firstthousand/_200.java",
                    "5c867bfdc6b283c7d6c0df33db6a5ccb54337926"),
            hackerrank(
                    "arraymanipulation",
                    "Array Manipulation",
                    AlgorithmShape.DIFFERENCE_SCAN,
                    "ChallengeSupersetKernels.rangeAddMaximum",
                    "HackerRankDashboard/CoreCS/DataStructures/src/main/java/com/javaaid/hackerrank/solutions/datastructures/arrays/ArrayManipulation.java",
                    "ad4a67c076d5fa8a0721fac7e1c1d714863eca8d",
                    "Data Structures/Arrays/Algorithmic Crush/Solution.java",
                    "ae65049388b641ee2343a427b75f1309c6233c7a"),
            hackerrank(
                    "balancedbrackets",
                    "Balanced Brackets",
                    AlgorithmShape.STACK_MACHINE,
                    "ChallengeSupersetKernels.balancedBrackets",
                    "HackerRankDashboard/CoreCS/DataStructures/src/main/java/com/javaaid/hackerrank/solutions/datastructures/stacks/BalancedBrackets.java",
                    "3a2ccf73a42797531d2b4477576f1e4f40534ccc",
                    "Data Structures/Stacks/Balanced Brackets/Solution.java",
                    "5c76ff2928c0536d633557f137db4dffa3dafc1f"),
            hackerrank(
                    "makinganagrams",
                    "Making Anagrams",
                    AlgorithmShape.FREQUENCY_L1_DISTANCE,
                    "ChallengeSupersetKernels.utf16MultisetDeletionDistance",
                    "HackerRankDashboard/Tutorials/CrackingTheCodingInterview/src/main/java/com/javaaid/hackerrank/solutions/tutorials/ctci/MakingAnagrams.java",
                    "f36bb3dcbd9abaf52b490edb2e440b737adb23a9",
                    "Cracking the Coding Interview/Data Structures/Strings - Making Anagrams/Solution.java",
                    "49e1ca04825a8c72bcbabf39a83b7d8e55f1b102"),
            hackerrank(
                    "breadthfirstsearchshortestreach",
                    "Breadth First Search: Shortest Reach",
                    AlgorithmShape.BFS,
                    "ChallengeSupersetKernels.shortestReach",
                    "HackerRankDashboard/CoreCS/Algorithms/src/main/java/com/javaaid/hackerrank/solutions/algorithms/graphtheory/BreadthFirstSearchShortestReach.java",
                    "3155364697eff29bc365f1079f740ef5f30eae17",
                    "Algorithms/Graph Theory/Breadth First Search - Shortest Reach/Solution.java",
                    "af98077da13e27c7b94f4cc1ac3a4976136aba48"),
            hackerrank(
                    "triescontacts",
                    "Tries: Contacts",
                    AlgorithmShape.TRIE,
                    "ChallengeSupersetKernels.contacts",
                    "HackerRankDashboard/Tutorials/CrackingTheCodingInterview/src/main/java/com/javaaid/hackerrank/solutions/tutorials/ctci/TriesContacts.java",
                    "8a7de58c99795fb92e9cc5aff16b2a984deb9f94",
                    "Cracking the Coding Interview/Data Structures/Tries - Contacts/Solution.java",
                    "6a115758f23d2fbada4ecfdd6612dc2484ea1e3f"),
            hackerrank(
                    "heapsfindtherunningmedian",
                    "Heaps: Find the Running Median",
                    AlgorithmShape.PRIORITY_QUEUE,
                    "ChallengeSupersetKernels.runningMediansDoubled",
                    "HackerRankDashboard/Tutorials/CrackingTheCodingInterview/src/main/java/com/javaaid/hackerrank/solutions/tutorials/ctci/HeapsFindTheRunningMedian.java",
                    "c9805ab858ce9ff031171464f052ec553aa50bba",
                    "Cracking the Coding Interview/Data Structures/Heaps - Find the Running Median/Solution.java",
                    "eec2313648da1afcf1bc1afa49386af8f7bfd81e"),
            geeksforgeeks(
                    "anagram",
                    "Anagram",
                    AlgorithmShape.FREQUENCY_COUNT,
                    "GeeksForGeeksTypedProblems.anagram",
                    "Solutions/Java/Anagram.java",
                    "082c393af687eb23b6fcc3c7172408a03d8d89",
                    "solutions/Anagram/Anagram.java",
                    "758c5a056f6c6a1ad82622ddb20e9a29800a13df"),
            new ProblemSuperset(
                    "geeksforgeeks:number-of-occurrence",
                    "Number of Occurrence",
                    AlgorithmShape.BINARY_SEARCH,
                    "ChallengeSupersetKernels.countSortedOccurrences",
                    List.of(
                            new DonorOrigin(
                                    "kishanrajput23/GFG-Problem-Solutions",
                                    GFG_KISHAN_CURRENT_COMMIT,
                                    "Solutions/Java/Number_of_occurrence.java",
                                    "615b2d0d62de481a79e0f9540a8cc5da08265080",
                                    "MIT"),
                            new DonorOrigin(
                                    "cvalingam/GeeksforGeeks",
                                    GFG_CVALINGAM_COMMIT,
                                    "solutions/Number of occurrence/Number of occurrence.java",
                                    "feb8b0aef90819958a83c17b088dd532ee05a874",
                                    "MIT"))),
            new ProblemSuperset(
                    "cross-platform:lcs-length",
                    "Longest Common Subsequence Length",
                    AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE,
                    "CanonicalAlgorithmDonors5MathDp.longestCommonSubsequence",
                    List.of(
                            new DonorOrigin(
                                    "Java-aid/Hackerrank-Solutions",
                                    JAVA_AID_COMMIT,
                                    "HackerRankDashboard/CoreCS/Algorithms/src/main/java/com/javaaid/hackerrank/solutions/algorithms/strings/CommonChild.java",
                                    "22eac1220b3dc392e4aee522aefa4df5cb58ee48",
                                    "MIT"),
                            new DonorOrigin(
                                    "cvalingam/GeeksforGeeks",
                                    GFG_CVALINGAM_COMMIT,
                                    "solutions/Longest Common Subsequence/Longest Common Subsequence.java",
                                    "e6985ecb49452839809f7cd955c29387a6bac327",
                                    "MIT"),
                            new DonorOrigin(
                                    "neetcode-gh/leetcode",
                                    NEETCODE_COMMIT,
                                    "java/1143-longest-common-subsequence.java",
                                    "d079fb422a1ed003fb2714039b9f961d11804075",
                                    "MIT"))));

    private static final Map<String, ProblemSuperset> BY_ID = index();
    private static final String ROOT = root(ALL);

    private ChallengeSupersetCatalog() {}

    public static List<ProblemSuperset> all() {
        return ALL;
    }

    public static ProblemSuperset require(String stableId) {
        ProblemSuperset problem = BY_ID.get(Objects.requireNonNull(stableId, "stableId"));
        if (problem == null) throw new IllegalArgumentException("unknown challenge superset: " + stableId);
        return problem;
    }

    public static java.util.Optional<ProblemSuperset> find(String stableId) {
        return java.util.Optional.ofNullable(
                BY_ID.get(Objects.requireNonNull(stableId, "stableId")));
    }

    public static boolean contains(String stableId) {
        return BY_ID.containsKey(Objects.requireNonNull(stableId, "stableId"));
    }

    public static int donorOriginCount() {
        return ALL.stream().mapToInt(problem -> problem.donors().size()).sum();
    }

    /** Deterministic receipt over every pinned problem/donor provenance field. */
    public static String root() {
        return ROOT;
    }

    private static ProblemSuperset leetcode(
            int number,
            String title,
            AlgorithmShape shape,
            String kernel,
            String neetcodePath,
            String neetcodeBlob,
            String fisherPath,
            String fisherBlob) {
        return new ProblemSuperset(
                "leetcode:" + number,
                title,
                shape,
                kernel,
                List.of(
                        new DonorOrigin(
                                "neetcode-gh/leetcode",
                                NEETCODE_COMMIT,
                                neetcodePath,
                                neetcodeBlob,
                                "MIT"),
                        new DonorOrigin(
                                "fishercoder1534/Leetcode",
                                FISHER_COMMIT,
                                fisherPath,
                                fisherBlob,
                                "Apache-2.0")));
    }

    private static ProblemSuperset hackerrank(
            String slug,
            String title,
            AlgorithmShape shape,
            String kernel,
            String javaAidPath,
            String javaAidBlob,
            String rodneyPath,
            String rodneyBlob) {
        return new ProblemSuperset(
                "hackerrank:" + slug,
                title,
                shape,
                kernel,
                List.of(
                        new DonorOrigin(
                                "Java-aid/Hackerrank-Solutions",
                                JAVA_AID_COMMIT,
                                javaAidPath,
                                javaAidBlob,
                                "MIT"),
                        new DonorOrigin(
                                "RodneyShag/HackerRank_solutions",
                                RODNEY_COMMIT,
                                rodneyPath,
                                rodneyBlob,
                                "MIT")));
    }

    private static ProblemSuperset geeksforgeeks(
            String slug,
            String title,
            AlgorithmShape shape,
            String kernel,
            String kishanPath,
            String kishanBlob,
            String cvalingamPath,
            String cvalingamBlob) {
        return new ProblemSuperset(
                "geeksforgeeks:" + slug,
                title,
                shape,
                kernel,
                List.of(
                        new DonorOrigin(
                                "kishanrajput23/GFG-Problem-Solutions",
                                GFG_KISHAN_COMMIT,
                                kishanPath,
                                kishanBlob,
                                "MIT"),
                        new DonorOrigin(
                                "cvalingam/GeeksforGeeks",
                                GFG_CVALINGAM_COMMIT,
                                cvalingamPath,
                                cvalingamBlob,
                                "MIT")));
    }

    private static Map<String, ProblemSuperset> index() {
        LinkedHashMap<String, ProblemSuperset> values = new LinkedHashMap<>();
        for (ProblemSuperset problem : ALL) {
            if (values.putIfAbsent(problem.stableId(), problem) != null) {
                throw new ExceptionInInitializerError("duplicate superset id " + problem.stableId());
            }
        }
        return Map.copyOf(values);
    }

    private static String root(List<ProblemSuperset> problems) {
        ArrayList<ProblemSuperset> stable = new ArrayList<>(problems);
        stable.sort(Comparator.comparing(ProblemSuperset::stableId));
        CatalogueDigest digest = new CatalogueDigest("SYNEXIA_CHALLENGE_SUPERSET_CATALOG_V1");
        for (ProblemSuperset problem : stable) {
            digest.text(problem.stableId())
                    .text(problem.title())
                    .text(problem.shape().name())
                    .text(problem.kernel());
            ArrayList<DonorOrigin> donors = new ArrayList<>(problem.donors());
            donors.sort(Comparator.comparing(DonorOrigin::repository)
                    .thenComparing(DonorOrigin::path));
            for (DonorOrigin donor : donors) {
                digest.text(donor.repository())
                        .text(donor.commit())
                        .text(donor.path())
                        .text(donor.blobSha())
                        .text(donor.license());
            }
        }
        return digest.finish();
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }
}
