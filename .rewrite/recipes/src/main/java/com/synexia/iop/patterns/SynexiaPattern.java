package com.synexia.iop.patterns;

import com.synexia.iop.patterns.PatternDescriptor.Authority;

/** Synexia-native paradigm and mechanical patterns with their owning modules. */
public enum SynexiaPattern {
    IOP("Contract", "Interface-oriented stable semantic boundaries", "synexia-iop", Authority.PURE_LOCAL),
    ANOP("Contract", "Annotation-oriented executable semantic metadata", "synexia-anop", Authority.PURE_LOCAL),
    AOP("CrossCutting", "Aspect-oriented cross-cutting behavior", "synexia-aop", Authority.LOCAL_ADMISSION_REQUIRED),
    BOP("Behavior", "Behavior-oriented reusable operations", "synexia-bop", Authority.PURE_LOCAL),
    COP("Context", "Context-oriented local state and layered behavior", "synexia-cop", Authority.LOCAL_ADMISSION_REQUIRED),
    FOP("Behavior", "Functional composition of behavioral leaves", "synexia-fop", Authority.PURE_LOCAL),
    TOP("Verification", "Test-oriented executable proof surfaces", "synexia-top", Authority.PURE_LOCAL),
    TMOP("Generation", "Template-oriented deterministic materialization", "synexia-tmop", Authority.CANDIDATE_ONLY),
    HPCPOP("Performance", "Hot-path/cold-path oriented execution separation", "synexia-hpcpop", Authority.CONTROL_ONLY),
    CMOP("Composition", "Component/command composition with ports and lazy calls", "synexia-cmop", Authority.CANDIDATE_ONLY),
    AIOP("Metadata", "AI-oriented compile-time intent annotations", "synexia-aiop", Authority.CANDIDATE_ONLY),
    LLDCIM("Refinement", "Lexical-logical decompress/solve/compress iterative methodology", "synexia-flldcim", Authority.CONTROL_ONLY),
    FLLDCIM("Refinement", "Fractal recursive LLDCIM across locality scales", "synexia-flldcim", Authority.CONTROL_ONLY),
    LSEM("Analysis", "Lexical skeleton extraction methodology", "synexia-specgrep", Authority.CONTROL_ONLY),
    MRL("Refactoring", "Mechanical refactoring law with behavior/contract preservation", "synexia-specgrep", Authority.CANDIDATE_ONLY),
    RMM("Memory", "Reactive memory recall driven by current signals", "synexia-mllm", Authority.CANDIDATE_ONLY),
    FMM("Memory", "Fluid multi-layer memory reconstruction", "synexia-mllm", Authority.CANDIDATE_ONLY),
    SELF_SIMILAR_LOCALITY(
            "Authority",
            "Local state, bounded projection, candidate-only downstream influence and target-local admission",
            "synexia-mllm",
            Authority.LOCAL_ADMISSION_REQUIRED),
    RSE_LOCALITY(
            "Remote",
            "Remote host/subsystem/resource/service/operation locality hierarchy",
            "synexia-mllm",
            Authority.LOCAL_ADMISSION_REQUIRED),
    CPU_FIRST_ROUTING(
            "Execution",
            "CPU mechanical path before local model before cloud model",
            "synexia-mllm",
            Authority.CONTROL_ONLY);

    private final String category;
    private final String description;
    private final String targetModule;
    private final Authority authority;

    SynexiaPattern(String category, String description, String targetModule, Authority authority) {
        this.category = category;
        this.description = description;
        this.targetModule = targetModule;
        this.authority = authority;
    }

    public String category() {
        return category;
    }

    public String description() {
        return description;
    }

    public String targetModule() {
        return targetModule;
    }

    public Authority authority() {
        return authority;
    }
}
