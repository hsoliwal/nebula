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
 * EIP - Enterprise Integration Patterns (Apache Camel Naming).
 *
 * <p>
 * Patterns from Gregor Hohpe and Bobby Woolf's "Enterprise Integration
 * Patterns" book, using Apache Camel naming conventions.
 *
 * @see <a href=
 *      "https://camel.apache.org/components/latest/eips/enterprise-integration-patterns.html">Apache
 *      Camel EIP</a>
 */
public enum EIP {
    // --- MESSAGE ROUTING (camel.apache.org/components/latest/eips/) ---

    /** aggregate - Aggregates many messages into a single message. */
    aggregate("Routing", "Aggregates many messages into a single message"),

    /** bean - Invokes a method on a bean. */
    bean("Endpoint", "Invokes a method on a bean"),

    /** choice - Routes messages based on content (when/otherwise). */
    choice("Routing", "Routes messages based on content using when/otherwise"),

    /** circuitBreaker - Circuit Breaker pattern for fault tolerance. */
    circuitBreaker("Resilience", "Circuit Breaker pattern for fault tolerance"),

    /** claimCheck - Store data temporarily and retrieve later. */
    claimCheck("Transformation", "Store large data, pass reference, retrieve later"),

    /** contentBasedRouter - Route based on message content. */
    contentBasedRouter("Routing", "Route based on message content"),

    /** convertBodyTo - Converts message body to another type. */
    convertBodyTo("Transformation", "Converts the message body to another type"),

    /** delay - Delays message processing. */
    delay("Routing", "Delays the processing of a message"),

    /** dynamicRouter - Route to dynamically determined destination. */
    dynamicRouter("Routing", "Route to dynamically determined destinations"),

    /** enrich - Enriches a message with additional data. */
    enrich("Transformation", "Enriches a message with additional data from a resource"),

    /** filter - Filters messages based on a predicate. */
    filter("Routing", "Filters messages based on a predicate"),

    /** from - Start of a route (consumer endpoint). */
    from("Endpoint", "Start of a route - message consumer"),

    /** idempotentConsumer - Filters duplicate messages. */
    idempotentConsumer("Routing", "Filters out duplicate messages"),

    /** loadBalance - Distributes load across endpoints. */
    loadBalance("Routing", "Load balances messages across multiple endpoints"),

    /** log - Logs messages to a logger. */
    log("Management", "Logs messages to a logger"),

    /** loop - Loops through processing N times. */
    loop("Routing", "Processes messages N times in a loop"),

    /** marshal - Marshals data to a specific format. */
    marshal("Transformation", "Marshals the message body to a specific data format"),

    /** multicast - Multicasts to multiple endpoints. */
    multicast("Routing", "Sends message to multiple endpoints simultaneously"),

    /** onCompletion - Callback on route completion. */
    onCompletion("Management", "Callback executed on route completion"),

    /** onException - Exception handler. */
    onException("ErrorHandling", "Defines exception handling policy"),

    /** otherwise - Default branch in choice. */
    otherwise("Routing", "Default branch in choice routing"),

    /** pipeline - Chains processors in sequence. */
    pipeline("Routing", "Chains processors in sequence (default)"),

    /** pollEnrich - Poll a resource to enrich message. */
    pollEnrich("Transformation", "Polls a resource to enrich the message"),

    /** process - Invokes a Processor. */
    process("Endpoint", "Invokes a custom Processor"),

    /** recipientList - Route to a list of recipients. */
    recipientList("Routing", "Routes to a list of dynamically determined recipients"),

    /** removeHeader - Removes a header. */
    removeHeader("Transformation", "Removes a header from the message"),

    /** removeHeaders - Removes headers by pattern. */
    removeHeaders("Transformation", "Removes headers matching a pattern"),

    /** removeProperty - Removes an exchange property. */
    removeProperty("Transformation", "Removes an exchange property"),

    /** resequence - Reorders messages by sequence. */
    resequence("Routing", "Reorders messages based on an expression"),

    /** routingSlip - Route through a dynamic sequence. */
    routingSlip("Routing", "Routes message through a dynamic sequence of endpoints"),

    /** saga - Saga pattern for long-running transactions. */
    saga("Orchestration", "Saga pattern for long-running distributed transactions"),

    /** sample - Sample messages at intervals. */
    sample("Routing", "Samples messages at specified intervals"),

    /** script - Executes a script. */
    script("Transformation", "Executes a script language"),

    /** serviceCall - Calls a remote service. */
    serviceCall("Endpoint", "Calls a remote service with service discovery"),

    /** setBody - Sets the message body. */
    setBody("Transformation", "Sets the message body"),

    /** setHeader - Sets a header. */
    setHeader("Transformation", "Sets a message header"),

    /** setProperty - Sets an exchange property. */
    setProperty("Transformation", "Sets an exchange property"),

    /** sort - Sorts messages. */
    sort("Transformation", "Sorts messages based on an expression"),

    /** split - Splits a message into parts. */
    split("Routing", "Splits a message into multiple parts"),

