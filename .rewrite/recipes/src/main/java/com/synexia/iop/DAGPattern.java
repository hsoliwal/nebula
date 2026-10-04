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
 * DAG Patterns - Flow Orchestration and Pipeline Patterns.
 *
 * <p>
 * Patterns for orchestrating work as directed acyclic graphs, pipelines, and
 * complex workflows.
 */
public enum DAGPattern {

    // --- BASIC FLOW ---
    /** Sequential pipeline. */
    PIPELINE("Basic", "Execute steps in sequence"),
    /** Parallel execution. */
    PARALLEL("Basic", "Execute steps concurrently"),
    /** Fork-join. */
    FORK_JOIN("Basic", "Fork to parallel, join results"),
    /** Branch. */
    BRANCH("Basic", "Conditional execution path"),
    /** Loop. */
    LOOP("Basic", "Repeat until condition"),
    /** Merge. */
    MERGE("Basic", "Combine multiple paths"),

    // --- ORCHESTRATION ---
    /** Saga pattern. */
    SAGA("Orchestration", "Distributed transaction with compensation"),
    /** Choreography. */
    CHOREOGRAPHY("Orchestration", "Decentralized coordination via events"),
    /** Orchestration. */
    ORCHESTRATION("Orchestration", "Centralized coordinator controls flow"),
    /** ReAct loop. */
    REACT_LOOP("Orchestration", "Reason-Act-Observe loop for agents"),
    /** Plan-Execute. */
    PLAN_EXECUTE("Orchestration", "Plan steps then execute"),

    // --- DATA FLOW ---
    /** Map-reduce. */
    MAP_REDUCE("DataFlow", "Map function then reduce results"),
    /** Scatter-gather. */
    SCATTER_GATHER("DataFlow", "Scatter to workers, gather results"),
    /** Splitter. */
    SPLITTER("DataFlow", "Split input into multiple outputs"),
    /** Aggregator. */
    AGGREGATOR("DataFlow", "Aggregate multiple inputs into one"),
    /** Router. */
    ROUTER("DataFlow", "Route to destination based on content"),
    /** Filter. */
    FILTER("DataFlow", "Filter items based on predicate"),
    /** Transform. */
    TRANSFORM("DataFlow", "Transform item to different form"),

    // --- ERROR HANDLING ---
    /** Compensation. */
    COMPENSATION("ErrorHandling", "Undo previous steps on failure"),
    /** Retry step. */
    RETRY_STEP("ErrorHandling", "Retry failed step"),
    /** Skip step. */
    SKIP_STEP("ErrorHandling", "Skip failed step and continue"),
    /** Error channel. */
    ERROR_CHANNEL("ErrorHandling", "Route errors to separate handler"),
    /** Dead letter. */
    DEAD_LETTER("ErrorHandling", "Store unprocessable items"),

    // --- STATE MANAGEMENT ---
    /** Checkpoint. */
    CHECKPOINT("State", "Save progress for recovery"),
    /** Resume. */
    RESUME("State", "Resume from checkpoint"),
    /** State machine. */
    STATE_MACHINE("State", "Manage states and transitions"),
    /** Event-driven. */
    EVENT_DRIVEN("State", "React to events"),

    // --- SCHEDULING ---
    /** Batch. */
    BATCH("Scheduling", "Process items in batches"),
    /** Windowing. */
    WINDOWING("Scheduling", "Process items in time/count windows"),
    /** Rate limiting. */
    RATE_LIMITING("Scheduling", "Control processing rate"),
    /** Priority queue. */
    PRIORITY_QUEUE("Scheduling", "Process by priority"),
    /** Delay. */
    DELAY("Scheduling", "Delay step execution"),
    /** Timeout. */
    DAG_TIMEOUT("Scheduling", "Timeout step if too slow"),

    // --- AGENT PATTERNS ---
    /** Tool use. */
    TOOL_USE("Agent", "Agent uses tools to accomplish task"),
    /** Multi-agent. */
    MULTI_AGENT("Agent", "Multiple agents collaborate"),
    /** Supervisor. */
    SUPERVISOR("Agent", "Supervisor coordinates worker agents"),
    /** Delegation. */
    DELEGATION("Agent", "Delegate subtask to another agent"),
    /** Reflection. */
    REFLECTION("Agent", "Agent reflects on own output"),
    /** Memory retrieval. */
    MEMORY_RETRIEVAL("Agent", "Retrieve from long-term memory");

    private final String category;
    private final String description;

    DAGPattern(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** Pattern category. */
    public String category() {
        return category;
    }

    /** Pattern description. */
    public String description() {
        return description;
    }
}
