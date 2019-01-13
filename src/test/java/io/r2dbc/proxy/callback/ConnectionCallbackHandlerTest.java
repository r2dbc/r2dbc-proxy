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

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
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
public class ConnectionCallbackHandlerTest {

    private static Method CREATE_BATCH_METHOD = ReflectionUtils.findMethod(Connection.class, "createBatch");

    private static Method CREATE_STATEMENT_METHOD = ReflectionUtils.findMethod(Connection.class, "createStatement", String.class);

    private static Method BEGIN_TRANSACTION_METHOD = ReflectionUtils.findMethod(Connection.class, "beginTransaction");

    private static Method COMMIT_TRANSACTION_METHOD = ReflectionUtils.findMethod(Connection.class, "commitTransaction");

    private static Method ROLLBACK_TRANSACTION_METHOD = ReflectionUtils.findMethod(Connection.class, "rollbackTransaction");

    private static Method CLOSE_METHOD = ReflectionUtils.findMethod(Connection.class, "close");

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    @Test
    void createBatch() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);
        when(proxyFactoryFactory.create(any())).thenReturn(proxyFactory);

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);
        proxyConfig.setProxyFactoryFactory(proxyFactoryFactory);

        Batch<?> originalBatch = mock(Batch.class);
        Batch<?> resultBatch = mock(Batch.class);
        doReturn(originalBatch).when(connection).createBatch();

        doReturn(resultBatch).when(proxyFactory).wrapBatch(originalBatch, connectionInfo);

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, CREATE_BATCH_METHOD, null);

        assertThat(result).isSameAs(resultBatch);

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getResult()).isEqualTo(originalBatch);
    }

    @Test
    void createStatement() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);
        when(proxyFactoryFactory.create(any())).thenReturn(proxyFactory);

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();

        String query = "MY-QUERY";

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);
        proxyConfig.setProxyFactoryFactory(proxyFactoryFactory);

        Statement<?> originalStatement = mock(Statement.class);
        Statement<?> resultStatement = mock(Statement.class);
        doReturn(originalStatement).when(connection).createStatement(query);

        doReturn(resultStatement).when(proxyFactory).wrapStatement(originalStatement, query, connectionInfo);

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, CREATE_STATEMENT_METHOD, new Object[]{query});

        assertThat(result).isSameAs(resultStatement);

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getResult()).isEqualTo(originalStatement);
    }

    @Test
    @SuppressWarnings("unchecked")
    void beginTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.beginTransaction()).thenReturn(Mono.empty());

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, BEGIN_TRANSACTION_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
            .expectSubscription()
            // since it is a Publisher<Void>, no steps for assertNext
            .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getConnectionInfo()).isSameAs(connectionInfo);

        assertThat(connectionInfo.getTransactionCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void commitTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.commitTransaction()).thenReturn(Mono.empty());

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, COMMIT_TRANSACTION_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
            .expectSubscription()
            // since it is a Publisher<Void>, no steps for assertNext
            .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getConnectionInfo()).isSameAs(connectionInfo);

        assertThat(connectionInfo.getCommitCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rollbackTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.rollbackTransaction()).thenReturn(Mono.empty());

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, ROLLBACK_TRANSACTION_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
            .expectSubscription()
            // since it is a Publisher<Void>, no steps for assertNext
            .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getConnectionInfo()).isSameAs(connectionInfo);

        assertThat(connectionInfo.getRollbackCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void close() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.close()).thenReturn(Mono.empty());

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, CLOSE_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
            .expectSubscription()
            // since it is a Publisher<Void>, no steps for assertNext
            .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getConnectionInfo()).isSameAs(connectionInfo);

        assertThat(connectionInfo.isClosed()).isTrue();
    }

    @Test
    void unwrap() throws Throwable {
        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(connection);
    }

}
