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
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link ProxyFactory} implementation using JDK dynamic proxy.
 *
 * @author Tadaya Tsuyukubo
 */
public class JdkProxyFactory implements ProxyFactory {

    private ProxyConfig proxyConfig;

    @Override
    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public ConnectionFactory createProxyConnectionFactory(ConnectionFactory connectionFactory) {
        return (ConnectionFactory) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{ConnectionFactory.class, Wrapped.class},
            new ConnectionFactoryInvocationHandler(connectionFactory, this.proxyConfig));
    }

    @Override
    public Connection createProxyConnection(Connection connection, ConnectionInfo connectionInfo) {
        return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Connection.class, Wrapped.class, ConnectionHolder.class},
            new ConnectionInvocationHandler(connection, connectionInfo, this.proxyConfig));
    }

    @Override
    public Batch<?> createProxyBatch(Batch<?> batch, ConnectionInfo connectionInfo) {
        return (Batch<?>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Batch.class, Wrapped.class, ConnectionHolder.class},
            new BatchInvocationHandler(batch, connectionInfo, this.proxyConfig));
    }

    @Override
    public Statement<?> createProxyStatement(Statement<?> statement, String query, ConnectionInfo connectionInfo) {
        return (Statement<?>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Statement.class, Wrapped.class, ConnectionHolder.class},
            new StatementInvocationHandler(statement, query, connectionInfo, this.proxyConfig));
    }

    @Override
    public Result createProxyResult(Result result, QueryExecutionInfo queryExecutionInfo) {
        return (Result) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Result.class, Wrapped.class, ConnectionHolder.class},
            new ResultInvocationHandler(result, queryExecutionInfo, this.proxyConfig));
    }

    static class ConnectionFactoryInvocationHandler implements InvocationHandler {

        private ReactiveConnectionFactoryCallback delegate;

        public ConnectionFactoryInvocationHandler(ConnectionFactory connectionFactory, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    static class ConnectionInvocationHandler implements InvocationHandler {

        private ReactiveConnectionCallback delegate;

        public ConnectionInvocationHandler(Connection connection, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    static class BatchInvocationHandler implements InvocationHandler {

        private ReactiveBatchCallback delegate;

        public BatchInvocationHandler(Batch<?> batch, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveBatchCallback(batch, connectionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    static class StatementInvocationHandler implements InvocationHandler {

        private ReactiveStatementCallback delegate;

        public StatementInvocationHandler(Statement<?> statement, String query, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveStatementCallback(statement, query, connectionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    static class ResultInvocationHandler implements InvocationHandler {

        private ReactiveResultCallback delegate;

        public ResultInvocationHandler(Result result, QueryExecutionInfo queryExecutionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveResultCallback(result, queryExecutionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

}
