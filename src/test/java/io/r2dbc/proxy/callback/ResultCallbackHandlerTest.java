/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ResultCallbackHandlerTest {

    private static Method MAP_METHOD = ReflectionUtils.findMethod(Result.class, "map", BiFunction.class);

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");


    @Test
    void map() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

        Row row1 = MockRow.builder().identified(0, String.class, "foo").build();
        Row row2 = MockRow.builder().identified(0, String.class, "bar").build();
        Row row3 = MockRow.builder().identified(0, String.class, "baz").build();
        Result mockResult = MockResult.builder().row(row1, row2, row3).rowMetadata(MockRowMetadata.empty()).build();

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig);

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
            })
            .verifyComplete();

    }

    @Test
    void mapWithPublisherException() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();


        // return a publisher that throws exception at execution
        Exception exception = new RuntimeException("map exception");
        TestPublisher<Object> publisher = TestPublisher.create().error(exception);

        Result mockResult = mock(Result.class);
        when(mockResult.map(any())).thenReturn(publisher);

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{null};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);
        assertThat(result).isNotSameAs(publisher);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .consumeErrorWith(thrown -> {
                assertThat(thrown).isSameAs(exception);
            })
            .verify();

        assertThat(listener.getEachQueryResultExecutionInfo()).isSameAs(queryExecutionInfo);

        // verify EACH_QUERY_RESULT
        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
        assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(1);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isNull();
        assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
        assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);
    }

    @Test
    void mapWithEmptyPublisher() throws Throwable {
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


        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig);

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
    @SuppressWarnings("unchecked")
    void mapWithResultThatErrorsAtExecutionTime() throws Throwable {

        // call to the "map()" method returns a publisher that fails(errors) at execution time

        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();


        RuntimeException exception = new RuntimeException("failure");

        // return mock results
        Row row1 = MockRow.builder().identified(0, String.class, "foo").build();
        Row row2 = MockRow.builder().identified(0, String.class, "bar").build();
        Row row3 = MockRow.builder().identified(0, String.class, "baz").build();
        Result mockResult = MockResult.builder().row(row1, row2, row3).rowMetadata(MockRowMetadata.empty()).build();

        // map function to throw exception
        BiFunction<Row, RowMetadata, String> mapBiFunction = (row, rowMetadata) -> {
            throw exception;
        };


        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{mapBiFunction};
        Object result = callback.invoke(mockResult, MAP_METHOD, args);

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
        assertThat(queryExecutionInfo.getProxyEventType()).isEqualTo(ProxyEventType.EACH_QUERY_RESULT);
        assertThat(queryExecutionInfo.getCurrentResultCount()).isEqualTo(1);
        assertThat(queryExecutionInfo.getCurrentMappedResult()).isNull();
        assertThat(queryExecutionInfo.getThrowable()).isSameAs(exception);
        assertThat(queryExecutionInfo.getThreadId()).isEqualTo(threadId);
        assertThat(queryExecutionInfo.getThreadName()).isEqualTo(threadName);

    }

    @Test
    void unwrap() throws Throwable {
        Result mockResult = MockResult.empty();
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        ResultCallbackHandler callback = new ResultCallbackHandler(mockResult, queryExecutionInfo, proxyConfig);

        Object result = callback.invoke(mockResult, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(mockResult);
    }

}
