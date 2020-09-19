/*
 * Copyright 2018-2020 the original author or authors.
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
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.proxy.listener.LifeCycleListener;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ConnectionFactoryCallbackHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ConnectionFactoryCallbackHandlerTest {

    private static Method CREATE_METHOD = ReflectionUtils.findMethod(ConnectionFactory.class, "create");

    private static Method GET_METADATA_METHOD = ReflectionUtils.findMethod(ConnectionFactory.class, "getMetadata");

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    private static Method GET_PROXY_CONFIG_METHOD = ReflectionUtils.findMethod(ProxyConfigHolder.class, "getProxyConfig");

    @Test
    void createConnection() throws Throwable {

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection originalConnection = mock(Connection.class);
        Connection mockedConnection = mock(Connection.class);
        ConnectionIdManager idManager = mock(ConnectionIdManager.class);
        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);
        when(proxyFactoryFactory.create(any())).thenReturn(proxyFactory);

        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        // mock call to original ConnectionFactory#create()
        doReturn(Mono.just(originalConnection)).when(connectionFactory).create();

        String connectionId = "100";
        when(idManager.getId(originalConnection)).thenReturn(connectionId);

        // mock where it creates proxied connection
        when(proxyFactory.wrapConnection(any(Connection.class), any(ConnectionInfo.class))).thenReturn(mockedConnection);


        ProxyConfig proxyConfig = ProxyConfig.builder()
            .proxyFactoryFactory(proxyFactoryFactory)
            .connectionIdManager(idManager)
            .listener(listener)
            .build();

        ConnectionFactoryCallbackHandler callback = new ConnectionFactoryCallbackHandler(connectionFactory, proxyConfig);

        Object result = callback.invoke(connectionFactory, CREATE_METHOD, null);

        assertThat(result).isInstanceOf(Publisher.class);

        StepVerifier.create((Publisher<?>) result)
            .expectSubscription()
            .assertNext(object -> {
                assertThat(object).isSameAs(mockedConnection);
            })
            .verifyComplete();

        MethodExecutionInfo afterMethod = listener.getAfterMethodExecutionInfo();
        assertThat(afterMethod).isNotNull();
        ConnectionInfo connectionInfo = afterMethod.getConnectionInfo();
        assertThat(connectionInfo).isNotNull();
        assertThat(connectionInfo.getConnectionId()).isEqualTo(connectionId);
        assertThat(connectionInfo.isClosed()).isFalse();
        assertThat(connectionInfo.getOriginalConnection()).isSameAs(originalConnection);

        assertThat(afterMethod.getTarget()).isSameAs(connectionFactory);
        assertThat(afterMethod.getResult()).isSameAs(originalConnection);
    }

    @Test
    void getMetadata() throws Throwable {

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ConnectionFactoryMetadata metadata = mock(ConnectionFactoryMetadata.class);

        when(connectionFactory.getMetadata()).thenReturn(metadata);

        ProxyConfig proxyConfig = new ProxyConfig();
        ConnectionFactoryCallbackHandler callback = new ConnectionFactoryCallbackHandler(connectionFactory, proxyConfig);

        Object result = callback.invoke(connectionFactory, GET_METADATA_METHOD, null);

        assertThat(result).isSameAs(metadata);

    }

    @Test
    void unwrap() throws Throwable {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ProxyConfig proxyConfig = new ProxyConfig();

        ConnectionFactoryCallbackHandler callback = new ConnectionFactoryCallbackHandler(connectionFactory, proxyConfig);

        Object result = callback.invoke(connectionFactory, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(connectionFactory);
    }

    // gh-68
    @Test
    void createConnectionWithUsingWhen() throws Throwable {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection mockedConnection = mock(Connection.class);
        doReturn(Mono.just(mockedConnection)).when(connectionFactory).create();

        List<String> list = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Object> createdConnectionHolder = new AtomicReference<>();

        LifeCycleListener listener = new LifeCycleListener() {

            @Override
            public void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
                list.add("listener-before-create");
            }

            @Override
            public void afterCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
                createdConnectionHolder.set(methodExecutionInfo.getResult());
                list.add("listener-after-create");
            }
        };

        Function<? super Connection, ? extends Mono<String>> resourceClosure = (conn) -> {
            list.add("resource-closure");
            return Mono.just("foo");
        };
        Function<? super Connection, ? extends Publisher<?>> asyncComplete = (conn) -> {
            list.add("async-complete");
            return Mono.empty();
        };
        BiFunction<? super Connection, ? super Throwable, ? extends Publisher<?>> asyncError = (conn, thr) -> {
            list.add("async-error");
            return Mono.empty();
        };
        Function<? super Connection, ? extends Publisher<?>> asyncCancel = (conn) -> {
            list.add("resource-cancel");
            return Mono.empty();
        };

        ProxyConfig proxyConfig = ProxyConfig.builder()
            .listener(listener)
            .build();

        ProxyFactory proxyFactory = proxyConfig.getProxyFactory();
        ConnectionFactory proxyConnectionFactory = proxyFactory.wrapConnectionFactory(connectionFactory);

        // Mono#usingWhen
        Mono<String> mono = Mono.usingWhen(proxyConnectionFactory.create(), resourceClosure, asyncComplete, asyncError, asyncCancel);
        StepVerifier.create(mono)
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete();

        assertThat(list).containsExactly("listener-before-create", "listener-after-create", "resource-closure", "async-complete");
        assertThat(createdConnectionHolder).hasValue(mockedConnection);

        list.clear();
        createdConnectionHolder.set(null);

        // Flux#usingWhen
        Flux<String> flux = Flux.usingWhen(proxyConnectionFactory.create(), resourceClosure, asyncComplete, asyncError, asyncCancel);
        StepVerifier.create(flux)
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete();

        assertThat(list).containsExactly("listener-before-create", "listener-after-create", "resource-closure", "async-complete");
        assertThat(createdConnectionHolder).hasValue(mockedConnection);
    }

    @Test
    void getProxyConfig() throws Throwable {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ProxyConfig proxyConfig = new ProxyConfig();

        ConnectionFactoryCallbackHandler callback = new ConnectionFactoryCallbackHandler(connectionFactory, proxyConfig);

        Object result = callback.invoke(connectionFactory, GET_PROXY_CONFIG_METHOD, null);
        assertThat(result).isSameAs(proxyConfig);
    }

}
