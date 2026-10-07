// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

/**
 * Real full-source differential oracle: actual registered SWT callbacks run in isolated
 * before/after classes, with foreign widget callees mocked. No predicate surrogate or display.
 */
final class RoundedToolbarRuntimeContractTest {
    private static final String PACKAGE = "org.eclipse.nebula.widgets.opal.roundedtoolbar.";
    private static final String TOOLBAR = PACKAGE + "RoundedToolbar";
    private static final String ITEM = PACKAGE + "RoundedToolItem";
    private static final String TOOLBAR_ROOT =
            "widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/src/org/eclipse/nebula/widgets/opal/roundedtoolbar/";
    private static final String ADVANCED_PATH =
            "widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons/src/org/eclipse/nebula/widgets/opal/commons/AdvancedPath.java";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/rounded-toolbar-stream/RoundedToolbar.";
    private static final String BEFORE_SHA256 =
            "fd00029ccb595dfedd0ec1c0604856359e065c9d2ca3fcbbe86382b15bbc0c59";
    private static final Map<String, String> NEIGHBOR_HASHES = Map.of(
            TOOLBAR_ROOT + "RoundedToolItem.java",
            "e85cb1ede372dba0ac02164a4d0300f17f43867119eea45139611e515f317edf",
            TOOLBAR_ROOT + "GradientColor.java",
            "0ccf8d46a2da4e5a039ef066bd861116a3e41ce8b095fe27acbd50f27a3d9869",
            ADVANCED_PATH,
            "391897789ba47701cc54a3b9a2d9624ff588fa300315618e5210b1b4a108736f");
    private static final Pattern SEGMENT = Pattern.compile(
            "items\\.stream\\(\\)\\s*//\\s*\\.filter\\(element\\s*->\\s*element\\.getBounds\\(\\)"
                    + "\\.contains\\(event\\.x,\\s*event\\.y\\)\\s*&&\\s*element\\.isEnabled\\(\\)\\)"
                    + "\\s*//\\s*\\.findFirst\\(\\)");
    private static final Map<String, Path> COMPILED = new HashMap<>();
    private static URLClassLoader sdkLoader;
    private static Path evidence;
    private static Path javac;

