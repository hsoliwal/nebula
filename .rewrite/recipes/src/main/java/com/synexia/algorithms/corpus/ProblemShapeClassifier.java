// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Classifies contest/problem implementations by invariant computational shape rather than story.
 *
 * <p>Name/path rules are intentionally conservative. Optional Java source text provides a second
 * signal for repositories whose file names are opaque. Unresolved entries remain UNCLASSIFIED so
 * they can drive discovery of genuinely new canonical shapes instead of being forced into a bad
 * match.
 */
public final class ProblemShapeClassifier {

    private record Rule(
            AlgorithmShape shape,
            ClassificationConfidence confidence,
            String rationale,
            String[] tokens) {}

    private static final List<Rule> RULES = List.of(
            r(AlgorithmShape.FENWICK_TREE, "Fenwick/Binary Indexed Tree", "fenwick", "binaryindexedtree", "bitree"),
            r(AlgorithmShape.SEGMENT_TREE, "segment tree", "segmenttree"),
            r(AlgorithmShape.SPARSE_TABLE, "sparse table", "sparsetable"),
            r(AlgorithmShape.AHO_CORASICK, "Aho-Corasick", "ahocorasick"),
            r(AlgorithmShape.MANACHER, "Manacher palindrome", "manacher"),
            r(AlgorithmShape.RABIN_KARP, "Rabin-Karp", "rabinkarp"),
            r(AlgorithmShape.Z_ALGORITHM, "Z algorithm", "zalgorithm"),
            r(AlgorithmShape.KMP, "KMP/string search", "kmp", "implementstrstr", "strstr"),
            r(AlgorithmShape.TRIE, "trie/prefix index", "implementtrie", "prefixtrie", "worddictionary",
                    "magicdictionary", "replacewords", "mapsumpairs", "autocompletesystem"),
            r(AlgorithmShape.LOWEST_COMMON_ANCESTOR, "lowest common ancestor", "lowestcommonancestor"),
            r(AlgorithmShape.BINARY_LIFTING, "binary lifting", "binarylifting", "kthancestor"),
            r(AlgorithmShape.EULER_TOUR, "Euler tour tree representation", "eulertour"),
            r(AlgorithmShape.STRONGLY_CONNECTED_COMPONENTS, "strongly connected components",
                    "strongconnect", "stronglyconnected", "kosaraju", "tarjan"),
            r(AlgorithmShape.BRIDGES_ARTICULATION, "bridges/articulation points",
                    "criticalconnection", "articulation", "bridgeingraph", "bridges"),
            r(AlgorithmShape.EULERIAN_PATH, "Eulerian path/itinerary", "eulerian", "reconstructitinerary"),
            r(AlgorithmShape.MIN_COST_MAX_FLOW, "min-cost max-flow", "mincostmaxflow"),
            r(AlgorithmShape.MAX_FLOW, "maximum flow", "maxflow", "fordfulkerson", "edmondskarp"),
            r(AlgorithmShape.BELLMAN_FORD, "bounded/negative-edge relaxation",
                    "bellmanford", "cheapestflightswithinkstops"),
            r(AlgorithmShape.FLOYD_WARSHALL, "all-pairs shortest path", "floydwarshall"),
            r(AlgorithmShape.ZERO_ONE_BFS, "0-1 BFS", "01bfs", "zeroonebfs"),
            r(AlgorithmShape.A_STAR, "A* search", "astar"),
            r(AlgorithmShape.PRIM_MST, "Prim minimum spanning tree", "primmst", "prims"),
            r(AlgorithmShape.KRUSKAL_MST, "Kruskal minimum spanning tree", "kruskal"),
            r(AlgorithmShape.UNION_FIND, "disjoint-set connectivity", "unionfind", "disjointset",
                    "accountsmerge", "redundantconnection", "friendcircles", "numberofislandsii",
                    "sentencesimilarityii"),
            r(AlgorithmShape.TOPOLOGICAL_SORT, "dependency topological order", "courseschedule",
                    "firstthousand207java", "0207courseschedule",
                    "aliendictionary", "topologicalsort", "sequenceconstruction"),
            r(AlgorithmShape.DIJKSTRA, "non-negative weighted shortest path", "dijkstra", "networkdelaytime",
                    "themazeii", "themazeiii"),
            r(AlgorithmShape.BIDIRECTIONAL_SEARCH, "bidirectional search", "bidirectional", "wordladder"),
            r(AlgorithmShape.BFS, "breadth-first traversal", "levelorder", "wallandgate", "wallsandgates",
                    "openthelock", "shortestbridge", "rottingoranges", "minimumgeneticmutation"),
            r(AlgorithmShape.GRID_CONNECTED_COMPONENTS, "implicit 4-neighbor grid components",
                    "numberofislands200", "0200numberofislands", "firstthousand200java",
                    "numberofislands"),
            r(AlgorithmShape.DFS, "depth-first traversal", "maxareaofisland",
                    "surroundedregions", "clonegraph", "pathsum", "subtree", "floodfill"),
            r(AlgorithmShape.CONNECTED_COMPONENTS, "connected components", "connectedcomponents"),
            r(AlgorithmShape.SLIDING_WINDOW, "sliding window", "minimumwindowsubstring",
                    "longestsubstringwithoutrepeating", "longestsubstringwithatmost",
                    "findallanagrams", "permutationinstring", "minimumsizesubarray",
                    "maximumaveragesubarray"),
            r(AlgorithmShape.MONOTONIC_QUEUE, "monotonic deque/window", "slidingwindowmaximum"),
            r(AlgorithmShape.MONOTONIC_STACK, "monotonic stack", "dailytemperatures", "nextgreater",
                    "largestrectangleinhistogram", "maximalrectangle"),
            r(AlgorithmShape.PREFIX_SCAN, "prefix/cumulative scan", "rangesumquery", "subarraysumequals",
                    "maximumsizesubarraysumequals", "contiguousarray", "productofarrayexceptself"),
            r(AlgorithmShape.DIFFERENCE_SCAN, "difference-array range update", "rangeaddition", "differencearray"),
            r(AlgorithmShape.KADANE, "maximum subarray recurrence", "maximumsubarray", "maxsubarray"),
            r(AlgorithmShape.LONGEST_INCREASING_SUBSEQUENCE, "LIS/patience sequence",
                    "longestincreasingsubsequence", "russiandollenvelopes", "numberoflongestincreasing"),
            r(AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE, "LCS recurrence", "longestcommonsubsequence"),
            r(AlgorithmShape.COIN_CHANGE, "coin-change recurrence",
                    "coinchange", "firstthousand322java", "0322coinchange"),
            r(AlgorithmShape.KNAPSACK_01, "0/1 knapsack recurrence", "zerooneknapsack", "01knapsack",
                    "partitionequalsubsetsum", "onesandzeroes"),
            r(AlgorithmShape.INTERVAL_DP, "interval dynamic programming", "burstballoons", "matrixchain"),
            r(AlgorithmShape.BITMASK_DP, "bitmask state dynamic programming", "caniwin",
                    "shortestsuperstring", "travellingsalesman", "travelingsalesman"),
            r(AlgorithmShape.DIGIT_DP, "digit dynamic programming", "numberofdigitone",
                    "countnumberswithuniquedigits"),
            r(AlgorithmShape.BACKTRACKING, "backtracking/enumeration", "permutations", "combinationsum",
                    "generateparentheses", "nqueens", "sudokusolver", "wordsearch", "restoreipaddresses",
                    "removeinvalidparentheses", "expressionaddoperators", "palindromepartitioning",
                    "subsets"),
            r(AlgorithmShape.CONSTRAINT_SEARCH, "constraint search", "sudoku", "androidunlockpatterns"),
            r(AlgorithmShape.GREEDY, "greedy selection", "jumpgame", "lemonadechange", "patchingarray",
                    "queuereconstruction", "taskScheduler", "handofstraights", "boatsToSavePeople"),
            r(AlgorithmShape.INTERVAL_MERGE, "interval merge", "mergeintervals", "insertinterval"),
            r(AlgorithmShape.SWEEP_LINE, "sweep line", "skyline", "rectangleareaii", "meetingroomsii"),
            r(AlgorithmShape.INTERVAL_INDEX, "interval index", "rangemodule", "datastreamasdisjointintervals"),
            r(AlgorithmShape.COVERAGE, "coverage/overlap", "meetingrooms", "employeefreetime",
                    "nonoverlappingintervals"),
            r(AlgorithmShape.ASSIGNMENT, "minimum-cost assignment", "hungarian", "assignmentproblem"),
            r(AlgorithmShape.BIPARTITE_MATCHING, "bipartite matching", "bipartitematching"),
            r(AlgorithmShape.LRU, "LRU cache", "lrucache"),
            r(AlgorithmShape.PRIORITY_QUEUE, "priority scheduling", "priorityqueue", "taskscheduler",
                    "rearrangestringkdistance", "reorganizestring"),
            r(AlgorithmShape.RENDEZVOUS_HASH, "rendezvous hashing", "rendezvoushash"),
            r(AlgorithmShape.STATE_TABLE, "state machine/table", "statemachine"),
            r(AlgorithmShape.MULTIWAY_MERGE, "multiway merge", "mergeksorted"),
            r(AlgorithmShape.ORDERED_MERGE, "ordered two-way merge", "mergesortedarray", "mergetwosorted"),
            r(AlgorithmShape.DUTCH_FLAG, "three-way partition", "sortcolors"),
            r(AlgorithmShape.BOYER_MOORE_MAJORITY, "majority vote", "majorityelement"),
            r(AlgorithmShape.QUICKSELECT, "quickselect", "quickselect"),
            r(AlgorithmShape.HEAP_SELECT, "top-k/kth selection", "topk", "kthlargest", "kthsmallest",
                    "findkpairswithsmallestsums", "medianfromdatastream"),
            r(AlgorithmShape.EXPONENTIAL_SEARCH,
                    "exponential bound expansion followed by binary lower-bound",
                    "searchinasortedarrayofunknownsize", "exponentialsearch"),
            r(AlgorithmShape.GALLOPING_SEARCH,
                    "galloping/exponential seek from a known ordered position",
                    "gallopingsearch"),
            r(AlgorithmShape.BINARY_SEARCH, "binary/monotonic search", "binarysearch", "searchinsert",
                    "firstbadversion", "findminimuminrotated", "searchinrotated", "koko",
                    "findpeakelement", "sqrtx", "medianoftwosortedarrays"),
            r(AlgorithmShape.PAIR_SUM_HASH, "one-pass complement hash pair lookup",
                    "001twosum", "twosum1", "firstthousand1java", "/twosum.java", "twosum.java"),
            r(AlgorithmShape.HASH_MEMBERSHIP, "hash membership", "containsduplicate",
                    "groupanagrams", "validanagram", "isomorphicstrings", "jewelsandstones"),
            r(AlgorithmShape.FAST_SLOW_POINTER, "fast/slow pointer", "linkedlistcycle",
                    "findtheduplicatenumber", "middleofthelinkedlist"),
            r(AlgorithmShape.TWO_POINTER, "two-pointer scan", "threesum", "foursum",
                    "containerwithmostwater", "validpalindrome", "trappingrainwater"),
            r(AlgorithmShape.SORT_UNIQUE, "sort/unique", "removeduplicatesfromsorted",
                    "intersectionoftwoarrays"),
            r(AlgorithmShape.INSERTION_SORT, "insertion sort", "insertionsort"),
            r(AlgorithmShape.MERGE_SORT, "merge sort", "mergesort", "countofrangesum", "reversepairs"),
            r(AlgorithmShape.QUICK_SORT, "quick sort", "quicksort"),
            r(AlgorithmShape.HEAP_SORT, "heap sort", "heapsort"),
            r(AlgorithmShape.COUNTING_SORT, "counting sort", "countingsort"),
            r(AlgorithmShape.RADIX_SORT, "radix sort", "radixsort"),
            r(AlgorithmShape.BUBBLE_SORT, "bubble sort", "bubblesort"),
            r(AlgorithmShape.SELECTION_SORT, "selection sort", "selectionsort"),
            r(AlgorithmShape.SHELL_SORT, "shell sort", "shellsort"),
            r(AlgorithmShape.BUCKET_SORT, "bucket sort", "bucketsort"),
            r(AlgorithmShape.STACK_MACHINE, "stack machine / delimiter evaluation",
                    "minstack", "balancedbrackets", "validparentheses", "basiccalculator",
                    "evaluatereversepolishnotation", "decodestring", "exclusiveTimeOfFunctions"),
            r(AlgorithmShape.QUEUE_MACHINE, "queue/deque state machine",
                    "designcircularqueue", "designcirculardeque", "implementqueueusingstacks",
                    "implementstackusingqueues"),
            r(AlgorithmShape.LINKED_LIST_REWRITE, "linked-list pointer rewrite",
                    "reverselinkedlist", "reversenodesinkgroup", "reorderlist", "oddevenlinkedlist",
                    "removenthnode", "swappairs", "partitionlist", "deletnodeinalinkedlist"),
            r(AlgorithmShape.MATRIX_SCAN, "matrix/grid traversal or rewrite",
                    "spiralmatrix", "rotateimage", "diagonaltraverse", "zeromatrix",
                    "imagesmoother", "setmatrixzeroes"),
            r(AlgorithmShape.FREQUENCY_L1_DISTANCE, "multiset frequency L1 distance",
                    "makinganagrams", "stringsmakinganagrams", "ctcimakinganagrams"),
            r(AlgorithmShape.STRING_TRANSFORM, "direct string transformation",
                    "addboldtaginstring", "backspacestringcompare", "buddystrings",
                    "compareversionnumbers", "licensekeyformatting", "reversewords",
                    "stringcompression", "tolowercase", "superreducedstring", "camelcase",
                    "pangrams", "marsExploration"),
            r(AlgorithmShape.BIT_MANIPULATION, "direct bit manipulation",
                    "countingbits", "singleNumber", "bitwiseandofnumbersrange", "hammingdistance",
                    "totalhammingdistance", "lonelyinteger", "maximizingxor", "graycode"),
            r(AlgorithmShape.ARITHMETIC, "direct numeric/arithmetic transformation",
                    "addbinary", "addstrings", "multiplystrings", "dividetwointegers",
                    "reverseinteger", "palindromenumber", "integertoroman", "romantointeger",
                    "excelsheetcolumn", "finddigits", "averybigsum", "diagonaldifference"),
            r(AlgorithmShape.CONCURRENCY_COORDINATION, "concurrency coordination",
                    "buildingh2o", "boundedblockingqueue", "foobarmultithreaded", "printinorder",
                    "zeroevenodd", "diningphilosophers", "trafficlightcontrolledintersection",
                    "webcrawlermultithreaded"),
            r(AlgorithmShape.SIMULATION, "direct state/process simulation",
                    "baseballgame", "gameoflife", "snakegame", "tictactoe", "countingvalleys",
                    "sockmerchant", "jumpingontheclouds", "saveThePrisoner", "libraryfine",
                    "gradingstudents", "appleandorange", "designerpdfviewer"),
            r(AlgorithmShape.TREE_TRAVERSAL, "tree traversal/aggregation", "binarytree", "narytree",
                    "bstiterator", "validatebinarysearchtree", "diameteroftree", "diameterofbinarytree"),
            r(AlgorithmShape.GCD_EUCLID, "Euclidean GCD", "greatestcommondivisor", "waterandjug"),
            r(AlgorithmShape.SIEVE, "prime sieve", "countprimes", "sieve"),
            r(AlgorithmShape.MODULAR_EXPONENTIATION, "fast exponentiation", "powxn", "modularpower"),
            r(AlgorithmShape.MILLER_RABIN, "primality test", "millerrabin"),
            r(AlgorithmShape.XOR_BASIS, "XOR optimization", "maximumxor", "xorbasis"),
            r(AlgorithmShape.BITSET_DP, "bitset state", "bitset", "subsetsum"),
            r(AlgorithmShape.CONVEX_HULL, "convex hull", "convexhull", "erectthefence"),
            r(AlgorithmShape.ORIENTATION, "geometry/orientation", "maxpointsonaline", "pointonline"),
            h(AlgorithmShape.DYNAMIC_PROGRAMMING, "dynamic-programming family", "dynamicprogramming",
                    "houserobber", "decodeways", "uniquepaths", "climbingstairs", "paintHouse",
                    "perfectsquares", "dungeongame", "maximalsquare", "stock", "wigglesubsequence"),
            h(AlgorithmShape.MEMOIZATION, "memoized recurrence", "memoization", "frogjump"),
            h(AlgorithmShape.GREEDY, "generic greedy family", "greedy"),
            h(AlgorithmShape.BFS, "generic BFS family", "breadthfirst", "bfs"),
            h(AlgorithmShape.DFS, "generic DFS family", "depthfirst", "dfs"),

            // Repository taxonomy is useful secondary evidence. These are deliberately HEURISTIC:
            // an exact problem-name rule above always wins.
            h(AlgorithmShape.BINARY_SEARCH, "repository binary-search category",
                    "02binarysearch", "binary_search"),
            h(AlgorithmShape.QUICK_SORT, "repository quick-sort category", "04quicksort"),
            h(AlgorithmShape.MERGE_SORT, "repository merge-sort category", "04mergesort"),
            h(AlgorithmShape.TWO_POINTER, "repository two-pointer category",
                    "06twopointers", "two_pointers"),
            h(AlgorithmShape.SLIDING_WINDOW, "repository sliding-window category",
                    "061slidingwindow", "sliding_window"),
            h(AlgorithmShape.PRIORITY_QUEUE, "repository heap/priority-queue category",
                    "09priorityqueueheap", "priority_queue", "/heap/"),
            h(AlgorithmShape.UNION_FIND, "repository union-find category",
                    "10unionfind", "union_find"),
            h(AlgorithmShape.TREE_TRAVERSAL, "repository tree category",
                    "11binarytree", "11binarysearchtree", "/tree/"),
            h(AlgorithmShape.BACKTRACKING, "repository backtracking category",
                    "12backtrackinganddfs", "/backtracking/"),
            h(AlgorithmShape.DYNAMIC_PROGRAMMING, "repository dynamic-programming category",
                    "13dynamicprogramming1", "14dynamicprogramming2", "dynamic_programming"),
            h(AlgorithmShape.GREEDY, "repository greedy category",
                    "15greedyalgorithm", "/greedy/"),
            h(AlgorithmShape.PREFIX_SCAN, "repository prefix-sum category",
                    "18prefixsum", "prefix_sum"),
            h(AlgorithmShape.BFS, "repository breadth-first-search category",
                    "19breadthfirstsearch", "breadth_first_search"),
            h(AlgorithmShape.DFS, "repository depth-first-search category",
                    "depth_first_search"),
            h(AlgorithmShape.TRIE, "repository trie category", "23trie", "/trie/"),
            h(AlgorithmShape.HASH_MEMBERSHIP, "repository hash-table category",
                    "17hashtable", "/hashing/"),
            h(AlgorithmShape.LINEAR_SCAN, "repository array/warmup category",
                    "srcmainjavaarray", "datastructuresarrays", "algorithmswarmup"),
            h(AlgorithmShape.FREQUENCY_COUNT, "repository map/counting category",
                    "srcmainjavahashtable", "countingfrequency", "frequencyqueries"),
            h(AlgorithmShape.STACK_MACHINE, "repository stack category",
                    "srcmainjavastack", "datastructuresstacks", "081stack"),
            h(AlgorithmShape.QUEUE_MACHINE, "repository queue category",
                    "srcmainjavaqueue", "datastructuresqueues", "082queue"),
            h(AlgorithmShape.LINKED_LIST_REWRITE, "repository linked-list category",
                    "srcmainjavalinkedlist", "linked_list", "datastructureslinkedlists", "07linkedlist"),
            h(AlgorithmShape.MATRIX_SCAN, "repository matrix/grid category",
                    "srcmainjavamatrix", "/matrix/", "matrixgrid"),
            h(AlgorithmShape.STRING_TRANSFORM, "repository string category",
                    "srcmainjavastring", "algorithmsstrings", "javastrings", "28string"),
            h(AlgorithmShape.BIT_MANIPULATION, "repository bit-manipulation category",
                    "srcmainjavabitmanipulation", "algorithmsbitmanipulation"),
            h(AlgorithmShape.ARITHMETIC, "repository math/numeric category",
                    "srcmainjavamath", "/math/"),
            h(AlgorithmShape.CONCURRENCY_COORDINATION, "repository concurrency category",
                    "concurrency"),
            h(AlgorithmShape.SIMULATION, "repository implementation/simulation category",
                    "algorithmsimplementation", "generalprogrammingbasicprogramming")
    );