    /** step - Step for saga transaction. */
    step("Orchestration", "Defines a step in a saga"),

    /** stop - Stops routing. */
    stop("Routing", "Stops routing the current message"),

    /** threads - Uses thread pool for async processing. */
    threads("Concurrency", "Processes messages using a thread pool"),

    /** throttle - Throttles message rate. */
    throttle("Resilience", "Throttles the rate of messages"),

    /** throwException - Throws an exception. */
    throwException("ErrorHandling", "Throws an exception"),

    /** to - Send to an endpoint. */
    to("Endpoint", "Sends message to an endpoint"),

    /** toD - Send to a dynamic endpoint. */
    toD("Endpoint", "Sends message to a dynamically computed endpoint"),

    /** transform - Transforms the message. */
    transform("Transformation", "Transforms the message body"),

    /** unmarshal - Unmarshals data from a format. */
    unmarshal("Transformation", "Unmarshals message body from a data format"),

    /** validate - Validates message content. */
    validate("Transformation", "Validates message content"),

    /** when - Conditional branch in choice. */
    when("Routing", "Conditional branch in choice routing"),

    /** wireTap - Taps a copy of the message. */
    wireTap("Management", "Sends a copy of the message to another endpoint"),

    // --- CLASSIC EIP PATTERNS (Book Terminology) ---

    /** messageChannel - Point-to-point or pub-sub channel. */
    messageChannel("Channel", "Connects sender and receiver via channel"),

    /** messageBus - Backbone connecting applications. */
    messageBus("Channel", "Backbone that connects all applications"),

    /** deadLetterChannel - Handle undeliverable messages. */
    deadLetterChannel("ErrorHandling", "Handles messages that cannot be delivered"),

    /** guaranteedDelivery - Ensure message is delivered. */
    guaranteedDelivery("Channel", "Ensures message is delivered despite failures"),

    /** messageTranslator - Translates message format. */
    messageTranslator("Transformation", "Translates message from one format to another"),

    /** messageEndpoint - Application connection point. */
    messageEndpoint("Endpoint", "Application's connection to the messaging system"),

    /** pollingConsumer - Polls for messages. */
    pollingConsumer("Endpoint", "Consumer that polls for messages"),

    /** eventDrivenConsumer - Pushed messages. */
    eventDrivenConsumer("Endpoint", "Consumer that receives pushed messages"),

    /** competingConsumers - Concurrent consumers. */
    competingConsumers("Endpoint", "Multiple consumers compete for messages"),

    /** messageDispatcher - Coordinates consumers. */
    messageDispatcher("Endpoint", "Coordinates multiple consumers"),

    /** selectiveConsumer - Selects messages to receive. */
    selectiveConsumer("Endpoint", "Selects which messages to receive"),

    /** durableSubscriber - Persists subscription. */
    durableSubscriber("Endpoint", "Subscription that persists across restarts"),

    /** messagingGateway - Encapsulates messaging access. */
    messagingGateway("Endpoint", "Encapsulates access to the messaging system"),

    /** messagingMapper - Maps domain to messages. */
    messagingMapper("Transformation", "Maps domain objects to/from messages"),

    /** transactionalClient - Transaction control. */
    transactionalClient("Endpoint", "Controls transactions with messaging"),

    /** serviceActivator - Connects service to messaging. */
    serviceActivator("Endpoint", "Connects application service to messaging system"),

    /** scatterGather - Broadcast and aggregate. */
    scatterGather("Routing", "Broadcasts to recipients and aggregates responses"),

    /** composedMessageProcessor - Process composed message. */
    composedMessageProcessor("Routing", "Processes a composed message"),

    /** processManager - Maintains routing state. */
    processManager("Routing", "Maintains state during multi-step routing"),

    /** normalizer - Converts to common format. */
    normalizer("Transformation", "Converts different formats to a common format"),

    /** canonicalDataModel - Independent data model. */
    canonicalDataModel("Transformation", "Application-independent data model"),

    /** contentEnricher - Adds data to message. */
    contentEnricher("Transformation", "Adds missing data to a message"),

    /** contentFilter - Removes data from message. */
    contentFilter("Transformation", "Removes unwanted data from a message"),

    /** envelopeWrapper - Wraps message in envelope. */
    envelopeWrapper("Transformation", "Wraps message data in an envelope"),

    /** controlBus - Administers messaging system. */
    controlBus("Management", "Administers the messaging system"),

    /** detour - Routes through extra steps. */
    detour("Management", "Routes messages through additional steps for testing"),

    /** messageHistory - Records processing path. */
    messageHistory("Management", "Records the path message has traveled"),

    /** messageStore - Stores for audit/replay. */
    messageStore("Management", "Stores messages for audit or replay"),

    /** smartProxy - Tracks request/response. */
    smartProxy("Management", "Tracks requests and matches with responses"),

    /** testMessage - Tests system health. */
    testMessage("Management", "Verifies messaging system is working correctly"),

    /** channelPurger - Removes pending messages. */
    channelPurger("Management", "Removes pending messages from a channel");

    private final String category;
    private final String description;

    EIP(String category, String description) {
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
