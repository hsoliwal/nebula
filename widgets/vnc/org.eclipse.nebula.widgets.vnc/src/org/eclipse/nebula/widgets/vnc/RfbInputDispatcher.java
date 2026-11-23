/*
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Serial, bounded remote input sender. SWT callbacks enqueue without writing to
 * potentially blocking transport streams. Queue overflow is a hard failure.
 */
final class RfbInputDispatcher implements AutoCloseable {
    private static final int CAPACITY = 512;
    private final RfbClient client;
    private final BlockingQueue<Command> queue = new ArrayBlockingQueue<>(CAPACITY);
    private final Consumer<IOException> onFailure;
    private final Thread worker;
    private volatile boolean closed;

    RfbInputDispatcher(RfbClient client, Consumer<IOException> onFailure) {
        this.client = Objects.requireNonNull(client, "client");
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
        worker = Thread.ofVirtual().name("nebula-vnc-input-sender").start(this::run);
    }

    boolean offerPointer(int x, int y, int buttons) {
        return offer(new Pointer(x, y, buttons));
    }

    boolean offerKey(boolean pressed, int keysym) {
        return offer(new Key(pressed, keysym));
    }

    private boolean offer(Command command) {
        return !closed && queue.offer(command);
    }

    private void run() {
        try {
            while (!closed) {
                Command command = queue.take();
                if (!closed) {
                    command.send(client);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException failure) {
            closed = true;
            queue.clear();
            onFailure.accept(failure);
        }
    }

    @Override
    public void close() {
        closed = true;
        queue.clear();
        worker.interrupt();
    }

    private sealed interface Command permits Pointer, Key {
        void send(RfbClient client) throws IOException;
    }

    private record Pointer(int x, int y, int buttons) implements Command {
        @Override
        public void send(RfbClient client) throws IOException {
            client.sendPointer(x, y, buttons);
        }
    }

    private record Key(boolean pressed, int keysym) implements Command {
        @Override
        public void send(RfbClient client) throws IOException {
            client.sendKey(pressed, keysym);
        }
    }
}
