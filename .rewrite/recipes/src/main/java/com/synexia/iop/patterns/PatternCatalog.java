package com.synexia.iop.patterns;

import com.synexia.iop.AOPAdvice;
import com.synexia.iop.DAGPattern;
import com.synexia.iop.Distributed;
import com.synexia.iop.EIP;
import com.synexia.iop.GoF;
import com.synexia.iop.patterns.PatternDescriptor.Authority;
import com.synexia.iop.patterns.PatternDescriptor.Family;
import com.synexia.iop.patterns.PatternDescriptor.Realization;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable deterministic catalog spanning every declared Synexia pattern family. */
public final class PatternCatalog {
    private record Seed(String name, String description) {}

    private static final List<Seed> MICROSERVICE = List.of(
            new Seed("CIRCUIT_BREAKER", "Fail fast on failure"),
            new Seed("BULKHEAD", "Isolate failures"),
            new Seed("RETRY", "Retry on failure"),
            new Seed("TIMEOUT", "Limit wait time"),
            new Seed("FALLBACK", "Default on failure"),
            new Seed("RATE_LIMITER", "Limit request rate"),
            new Seed("SERVICE_REGISTRY", "Register services"),
            new Seed("SERVICE_DISCOVERY", "Find services"),
            new Seed("LOAD_BALANCER", "Distribute load"),
            new Seed("API_GATEWAY", "Single entry point"),
            new Seed("SIDECAR", "Attach functionality"),
            new Seed("DATABASE_PER_SERVICE", "Isolated data"),
            new Seed("SAGA", "Distributed transaction"),
            new Seed("EVENT_SOURCING", "Store events"),
            new Seed("CQRS", "Separate read/write"),
            new Seed("OUTBOX", "Reliable events"),
            new Seed("DISTRIBUTED_TRACING", "Track requests"),
            new Seed("HEALTH_CHECK", "Monitor health"),
            new Seed("LOG_AGGREGATION", "Centralize logs"),
            new Seed("METRICS", "Collect metrics"));

    private static final List<Seed> SPRING = List.of(
            new Seed("DEPENDENCY_INJECTION", "Invert control"),
            new Seed("IOC_CONTAINER", "Manage beans"),
            new Seed("MVC", "Model-View-Controller"),
            new Seed("REPOSITORY", "Data access"),
            new Seed("SERVICE", "Business logic"),
            new Seed("COMPONENT", "Generic bean"),
            new Seed("CONFIGURATION", "Java config"),
            new Seed("TEMPLATE_METHOD", "JdbcTemplate-style template"),
            new Seed("PROFILE", "Environment config"),
            new Seed("CONDITIONAL", "Conditional beans"));

    private final List<PatternDescriptor> patterns;
    private final Map<String, PatternDescriptor> byId;
    private final Map<String, PatternRecipe> recipes;
    private final String root;

    public PatternCatalog(Collection<PatternDescriptor> descriptors) {
        List<PatternDescriptor> stable = descriptors.stream()
                .sorted(Comparator.comparing(PatternDescriptor::id))
                .toList();
        LinkedHashMap<String, PatternDescriptor> index = new LinkedHashMap<>();
        LinkedHashMap<String, PatternRecipe> recipeIndex = new LinkedHashMap<>();
        for (PatternDescriptor descriptor : stable) {
            PatternDescriptor prior = index.putIfAbsent(descriptor.id(), descriptor);
            if (prior != null && !prior.equals(descriptor)) {
                throw new IllegalArgumentException("conflicting pattern id: " + descriptor.id());
            }
            recipeIndex.put(descriptor.id(), PatternRecipeBook.recipeFor(descriptor));
        }
        this.byId = Map.copyOf(index);
        this.patterns = List.copyOf(index.values());
        this.recipes = Map.copyOf(recipeIndex);
        this.root = PatternHash.sha256("SYNEXIA_PATTERN_CATALOG_JAVA21_V1|"
                + patterns.stream().map(PatternDescriptor::root).reduce("", (a, b) -> a + b + "\n")
                + "|"
                + patterns.stream().map(value -> recipes.get(value.id()).root())
                        .reduce("", (a, b) -> a + b + "\n"));
    }

    public static PatternCatalog canonical() {
        return new PatternCatalog(buildCanonical());
    }

    public List<PatternDescriptor> patterns() {
        return patterns;
    }

    public Optional<PatternDescriptor> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public PatternRecipe recipe(String id) {
        PatternRecipe recipe = recipes.get(id);
        if (recipe == null) throw new IllegalArgumentException("unknown pattern id: " + id);
        return recipe;
    }

