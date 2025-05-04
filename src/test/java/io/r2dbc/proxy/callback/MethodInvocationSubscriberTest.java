/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link MethodInvocationSubscriber}.
 *
 * @author Tadaya Tsuyukubo
 */
class MethodInvocationSubscriberTest {

    private AtomicReference<Throwable> droppedError = new AtomicReference<>();

    @BeforeEach
    void registerHooks() {
        Hooks.onErrorDropped(ex -> {
            droppedError.set(ex);
        });
    }

    @AfterEach
    void clearHooks() {
        Hooks.resetOnErrorDropped();
    }

    @Test
    void reactorContextPopulatedInValueStore() {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(testListener).build();
        MutableMethodExecutionInfo executionInfo = new MutableMethodExecutionInfo();

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer =
            Operators.liftPublisher((pub, subscriber) -> new MethodInvocationSubscriber(subscriber, executionInfo, proxyConfig, null));

        Mono<Object> mono = Mono.just(new Object())
            .transform(transformer)
            .contextWrite(context -> context.put("foo", "FOO"));

        StepVerifier.create(mono)
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete();

        MethodExecutionInfo methodExecutionInfo = testListener.getBeforeMethodExecutionInfo();
        ContextView contextView = methodExecutionInfo.getValueStore().get(ContextView.class, ContextView.class);
        assertThat(contextView).isNotNull();
        assertThat(contextView.hasKey("foo")).isTrue();
        assertThat((String) contextView.get("foo")).isEqualTo("FOO");

        // afterQuery has called with executionInfo that contains reactor context
        assertThat(testListener.getAfterMethodExecutionInfo()).isSameAs(methodExecutionInfo);
    }

    @Test
    void beforeMethodError() {
        RuntimeException runtimeException = new RuntimeException("Boom");

        AtomicBoolean beforeMethodCalled = new AtomicBoolean();
        AtomicBoolean afterMethodCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                beforeMethodCalled.set(true);
                throw runtimeException;
            }

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                afterMethodCalled.set(true);
            }

        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        Mono<Object> mono = Mono.just("foo").cast(Object.class).transform(transformer);

        // error from beforeMethod should not affect the mono.
        StepVerifier.create(mono)
            .expectNext("foo")
            .verifyComplete();

        assertThat(beforeMethodCalled).isTrue();
        assertThat(droppedError.get()).isSameAs(runtimeException);
        assertThat(afterMethodCalled).isTrue();
    }

    @Test
    void afterMethodErrorInOnComplete() {
        RuntimeException runtimeException = new RuntimeException("Boom");

        AtomicBoolean afterMethodCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                afterMethodCalled.set(true);
                throw runtimeException;
            }

        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        Mono<Object> mono = Mono.just("foo").cast(Object.class).transform(transformer);

        // error from afterMethod should not affect the mono.
        StepVerifier.create(mono)
            .expectNext("foo")
            .verifyComplete();

        assertThat(droppedError.get()).isSameAs(runtimeException);
        assertThat(afterMethodCalled).isTrue();
    }

    @Test
    void afterMethodErrorInOnCancel() {
        AtomicReference<Throwable> droppedError = new AtomicReference<>();
        Hooks.onErrorDropped(ex -> {
            droppedError.set(ex);
        });

        RuntimeException runtimeException = new RuntimeException("Boom");

        AtomicBoolean afterMethodCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                afterMethodCalled.set(true);
                throw runtimeException;
            }

        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        Mono<Object> mono = Mono.just("foo").cast(Object.class).transform(transformer);

        // error from afterMethod should not affect the mono.
        StepVerifier.create(mono)
            .expectNext("foo")
            .thenCancel()
            .verify();

        assertThat(droppedError.get()).isSameAs(runtimeException);
        assertThat(afterMethodCalled).isTrue();
    }


    @Test
    void afterMethodErrorInError() {
        RuntimeException runtimeException = new RuntimeException("Boom");

        AtomicBoolean afterMethodCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                afterMethodCalled.set(true);
                throw runtimeException;
            }

        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        RuntimeException monoError = new RuntimeException("My Error");
        Mono<Object> mono = Mono.error(monoError).transform(transformer);

        // error from afterMethod should not affect the mono.
        StepVerifier.create(mono)
            .expectErrorSatisfies((ex) -> assertThat(ex).isSameAs(monoError))
            .verify();

        assertThat(droppedError.get()).isSameAs(runtimeException);
        assertThat(afterMethodCalled).isTrue();
    }

    private Function<? super Publisher<Object>, ? extends Publisher<Object>> createTransformer(ProxyExecutionListener listener) {
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();
        MutableMethodExecutionInfo executionInfo = new MutableMethodExecutionInfo();
        return Operators.liftPublisher((pub, delegate) ->
            new MethodInvocationSubscriber(delegate, executionInfo, proxyConfig, null));
    }

}