    private ProblemShapeClassifier() {}

    public static ProblemShape classify(String problemOrPath) {
        return classify(problemOrPath, null);
    }

    public static ProblemShape classify(String problemOrPath, String javaSource) {
        Objects.requireNonNull(problemOrPath, "problemOrPath");
        String normalized = normalize(problemOrPath);
        List<String> segments = normalizedSegments(problemOrPath);
        for (Rule rule : RULES) {
            if (matches(normalized, segments, rule)) {
                return classified(problemOrPath, rule);
            }
        }

        if (javaSource != null && !javaSource.isBlank()) {
            String source = javaSource.toLowerCase(Locale.ROOT);
            ProblemShape sourceResult = classifySource(problemOrPath, source);
            if (sourceResult != null) return sourceResult;
        }

        return new ProblemShape(
                problemOrPath,
                null,
                EnterpriseTemplateKind.CUSTOM,
                ClassificationConfidence.UNCLASSIFIED,
                "No sufficiently specific shape rule matched");
    }

    private static ProblemShape classifySource(String problem, String source) {
        String compact = normalize(source);
        if (compact.contains("twosum") && compact.contains("containskey")
                && compact.contains("target") && compact.contains("map")) {
            return source(problem, AlgorithmShape.PAIR_SUM_HASH,
                    "pair-sum complement hash source signal");
        }
        if (source.contains("lowbit(") || source.contains("i += i & -i") || source.contains("k += lowbit")) {
            return source(problem, AlgorithmShape.FENWICK_TREE, "Fenwick low-bit update/query pattern");
        }
        if (compact.contains("numislands")
                || (compact.contains("grid") && compact.contains("island")
                    && (compact.contains("dfs") || compact.contains("queue")
                        || compact.contains("unionfind")))) {
            return source(problem, AlgorithmShape.GRID_CONNECTED_COMPONENTS,
                    "implicit-grid connected-component source signal");
        }
        if (source.contains("unionfind") || source.contains("disjointset")) {
            return source(problem, AlgorithmShape.UNION_FIND, "union/disjoint-set source signal");
        }
        if (source.contains("dijkstra")) {
            return source(problem, AlgorithmShape.DIJKSTRA, "Dijkstra source signal");
        }
        if (source.contains("bellman") && source.contains("relax")) {
            return source(problem, AlgorithmShape.BELLMAN_FORD, "Bellman-Ford relaxation source signal");
        }
        if (source.contains("indegree") || source.contains("in-degree") || source.contains("topological")) {
            return source(problem, AlgorithmShape.TOPOLOGICAL_SORT, "in-degree/topological source signal");
        }
        if (source.contains("trie") && (source.contains("child") || source.contains("children"))) {
            return source(problem, AlgorithmShape.TRIE, "trie node source signal");
        }
        if (source.contains("priorityqueue")) {
            return source(problem, AlgorithmShape.PRIORITY_QUEUE, "priority queue source signal");
        }
        if (source.contains("stack<") || source.contains("deque<") && source.contains("push(")) {
            return source(problem, AlgorithmShape.STACK_MACHINE, "stack operations source signal");
        }
        if (source.contains("queue<") || source.contains("arraydeque<") && source.contains("addlast(")) {
            return source(problem, AlgorithmShape.QUEUE_MACHINE, "queue operations source signal");
        }
        if (source.contains("semaphore") || source.contains("countdownlatch")
                || source.contains("cyclicbarrier") || source.contains("synchronized")) {
            return source(problem, AlgorithmShape.CONCURRENCY_COORDINATION,
                    "concurrency primitive source signal");
        }
        if (source.contains(">>>") || source.contains("integer.bitcount")
                || source.contains("long.bitcount")) {
            return source(problem, AlgorithmShape.BIT_MANIPULATION, "bit-operation source signal");
        }
        if (source.contains("memo") && source.contains("dfs")) {
            return source(problem, AlgorithmShape.MEMOIZATION, "memoized DFS source signal");
        }
        if (source.contains("boolean[][] dp") || source.contains("int[][] dp")
                || source.contains("long[][] dp") || source.contains("int[] dp")) {
            return source(problem, AlgorithmShape.DYNAMIC_PROGRAMMING, "DP table source signal");
        }
        if (source.contains("arraydeque") && source.contains("offer") && source.contains("poll")) {
            return source(problem, AlgorithmShape.BFS, "queue traversal source signal");
        }
        return null;
    }

