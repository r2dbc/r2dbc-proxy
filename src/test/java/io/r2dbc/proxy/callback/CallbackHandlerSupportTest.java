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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.listener.CompositeProxyExecutionListener;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.TestPublisher;
import reactor.util.context.Context;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
@ExtendWith(MockitoExtension.class)
public class CallbackHandlerSupportTest {

    private CallbackHandlerSupport callbackHandlerSupport;

    @Mock(lenient = true)
    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneId.systemDefault());
        when(this.proxyConfig.getClock()).thenReturn(clock);

        this.callbackHandlerSupport = new CallbackHandlerSupport(this.proxyConfig) {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
        };
    }

    @Test
    void interceptQueryExecution() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);
        when(this.proxyConfig.getProxyFactory()).thenReturn(proxyFactory);

        // when it creates a proxy for Result
        Result mockResultProxy = MockResult.empty();
        when(proxyFactory.wrapResult(any(), any(), any())).thenAnswer((args) -> {
            ((QueriesExecutionContext) args.getArgument(2)).incrementConsumedCount();
            return mockResultProxy;
        });

        // produce single result
        Result mockResult = MockResult.empty();
        Mono<Result> resultPublisher = Mono.just(mockResult);

        Flux<? extends Result> result = this.callbackHandlerSupport.interceptQueryExecution(resultPublisher, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
            .expectSubscription()
            .consumeNextWith(c -> {
                //executionInfo.resultProcessed(c);
                // verify produced result is the proxy result
                assertThat(c).isSameAs(mockResultProxy);
            })
            .expectComplete()
            .verify();


        assertThat(listener.getBeforeMethodExecutionInfo()).isNull();
        assertThat(listener.getAfterMethodExecutionInfo()).isNull();
        assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(listener.getAfterQueryExecutionInfo()).isSameAs(executionInfo);

        assertThat(executionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(executionInfo.getThreadName()).isEqualTo(threadName);
        assertThat(executionInfo.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(executionInfo.getExecuteDuration()).isEqualTo(Duration.ZERO);

        // verify success
        assertThat(executionInfo.isSuccess()).isTrue();
        assertThat(executionInfo.getThrowable()).isNull();

        assertThat(executionInfo.getCurrentResultCount()).isEqualTo(0);
        assertThat(executionInfo.getCurrentMappedResult()).isNull();


        // verify the call to create a proxy result
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(proxyFactory).wrapResult(resultCaptor.capture(), eq(executionInfo), any());

        Result captureResult = resultCaptor.getValue();
        assertThat(captureResult).isSameAs(mockResult);
    }

    @Test
    void interceptQueryExecutionWithFailure() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);

        // publisher that throws exception
        RuntimeException exception = new RuntimeException();
        Publisher<Result> publisher = TestPublisher.<Result>create().error(exception);

        Flux<? extends Result> result = this.callbackHandlerSupport.interceptQueryExecution(publisher, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
            .expectSubscription()
            .verifyError(RuntimeException.class);


        assertThat(listener.getBeforeMethodExecutionInfo()).isNull();
        assertThat(listener.getAfterMethodExecutionInfo()).isNull();
        assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(listener.getAfterQueryExecutionInfo()).isSameAs(executionInfo);

        assertThat(executionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(executionInfo.getThreadName()).isEqualTo(threadName);
        assertThat(executionInfo.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(executionInfo.getExecuteDuration()).isEqualTo(Duration.ZERO);

        // verify failure
        assertThat(executionInfo.isSuccess()).isFalse();
        assertThat(executionInfo.getThrowable()).isSameAs(exception);

        assertThat(executionInfo.getCurrentResultCount()).isEqualTo(0);
    }

    @Test
    void interceptQueryExecutionWithImmediateCancel() {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);
        when(this.proxyConfig.getProxyFactory()).thenReturn(proxyFactory);

        // produce single result
        Result mockResult = MockResult.empty();
        Mono<Result> resultPublisher = Mono.just(mockResult);

        Flux<? extends Result> result = this.callbackHandlerSupport.interceptQueryExecution(resultPublisher, executionInfo);


        // Cancels immediately
        StepVerifier.create(result.log())
            .expectSubscription()
            .thenCancel()
            .verify();

        assertThat(listener.getBeforeMethodExecutionInfo()).isNull();
        assertThat(listener.getAfterMethodExecutionInfo()).isNull();
        assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(listener.getAfterQueryExecutionInfo()).isSameAs(executionInfo);

        assertThat(executionInfo.getExecuteDuration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void interceptQueryExecutionWithMultipleResult() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);
        when(this.proxyConfig.getProxyFactory()).thenReturn(proxyFactory);

        // when it creates a proxy for Result
        Result mockResultProxy = mock(Result.class);
        when(proxyFactory.wrapResult(any(), any(), any())).thenAnswer((args) -> {
            ((QueriesExecutionContext) args.getArgument(2)).incrementConsumedCount();
            return mockResultProxy;
        });

        // produce multiple results
        Result mockResult1 = mock(Result.class);
        Result mockResult2 = mock(Result.class);
        Result mockResult3 = mock(Result.class);
        Flux<Result> publisher = Flux.just(mockResult1, mockResult2, mockResult3)
            .doOnRequest(subscription -> {
                // this will be called AFTER listener.beforeQuery() but BEFORE emitting query result from this publisher.
                // verify BEFORE_QUERY
                assertThat(executionInfo.getProxyEventType()).isEqualTo(ProxyEventType.BEFORE_QUERY);
                assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);

                assertThat(executionInfo.getCurrentResultCount()).isEqualTo(0);
                assertThat(executionInfo.getCurrentMappedResult()).isNull();
            });

        Flux<? extends Result> result = this.callbackHandlerSupport.interceptQueryExecution(publisher, executionInfo);

        // result should return a publisher that emits 3 proxy results
        // verifies result flux
        StepVerifier.create(result)
            .expectSubscription()
            .assertNext(c -> {
                assertThat(c).as("first result").isSameAs(mockResultProxy);
            })
            .assertNext(c -> {
                assertThat(c).as("second result").isSameAs(mockResultProxy);
            })
            .assertNext(c -> {
                assertThat(c).as("third result").isSameAs(mockResultProxy);
            })
            .expectComplete()
            .verify();


        assertThat(listener.getBeforeMethodExecutionInfo()).isNull();
        assertThat(listener.getAfterMethodExecutionInfo()).isNull();
        assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(listener.getAfterQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(executionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(executionInfo.getThreadName()).isEqualTo(threadName);
        assertThat(executionInfo.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(executionInfo.getExecuteDuration()).isEqualTo(Duration.ZERO);

        // verify success
        assertThat(executionInfo.isSuccess()).isTrue();
        assertThat(executionInfo.getThrowable()).isNull();

        assertThat(executionInfo.getCurrentResultCount()).isEqualTo(0);
        assertThat(executionInfo.getCurrentMappedResult()).isNull();


        // verify the call to create proxy result
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(proxyFactory, times(3)).wrapResult(resultCaptor.capture(), eq(executionInfo), any());

        List<Result> captured = resultCaptor.getAllValues();
        assertThat(captured).hasSize(3).containsExactly(mockResult1, mockResult2, mockResult3);
    }

    @Test
    void interceptQueryExecutionWithEmptyResult() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);

        // produce multiple results
        Flux<Result> publisher = Flux.empty()
            .ofType(Result.class)
            .doOnRequest(subscription -> {
                // this will be called AFTER listener.beforeQuery() but BEFORE emitting query result from this publisher.
                // verify BEFORE_QUERY
                assertThat(executionInfo.getProxyEventType()).isEqualTo(ProxyEventType.BEFORE_QUERY);
                assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);

                assertThat(executionInfo.getCurrentResultCount()).isEqualTo(0);
                assertThat(executionInfo.getCurrentMappedResult()).isNull();
            });

        Flux<? extends Result> result = this.callbackHandlerSupport.interceptQueryExecution(publisher, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
            .expectSubscription()
            .expectNextCount(0)
            .expectComplete()
            .verify();


        assertThat(listener.getBeforeMethodExecutionInfo()).isNull();
        assertThat(listener.getAfterMethodExecutionInfo()).isNull();
        assertThat(listener.getBeforeQueryExecutionInfo()).isSameAs(executionInfo);
        assertThat(listener.getAfterQueryExecutionInfo()).isSameAs(executionInfo);

        assertThat(executionInfo.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_QUERY);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(executionInfo.getThreadName()).isEqualTo(threadName);
        assertThat(executionInfo.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(executionInfo.getExecuteDuration()).isEqualTo(Duration.ZERO);

        // verify success
        assertThat(executionInfo.isSuccess()).isTrue();
        assertThat(executionInfo.getThrowable()).isNull();

        assertThat(executionInfo.getCurrentResultCount()).isEqualTo(0);
        assertThat(executionInfo.getCurrentMappedResult()).isNull();

    }

    @Test
    void interceptQueryExecutionVerifyContext() {
        MutableQueryExecutionInfo executionInfo = new MutableQueryExecutionInfo();
        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        when(this.proxyConfig.getListeners()).thenReturn(new CompositeProxyExecutionListener());
        when(this.proxyConfig.getProxyFactory()).thenReturn(proxyFactory);

        // when it creates a proxy for Result
        Result mockResultProxy = MockResult.empty();
        when(proxyFactory.wrapResult(any(), any(), any())).thenReturn(mockResultProxy);

        // produce single result
        Result mockResult = MockResult.empty();
        Mono<Result> resultPublisher = Mono.just(mockResult);

        Flux<Result> result = this.callbackHandlerSupport.interceptQueryExecution(resultPublisher, executionInfo).cast(Result.class);

        // verifies result flux
        StepVerifier.create(result, StepVerifierOptions.create().withInitialContext(Context.of("foo", "FOO")))
            .expectSubscription()
            .expectAccessibleContext()
            .contains("foo", "FOO")
            .then()
            .expectNext(mockResultProxy)
            .expectComplete()
            .verify();
    }


    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisher() throws Throwable {

        // target method returns Producer
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        when(this.proxyConfig.getListeners()).thenReturn(new CompositeProxyExecutionListener(listener));

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Result mockResult = MockResult.empty();
        Mono<Result> publisher = Mono.just(mockResult);

        doReturn(publisher).when(target).execute();

        Object result = this.callbackHandlerSupport.proceedExecution(executeMethod, target, args, listener, connectionInfo, null);

        // verify method on target is invoked
        verify(target).execute();

        StepVerifier.create((Publisher<Result>) result)
            .expectSubscription()
            .consumeNextWith(c -> {
                // in middle of chain, beforeMethod must be called
                assertThat(c).isSameAs(mockResult);

                MethodExecutionInfo beforeMethod = listener.getBeforeMethodExecutionInfo();
                assertThat(beforeMethod).isNotNull();
                assertThat(listener.getAfterMethodExecutionInfo()).isNull();

                assertThat(beforeMethod.getProxyEventType()).isEqualTo(ProxyEventType.BEFORE_METHOD);
            })
            .expectComplete()
            .verify();


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethodExecution).isSameAs(beforeMethodExecution);

        assertThat(listener.getBeforeQueryExecutionInfo()).isNull();
        assertThat(listener.getAfterQueryExecutionInfo()).isNull();

        assertThat(afterMethodExecution.getTarget()).isEqualTo(target);
        assertThat(afterMethodExecution.getResult()).isEqualTo(mockResult);
        assertThat(afterMethodExecution.getMethod()).isEqualTo(executeMethod);
        assertThat(afterMethodExecution.getMethodArgs()).isEqualTo(args);
        assertThat(afterMethodExecution.getConnectionInfo()).isSameAs(connectionInfo);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(afterMethodExecution.getThreadName()).isEqualTo(threadName);
        assertThat(afterMethodExecution.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(afterMethodExecution.getExecuteDuration()).isEqualTo(Duration.ZERO);

        assertThat(afterMethodExecution.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_METHOD);

        assertThat(afterMethodExecution.getThrown()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisherThrowsException() throws Throwable {

        // target method returns Producer
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        when(this.proxyConfig.getListeners()).thenReturn(new CompositeProxyExecutionListener(listener));
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();

        // publisher that throws exception
        RuntimeException exception = new RuntimeException();
        Publisher<Result> publisher = TestPublisher.<Result>create().error(exception);

        doReturn(publisher).when(target).execute();

        Object result = this.callbackHandlerSupport.proceedExecution(executeMethod, target, args, listener, connectionInfo, null);

        // verify method on target is invoked
        verify(target).execute();

        StepVerifier.create((Publisher<Result>) result)
            .expectSubscription()
            .expectError(RuntimeException.class)
            .verify();


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethodExecution).isSameAs(beforeMethodExecution);

        assertThat(listener.getBeforeQueryExecutionInfo()).isNull();
        assertThat(listener.getAfterQueryExecutionInfo()).isNull();

        assertThat(afterMethodExecution.getResult()).isNull();

        assertThat(afterMethodExecution.getTarget()).isEqualTo(target);
        assertThat(afterMethodExecution.getMethod()).isEqualTo(executeMethod);
        assertThat(afterMethodExecution.getMethodArgs()).isEqualTo(args);
        assertThat(afterMethodExecution.getConnectionInfo()).isEqualTo(connectionInfo);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(afterMethodExecution.getThreadName()).isEqualTo(threadName);
        assertThat(afterMethodExecution.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(afterMethodExecution.getExecuteDuration()).isEqualTo(Duration.ZERO);

        assertThat(afterMethodExecution.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_METHOD);

        assertThat(afterMethodExecution.getThrown()).isSameAs(exception);
    }

    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisherImmediateCancel() throws Throwable {

        // target method returns Publisher
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        when(this.proxyConfig.getListeners()).thenReturn(new CompositeProxyExecutionListener(listener));
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Result mockResult = MockResult.empty();
        Mono<Result> publisher = Mono.just(mockResult);

        doReturn(publisher).when(target).execute();

        Object result = this.callbackHandlerSupport.proceedExecution(executeMethod, target, args, listener, connectionInfo, null);

        // verify method on target is invoked
        verify(target).execute();

        StepVerifier.create((Publisher<Result>) result)
            .thenCancel()
            .verify();


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethodExecution).isSameAs(beforeMethodExecution);

        assertThat(listener.getBeforeQueryExecutionInfo()).isNull();
        assertThat(listener.getAfterQueryExecutionInfo()).isNull();

        assertThat(afterMethodExecution.getExecuteDuration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void proceedExecutionWithNonPublisher() throws Throwable {

        // target method returns Batch (not Publisher)
        Method addMethod = ReflectionUtils.findMethod(Batch.class, "add", String.class);
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{"QUERY"};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Batch mockBatch = mock(Batch.class);

        doReturn(mockBatch).when(target).add("QUERY");

        Object result = this.callbackHandlerSupport.proceedExecution(addMethod, target, args, listener, connectionInfo, null);

        // verify method on target is invoked
        verify(target).add("QUERY");

        assertThat(result).isSameAs(mockBatch);

        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethodExecution).isSameAs(beforeMethodExecution);

        assertThat(listener.getBeforeQueryExecutionInfo()).isNull();
        assertThat(listener.getAfterQueryExecutionInfo()).isNull();

        assertThat(afterMethodExecution.getTarget()).isEqualTo(target);
        assertThat(afterMethodExecution.getResult()).isEqualTo(mockBatch);
        assertThat(afterMethodExecution.getMethod()).isEqualTo(addMethod);
        assertThat(afterMethodExecution.getMethodArgs()).isEqualTo(args);
        assertThat(afterMethodExecution.getConnectionInfo()).isSameAs(connectionInfo);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(afterMethodExecution.getThreadName()).isEqualTo(threadName);
        assertThat(afterMethodExecution.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(afterMethodExecution.getExecuteDuration()).isEqualTo(Duration.ZERO);

        assertThat(afterMethodExecution.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_METHOD);

        assertThat(afterMethodExecution.getThrown()).isNull();
    }

    @Test
    void proceedExecutionWithNonPublisherThrowsException() throws Throwable {

        // target method returns Batch (not Publisher)
        Method addMethod = ReflectionUtils.findMethod(Batch.class, "add", String.class);
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{"QUERY"};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();

        // method invocation throws exception
        RuntimeException exception = new RuntimeException();
        when(target.add("QUERY")).thenThrow(exception);

        assertThatThrownBy(() -> {
            this.callbackHandlerSupport.proceedExecution(addMethod, target, args, listener, connectionInfo, null);
        }).isInstanceOf(RuntimeException.class);

        verify(target).add("QUERY");


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethodExecution).isSameAs(beforeMethodExecution);

        assertThat(listener.getBeforeQueryExecutionInfo()).isNull();
        assertThat(listener.getAfterQueryExecutionInfo()).isNull();

        assertThat(afterMethodExecution.getResult()).isNull();

        assertThat(afterMethodExecution.getTarget()).isEqualTo(target);
        assertThat(afterMethodExecution.getMethod()).isEqualTo(addMethod);
        assertThat(afterMethodExecution.getMethodArgs()).isEqualTo(args);
        assertThat(afterMethodExecution.getConnectionInfo()).isEqualTo(connectionInfo);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(afterMethodExecution.getThreadName()).isEqualTo(threadName);
        assertThat(afterMethodExecution.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(afterMethodExecution.getExecuteDuration()).isEqualTo(Duration.ZERO);

        assertThat(afterMethodExecution.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_METHOD);

        assertThat(afterMethodExecution.getThrown()).isSameAs(exception);
    }

    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisherVerifyContext() throws Throwable {

        // target method returns Publisher
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        CompositeProxyExecutionListener listener = new CompositeProxyExecutionListener();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        when(this.proxyConfig.getListeners()).thenReturn(listener);

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Result mockResult = MockResult.empty();
        Mono<Result> publisher = Mono.just(mockResult);

        doReturn(publisher).when(target).execute();

        Object result = this.callbackHandlerSupport.proceedExecution(executeMethod, target, args, listener, connectionInfo, null);

        // verify method on target is invoked
        verify(target).execute();

        StepVerifier.create((Publisher<Result>) result, StepVerifierOptions.create().withInitialContext(Context.of("foo", "FOO")))
            .expectSubscription()
            .expectAccessibleContext()
            .contains("foo", "FOO")
            .then()
            .expectNext(mockResult)
            .expectComplete()
            .verify();

    }

    @Test
    void handleCommonMethods() {
        class MyStub implements Wrapped<String> {

            @Override
            public String toString() {
                return "FOO-toString";
            }

            @Override
            public String unwrap() {
                return "unwrapped";  // not used since target object is passed
            }

            @Override
            @SuppressWarnings("unchecked")
            public <E> E unwrap(Class<E> targetClass) {
                return (E) "unwrapped-with-target";
            }

        }
        MyStub target = new MyStub();
        Object result;

        // verify toString()
        result = this.callbackHandlerSupport.handleCommonMethod("toString", target, null, null);
        assertThat(result).isEqualTo("MyStub-proxy [FOO-toString]");

        // verify hashCode()
        result = this.callbackHandlerSupport.handleCommonMethod("hashCode", target, null, null);
        assertThat(result).isEqualTo(target.hashCode());

        // verify equals() with null argument - ".equals(null)"
        result = this.callbackHandlerSupport.handleCommonMethod("equals", target, new Object[]{null}, null);
        assertThat(result).isEqualTo(false);

        // verify equals() with null - ".equals(null)"
        result = this.callbackHandlerSupport.handleCommonMethod("equals", target, new Object[]{null}, null);
        assertThat(result).isEqualTo(false);

        // verify equals() with target - ".equals(target)"
        result = this.callbackHandlerSupport.handleCommonMethod("equals", target, new Object[]{target}, null);
        assertThat(result).isEqualTo(true);

        // verify getProxyConfig()
        result = this.callbackHandlerSupport.handleCommonMethod("getProxyConfig", target, null, null);
        assertThat(result).isEqualTo(this.proxyConfig);

        // verify unwrap()
        result = this.callbackHandlerSupport.handleCommonMethod("unwrap", target, null, null);
        assertThat(result).isSameAs(target);

        // verify unwrap(String.class)
        result = this.callbackHandlerSupport.handleCommonMethod("unwrap", target, new Object[]{String.class}, null);
        assertThat(result).isEqualTo("unwrapped-with-target");

        // verify unwrapConnection()
        Connection mockConnection = mock(Connection.class);
        result = this.callbackHandlerSupport.handleCommonMethod("unwrapConnection", target, null, mockConnection);
        assertThat(result).isSameAs(mockConnection);
    }
    @Test
    void equality() {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        ConnectionFactory original = mock(ConnectionFactory.class);
        ConnectionFactory another = mock(ConnectionFactory.class);
        ConnectionFactory proxy = proxyConfig.getProxyFactory().wrapConnectionFactory(original);
        ConnectionFactory anotherProxy = proxyConfig.getProxyFactory().wrapConnectionFactory(another);

        assertThat(proxy.equals(original)).isTrue();
        assertThat(proxy.equals(another)).isFalse();
        assertThat(proxy.equals(anotherProxy)).isFalse();

        Object result;

        // original.equals(original)
        result = this.callbackHandlerSupport.handleCommonMethod("equals", original, new Object[]{original}, null);
        assertThat(result).isEqualTo(true);

        // original.equals(proxy)
        result = this.callbackHandlerSupport.handleCommonMethod("equals", original, new Object[]{proxy}, null);
        assertThat(result).isEqualTo(true);

        // original.equals(another)
        result = this.callbackHandlerSupport.handleCommonMethod("equals", original, new Object[]{another}, null);
        assertThat(result).isEqualTo(false);

        // original.equals(anotherProxy)
        result = this.callbackHandlerSupport.handleCommonMethod("equals", original, new Object[]{anotherProxy}, null);
        assertThat(result).isEqualTo(false);
    }

    @Test
    void methodInvocationStrategy() throws Throwable {
        // target method returns Batch (not Publisher)
        Method addMethod = ReflectionUtils.findMethod(Batch.class, "add", String.class);
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{"QUERY"};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();

        Object resultMock = new Object();

        AtomicReference<Tuple3<Method, Object, Object[]>> invokedArgumentsHolder = new AtomicReference<>();

        this.callbackHandlerSupport.setMethodInvocationStrategy((invokingMethod, invokingTarget, invokingArgs) -> {
            invokedArgumentsHolder.set(Tuples.of(invokingMethod, invokingTarget, invokingArgs));  // capture args
            return resultMock;
        });

        Object result = this.callbackHandlerSupport.proceedExecution(addMethod, target, args, listener, connectionInfo, null);

        assertThat(result).isSameAs(resultMock);

        Tuple3<Method, Object, Object[]> invokedArguments = invokedArgumentsHolder.get();
        assertThat(invokedArguments.getT1()).isSameAs(addMethod);
        assertThat(invokedArguments.getT2()).isSameAs(target);
        assertThat(invokedArguments.getT3()).isSameAs(args);

        // target should not be invoked since invocation strategy returns resultMock
        verifyNoInteractions(target);

        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethodExecution).isSameAs(beforeMethodExecution);

        assertThat(listener.getBeforeQueryExecutionInfo()).isNull();
        assertThat(listener.getAfterQueryExecutionInfo()).isNull();

        assertThat(afterMethodExecution.getTarget()).isEqualTo(target);
        assertThat(afterMethodExecution.getResult()).isEqualTo(resultMock);
        assertThat(afterMethodExecution.getMethod()).isEqualTo(addMethod);
        assertThat(afterMethodExecution.getMethodArgs()).isEqualTo(args);
        assertThat(afterMethodExecution.getConnectionInfo()).isSameAs(connectionInfo);

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertThat(afterMethodExecution.getThreadName()).isEqualTo(threadName);
        assertThat(afterMethodExecution.getThreadId()).isEqualTo(threadId);

        // since it uses fixed clock that returns same time, duration is 0
        assertThat(afterMethodExecution.getExecuteDuration()).isEqualTo(Duration.ZERO);

        assertThat(afterMethodExecution.getProxyEventType()).isEqualTo(ProxyEventType.AFTER_METHOD);

        assertThat(afterMethodExecution.getThrown()).isNull();
    }

}
