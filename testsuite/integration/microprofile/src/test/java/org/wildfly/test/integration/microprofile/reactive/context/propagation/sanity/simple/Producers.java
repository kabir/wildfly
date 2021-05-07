/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.reactive.context.propagation.sanity.simple;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class Producers {

    @Produces
    @ApplicationScoped
    @TestQualifier
    public ManagedExecutor produceExecutor() {
        //return new TestExecutor(ManagedExecutor.builder().build());
        return ManagedExecutor.builder().build();
    }

    @Produces
    @ApplicationScoped
    @TestQualifier
    public ThreadContext produceContext() {
        //return new TestContext(ThreadContext.builder().build());
        return ThreadContext.builder().build();
    }

    void disposeExecutor(@Disposes @TestQualifier ManagedExecutor executor) {
        executor.shutdownNow();
    }

    static class TestExecutor implements ManagedExecutor {
        private final ManagedExecutor delegate;

        public TestExecutor(ManagedExecutor delegate) {
            this.delegate = delegate;
        }

        public static Builder builder() {
            return ManagedExecutor.builder();
        }

        @Override
        public <U> CompletableFuture<U> completedFuture(U value) {
            return delegate.completedFuture(value);
        }

        @Override
        public <U> CompletionStage<U> completedStage(U value) {
            return delegate.completedStage(value);
        }

        @Override
        public <U> CompletableFuture<U> failedFuture(Throwable ex) {
            return delegate.failedFuture(ex);
        }

        @Override
        public <U> CompletionStage<U> failedStage(Throwable ex) {
            return delegate.failedStage(ex);
        }

        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return delegate.newIncompleteFuture();
        }

        @Override
        public CompletableFuture<Void> runAsync(Runnable runnable) {
            return delegate.runAsync(runnable);
        }

        @Override
        public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
            return delegate.supplyAsync(supplier);
        }

        @Override
        public <T> CompletableFuture<T> copy(CompletableFuture<T> stage) {
            return delegate.copy(stage);
        }

        @Override
        public <T> CompletionStage<T> copy(CompletionStage<T> stage) {
            return delegate.copy(stage);
        }

        @Override
        public ThreadContext getThreadContext() {
            return delegate.getThreadContext();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }
    }

    static class TestContext implements ThreadContext {
        private final ThreadContext delegate;

        public TestContext(ThreadContext delegate) {
            this.delegate = delegate;
        }

        public static Builder builder() {
            return ThreadContext.builder();
        }

        @Override
        public Executor currentContextExecutor() {
            return delegate.currentContextExecutor();
        }

        @Override
        public <R> Callable<R> contextualCallable(Callable<R> callable) {
            return delegate.contextualCallable(callable);
        }

        @Override
        public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
            return delegate.contextualConsumer(consumer);
        }

        @Override
        public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
            return delegate.contextualConsumer(consumer);
        }

        @Override
        public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
            return delegate.contextualFunction(function);
        }

        @Override
        public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
            return delegate.contextualFunction(function);
        }

        @Override
        public Runnable contextualRunnable(Runnable runnable) {
            return delegate.contextualRunnable(runnable);
        }

        @Override
        public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
            return delegate.contextualSupplier(supplier);
        }

        @Override
        public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
            return delegate.withContextCapture(stage);
        }

        @Override
        public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
            return delegate.withContextCapture(stage);
        }
    }
}
