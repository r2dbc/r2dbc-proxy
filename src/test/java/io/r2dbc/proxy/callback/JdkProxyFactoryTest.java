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
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.proxy.test.MockStatementInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockBatch;
import io.r2dbc.spi.test.MockConnection;
import io.r2dbc.spi.test.MockConnectionFactory;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link JdkProxyFactory}.
 *
 * @author Tadaya Tsuyukubo
 */
public class JdkProxyFactoryTest {

    private JdkProxyFactory proxyFactory;

    @BeforeEach
    void setUp() {
        ProxyConfig proxyConfig = new ProxyConfig();
        JdkProxyFactory jdkProxyFactory = new JdkProxyFactory(proxyConfig);

        // solve the circular reference between ProxyConfig and JdkProxyFactory
        ProxyFactoryFactory proxyFactoryFactory = mock(ProxyFactoryFactory.class);
        when(proxyFactoryFactory.create(proxyConfig)).thenReturn(jdkProxyFactory);
        proxyConfig.setProxyFactoryFactory(proxyFactoryFactory);

        this.proxyFactory = jdkProxyFactory;
    }

    @Test
    void isProxy() {
        ConnectionFactory connectionFactory = MockConnectionFactory.empty();
        Connection connection = MockConnection.empty();
        Batch batch = MockBatch.empty();
        Statement statement = MockStatement.empty();
        Result result = MockResult.empty();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        StatementInfo statementInfo = MockStatementInfo.empty();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();  // need to be mutable
        QueriesExecutionContext queriesExecutionContext = mock(QueriesExecutionContext.class);
        Object wrapped;

        wrapped = this.proxyFactory.wrapConnectionFactory(connectionFactory);
        assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
        assertThat(wrapped).isInstanceOf(Wrapped.class);
        assertThat(wrapped).isNotInstanceOf(ConnectionHolder.class);
        assertThat(wrapped).isInstanceOf(ProxyConfigHolder.class);

        wrapped = this.proxyFactory.wrapConnection(connection, connectionInfo);
        assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
        assertThat(wrapped).isInstanceOf(Wrapped.class);
        assertThat(wrapped).isInstanceOf(ConnectionHolder.class);
        assertThat(wrapped).isInstanceOf(ProxyConfigHolder.class);

        wrapped = this.proxyFactory.wrapBatch(batch, connectionInfo);
        assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
        assertThat(wrapped).isInstanceOf(Wrapped.class);
        assertThat(wrapped).isInstanceOf(ConnectionHolder.class);
        assertThat(wrapped).isInstanceOf(ProxyConfigHolder.class);

        wrapped = this.proxyFactory.wrapStatement(statement, statementInfo, connectionInfo);
        assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
        assertThat(wrapped).isInstanceOf(Wrapped.class);
        assertThat(wrapped).isInstanceOf(ConnectionHolder.class);
        assertThat(wrapped).isInstanceOf(ProxyConfigHolder.class);

        wrapped = this.proxyFactory.wrapResult(result, queryExecutionInfo, queriesExecutionContext);
        assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
        assertThat(wrapped).isInstanceOf(Wrapped.class);
        assertThat(wrapped).isInstanceOf(ConnectionHolder.class);
        assertThat(wrapped).isInstanceOf(ProxyConfigHolder.class);
    }

    @Test
    void testToString() {
        ConnectionFactory connectionFactory = MockConnectionFactory.empty();
        Connection connection = MockConnection.empty();
        Batch batch = MockBatch.empty();
        Statement statement = MockStatement.empty();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        StatementInfo statementInfo = MockStatementInfo.empty();

        String expected;
        Object result;

        result = this.proxyFactory.wrapConnectionFactory(connectionFactory);
        expected = getExpectedToString(connectionFactory);
        assertThat(result.toString()).isEqualTo(expected);

        result = this.proxyFactory.wrapConnection(connection, connectionInfo);
        expected = getExpectedToString(connection);
        assertThat(result.toString()).isEqualTo(expected);

        result = this.proxyFactory.wrapBatch(batch, connectionInfo);
        expected = getExpectedToString(batch);
        assertThat(result.toString()).isEqualTo(expected);

        result = this.proxyFactory.wrapStatement(statement, statementInfo, connectionInfo);
        expected = getExpectedToString(statement);
        assertThat(result.toString()).isEqualTo(expected);

    }

    @Test
    void noThreadContextClassloader() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<Exception> exHolder = new AtomicReference<>();
        Runnable logic = () -> {
            try {
                this.proxyFactory.wrapConnectionFactory(MockConnectionFactory.empty());
            } catch (Exception ex) {
                exHolder.set(ex);
            }
            latch.countDown();
        };

        ClassLoader classLoader = mock(ClassLoader.class);
        Thread thread = new Thread(logic);
        thread.setContextClassLoader(classLoader);
        thread.start();

        latch.await();

        assertThat(exHolder.get()).isNull();
        verifyNoInteractions(classLoader);
    }

    private String getExpectedToString(Object target) {
        return target.getClass().getSimpleName() + "-proxy [" + target.toString() + "]";
    }
}