    private static ProblemShape source(String problem, AlgorithmShape shape, String rationale) {
        return new ProblemShape(
                problem,
                shape,
                ProblemTemplateCatalog.style(shape).kind(),
                ClassificationConfidence.HEURISTIC,
                rationale);
    }

    private static ProblemShape classified(String problem, Rule rule) {
        return new ProblemShape(
                problem,
                rule.shape(),
                ProblemTemplateCatalog.style(rule.shape()).kind(),
                rule.confidence(),
                rule.rationale());
    }

    private static Rule r(AlgorithmShape shape, String rationale, String... tokens) {
        return new Rule(shape, ClassificationConfidence.EXACT, rationale, normalize(tokens));
    }

    private static Rule h(AlgorithmShape shape, String rationale, String... tokens) {
        return new Rule(shape, ClassificationConfidence.HEURISTIC, rationale, normalize(tokens));
    }

    private static String[] normalize(String[] tokens) {
        String[] out = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) out[i] = normalize(tokens[i]);
        return out;
    }

    private static boolean matches(String compact, List<String> segments, Rule rule) {
        for (String token : rule.tokens()) {
            for (String segment : segments) {
                if (segment.contains(token)) return true;
            }
            if ((rule.confidence() == ClassificationConfidence.HEURISTIC
                    || token.startsWith("firstthousand"))
                    && compact.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> normalizedSegments(String value) {
        return java.util.Arrays.stream(value.split("[/\\\\]"))
                .map(ProblemShapeClassifier::normalize)
                .filter(segment -> !segment.isEmpty())
                .toList();
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) out.append(c);
        }
        return out.toString();
    }
}
