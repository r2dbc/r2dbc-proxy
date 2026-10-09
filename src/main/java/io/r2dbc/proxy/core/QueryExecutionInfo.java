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

package io.r2dbc.proxy.core;

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

/**
 * Holds information about a query execution.
 *
 * @author Tadaya Tsuyukubo
 */
public interface QueryExecutionInfo {

    /**
     * Returns the method used for the query execution.
     *
     * @return invoked method
     */
    Method getMethod();

    /**
     * Returns the arguments passed to the invocation.
     *
     * This value may be {@code null} when the method is invoked without any arguments.
     *
     * @return argument lists or {@code null} if the invoked method did not take any arguments
     */
    @Nullable
    Object[] getMethodArgs();

    /**
     * Returns the exception thrown by the invocation.
     * For the {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)} callback, or when the query execution
     * did not throw any error, this returns {@code null}.
     *
     * @return thrown exception
     */
    @Nullable
    Throwable getThrowable();

    /**
     * Returns the associated {@link ConnectionInfo}.
     *
     * @return connection info
     */
    ConnectionInfo getConnectionInfo();

    /**
     * Returns whether the query execution was successful.
     * This value is populated only after the query execution completes.
     *
     * A query execution is considered successful when the {@link org.reactivestreams.Publisher}
     * returned from {@link Statement#execute()} either received completion
     * or at least one element is emitted regardless of it has received cancellation.
     *
     * @return true when query has successfully executed
     */
    boolean isSuccess();

    /**
     * Returns the size of the batch query.
     *
     * In other words, this is the number of calls to {@link Batch#add(String)}.
     *
     * @return batch size
     */
    int getBatchSize();

    /**
     * Returns the list of {@link QueryInfo}.
     *
     * @return the list of queries; never {@code null}
     */
    List<QueryInfo> getQueries();

    /**
     * Returns the type of query execution.
     *
     * @return type of query execution
     */
    ExecutionType getType();

    /**
     * Returns the number of bindings.
     *
     * In other words, this is the number of calls to {@link Statement#add()}.
     *
     * @return size of the binding
     */
    int getBindingsSize();

    /**
     * Returns the time spent executing the query.
     * <p>
     * Duration is only populated in appropriate phase.
     * (e.g.: {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)})
     *
     * @return query execution duration
     */
    Duration getExecuteDuration();


    /**
     * Returns the name of the thread executing the query.
     *
     * @return thread name
     */
    String getThreadName();

    /**
     * Returns the ID of the thread executing the query.
     *
     * @return thread ID
     */
    long getThreadId();


    /**
     * Returns the proxy event type for the query execution.
     *
     * @return proxy event type; one of {@link ProxyEventType#BEFORE_QUERY}, {@link ProxyEventType#AFTER_QUERY},
     * or {@link ProxyEventType#EACH_QUERY_RESULT}
     */
    ProxyEventType getProxyEventType();

    /**
     * Represents the Nth {@link io.r2dbc.spi.Result}.
     *
     * For each query-result callback ({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}),
     * this value indicates the Nth {@link Result}, starting from 1 (first, second, third, and so on).
     *
     * This returns 0 before query execution ({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}).
     * For after-query execution ({@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), it returns the
     * total number of {@link io.r2dbc.spi.Result} objects returned by this query execution.
     *
     * @return Nth number of query result
     */
    int getCurrentResultCount();


    /**
     * The mapped query result available for each query-result callback ({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}).
     *
     * For the before- and after-query execution callbacks ({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}
     * and {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), this returns {@code null}.
     *
     * @return currently mapped result
     */
    @Nullable
    Object getCurrentMappedResult();

    /**
     * Returns the {@link ValueStore} associated with the scope of the before/after query execution.
     *
     * Mainly used for passing values between {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)} and
     * {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}.
     *
     * @return value store
     */
    ValueStore getValueStore();

}