    public List<PatternDescriptor> family(Family family) {
        return patterns.stream().filter(value -> value.family() == family).toList();
    }

    public Map<Family, Integer> countsByFamily() {
        EnumMap<Family, Integer> counts = new EnumMap<>(Family.class);
        for (Family family : Family.values()) counts.put(family, 0);
        patterns.forEach(value -> counts.compute(value.family(), (key, count) -> count == null ? 1 : count + 1));
        return Map.copyOf(counts);
    }

    public String root() {
        return root;
    }

    private static List<PatternDescriptor> buildCanonical() {
        List<PatternDescriptor> out = new ArrayList<>();

        for (GoF value : GoF.values()) {
            out.add(PatternDescriptor.of(
                    Family.GOF,
                    value.name(),
                    value.category(),
                    value.description(),
                    "synexia-iop",
                    Realization.JAVA_PRIMITIVE,
                    Authority.PURE_LOCAL,
                    List.of("gof", value.category())));
        }

        for (EIP value : EIP.values()) {
            out.add(PatternDescriptor.of(
                    Family.EIP,
                    value.name(),
                    value.category(),
                    value.description(),
                    "synexia-camel-dag-bridge",
                    Realization.MODULE_RUNTIME,
                    eipAuthority(value.category()),
                    List.of("eip", "camel", value.category())));
        }

        for (DAGPattern value : DAGPattern.values()) {
            out.add(PatternDescriptor.of(
                    Family.DAG,
                    value.name(),
                    value.category(),
                    value.description(),
                    "synexia-flldcim",
                    Realization.MODULE_RUNTIME,
                    Authority.CANDIDATE_ONLY,
                    List.of("dag", value.category())));
        }

        for (Distributed value : Distributed.values()) {
            out.add(PatternDescriptor.of(
                    Family.DISTRIBUTED,
                    value.name(),
                    value.category(),
                    value.description(),
                    "synexia-cop",
                    Realization.MODULE_RUNTIME,
                    distributedAuthority(value.category()),
                    List.of("distributed", value.category())));
        }

        for (Seed value : MICROSERVICE) {
            out.add(PatternDescriptor.of(
                    Family.MICROSERVICE,
                    value.name(),
                    "Microservice",
                    value.description(),
                    "synexia-cop",
                    Realization.MODULE_RUNTIME,
                    Authority.LOCAL_ADMISSION_REQUIRED,
                    List.of("microservice")));
        }

        for (Seed value : SPRING) {
            out.add(PatternDescriptor.of(
                    Family.SPRING,
                    value.name(),
                    "Spring",
                    value.description(),
                    "synexia-iop",
                    Realization.FRAMEWORK_ADAPTER,
                    Authority.LOCAL_ADMISSION_REQUIRED,
                    List.of("spring")));
        }

        for (AOPAdvice value : AOPAdvice.values()) {
            out.add(PatternDescriptor.of(
                    Family.AOP_ADVICE,
                    value.name(),
                    value.category(),
                    value.description(),
                    "synexia-aop",
                    Realization.MODULE_RUNTIME,
                    Authority.LOCAL_ADMISSION_REQUIRED,
                    List.of("aop", value.category())));
        }

        for (SynexiaPattern value : SynexiaPattern.values()) {
            out.add(PatternDescriptor.of(
                    Family.SYNEXIA,
                    value.name(),
                    value.category(),
                    value.description(),
                    value.targetModule(),
                    Realization.CONTRACT_RECIPE,
                    value.authority(),
                    List.of("synexia", value.category())));
        }

        return List.copyOf(out);
    }

    private static Authority eipAuthority(String category) {
        return switch (category) {
            case "Transformation", "Routing", "Management" -> Authority.CANDIDATE_ONLY;
            case "Endpoint", "Orchestration", "ErrorHandling", "Channel", "Concurrency", "Resilience" ->
                    Authority.LOCAL_ADMISSION_REQUIRED;
            default -> Authority.CANDIDATE_ONLY;
        };
    }

    private static Authority distributedAuthority(String category) {
        return switch (category) {
            case "Data" -> Authority.LOCAL_ADMISSION_REQUIRED;
            case "Communication", "Concurrency", "Failure", "Deployment", "Security", "Messaging" ->
                    Authority.EXTERNAL_EFFECT;
            default -> Authority.LOCAL_ADMISSION_REQUIRED;
        };
    }
}
