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

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Statement;

import java.util.function.BiFunction;

/**
 * Listener interface that is called when proxy is invoked.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyExecutionListener {

    /**
     * Called before every invocation of methods.
     *
     * @param executionInfo method execution context
     */
    default void beforeMethod(MethodExecutionInfo executionInfo) {
    }

    /**
     * Called after every invocation of methods.
     *
     * @param executionInfo method execution context
     */
    default void afterMethod(MethodExecutionInfo executionInfo) {
    }

    /**
     * Called before execution of query.
     *
     * Query execution is {@link Batch#execute()} or {@link Statement#execute()}.
     *
     * Note: this callback is called when the publisher, result of the {@code execute()}, is being
     * subscribed. Not at the time of {@code execute()} is called,
     *
     * @param execInfo query execution context
     */
    default void beforeQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called after execution of query.
     *
     * Query execution is {@link Batch#execute()} or {@link Statement#execute()}.
     *
     * Note: this callback is called when the publisher, result of the {@code execute()}, is being
     * subscribed. Not at the time of {@code execute()} is called,
     *
     * @param execInfo query execution context
     */
    default void afterQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called on processing each query {@link io.r2dbc.spi.Result}.
     *
     * While processing query results with {@link io.r2dbc.spi.Result#map(BiFunction)}, this callback
     * is called per result.
     * {@link QueryExecutionInfo#getCurrentMappedResult()} contains the mapped result.
     *
     * @param execInfo query execution context
     */
    default void eachQueryResult(QueryExecutionInfo execInfo) {
    }

}
