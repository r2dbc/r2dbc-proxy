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
import reactor.core.publisher.Hooks;

/**
 * Listener interface that is called when proxy is invoked.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyExecutionListener {

    /**
     * Called before every invocation of methods.
     * <p>
     * Exception Handling:
     * Exceptions thrown by this method are dropped and do not affect the original subscription's publisher flow.
     * Such exceptions are reported to {@link Hooks#onErrorDropped(java.util.function.Consumer)}.
     *
     * @param executionInfo method execution context
     */
    default void beforeMethod(MethodExecutionInfo executionInfo) {
    }

    /**
     * Called after every invocation of methods.
     * <p>
     * Exception Handling:
     * Exceptions thrown by this method are dropped and do not affect the original subscription's publisher flow.
     * Such exceptions are reported to {@link Hooks#onErrorDropped(java.util.function.Consumer)}.
     *
     * @param executionInfo method execution context
     */
    default void afterMethod(MethodExecutionInfo executionInfo) {
    }

    /**
     * Called before executing a query ({@link Batch#execute()} or {@link Statement#execute()}).
     * <p>
     * Note: this callback is called when the publisher, result of the {@code execute()}, is being
     * subscribed. Not at the time of {@code execute()} is called,
     * <p>
     * Exception Handling:
     * Exceptions thrown by this method are dropped and do not affect the original subscription's publisher flow.
     * Such exceptions are reported to {@link Hooks#onErrorDropped(java.util.function.Consumer)}.
     *
     * @param execInfo query execution context
     */
    default void beforeQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called after executing a query ({@link Batch#execute()} or {@link Statement#execute()}).
     * <p>
     * The callback order is:
     * <ul>
     *     <li>{@link #beforeQuery(QueryExecutionInfo)}
     *     <li>{@link #eachQueryResult(QueryExecutionInfo)} for 1st result
     *     <li>{@link #eachQueryResult(QueryExecutionInfo)} for 2nd result
     *     <li>...
     *     <li>{@link #eachQueryResult(QueryExecutionInfo)} for Nth result
     *     <li>{@link #afterQuery(QueryExecutionInfo)}
     * </ul>
     * {@link QueryExecutionInfo#getExecuteDuration()} is available in this callback and it holds
     * the duration since {@link #beforeQuery(QueryExecutionInfo)}.
     * <p>
     * Note: this callback is called when the publisher, result of the {@code execute()}, is being
     * subscribed. Not at the time of {@code execute()} is called,
     * <p>
     * Exception Handling:
     * Exceptions thrown by this method are dropped and do not affect the original subscription's publisher flow.
     * Such exceptions are reported to {@link Hooks#onErrorDropped(java.util.function.Consumer)}.
     *
     * @param execInfo query execution context
     */
    default void afterQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called on processing each query {@link io.r2dbc.spi.Result}.
     * <p>
     * While processing query results {@link io.r2dbc.spi.Result}, this callback
     * is called per result.
     * <p>
     * {@link QueryExecutionInfo#getCurrentMappedResult()} contains the mapped result.
     * <p>
     * Exception Handling:
     * Exceptions thrown by this method are dropped and do not affect the original subscription's publisher flow.
     * Such exceptions are reported to {@link Hooks#onErrorDropped(java.util.function.Consumer)}.
     *
     * @param execInfo query execution context
     */
    default void eachQueryResult(QueryExecutionInfo execInfo) {
    }

}
