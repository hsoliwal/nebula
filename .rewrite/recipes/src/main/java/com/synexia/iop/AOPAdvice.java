/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synexia.iop;

/**
 * AOP Advice Types - Cross-cutting concern advice.
 *
 * <p>
 * Defines when and how aspects are applied in Aspect-Oriented Programming.
 * These advice types are used in Spring AOP and AspectJ.
 *
 * @see <a href=
 *      "https://www.geeksforgeeks.org/advance-java/aspect-oriented-programming-aop-in-spring-framework/">AOP
 *      in Spring</a>
 */
public enum AOPAdvice {

    // --- ADVICE TYPES ---
    /** Before - executes before method invocation. */
    BEFORE("Advice", "Executes before the target method"),
    /** After - executes after method, regardless of outcome. */
    AFTER("Advice", "Executes after method completion (finally)"),
    /** After Returning - executes after successful return. */
    AFTER_RETURNING("Advice", "Executes after successful method return"),
    /** After Throwing - executes when method throws exception. */
    AFTER_THROWING("Advice", "Executes when method throws exception"),
    /** Around - surrounds method, controls execution. */
    AROUND("Advice", "Wraps method, controls if/when it executes"),

    // --- POINTCUT DESIGNATORS ---
    /** Execution - matches method execution join points. */
    EXECUTION("Pointcut", "Matches method execution join points"),
    /** Within - matches join points within types. */
    WITHIN("Pointcut", "Matches join points within specified types"),
    /** This - matches where bean reference is instance of type. */
    THIS("Pointcut", "Matches where AOP proxy is instance of type"),
    /** Target - matches where target object is instance of type. */
    TARGET("Pointcut", "Matches where target is instance of type"),
    /** Args - matches where arguments are instances of types. */
    ARGS("Pointcut", "Matches based on method argument types"),
    /** Annotation - matches where subject has annotation. */
    ANNOTATION("Pointcut", "Matches methods/classes with annotation"),
    /** Bean - matches by bean name (Spring-specific). */
    BEAN("Pointcut", "Matches by Spring bean name"),

    // --- WEAVING TYPES ---
    /** Compile-time weaving - AspectJ compiler. */
    COMPILE_TIME_WEAVING("Weaving", "Aspects woven at compile time (AspectJ)"),
    /** Load-time weaving - class loader weaving. */
    LOAD_TIME_WEAVING("Weaving", "Aspects woven when classes are loaded"),
    /** Runtime weaving - proxy-based (Spring AOP). */
    RUNTIME_WEAVING("Weaving", "Aspects woven via proxies at runtime"),

    // --- PROXY TYPES ---
    /** JDK Dynamic Proxy - interface-based proxies. */
    JDK_PROXY("Proxy", "Interface-based dynamic proxy"),
    /** CGLIB Proxy - class-based proxies. */
    CGLIB_PROXY("Proxy", "Subclass-based proxy for concrete classes"),

    // --- ASPECT TYPES ---
    /** Singleton Aspect - one instance shared. */
    SINGLETON_ASPECT("Aspect", "Single aspect instance shared across targets"),
    /** Per-This Aspect - one instance per proxy. */
    PER_THIS("Aspect", "One aspect instance per AOP proxy"),
    /** Per-Target Aspect - one instance per target. */
    PER_TARGET("Aspect", "One aspect instance per target object"),
    /** Per-Type-Within Aspect - one instance per type. */
    PER_TYPE_WITHIN("Aspect", "One aspect instance per matched type");

    private final String category;
    private final String description;

    AOPAdvice(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** Advice category. */
    public String category() {
        return category;
    }

    /** Advice description. */
    public String description() {
        return description;
    }
}
