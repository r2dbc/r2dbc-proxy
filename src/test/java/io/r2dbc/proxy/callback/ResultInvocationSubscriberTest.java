/*
 * Copyright 2025 the original author or authors.
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

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ResultInvocationSubscriber}.
 *
 * @author Tadaya Tsuyukubo
 */
class ResultInvocationSubscriberTest {

    AtomicReference<Throwable> droppedError = new AtomicReference<>();

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
    void eachQueryResultErrorInOnNext() {
        RuntimeException runtimeException = new RuntimeException("Boom");
        AtomicBoolean eachQueryResultCalled = new AtomicBoolean();
        AtomicBoolean afterQueryCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void eachQueryResult(QueryExecutionInfo execInfo) {
                eachQueryResultCalled.set(true);
                throw runtimeException;
            }

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                afterQueryCalled.set(true);
            }
        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);

        Mono<Object> mono = Mono.just("foo").cast(Object.class).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectNext("foo")
            .verifyComplete();

        assertThat(eachQueryResultCalled).isTrue();
        assertThat(this.droppedError.get()).isSameAs(runtimeException);
        assertThat(afterQueryCalled).isTrue();
    }

    @Test
    void eachQueryResultErrorInOnError() {
        RuntimeException runtimeException = new RuntimeException("Boom");
        AtomicBoolean eachQueryResultCalled = new AtomicBoolean();
        AtomicBoolean afterQueryCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void eachQueryResult(QueryExecutionInfo execInfo) {
                eachQueryResultCalled.set(true);
                throw runtimeException;
            }

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                afterQueryCalled.set(true);
            }
        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        RuntimeException monoError = new RuntimeException("My Error");
        Mono<Object> mono = Mono.error(monoError).cast(Object.class).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectErrorSatisfies((ex) -> assertThat(ex).isSameAs(monoError))
            .verify();

        assertThat(eachQueryResultCalled).isTrue();
        assertThat(this.droppedError.get()).isSameAs(runtimeException);
        assertThat(afterQueryCalled).isTrue();
    }


    @Test
    void afterQueryErrorInOnComplete() {
        RuntimeException runtimeException = new RuntimeException("Boom");
        AtomicBoolean afterQueryCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                afterQueryCalled.set(true);
                throw runtimeException;
            }
        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        Mono<Object> mono = Mono.just("foo").cast(Object.class).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectNext("foo")
            .verifyComplete();

        assertThat(this.droppedError.get()).isSameAs(runtimeException);
        assertThat(afterQueryCalled).isTrue();
    }

    @Test
    void afterQueryErrorInOnCancel() {
        RuntimeException runtimeException = new RuntimeException("Boom");
        AtomicBoolean afterQueryCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                afterQueryCalled.set(true);
                throw runtimeException;
            }
        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        Mono<Object> mono = Mono.just("foo").cast(Object.class).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectNext("foo")
            .thenCancel()
            .verify();

        assertThat(this.droppedError.get()).isSameAs(runtimeException);
        assertThat(afterQueryCalled).isTrue();
    }

    @Test
    void afterQueryErrorInOnError() {
        RuntimeException runtimeException = new RuntimeException("Boom");

        AtomicBoolean afterQueryCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                afterQueryCalled.set(true);
                throw runtimeException;
            }
        };

        Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer = createTransformer(listener);
        RuntimeException monoError = new RuntimeException("My Error");
        Mono<Object> mono = Mono.error(monoError).cast(Object.class).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectErrorSatisfies((ex) -> assertThat(ex).isSameAs(monoError))
            .verify();

        assertThat(this.droppedError.get()).isSameAs(runtimeException);
        assertThat(afterQueryCalled).isTrue();
    }

    private Function<? super Publisher<Object>, ? extends Publisher<Object>> createTransformer(ProxyExecutionListener listener) {
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();
        QueriesExecutionContext queriesExecutionContext = mock(QueriesExecutionContext.class);
        when(queriesExecutionContext.isQueryFinished()).thenReturn(true);

        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();
        return Operators.liftPublisher((pub, delegate) ->
            new ResultInvocationSubscriber(delegate, executionInfo, proxyConfig, queriesExecutionContext));
    }

}
