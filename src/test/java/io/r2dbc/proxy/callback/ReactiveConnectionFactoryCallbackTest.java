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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ReactiveConnectionFactoryCallbackTest {

    private static Method CREATE_METHOD = ReflectionUtils.findMethod(ConnectionFactory.class, "create");

    private static Method GET_METADATA_METHOD = ReflectionUtils.findMethod(ConnectionFactory.class, "getMetadata");

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    @Test
    void createConnection() throws Throwable {

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection originalConnection = mock(Connection.class);
        Connection mockedConnection = mock(Connection.class);
        ConnectionIdManager idManager = mock(ConnectionIdManager.class);
        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        // mock call to original ConnectionFactory#create()
        doReturn(Mono.just(originalConnection)).when(connectionFactory).create();

        String connectionId = "100";
        when(idManager.getId(originalConnection)).thenReturn(connectionId);

        // mock where it creates proxied connection
        when(proxyFactory.createProxyConnection(any(Connection.class), any(ConnectionInfo.class))).thenReturn(mockedConnection);


        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setConnectionIdManager(idManager);
        proxyConfig.setProxyFactory(proxyFactory);
        proxyConfig.addListener(listener);

        ReactiveConnectionFactoryCallback callback = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);

        Object result = callback.invoke(null, CREATE_METHOD, null);

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
        ReactiveConnectionFactoryCallback callback = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);

        Object result = callback.invoke(null, GET_METADATA_METHOD, null);

        assertThat(result).isSameAs(metadata);

    }

    @Test
    void unwrap() throws Throwable {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ProxyConfig proxyConfig = new ProxyConfig();

        ReactiveConnectionFactoryCallback callback = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);

        Object result = callback.invoke(null, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(connectionFactory);
    }

}
