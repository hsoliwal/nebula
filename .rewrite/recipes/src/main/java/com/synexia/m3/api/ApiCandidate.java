package com.synexia.m3.api;

/** A deterministic candidate with auditable fuzzy score components. */
public record ApiCandidate(ApiDescriptor api, double score, int editDistance, boolean exact, boolean prefix) {
    public ApiCandidate {
        if (score < 0.0 || score > 1.0 || editDistance < 0) {
            throw new IllegalArgumentException("invalid candidate score");
        }
    }
}
