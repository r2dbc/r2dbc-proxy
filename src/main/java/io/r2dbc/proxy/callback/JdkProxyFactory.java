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
import io.r2dbc.proxy.util.Assert;
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
 * @see JdkProxyFactoryFactory
 */
public class JdkProxyFactory implements ProxyFactory {

    private ProxyConfig proxyConfig;

    /**
     * Constructor
     *
     * @param proxyConfig proxyConfig
     * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
     */
    JdkProxyFactory(ProxyConfig proxyConfig) {
        Assert.requireNonNull(proxyConfig, "proxyConfig must not be null");

        this.proxyConfig = proxyConfig;
    }

    @Override
    public ConnectionFactory wrapConnectionFactory(ConnectionFactory connectionFactory) {
        Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");

        CallbackHandler logic = new ConnectionFactoryCallbackHandler(connectionFactory, this.proxyConfig);
        CallbackInvocationHandler invocationHandler = new CallbackInvocationHandler(logic);
        return (ConnectionFactory) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{ConnectionFactory.class, Wrapped.class}, invocationHandler);
    }

    @Override
    public Connection wrapConnection(Connection connection, ConnectionInfo connectionInfo) {
        Assert.requireNonNull(connection, "connection must not be null");
        Assert.requireNonNull(connectionInfo, "connectionInfo must not be null");

        CallbackHandler logic = new ConnectionCallbackHandler(connection, connectionInfo, this.proxyConfig);
        CallbackInvocationHandler invocationHandler = new CallbackInvocationHandler(logic);
        return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Connection.class, Wrapped.class, ConnectionHolder.class}, invocationHandler);
    }

    @Override
    public Batch<?> wrapBatch(Batch<?> batch, ConnectionInfo connectionInfo) {
        Assert.requireNonNull(batch, "batch must not be null");
        Assert.requireNonNull(connectionInfo, "connectionInfo must not be null");

        CallbackHandler logic = new BatchCallbackHandler(batch, connectionInfo, this.proxyConfig);
        CallbackInvocationHandler invocationHandler = new CallbackInvocationHandler(logic);
        return (Batch<?>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Batch.class, Wrapped.class, ConnectionHolder.class}, invocationHandler);
    }

    @Override
    public Statement<?> wrapStatement(Statement<?> statement, String query, ConnectionInfo connectionInfo) {
        Assert.requireNonNull(statement, "statement must not be null");
        Assert.requireNonNull(query, "query must not be null");
        Assert.requireNonNull(connectionInfo, "connectionInfo must not be null");

        CallbackHandler logic = new StatementCallbackHandler(statement, query, connectionInfo, this.proxyConfig);
        CallbackInvocationHandler invocationHandler = new CallbackInvocationHandler(logic);
        return (Statement<?>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Statement.class, Wrapped.class, ConnectionHolder.class}, invocationHandler);
    }

    @Override
    public Result wrapResult(Result result, QueryExecutionInfo queryExecutionInfo) {
        Assert.requireNonNull(result, "result must not be null");
        Assert.requireNonNull(queryExecutionInfo, "queryExecutionInfo must not be null");

        CallbackHandler logic = new ResultCallbackHandler(result, queryExecutionInfo, this.proxyConfig);
        CallbackInvocationHandler invocationHandler = new CallbackInvocationHandler(logic);
        return (Result) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Result.class, Wrapped.class, ConnectionHolder.class}, invocationHandler);
    }


    /**
     * {@link InvocationHandler} implementation that delegates to {@link CallbackHandler}.
     */
    static class CallbackInvocationHandler implements InvocationHandler {

        private CallbackHandler delegate;

        public CallbackInvocationHandler(CallbackHandler delegate) {
            Assert.requireNonNull(delegate, "delegate must not be null");

            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return this.delegate.invoke(proxy, method, args);
        }
    }

}
