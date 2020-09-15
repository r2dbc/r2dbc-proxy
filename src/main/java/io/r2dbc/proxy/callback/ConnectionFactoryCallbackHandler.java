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

import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

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

        BiFunction<Object, MutableMethodExecutionInfo, Object> onMap = null;

        if ("create".equals(methodName)) {
            // callback for creating connection proxy
            onMap = (resultObj, executionInfo) -> {
                executionInfo.setResult(resultObj);

                Connection connection = (Connection) resultObj;  // original connection
                String connectionId = this.proxyConfig.getConnectionIdManager().getId(connection);

                DefaultConnectionInfo connectionInfo = new DefaultConnectionInfo();
                connectionInfo.setConnectionId(connectionId);
                connectionInfo.setClosed(false);
                connectionInfo.setOriginalConnection(connection);

                executionInfo.setConnectionInfo(connectionInfo);

                Connection proxyConnection = this.proxyConfig.getProxyFactory().wrapConnection(connection, connectionInfo);
                return proxyConnection;
            };
        }

        Object result = proceedExecution(method, this.connectionFactory, args, this.proxyConfig.getListeners(), null, onMap, null);

        if ("create".equals(methodName)) {
            // gh-68:
            // "proceedExecution" returns a Flux that has logic to performs before/after method callbacks.
            // When "usingWhen" is used with "create" method (e.g.: "Mono.usingWhen(connectionFactory.create(), resourceClosure, ...)"),
            // the calling order becomes "before-method", actual "create", "resource-closure", then "after-method".
            // Instead, we want "before-method", actual "create", *"after-method"*, then "resource-closure"
            // By wrapping the Flux with Mono, when a connection is emitted (onNext), the Mono sends "cancel" to the Flux which triggers
            // "doFinally" on Flux and calls "after-method" callback before "resource-closure" is called.
            return Mono.from((Publisher<? extends Connection>) result);
        }

        return result;
    }

}
