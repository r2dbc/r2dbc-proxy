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
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.listener.LastExecutionAwareListener;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import reactor.util.context.ContextView;

/**
 * Test for {@link ConnectionCallbackHandler}.
 *
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

    private static Method GET_PROXY_CONFIG_METHOD = ReflectionUtils.findMethod(ProxyConfigHolder.class, "getProxyConfig");

    @Test
    void createBatch() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);
        when(proxyFactoryFactory.create(any())).thenReturn(proxyFactory);

        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).proxyFactoryFactory(proxyFactoryFactory).build();

        Batch originalBatch = mock(Batch.class);
        Batch resultBatch = mock(Batch.class);
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
        ContextView contextView = mock(ContextView.class);
        connectionInfo.getValueStore().put(ContextView.class, contextView);

        String query = "MY-QUERY";

        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).proxyFactoryFactory(proxyFactoryFactory).build();

        Statement originalStatement = mock(Statement.class);
        Statement resultStatement = mock(Statement.class);
        doReturn(originalStatement).when(connection).createStatement(query);

        ArgumentCaptor<StatementInfo> statementInfoCaptor = ArgumentCaptor.forClass(StatementInfo.class);

        doReturn(resultStatement).when(proxyFactory).wrapStatement(eq(originalStatement), statementInfoCaptor.capture(), eq(connectionInfo));

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, CREATE_STATEMENT_METHOD, new Object[]{query});

        assertThat(result).isSameAs(resultStatement);

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertThat(executionInfo.getResult()).isEqualTo(originalStatement);

        assertThat(statementInfoCaptor.getValue().getValueStore().get(ContextView.class)).isEqualTo(contextView);
    }

    @Test
    @SuppressWarnings("unchecked")
    void beginTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

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
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

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
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

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
        ProxyConfig proxyConfig = ProxyConfig.builder().listener(listener).build();

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

    @Test // relates to issue-14
    @SuppressWarnings("unchecked")
    void closeWithCustomConnectionInfo() throws Throwable {
        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);  // non DefaultConnectionInfo implementation
        ProxyConfig proxyConfig = new ProxyConfig();

        when(connection.close()).thenReturn(Mono.empty());

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, CLOSE_METHOD, null);
        StepVerifier.create((Publisher<Void>) result)
            .expectSubscription()
            // since it is a Publisher<Void>, no steps for assertNext
            .verifyComplete();

        //  "setClosed" should be called on non DefaultConnectionInfo implementation
        verify(connectionInfo).setClosed(true);
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

    @Test
    void getProxyConfig() throws Throwable {
        Connection connection = mock(Connection.class);
        DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        ConnectionCallbackHandler callback = new ConnectionCallbackHandler(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(connection, GET_PROXY_CONFIG_METHOD, null);
        assertThat(result).isSameAs(proxyConfig);
    }

}
