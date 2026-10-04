package com.synexia.m3.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, donor-neutral API contract metadata. */
public record ApiDescriptor(
        String id,
        String name,
        String version,
        String source,
        String license,
        List<String> categories,
        Map<String, String> metadata) {

    public ApiDescriptor {
        id = require(id, "id");
        name = require(name, "name");
        version = require(version, "version");
        source = require(source, "source");
        license = require(license, "license");
        categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    private static String require(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
