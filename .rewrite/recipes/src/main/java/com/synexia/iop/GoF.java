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
 * GoF - Gang of Four Design Patterns (23 patterns).
 *
 * <p>
 * Creational, Structural, and Behavioral patterns from the classic "Design
 * Patterns: Elements of Reusable Object-Oriented Software" book.
 */
public enum GoF {

    // --- CREATIONAL PATTERNS (5) ---
    /** Create objects without specifying exact class. */
    ABSTRACT_FACTORY("Creational", "Provide interface for creating families of related objects"),
    /** Separate construction from representation. */
    BUILDER("Creational", "Separate complex object construction from representation"),
    /** Create objects by cloning. */
    FACTORY_METHOD("Creational", "Define interface for creating objects, let subclasses decide"),
    /** Clone existing objects. */
    PROTOTYPE("Creational", "Create objects by copying existing prototype"),
    /** Ensure single instance. */
    SINGLETON("Creational", "Ensure class has only one instance"),

    // --- STRUCTURAL PATTERNS (7) ---
    /** Convert interface to another. */
    ADAPTER("Structural", "Convert interface to one client expects"),
    /** Separate abstraction from implementation. */
    BRIDGE("Structural", "Decouple abstraction from implementation"),
    /** Treat individual and composite uniformly. */
    COMPOSITE("Structural", "Compose objects into tree structures"),
    /** Add responsibilities dynamically. */
    DECORATOR("Structural", "Attach additional responsibilities dynamically"),
    /** Simplified interface to complex subsystem. */
    FACADE("Structural", "Provide unified interface to subsystem"),
    /** Share fine-grained objects efficiently. */
    FLYWEIGHT("Structural", "Share fine-grained objects efficiently"),
    /** Placeholder for another object. */
    PROXY("Structural", "Provide surrogate for another object"),

    // --- BEHAVIORAL PATTERNS (11) ---
    /** Pass request along chain. */
    CHAIN_OF_RESPONSIBILITY("Behavioral", "Pass request along chain of handlers"),
    /** Encapsulate request as object. */
    COMMAND("Behavioral", "Encapsulate request as object"),
    /** Define language grammar. */
    INTERPRETER("Behavioral", "Define grammar representation for language"),
    /** Access elements sequentially. */
    ITERATOR("Behavioral", "Access elements sequentially without exposing representation"),
    /** Define object interaction. */
    MEDIATOR("Behavioral", "Define object that encapsulates interaction"),
    /** Capture and restore state. */
    MEMENTO("Behavioral", "Capture and externalize object's internal state"),
    /** Define one-to-many dependency. */
    OBSERVER("Behavioral", "Define one-to-many dependency for state changes"),
    /** Alter behavior when state changes. */
    STATE("Behavioral", "Allow object to alter behavior when state changes"),
    /** Encapsulate algorithm family. */
    STRATEGY("Behavioral", "Encapsulate family of algorithms"),
    /** Define algorithm skeleton. */
    TEMPLATE_METHOD("Behavioral", "Define algorithm skeleton, let subclasses override steps"),
    /** Separate algorithm from objects. */
    VISITOR("Behavioral", "Define new operation without changing classes");

    private final String category;
    private final String description;

    GoF(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** Pattern category (Creational, Structural, Behavioral). */
    public String category() {
        return category;
    }

    /** Pattern description. */
    public String description() {
        return description;
    }
}
