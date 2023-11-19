/*
 * Copyright 2018-2023 the original author or authors.
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
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
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

        // replace mapping function
        boolean isMapRowMethod = "map".equals(methodName);
        if (isMapRowMethod) {
            if (args[0] instanceof BiFunction) {
                args[0] = createMappingForMap((BiFunction<Row, RowMetadata, ?>) args[0]);
            } else {
                args[0] = createMappingForMap((Function<? super Readable, ?>) args[0]);
            }
        } else if ("flatMap".equals(methodName)) {
            args[0] = createMappingForFlatMap((Function<Result.Segment, Publisher<?>>) args[0]);
        }

        Object invocationResult = proceedExecution(method, this.result, args, this.proxyConfig.getListeners(), connectionInfo, null);

        if (isMapRowMethod || "flatMap".equals(methodName) || "getRowsUpdated".equals(methodName)) {
            Function<? super Publisher<Object>, ? extends Publisher<Object>> transformer =
                Operators.liftPublisher((pub, subscriber) ->
                    new ResultInvocationSubscriber(subscriber, this.queryExecutionInfo, this.proxyConfig, this.queriesExecutionContext));
            return Flux.from((Publisher<Object>) invocationResult).transform(transformer);
        }

        return invocationResult;
    }

    // for Result#map(Function)
    private Function<? super Readable, ?> createMappingForMap(Function<? super Readable, ?> mapping) {
        return (readable) -> {
            if (readable instanceof Row) {
                Row rowProxy = this.proxyConfig.getProxyFactory().wrapRow((Row) readable, this.queryExecutionInfo);
                return mapping.apply(rowProxy);
            }
            return mapping.apply(readable);
        };
    }

    // for Result#map(BiFunction)
    private BiFunction<Row, RowMetadata, ?> createMappingForMap(BiFunction<Row, RowMetadata, ?> mapping) {
        return (row, rowMetadata) -> {
            Row rowProxy = this.proxyConfig.getProxyFactory().wrapRow(row, this.queryExecutionInfo);
            return mapping.apply(rowProxy, rowMetadata);
        };
    }

    // for Result#flatMap(Function)
    private Function<Result.Segment, Publisher<?>> createMappingForFlatMap(Function<Result.Segment, Publisher<?>> mapping) {
        return (segment) -> {
            if (segment instanceof Result.RowSegment) {
                Result.RowSegment rowSegmentProxy = this.proxyConfig.getProxyFactory().wrapRowSegment((Result.RowSegment)segment, this.queryExecutionInfo);
                return mapping.apply(rowSegmentProxy);
            }
            return mapping.apply(segment);
        };
    }

    ;
}
