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

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QueryInvocationSubscriber}.
 *
 * @author Tadaya Tsuyukubo
 */
class QueryInvocationSubscriberTest {

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
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();
        QueriesExecutionContext executionContext = new QueriesExecutionContext(Clock.systemUTC());

        Function<? super Publisher<Result>, ? extends Publisher<Result>> transformer =
            Operators.liftPublisher((pub, subscriber) -> new QueryInvocationSubscriber(subscriber, executionInfo, proxyConfig, executionContext));

        Result result = mock(Result.class);
        Mono<Result> mono = Mono.just(result)
            .transform(transformer)
            .contextWrite(context -> context.put("foo", "FOO"));

        // simulate as if produced Result has consumed. This triggers afterQuery when above mono is completed.
        executionContext.incrementConsumedCount();

        StepVerifier.create(mono)
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete();

        QueryExecutionInfo queryExecutionInfo = testListener.getBeforeQueryExecutionInfo();
        ContextView contextView = queryExecutionInfo.getValueStore().get(ContextView.class, ContextView.class);
        assertThat(contextView).isNotNull();
        assertThat(contextView.hasKey("foo")).isTrue();
        assertThat((String) contextView.get("foo")).isEqualTo("FOO");

        // afterQuery has called with executionInfo that contains reactor context
        assertThat(testListener.getAfterQueryExecutionInfo()).isSameAs(queryExecutionInfo);
    }

    @Test
    void beforeQueryError() {
        RuntimeException runtimeException = new RuntimeException("Boom");

        AtomicBoolean beforeQueryCalled = new AtomicBoolean();
        AtomicBoolean afterQueryCalled = new AtomicBoolean();
        ProxyExecutionListener listener = new ProxyExecutionListener() {

            @Override
            public void beforeQuery(QueryExecutionInfo execInfo) {
                beforeQueryCalled.set(true);
                throw runtimeException;
            }

            @Override
            public void afterQuery(QueryExecutionInfo execInfo) {
                afterQueryCalled.set(true);
            }
        };

        Function<? super Publisher<Result>, ? extends Publisher<Result>> transformer = createTransformer(listener);
        Result result = mock(Result.class);
        Mono<Result> mono = Mono.just(result).transform(transformer);

        // error from beforeQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectNext(result)
            .verifyComplete();

        assertThat(beforeQueryCalled).isTrue();
        assertThat(droppedError.get()).isSameAs(runtimeException);
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

        Function<? super Publisher<Result>, ? extends Publisher<Result>> transformer = createTransformer(listener);
        Result result = mock(Result.class);
        Mono<Result> mono = Mono.just(result).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectNext(result)
            .verifyComplete();

        assertThat(droppedError.get()).isSameAs(runtimeException);
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

        Function<? super Publisher<Result>, ? extends Publisher<Result>> transformer = createTransformer(listener);
        Result result = mock(Result.class);
        Mono<Result> mono = Mono.just(result).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectNext(result)
            .thenCancel()
            .verify();

        assertThat(droppedError.get()).isSameAs(runtimeException);
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

        Function<? super Publisher<Result>, ? extends Publisher<Result>> transformer = createTransformer(listener);
        RuntimeException monoError = new RuntimeException("My Error");
        Mono<Result> mono = Mono.error(monoError).cast(Result.class).transform(transformer);

        // error from afterQuery should not affect the mono.
        StepVerifier.create(mono)
            .expectErrorSatisfies((ex) -> assertThat(ex).isSameAs(monoError))
            .verify();

        assertThat(droppedError.get()).isSameAs(runtimeException);
        assertThat(afterQueryCalled).isTrue();
    }

    private Function<? super Publisher<Result>, ? extends Publisher<Result>> createTransformer(ProxyExecutionListener listener) {
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();
        QueriesExecutionContext queriesExecutionContext = mock(QueriesExecutionContext.class);
        when(queriesExecutionContext.isQueryFinished()).thenReturn(true);

        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();
        return Operators.liftPublisher((pub, subscriber) ->
            new QueryInvocationSubscriber(subscriber, executionInfo, proxyConfig, queriesExecutionContext));
    }

}
