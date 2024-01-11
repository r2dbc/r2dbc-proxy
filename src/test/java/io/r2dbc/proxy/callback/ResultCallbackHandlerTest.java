/*
 * Copyright 2018-2023 the original author or authors.
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

import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.spi.OutParameters;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.TestPublisher;
import reactor.util.context.Context;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Test for {@link ResultCallbackHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ResultCallbackHandlerTest {

    private static Method MAP_METHOD = ReflectionUtils.findMethod(Result.class, "map", BiFunction.class);

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    private static Method GET_PROXY_CONFIG_METHOD = ReflectionUtils.findMethod(ProxyConfigHolder.class, "getProxyConfig");


    @Test
    void mapWhenAllResultAreNotAlreadyGenerated() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

        Row row1 = MockRow.builder().identified(0, String.class, "foo").build();
        Row row2 = MockRow.builder().identified(0, String.class, "bar").build();
        Row row3 = MockRow.builder().identified(0, String.class, "baz").build();
        Result mockResult = MockResult.builder().row(row1, row2, row3).build();
        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));
        queriesExecutionContext.incrementProducedCount();

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        // map function to return the String value
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> row.get(0, String.class);

        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result)
            .isInstanceOf(Publisher.class);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .assertNext(obj -> {  // first
                assertThat(obj).isEqualTo("foo");
                assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

                // verify EACH_QUERY_RESULT
                assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
                assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(1);
                assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo("foo");
                assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
                assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
                assertThat(queryExecutionInfo.getThrowable()).isNull();
                assertThat(queriesExecutionContext.isAllConsumed()).isFalse();
            })
            .assertNext(obj -> {  // second
                assertThat(obj).isEqualTo("bar");
                assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

                // verify EACH_QUERY_RESULT
                assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
                assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(2);
                assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo("bar");
                assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
                assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
                assertThat(queryExecutionInfo.getThrowable()).isNull();
                assertThat(queriesExecutionContext.isAllConsumed()).isFalse();
            })
            .assertNext(obj -> {  // third
                assertThat(obj).isEqualTo("baz");
                assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

                // verify EACH_QUERY_RESULT
                assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
                assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(3);
                assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo("baz");
                assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
                assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
                assertThat(queryExecutionInfo.getThrowable()).isNull();
                assertThat(queriesExecutionContext.isAllConsumed()).isFalse();
            })
            .verifyComplete();

        assertThat(queriesExecutionContext.isAllConsumed()).isTrue();
        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT).as("alert query has not be called");
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapWithPublisherExceptionWhenAllResultAreNotAlreadyGenerated() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

        // return a publisher that throws exception at execution
        Exception exception = new RuntimeException("map exception");
        TestPublisher<Object> publisher = TestPublisher.create().error(exception);

        Result mockResult = mock(Result.class);
        when(mockResult.map(any(BiFunction.class))).thenReturn(publisher);

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        // the arg type is checked in handler, so need an instance with BiFunction type
        BiFunction<Row, RowMetadata, Object> biFunction = mock(BiFunction.class);
        Object[] args = new Object[]{biFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);
        assertThat(result).isNotSameAs(publisher);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        queriesExecutionContext.incrementProducedCount();
        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .consumeErrorWith(thrown -> {
                assertThat(thrown).isSameAs(exception);
            })
            .verify();

        assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
        assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(1);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isNull();
        assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
        assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
    }

    @Test
    void mapWithEmptyPublisherWhenAllResultAreNotAlreadyGenerated() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

        // return empty result
        Result mockResult = MockResult.builder().build();

        AtomicBoolean isCalled = new AtomicBoolean();
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> {
            isCalled.set(true);
            return null;
        };

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);
        assertThat(isCalled).as("map function should not be called").isFalse();

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .verifyComplete();

        assertThat(listener.getEachQueryResultExecutionInfo()).as("EachQueryResult callback should not be called")
            .isNull();
    }

    @Test
    void mapWhenAllResultHasBeenGenerated() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

        Row row1 = MockRow.builder().identified(0, String.class, "foo").build();
        Row row2 = MockRow.builder().identified(0, String.class, "bar").build();
        Row row3 = MockRow.builder().identified(0, String.class, "baz").build();
        Result mockResult = MockResult.builder().row(row1, row2, row3).build();

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));
        queriesExecutionContext.incrementProducedCount();
        queriesExecutionContext.markAllProduced();

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        // map function to return the String value
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> row.get(0, String.class);

        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result)
            .isInstanceOf(Publisher.class);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .assertNext(obj -> {  // first
                assertThat(obj).isEqualTo("foo");
                assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

                // verify EACH_QUERY_RESULT
                assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
                assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(1);
                assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo("foo");
                assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
                assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
                assertThat(queryExecutionInfo.getThrowable()).isNull();
                assertThat(queriesExecutionContext.isAllConsumed()).isFalse();
            })
            .assertNext(obj -> {  // second
                assertThat(obj).isEqualTo("bar");
                assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

                // verify EACH_QUERY_RESULT
                assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
                assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(2);
                assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo("bar");
                assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
                assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
                assertThat(queryExecutionInfo.getThrowable()).isNull();
                assertThat(queriesExecutionContext.isAllConsumed()).isFalse();
            })
            .assertNext(obj -> {  // third
                assertThat(obj).isEqualTo("baz");
                assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

                // verify EACH_QUERY_RESULT
                assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
                assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(3);
                assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo("baz");
                assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
                assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
                assertThat(queryExecutionInfo.getThrowable()).isNull();
                assertThat(queriesExecutionContext.isAllConsumed()).isFalse();
            })
            .verifyComplete();

        assertThat(queriesExecutionContext.isAllConsumed()).isTrue();
        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo(null);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo(null);
        assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
        assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
        assertThat(queryExecutionInfo.getThrowable()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapWithPublisherExceptionWhenAllHasBeenGenerated() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();


        // return a publisher that throws exception at execution
        Exception exception = new RuntimeException("map exception");
        TestPublisher<Object> publisher = TestPublisher.create().error(exception);

        Result mockResult = mock(Result.class);
        when(mockResult.map(any(BiFunction.class))).thenReturn(publisher);

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        BiFunction<Row, RowMetadata, String> biFunction = mock(BiFunction.class);
        Object[] args = new Object[]{biFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);
        assertThat(result).isNotSameAs(publisher);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        queriesExecutionContext.incrementProducedCount();
        queriesExecutionContext.markAllProduced();

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .consumeErrorWith(thrown -> {
                assertThat(thrown).isSameAs(exception);
            })
            .verify();

        assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo(null);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isNull();
        assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
        assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
    }

    @Test
    void mapWithEmptyPublisherWhenAllResultHasBeenGenerated() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

        // return empty result
        Result mockResult = MockResult.builder().build();

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));
        queriesExecutionContext.incrementProducedCount();
        queriesExecutionContext.markAllProduced();

        AtomicBoolean isCalled = new AtomicBoolean();
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> {
            isCalled.set(true);
            return null;
        };

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);
        assertThat(isCalled).as("map function should not be called").isFalse();

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .verifyComplete();

        assertThat(listener.getAfterQueryExecutionInfo()).isSameAs(queryExecutionInfo);
        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isEqualTo(null);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapWithResultThatErrorsAtExecutionTimeWhenAllResultAreNotAlreadyGenerated() throws Throwable {

        // call to the "map()" method returns a publisher that fails(errors) at execution time

        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();


        RuntimeException exception = new RuntimeException("failure");

        // return mock results
        Row row1 = MockRow.builder().identified(0, String.class, "foo").build();
        Row row2 = MockRow.builder().identified(0, String.class, "bar").build();
        Row row3 = MockRow.builder().identified(0, String.class, "baz").build();
        Result mockResult = MockResult.builder().row(row1, row2, row3).build();

        // map function to throw exception
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> {
            throw exception;
        };

        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        queriesExecutionContext.incrementProducedCount();

        assertThat(result)
            .isInstanceOf(Publisher.class);

        Flux<String> resultConsumer = Flux.from((Publisher<String>) result);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create(resultConsumer)
            .expectSubscription()
            .consumeErrorWith(thrown -> {
                assertThat(thrown).isSameAs(exception);
            })
            .verify();

        // verify callback
        assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo).as(
            "listener should be called even consuming throws exception");
        assertThat(queriesExecutionContext.isAllConsumed()).isTrue().as("there are only one result processing, so after .map all result are processed");
        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
        assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(1);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isNull();
        assertThat(queryExecutionInfo.getThrowable()).isSameAs(exception);
        assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
        assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);

    }

    @SuppressWarnings("unchecked")
    @Test
    void mapVerifyContext() throws Throwable {
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        Row row1 = MockRow.builder().identified(0, String.class, "foo").build();
        Row row2 = MockRow.builder().identified(0, String.class, "bar").build();
        Row row3 = MockRow.builder().identified(0, String.class, "baz").build();
        Result mockResult = MockResult.builder().row(row1, row2, row3).build();
        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        // map function to return the String value
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> row.get(0, String.class);

        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);

        StepVerifier.create((Publisher<String>) result, StepVerifierOptions.create().withInitialContext(Context.of("foo", "FOO")))
            .expectSubscription()
            .expectAccessibleContext()
            .contains("foo", "FOO")
            .then()
            .expectNext("foo")
            .expectNext("bar")
            .expectNext("baz")
            .verifyComplete();
    }

    @Test
    void unwrap() throws Throwable {
        Result mockResult = MockResult.empty();
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = new ProxyConfig();
        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        Object result = callback.invoke(mockResult, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(mockResult);
    }

    @Test
    void getProxyConfig() throws Throwable {
        Result mockResult = MockResult.empty();
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = new ProxyConfig();
        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        Object result = callback.invoke(mockResult, GET_PROXY_CONFIG_METHOD, null);
        assertThat(result).isSameAs(proxyConfig);
    }

    @ParameterizedTest
    @MethodSource
    void mapWithFunction(Result mockResult, boolean expectProxy, List<Class<?>> interfaces) throws Throwable {
        Method mapMethod = ReflectionUtils.findMethod(Result.class, "map", Function.class);
        AtomicReference<Readable> holder = new AtomicReference<>();
        Function<Readable, ?> mapFunction = (readable) -> {
            holder.set(readable);
            return "done";
        };

        invokeAndVerifyResult(mockResult, mapMethod, mapFunction);

        Readable value = holder.get();
        if (expectProxy) {
            assertThat(value).isInstanceOf(Wrapped.class);
            assertThat(((Wrapped<?>) value).unwrap()).isSameAs(CustomRow.INSTANCE);
        } else {
            assertThat(value).isNotInstanceOf(Wrapped.class);
        }

        for (Class<?> expectedInterface : interfaces) {
            assertThat(value).isInstanceOf(expectedInterface);
        }
    }

    @ParameterizedTest
    @MethodSource
    void mapWithBiFunction(Result mockResult, boolean expectProxy, List<Class<?>> interfaces) throws Throwable {
        AtomicReference<Row> holder = new AtomicReference<>();
        BiFunction<Row, RowMetadata, ?> mapFunction = (row, metadata) -> {
            holder.set(row);
            return "done";
        };
        Method mapMethod = ReflectionUtils.findMethod(Result.class, "map", BiFunction.class);

        invokeAndVerifyResult(mockResult, mapMethod, mapFunction);

        Readable value = holder.get();
        if (expectProxy) {
            assertThat(value).isInstanceOf(Wrapped.class);
            assertThat(((Wrapped<?>) value).unwrap()).isSameAs(CustomRow.INSTANCE);
        }
        for (Class<?> expectedInterface : interfaces) {
            assertThat(value).isInstanceOf(expectedInterface);
        }
    }

    @ParameterizedTest
    @MethodSource
    void flatMap(Result mockResult, boolean expectProxy, List<Class<?>> interfaces) throws Throwable {
        AtomicReference<Result.Segment> holder = new AtomicReference<>();
        Function<Result.Segment, Publisher<?>> mapFunction = (segment) -> {
            holder.set(segment);
            return Mono.just("done");
        };
        Method method = ReflectionUtils.findMethod(Result.class, "flatMap", Function.class);

        invokeAndVerifyResult(mockResult, method, mapFunction);

        Result.Segment value = holder.get();
        if (expectProxy) {
            assertThat(value).isInstanceOf(Wrapped.class);
            assertThat(((Wrapped<?>) value).unwrap()).isSameAs(CustomRowSegment.INSTANCE);
        } else {
            assertThat(value).isNotInstanceOf(Wrapped.class);
        }
        for (Class<?> expectedInterface : interfaces) {
            assertThat(value).isInstanceOf(expectedInterface);
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeAndVerifyResult(Result mockResult, Method method, Object mapFunction) throws Throwable {
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        QueriesExecutionContext queriesExecutionContext = new QueriesExecutionContext(mock(Clock.class));
        Object[] args = new Object[]{mapFunction};
        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig, queriesExecutionContext);

        Object result = callback.invoke(mockResult, method, args);

        assertThat(result).isInstanceOf(Publisher.class);
        StepVerifier.create((Publisher<String>) result)
            .expectSubscription()
            .expectNext("done")
            .verifyComplete();
    }

    static Stream<Arguments> mapWithBiFunction() {
        Result rowResult = MockResult.builder().row(CustomRow.INSTANCE).build();
        return Stream.of(
            arguments(rowResult, true, Arrays.asList(Row.class, Readable.class),
                Arrays.asList(CustomRow.class, CustomMarker.class))
        );
    }

    static Stream<Arguments> mapWithFunction() {
        // Readable
        Result rowResult = MockResult.builder().row(CustomRow.INSTANCE).build();
        Result outParametersResult = MockResult.builder().outParameters(CustomOutParameters.INSTANCE).build();
        return Stream.of(
            // only row is proxied
            arguments(rowResult, true, Arrays.asList(Row.class, Readable.class)),
            arguments(outParametersResult, false, Arrays.asList(CustomOutParameters.class, OutParameters.class, Readable.class, CustomMarker.class))
        );
    }

    static Stream<Arguments> flatMap() {
        // Test all types of Result.Segment
        Result rowSegmentResult = MockResult.builder().segment(CustomRowSegment.INSTANCE).build();
        Result outSegmentResult = MockResult.builder().segment(CustomOutSegment.INSTANCE).build();
        Result updateCountSegment = MockResult.builder().segment(CustomUpdateCountSegment.INSTANCE).build();
        Result messageSegment = MockResult.builder().segment(CustomMessageSegment.INSTANCE).build();
        Result segment = MockResult.builder().segment(CustomSegment.INSTANCE).build();
        return Stream.of(
            // only row segment is proxied for ".row()"
            arguments(rowSegmentResult, true, Arrays.asList(Result.RowSegment.class, Result.Segment.class)),
            arguments(outSegmentResult, false, Arrays.asList(CustomOutSegment.class, Result.OutSegment.class, Result.Segment.class, CustomMarker.class)),
            arguments(updateCountSegment, false, Arrays.asList(CustomUpdateCountSegment.class, Result.UpdateCount.class, Result.Segment.class, CustomMarker.class)),
            arguments(messageSegment, false, Arrays.asList(CustomMessageSegment.class, Result.Message.class, Result.Segment.class, CustomMarker.class)),
            arguments(segment, false, Arrays.asList(CustomSegment.class, Result.Segment.class, CustomMarker.class))
        );
    }

    interface CustomMarker {

    }

    interface CustomRow extends Row, CustomMarker {

        CustomRow INSTANCE = createMock(CustomRow.class);
    }

    interface CustomOutParameters extends OutParameters, CustomMarker {

        CustomOutParameters INSTANCE = mock(CustomOutParameters.class, withSettings().extraInterfaces(CustomOutParameters.class.getInterfaces()));

    }

    interface CustomRowSegment extends Result.RowSegment, CustomMarker {

        CustomRowSegment INSTANCE = createMock(CustomRowSegment.class);


    }

    interface CustomOutSegment extends Result.OutSegment, CustomMarker {

        CustomOutSegment INSTANCE = createMock(CustomOutSegment.class);

    }

    interface CustomUpdateCountSegment extends Result.UpdateCount, CustomMarker {

        CustomUpdateCountSegment INSTANCE = createMock(CustomUpdateCountSegment.class);

    }

    interface CustomMessageSegment extends Result.Message, CustomMarker {

        CustomMessageSegment INSTANCE = createMock(CustomMessageSegment.class);

    }

    interface CustomSegment extends Result.Segment, CustomMarker {

        CustomSegment INSTANCE = createMock(CustomSegment.class);

    }

    private static <T> T createMock(Class<T> type) {
        return mock(type, withSettings().extraInterfaces(type.getInterfaces()));
    }

}
