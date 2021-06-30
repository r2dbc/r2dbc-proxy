/*
 * Copyright 2018 the original author or authors.
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
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Proxy callback handler for {@link Result}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class ResultCallbackHandler extends CallbackHandlerSupport {

    private final Result result;

    private final MutableQueryExecutionInfo queryExecutionInfo;

    private final QueriesExecutionContext queriesExecutionContext;

    /**
     * Callback handler logic for {@link Result}.
     *
     * This constructor purposely uses {@link QueryExecutionInfo} interface for arguments instead of {@link MutableQueryExecutionInfo} implementation.
     * This way, creator of this callback handler ({@link ProxyFactory}) does not depend on {@link MutableQueryExecutionInfo} implementation.
     *
     * @param result                  query result
     * @param queryExecutionInfo      query execution info
     * @param proxyConfig             proxy config
     * @param queriesExecutionContext queries execution counter
     * @throws IllegalArgumentException if {@code result} is {@code null}
     * @throws IllegalArgumentException if {@code queryExecutionInfo} is {@code null}
     * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
     * @throws IllegalArgumentException if {@code queriesExecutionCounter} is {@code null}
     * @throws IllegalArgumentException if {@code queryExecutionInfo} is not an instance of {@link MutableQueryExecutionInfo}
     */
    public ResultCallbackHandler(Result result, QueryExecutionInfo queryExecutionInfo, ProxyConfig proxyConfig, QueriesExecutionContext queriesExecutionContext) {
        super(proxyConfig);
        this.result = Assert.requireNonNull(result, "result must not be null");
        Assert.requireNonNull(queryExecutionInfo, "queryExecutionInfo must not be null");
        this.queryExecutionInfo = Assert.requireType(queryExecutionInfo, MutableQueryExecutionInfo.class, "queryExecutionInfo must be MutableQueryExecutionInfo");
        Assert.requireNonNull(queriesExecutionContext, "queriesExecutionContext must not be null");
        this.queriesExecutionContext = queriesExecutionContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();
        ConnectionInfo connectionInfo = this.queryExecutionInfo.getConnectionInfo();

        if ("unwrap".equals(methodName)) {  // for Wrapped
            return this.result;
        } else if ("unwrapConnection".equals(methodName)) {  // for ConnectionHolder
            return connectionInfo.getOriginalConnection();
        }

        Object invocationResult = proceedExecution(method, this.result, args, this.proxyConfig.getListeners(), connectionInfo, null);

        if ("map".equals(methodName) || "getRowsUpdated".equals(methodName)) {
            Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer =
                Operators.liftPublisher((pub, subscriber) ->
                    new ResultInvocationSubscriber(subscriber, this.queryExecutionInfo, this.proxyConfig, this.queriesExecutionContext));

            return Flux.from((Publisher<Object>) invocationResult).transform(transformer);
        }

        return invocationResult;
    }
}
