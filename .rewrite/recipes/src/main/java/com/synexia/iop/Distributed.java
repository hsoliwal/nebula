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
 * Distributed System Patterns.
 *
 * <p>
 * Patterns for building distributed systems including communication, data
 * management, concurrency, failure handling, and security.
 *
 * @see <a href=
 *      "https://www.geeksforgeeks.org/system-design/distributed-system-patterns/">Distributed
 *      System Patterns</a>
 */
public enum Distributed {

    // --- COMMUNICATION ---
    /** Client-server communication. */
    CLIENT_SERVER("Communication", "Clients send requests to servers for processing"),
    /** Publish-subscribe messaging. */
    PUB_SUB("Communication", "Publishers send to brokers, subscribers receive by interest"),
    /** Master-slave delegation. */
    MASTER_SLAVE("Communication", "Master delegates tasks to slave nodes"),
    /** Peer-to-peer networking. */
    PEER_TO_PEER("Communication", "Nodes act as both clients and servers"),
    /** Leader-follower coordination. */
    LEADER_FOLLOWER("Communication", "Leader coordinates, followers replicate"),

    // --- DATA MANAGEMENT ---
    /** Data replication. */
    REPLICATION("Data", "Maintains multiple data copies across nodes"),
    /** Data partitioning/sharding. */
    SHARDING("Data", "Divides datasets into subsets across nodes"),
    /** Strong consistency. */
    STRONG_CONSISTENCY("Data", "All nodes see same data simultaneously"),
    /** Eventual consistency. */
    EVENTUAL_CONSISTENCY("Data", "Nodes converge to same state over time"),
    /** Causal consistency. */
    CAUSAL_CONSISTENCY("Data", "Causally related operations seen in order"),
    /** Distributed caching. */
    DISTRIBUTED_CACHE("Data", "Stores data in fast-access memory across nodes"),

    // --- CONCURRENCY & COORDINATION ---
    /** Distributed locking. */
    DISTRIBUTED_LOCK("Concurrency", "Prevents concurrent access to shared resources"),
    /** Semaphore. */
    SEMAPHORE("Concurrency", "Controls finite resource access via counters"),
    /** Leader election. */
    LEADER_ELECTION("Concurrency", "Selects single coordinator among group nodes"),
    /** Two-phase commit (2PC). */
    TWO_PHASE_COMMIT("Concurrency", "Coordinates distributed transactions via 2PC"),
    /** Three-phase commit (3PC). */
    THREE_PHASE_COMMIT("Concurrency", "Non-blocking distributed commit protocol"),
    /** Consensus (Paxos/Raft). */
    CONSENSUS("Concurrency", "Agreement among distributed nodes (Paxos, Raft)"),

    // --- FAILURE HANDLING ---
    /** Retry with backoff. */
    RETRY_BACKOFF("Failure", "Retries failed operations with exponential backoff"),
    /** Failover. */
    FAILOVER("Failure", "Switches to backup resources when primaries fail"),
    /** Graceful degradation. */
    GRACEFUL_DEGRADATION("Failure", "Maintains reduced functionality during failures"),
    /** Health checking. */
    HEALTH_CHECK("Failure", "Monitors service health status"),

    // --- DEPLOYMENT ---
    /** Rolling deployment. */
    ROLLING_DEPLOYMENT("Deployment", "Deploys gradually across servers"),
    /** Shadow deployment. */
    SHADOW_DEPLOYMENT("Deployment", "Tests new version alongside existing"),
    /** A/B testing deployment. */
    AB_TESTING("Deployment", "Routes subset of traffic to new version"),

    // --- SECURITY ---
    /** Authentication. */
    AUTHENTICATION("Security", "Verifies user identity (passwords, tokens, MFA)"),
    /** Authorization (RBAC/ABAC). */
    AUTHORIZATION("Security", "Controls resource access via RBAC/ABAC"),
    /** Encryption in transit. */
    ENCRYPTION_TRANSIT("Security", "Protects data during transmission"),
    /** Encryption at rest. */
    ENCRYPTION_REST("Security", "Protects stored data"),
    /** Access control lists. */
    ACL("Security", "Defines resource-level permissions"),
    /** Audit logging. */
    AUDIT_LOG("Security", "Tracks security events and activities"),
    /** Tokenization. */
    TOKENIZATION("Security", "Replaces sensitive data with non-sensitive tokens"),

    // --- MESSAGING ---
    /** Message queue. */
    MESSAGE_QUEUE("Messaging", "Asynchronous message passing via queues"),
    /** Event streaming. */
    EVENT_STREAMING("Messaging", "Continuous event flow processing (Kafka)"),
    /** Request-response. */
    REQUEST_RESPONSE("Messaging", "Synchronous request and reply pattern");

    private final String category;
    private final String description;

    Distributed(String category, String description) {
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
