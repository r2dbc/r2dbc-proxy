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

import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * Proxy callback handler for {@link ConnectionFactory}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class ConnectionFactoryCallbackHandler extends CallbackHandlerSupport {

    private final ConnectionFactory connectionFactory;

    public ConnectionFactoryCallbackHandler(ConnectionFactory connectionFactory, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.connectionFactory = Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();

        if ("unwrap".equals(methodName)) {
            return this.connectionFactory;
        }

        if ("create".equals(methodName)) {
            Object target = this.connectionFactory;
            StopWatch stopWatch = new StopWatch(this.proxyConfig.getClock());

            // Method execution info
            // Since Connection is not yet created, do not set ConnectionInfo
            MutableMethodExecutionInfo executionInfo = new MutableMethodExecutionInfo();
            executionInfo.setMethod(method);
            executionInfo.setMethodArgs(args);
            executionInfo.setTarget(target);

            Publisher<?> result = (Publisher<?>) this.methodInvocationStrategy.invoke(method, target, args);

            // gh-68: Use special operator dedicated to "ConnectionFactory#create" method.
            // Normally, method that returns a Publisher uses "proceedExecution(...)" from parent class. This method returns
            // a "[Mono|Flux]MethodInvocation" that have logic to performs before/after method callbacks.
            // However, when "ConnectionFactory#create" is used with "usingWhen",
            // (e.g.: "Mono.usingWhen(connectionFactory.create(), resourceClosure, ...)"), the calling order becomes
            // ["before-method", actual "create", "resource-closure", "after-method"].
            // Instead, we want ["before-method", actual "create", *"after-method"*, "resource-closure"]
            // Therefore, here uses special mono operator that does not invoke "afterMethod" in "onComplete".
            // Then, use "doOnSuccess()" to call "afterMethod" callback. This way, "after-method" is performed
            // before "resource-closure"
            return new MonoMethodInvocationConnectionFactoryCreate(Mono.from(result), executionInfo, proxyConfig)
                .map(resultObj -> {
                    // set produced object as result
                    executionInfo.setResult(resultObj);

                    // construct ConnectionInfo and returns proxy Connection
                    Connection connection = (Connection) resultObj;  // original connection
                    String connectionId = this.proxyConfig.getConnectionIdManager().getId(connection);

                    DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
                    connectionInfo.setConnectionId(connectionId);
                    connectionInfo.setClosed(false);
                    connectionInfo.setOriginalConnection(connection);
                    executionInfo.setConnectionInfo(connectionInfo);

                    Connection proxyConnection = this.proxyConfig.getProxyFactory().wrapConnection(connection, connectionInfo);
                    return proxyConnection;
                })
                .doOnSuccess((o) -> {
                    // invoke "afterMethod" callback
                    executionInfo.setExecuteDuration(stopWatch.getElapsedDuration());
                    executionInfo.setThreadName(Thread.currentThread().getName());
                    executionInfo.setThreadId(Thread.currentThread().getId());
                    executionInfo.setProxyEventType(ProxyEventType.AFTER_METHOD);
                    this.proxyConfig.getListeners().afterMethod(executionInfo);
                });
        }

        Object result = proceedExecution(method, this.connectionFactory, args, this.proxyConfig.getListeners(), null, null);
        return result;
    }

}