    @BeforeAll
    static void compileActualSourcesAndTwoFiniteMutants() throws Exception {
        Path root = Path.of(System.getProperty("m3.stream.root",
                Path.of(System.getProperty("basedir", ".")).resolve("../..").toString())).toAbsolutePath().normalize();
        String classpath = Objects.requireNonNull(System.getProperty("m3.stream.classpath"),
                "m3.stream.classpath must bind the real SWT/JFace SDK");
        List<URL> sdk = new ArrayList<>();
        Map<String, String> sdkHashes = Map.of(
                "org.eclipse.swt.win32.win32.x86_64-3.126.0.jar",
                "72039b5a1552923783770f714f5ff982486ecbb48c95b0325881bce3691d5655",
                "org.eclipse.jface-3.33.0.jar",
                "ed854ce69edb931e954ba5a155fe0e8dd61ee5f65b99f33a1c767210e1ee2fb2");
        for (String entry : classpath.split(Pattern.quote(System.getProperty("path.separator")))) {
            if (!entry.isBlank()) {
                Path jar = Path.of(entry);
                assertEquals(Objects.requireNonNull(sdkHashes.get(jar.getFileName().toString()),
                        "Only the custodied Java17 proof SDK forms this lane"), sha256(Files.readAllBytes(jar)));
                sdk.add(jar.toUri().toURL());
            }
        }
        assertEquals(2, sdk.size());
        sdkLoader = new URLClassLoader(sdk.toArray(URL[]::new), RoundedToolbarRuntimeContractTest.class.getClassLoader());
        evidence = Path.of(System.getProperty("m3.stream.evidence",
                Path.of(System.getProperty("basedir", ".")).resolve("target/runtime-evidence").toString()));
        Files.createDirectories(evidence);
        javac = Path.of(Objects.requireNonNull(System.getProperty("m3.stream.javac"),
                "m3.stream.javac must bind the real Java17 compiler for JavaSE-17 widget sources"));
        Process versionProcess = new ProcessBuilder(javac.toString(), "-version").redirectErrorStream(true)
                .redirectOutput(evidence.resolve("compiler-version.txt").toFile()).start();
        assertTrue(versionProcess.waitFor(60, TimeUnit.SECONDS), "Compiler version probe must finish");
        assertEquals(0, versionProcess.exitValue());
        String version = Files.readString(evidence.resolve("compiler-version.txt"), StandardCharsets.UTF_8).trim();
        assertTrue(version.startsWith("javac 17."), "Historic widget source lane requires actual Java17: " + version);
        Files.writeString(evidence.resolve("compiler-binding.tsv"),
                "javac\t" + javac + "\t" + sha256(Files.readAllBytes(javac)) + "\t" + version + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(evidence.resolve("observations.tsv"), "lane\tcase\texception\ttrace\n", StandardCharsets.UTF_8);
        Files.writeString(evidence.resolve("mutations.tsv"),
                "mutant\tcase\tbaseline_exception\tmutant_exception\tbaseline_trace\tmutant_trace\tdetected\n", StandardCharsets.UTF_8);
        byte[] originalBytes = resource("before");
        assertEquals(BEFORE_SHA256, sha256(originalBytes), "Oracle must be the exact pinned full real Nebula source");
        String before = new String(originalBytes, StandardCharsets.UTF_8);
        String after = new String(resource("after"), StandardCharsets.UTF_8);
        assertNotEquals(before, after, "The candidate must actually change the selected source");
        compile(root, classpath, "before", before);
        compile(root, classpath, "after", after);
        compile(root, classpath, "enhanced-for", mutant(before, false));
        compile(root, classpath, "early-coordinates", mutant(before, true));
        Files.writeString(evidence.resolve("source-bindings.tsv"),
                "before\t" + sha256(originalBytes) + "\nafter\t"
                        + sha256(after.getBytes(StandardCharsets.UTF_8)) + "\n"
                        + String.join("\n", NEIGHBOR_HASHES.entrySet().stream().sorted(Map.Entry.comparingByKey())
                                .map(entry -> entry.getKey() + "\t" + entry.getValue()).toList()) + "\n",
                StandardCharsets.UTF_8);
    }

    @AfterAll
    static void closeSdkLoader() throws Exception {
        if (sdkLoader != null) sdkLoader.close();
    }

    @TestFactory
    Stream<DynamicTest> actualCallbackBehaviorMatchesThePrewrittenContract() {
        return Arrays.stream(Scenario.values()).map(scenario -> DynamicTest.dynamicTest(scenario.name(), () -> {
            var supplied = new IllegalStateException("supplied getter failure");
            Outcome before = observe("before", scenario, supplied);
            Outcome after = observe("after", scenario, supplied);
            assertEquals(before.exceptionClass(), after.exceptionClass());
            assertEquals(before.trace(), after.trace());
            if (scenario.suppliedFailure()) {
                assertSame(supplied, before.throwable());
                assertSame(supplied, after.throwable());
            }
            if (scenario == Scenario.MUTATE_MATCH || scenario == Scenario.MUTATE_MISS) {
                assertEquals("java.util.ConcurrentModificationException", before.exceptionClass());
                assertFalse(before.trace().stream().anyMatch(value -> value.startsWith("toolbar.tooltip:")));
            }
            if (scenario == Scenario.MUTABLE_EVENT || scenario == Scenario.POST_REGISTRATION_EVENT) {
                assertTrue(before.trace().contains("toolbar.tooltip:tip-a"));
            }
        }));
    }

    @Test
    void registrationIsLazyAndNonprimaryMouseGuardsDoNotSearchItems() throws Exception {
        for (String lane : List.of("before", "after")) {
            try (Fixture fixture = new Fixture(lane, new IllegalStateException("unused"))) {
                fixture.configure(Scenario.OVERLAP);
                assertTrue(fixture.trace.isEmpty(), "Registering actual listeners performs no predicate work");
                assertEquals(4, fixture.listeners.size());
                assertEquals(1, fixture.paintListeners);
                Object event = fixture.event(5, 5, 2);
                assertEquals(null, fixture.invoke("MouseDown", event));
                assertEquals(null, fixture.invoke("MouseUp", event));
                assertTrue(fixture.trace.isEmpty(), "Guard returns precede all getters and toolbar effects");
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> primaryDownAndUpExecuteBothRewrittenSites() {
        return Stream.of("MouseDown", "MouseUp").flatMap(callback -> Stream.of(
                Scenario.EMPTY, Scenario.OVERLAP, Scenario.MISS_DISABLED_THEN_MATCH,
                Scenario.MUTABLE_EVENT, Scenario.MUTATE_MATCH, Scenario.BOUNDS_THROW)
                .map(scenario -> DynamicTest.dynamicTest(callback + ":" + scenario, () -> {
                    var supplied = new IllegalStateException("supplied primary callback failure");
                    Outcome before = observe("before", callback, scenario, supplied);
                    Outcome after = observe("after", callback, scenario, supplied);
                    assertEquals(before.exceptionClass(), after.exceptionClass());
                    assertEquals(before.trace(), after.trace());
                    if (scenario == Scenario.EMPTY) assertTrue(before.trace().isEmpty());
                    if (scenario == Scenario.BOUNDS_THROW) {
                        assertSame(supplied, before.throwable());
                        assertSame(supplied, after.throwable());
                        assertEquals(List.of("a.bounds"), before.trace());
                    } else if (scenario == Scenario.MUTATE_MATCH) {
                        assertEquals("java.util.ConcurrentModificationException", before.exceptionClass());
                        assertEquals(List.of("a.bounds", "a.enabled"), before.trace());
                    } else {
                        assertEquals("", before.exceptionClass());
                    }
                    List<String> searched = before.trace().stream()
                            .filter(value -> value.endsWith(".bounds") || value.endsWith(".enabled")).toList();
                    if (scenario == Scenario.OVERLAP || scenario == Scenario.MUTABLE_EVENT) {
                        assertEquals(List.of("a.bounds", "a.enabled"), searched);
                        assertTrue(before.trace().contains(callback.equals("MouseDown")
                                ? "a.setSelection" : "a.fireSelectionEvent"));
                    }
                    if (scenario == Scenario.MISS_DISABLED_THEN_MATCH) {
                        assertEquals(List.of("a.bounds", "b.bounds", "b.enabled", "c.bounds", "c.enabled"), searched);
                        assertTrue(before.trace().contains(callback.equals("MouseDown")
                                ? "c.setSelection" : "c.fireSelectionEvent"));
                    }
                })));
    }

    @Test
    void observableApiSignaturesStayTheSame() throws Exception {
        try (var before = ownerLoader("before"); var after = ownerLoader("after")) {
            Class<?> a = before.loadClass(TOOLBAR);
            Class<?> b = after.loadClass(TOOLBAR);
            assertSame(before, a.getClassLoader());
            assertSame(after, b.getClassLoader());
            assertNotEquals(a, b, "Before and after must be different actual loaded classes");
            List<String> original = api(a);
            assertEquals(original, api(b));
            assertTrue(original.stream().anyMatch(value -> value.contains("getItem(org.eclipse.swt.graphics.Point)")));
            Files.writeString(evidence.resolve("api-signatures.txt"), String.join("\n", original) + "\n", StandardCharsets.UTF_8);
            Files.writeString(evidence.resolve("api-census.tsv"), "lane\tmethods\tconstructors\tfields\n"
                    + apiCensus("before", a) + apiCensus("after", b), StandardCharsets.UTF_8);
        }
    }

    @Test
    void enhancedForMutantFailsBothMatchedAndMissedArrayListMutationDiscriminators() throws Exception {
        for (Scenario scenario : List.of(Scenario.MUTATE_MATCH, Scenario.MUTATE_MISS)) {
            var supplied = new IllegalStateException("unused");
            Outcome before = observe("before", scenario, supplied);
            Outcome mutant = observe("enhanced-for", scenario, supplied);
            assertEquals("java.util.ConcurrentModificationException", before.exceptionClass());
            assertEquals("", mutant.exceptionClass(), "Naive early return/iterator termination misses the check");
            mutation("enhanced-for", scenario, before, mutant);
        }
    }

    @Test
    void hoistingPrimitiveCoordinatesBeforeTheGetterIsDetected() throws Exception {
        var supplied = new IllegalStateException("unused");
        Outcome before = observe("before", Scenario.MUTABLE_EVENT, supplied);
        Outcome mutant = observe("early-coordinates", Scenario.MUTABLE_EVENT, supplied);
        assertEquals("", before.exceptionClass());
        assertEquals("", mutant.exceptionClass());
        assertNotEquals(before.trace(), mutant.trace());
        assertTrue(before.trace().contains("toolbar.tooltip:tip-a"));
        assertFalse(mutant.trace().stream().anyMatch(value -> value.startsWith("toolbar.tooltip:")));
        mutation("early-coordinates", Scenario.MUTABLE_EVENT, before, mutant);
    }

    private enum Scenario {
        EMPTY, OVERLAP, MISS_DISABLED_THEN_MATCH, NO_MATCH, NULL_ITEM, NULL_BOUNDS,
        EMPTY_NULL_EVENT, NULL_EVENT, BOUNDS_THROW, ENABLED_THROW, MUTABLE_EVENT,
        POST_REGISTRATION_EVENT, MUTATE_MATCH, MUTATE_MISS, MUTATE_THEN_THROW, NULL_TOOLTIP;
        boolean suppliedFailure() {
            return this == BOUNDS_THROW || this == ENABLED_THROW || this == MUTATE_THEN_THROW;
        }
    }

    private record Outcome(String exceptionClass, List<String> trace, Throwable throwable) {}

    private static Outcome observe(String lane, Scenario scenario, Throwable supplied) throws Exception {
        return observe(lane, "MouseHover", scenario, supplied);
    }

    private static Outcome observe(String lane, String callback, Scenario scenario, Throwable supplied) throws Exception {
        try (Fixture fixture = new Fixture(lane, supplied)) {
            fixture.configure(scenario);
            assertTrue(fixture.trace.isEmpty(), "Deferred listener registration must stay lazy");
            Object event = scenario == Scenario.EMPTY_NULL_EVENT || scenario == Scenario.NULL_EVENT
                    ? null : fixture.event(scenario == Scenario.MUTABLE_EVENT ? 100 : 5,
                            scenario == Scenario.MUTABLE_EVENT ? 100 : 5, 1);
            fixture.currentEvent = event;
            if (scenario == Scenario.POST_REGISTRATION_EVENT) {
                event = fixture.event(100, 100, 1);
                fixture.currentEvent = event;
                fixture.coordinate(event, "x", 5);
                fixture.coordinate(event, "y", 5);
            }
            Throwable thrown = fixture.invoke(callback, event);
            Outcome result = new Outcome(thrown == null ? "" : thrown.getClass().getName(),
                    List.copyOf(fixture.trace), thrown);
            Files.writeString(evidence.resolve("observations.tsv"),
                    lane + "\t" + callback + ":" + scenario + "\t" + result.exceptionClass() + "\t"
                            + String.join(";", result.trace()) + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            return result;
        }
    }

    private static final class Fixture implements AutoCloseable {
        final URLClassLoader owner;
        final Class<?> toolbarType;
        final Class<?> itemType;
        final Class<?> eventType;
        final Class<?> rectangleType;
        final Class<?> listenerType;
        final Class<?> swtType;
        final Object toolbar;
        final List<Object> items = new ArrayList<>();
        final Map<Integer, Object> listeners = new HashMap<>();
        final List<String> trace = new ArrayList<>();
        final Throwable supplied;
        int paintListeners;
        Object currentEvent;

        Fixture(String lane, Throwable supplied) throws Exception {
            this.supplied = supplied;
            owner = ownerLoader(lane);
            toolbarType = owner.loadClass(TOOLBAR);
            itemType = owner.loadClass(ITEM);
            assertSame(owner, toolbarType.getClassLoader());
            assertSame(owner, itemType.getClassLoader());
            eventType = sdkLoader.loadClass("org.eclipse.swt.widgets.Event");
            rectangleType = sdkLoader.loadClass("org.eclipse.swt.graphics.Rectangle");
            listenerType = sdkLoader.loadClass("org.eclipse.swt.widgets.Listener");
            swtType = sdkLoader.loadClass("org.eclipse.swt.SWT");
            assertSame(sdkLoader, eventType.getClassLoader(), "The parent must not shadow the bound historical SDK");
            toolbar = Mockito.mock(toolbarType, invocation -> {
                String name = invocation.getMethod().getName();
                if (name.equals("addListener")) {
                    listeners.put((Integer) invocation.getArgument(0), invocation.getArgument(1));
                    return null;
                }
                if (name.equals("addPaintListener")) { paintListeners++; return null; }
                if (name.equals("setToolTipText")) {
                    trace.add("toolbar.tooltip:" + invocation.getArgument(0));
                    return null;
                }
                if (name.equals("redraw") || name.equals("update")) { trace.add("toolbar." + name); return null; }
                if (name.equals("setData")) {
                    trace.add("toolbar.setData:" + Objects.toString(invocation.getArgument(0))
                            + ":" + Objects.toString(invocation.getArgument(1)));
                    return null;
                }
                if (name.equals("getData")) { trace.add("toolbar.getData"); return null; }
                if (name.equals("toString")) return "actual-toolbar-" + lane;
                return Mockito.CALLS_REAL_METHODS.answer(invocation);
            });
            var field = toolbarType.getDeclaredField("items");
            field.setAccessible(true);
            field.set(toolbar, items);
            var register = toolbarType.getDeclaredMethod("addListeners");
            register.setAccessible(true);
            register.invoke(toolbar);
        }

        void configure(Scenario scenario) throws Exception {
            if (scenario == Scenario.EMPTY || scenario == Scenario.EMPTY_NULL_EVENT) return;
            if (scenario == Scenario.NULL_ITEM) { items.add(null); return; }
            Object inside = rectangle(0, 0, 10, 10);
            Object outside = rectangle(100, 100, 10, 10);
            if (scenario == Scenario.MISS_DISABLED_THEN_MATCH) {
                items.add(item("a", outside, true, null, null, false));
                items.add(item("b", inside, false, null, null, false));
                items.add(item("c", inside, true, null, null, false));
                items.add(item("later", inside, true, () -> { throw new AssertionError("short-circuit failed"); }, null, false));
                return;
            }
            if (scenario == Scenario.NO_MATCH) {
                items.add(item("a", outside, true, null, null, false));
                items.add(item("b", inside, false, null, null, false));
                return;
            }
            Runnable boundsHook = null;
            Runnable enabledHook = null;
            if (scenario == Scenario.BOUNDS_THROW) boundsHook = () -> sneakyThrow(supplied);
            if (scenario == Scenario.ENABLED_THROW) enabledHook = () -> sneakyThrow(supplied);
            if (scenario == Scenario.MUTABLE_EVENT) {
                boundsHook = () -> {
                    try { coordinate(currentEvent, "x", 5); coordinate(currentEvent, "y", 5); }
                    catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                };
            }
            if (scenario == Scenario.MUTATE_MATCH) boundsHook = () -> items.add(items.getLast());
            if (scenario == Scenario.MUTATE_MISS) boundsHook = () -> items.removeLast();
            if (scenario == Scenario.MUTATE_THEN_THROW) boundsHook = () -> { items.add(items.getLast()); sneakyThrow(supplied); };
            Object bounds = scenario == Scenario.NULL_BOUNDS ? null : scenario == Scenario.MUTATE_MISS ? outside : inside;
            items.add(item("a", bounds, true, boundsHook, enabledHook, scenario == Scenario.NULL_TOOLTIP));
            if (scenario == Scenario.OVERLAP || scenario == Scenario.MUTATE_MATCH
                    || scenario == Scenario.MUTATE_MISS || scenario == Scenario.MUTATE_THEN_THROW) {
                items.add(item("later", inside, true, () -> { throw new AssertionError("later item reached"); }, null, false));
            }
        }

        Object item(String label, Object bounds, boolean enabled, Runnable boundsHook,
                Runnable enabledHook, boolean nullTooltip) {
            return Mockito.mock(itemType, invocation -> {
                String name = invocation.getMethod().getName();
                if (name.equals("toString")) return label;
                if (name.equals("getBounds")) {
                    trace.add(label + ".bounds");
                    if (boundsHook != null) boundsHook.run();
                    return bounds;
                }
                if (name.equals("isEnabled")) {
                    trace.add(label + ".enabled");
                    if (enabledHook != null) enabledHook.run();
                    return enabled;
                }
                if (name.equals("getTooltipText")) {
                    trace.add(label + ".tooltip");
                    return nullTooltip ? null : "tip-" + label;
                }
                trace.add(label + "." + name);
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            });
        }

        Object rectangle(int x, int y, int width, int height) throws ReflectiveOperationException {
            return rectangleType.getConstructor(int.class, int.class, int.class, int.class).newInstance(x, y, width, height);
        }

        Object event(int x, int y, int button) throws ReflectiveOperationException {
            Object event = eventType.getConstructor().newInstance();
            coordinate(event, "x", x); coordinate(event, "y", y); coordinate(event, "button", button);
            return event;
        }

        void coordinate(Object event, String name, int value) throws ReflectiveOperationException {
            eventType.getField(name).setInt(event, value);
        }

        Throwable invoke(String type, Object event) throws Exception {
            int eventId = swtType.getField(type).getInt(null);
            Object listener = Objects.requireNonNull(listeners.get(eventId));
            try {
                listenerType.getMethod("handleEvent", eventType).invoke(listener, event);
                return null;
            } catch (InvocationTargetException failure) {
                return failure.getCause();
            }
        }

        @Override public void close() throws IOException { owner.close(); }
    }

    private static URLClassLoader ownerLoader(String lane) throws Exception {
        URL[] source = {Objects.requireNonNull(COMPILED.get(lane)).toUri().toURL()};
        return new URLClassLoader(source, sdkLoader) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith(PACKAGE) && !name.equals("org.eclipse.nebula.widgets.opal.commons.AdvancedPath")) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
    }

    private static List<String> api(Class<?> type) {
        List<String> signatures = new ArrayList<>();
        Arrays.stream(type.getDeclaredMethods()).filter(method -> !Modifier.isPrivate(method.getModifiers()))
                .map(method -> method.toGenericString()).forEach(signatures::add);
        Arrays.stream(type.getDeclaredConstructors()).map(constructor -> constructor.toGenericString()).forEach(signatures::add);
        Arrays.stream(type.getDeclaredFields()).filter(field -> !Modifier.isPrivate(field.getModifiers()))
                .map(field -> field.toGenericString()).forEach(signatures::add);
        signatures.sort(String::compareTo);
        return List.copyOf(signatures);
    }

    private static String apiCensus(String lane, Class<?> type) {
        long methods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !Modifier.isPrivate(method.getModifiers())).count();
        long fields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isPrivate(field.getModifiers())).count();
        return lane + "\t" + methods + "\t" + type.getDeclaredConstructors().length + "\t" + fields + "\n";
    }

    private static byte[] resource(String lane) throws Exception {
        try (InputStream input = RoundedToolbarRuntimeContractTest.class.getResourceAsStream(RESOURCE + lane + ".java.txt")) {
            return Objects.requireNonNull(input, "Exact full-source " + lane + " resource required").readAllBytes();
        }
    }

    private static void compile(Path root, String classpath, String lane, String toolbarSource) throws Exception {
        Path work = Files.createDirectories(evidence.resolve(lane));
        Path sources = Files.createDirectories(work.resolve("source"));
        Path classes = Files.createDirectories(work.resolve("classes"));
        List<Path> inputs = new ArrayList<>();
        Path toolbar = sources.resolve("RoundedToolbar.java");
        Files.writeString(toolbar, toolbarSource, StandardCharsets.UTF_8);
        inputs.add(toolbar);
        for (String neighbor : List.of(TOOLBAR_ROOT + "RoundedToolItem.java", TOOLBAR_ROOT + "GradientColor.java", ADVANCED_PATH)) {
            Path source = root.resolve(neighbor);
            Path copy = sources.resolve(source.getFileName());
            byte[] bytes = Files.readAllBytes(source);
            assertEquals(NEIGHBOR_HASHES.get(neighbor), sha256(bytes), "Actual source neighbor custody: " + neighbor);
            Files.write(copy, bytes);
            inputs.add(copy);
        }
        List<String> command = new ArrayList<>(List.of(javac.toString(), "--release", "17", "-proc:none",
                "-Xlint:all", "-Werror", "-encoding", "UTF-8", "-classpath", classpath, "-d", classes.toString()));
        inputs.stream().map(Path::toString).forEach(command::add);
        Files.writeString(work.resolve("compile-command.txt"), String.join("\n", command) + "\n", StandardCharsets.UTF_8);
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(work.resolve("compile.log").toFile()).start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertTrue(finished, "Actual full-source strict compilation must finish, " + lane);
        String log = Files.readString(work.resolve("compile.log"), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), "Actual full-source strict compilation must pass, " + lane + "\n" + log);
        COMPILED.put(lane, classes);
    }

    private static String mutant(String before, boolean earlyCoordinates) {
        Matcher matches = SEGMENT.matcher(before);
        int count = 0;
        while (matches.find()) count++;
        assertEquals(3, count, "Only the three exact actual event pipelines form this mutant");
        String call = earlyCoordinates ? "naiveCoordinates(event.x, event.y)" : "naiveEnhancedFor(event)";
        String result = SEGMENT.matcher(before).replaceAll(Matcher.quoteReplacement(call));
        String signature = earlyCoordinates ? "naiveCoordinates(int x, int y)"
                : "naiveEnhancedFor(org.eclipse.swt.widgets.Event event)";
        String coordinates = earlyCoordinates ? "x, y" : "event.x, event.y";
        String method = "\n\tprivate Optional<RoundedToolItem> " + signature + " {\n"
                + "\t\tfor (RoundedToolItem element : items) {\n"
                + "\t\t\tif (element.getBounds().contains(" + coordinates + ") && element.isEnabled()) return Optional.of(element);\n"
                + "\t\t}\n\t\treturn Optional.empty();\n\t}\n";
        int end = result.lastIndexOf('}');
        return result.substring(0, end) + method + result.substring(end);
    }

    private static void mutation(String name, Scenario scenario, Outcome before, Outcome mutant) throws Exception {
        boolean detected = !before.exceptionClass().equals(mutant.exceptionClass()) || !before.trace().equals(mutant.trace());
        assertTrue(detected);
        Files.writeString(evidence.resolve("mutations.tsv"), name + "\t" + scenario + "\t"
                + before.exceptionClass() + "\t" + mutant.exceptionClass() + "\t"
                + String.join(";", before.trace()) + "\t" + String.join(";", mutant.trace()) + "\t" + detected + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void sneakyThrow(Throwable throwable) {
        if (throwable instanceof RuntimeException runtime) throw runtime;
        if (throwable instanceof Error error) throw error;
        throw new IllegalStateException(throwable);
    }
}
