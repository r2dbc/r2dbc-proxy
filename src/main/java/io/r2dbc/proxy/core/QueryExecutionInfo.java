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

package io.r2dbc.proxy.core;

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

/**
 * Hold query execution related information.
 *
 * @author Tadaya Tsuyukubo
 */
public interface QueryExecutionInfo {

    /**
     * Get the invoked query execution {@code Method}.
     *
     * @return invoked method
     */
    Method getMethod();

    /**
     * Get the arguments of the invocation.
     *
     * This can be {@code null} when method is invoked with no argument.
     *
     * @return argument lists or {@code null} if the invoked method did not take any arguments
     */
    Object[] getMethodArgs();

    /**
     * Get the thrown exception.
     * For {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)} callback or query execution
     * did't throw any error, this returns {@code null}.
     *
     * @return thrown exception
     */
    Throwable getThrowable();

    /**
     * Get the associated {@link ConnectionInfo}.
     *
     * @return connection info
     */
    ConnectionInfo getConnectionInfo();

    /**
     * Indicate whether the query execution was successful or not.
     * Contains valid value only after the query execution.
     *
     * @return true when query has successfully executed
     */
    boolean isSuccess();

    /**
     * Get the size of the batch query.
     *
     * i.e. Number of the calls of {@link Batch#add(String)}.
     *
     * @return batch size
     */
    int getBatchSize();

    /**
     * Get the list of {@link QueryInfo}.
     *
     * @return list of queries. This will NOT return {@code null}.
     */
    List<QueryInfo> getQueries();

    /**
     * Get the type of query execution.
     *
     * @return type of query execution
     */
    ExecutionType getType();

    /**
     * Get the number of the binding.
     *
     * i.e. Number of the calls of {@link Statement#add()}.
     *
     * @return size of the binding
     */
    int getBindingsSize();

    /**
     * Get the time that took queries to execute.
     *
     * @return query execution duration
     */
    Duration getExecuteDuration();


    /**
     * Get the currently executed thread name.
     *
     * @return thread name
     */
    String getThreadName();

    /**
     * Get the currently executed thread ID.
     *
     * @return thread ID
     */
    long getThreadId();


    /**
     * Get the proxy event type for query execution.
     *
     * @return proxy event type; one of {@link ProxyEventType#BEFORE_QUERY}, {@link ProxyEventType#AFTER_QUERY},
     * or {@link ProxyEventType#EACH_QUERY_RESULT}
     */
    ProxyEventType getProxyEventType();

    /**
     * Represent Nth {@link io.r2dbc.spi.Result}.
     *
     * On each query result callback({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}),
     * this value indicates Nth {@link Result} starting from 1.
     * (1st query result, 2nd query result, 3rd, 4th,...).
     *
     * This returns 0 for before query execution({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}).
     * For after query execution({@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), this returns
     * total number of {@link io.r2dbc.spi.Result} returned by this query execution.
     *
     * @return Nth number of query result
     */
    int getCurrentResultCount();


    /**
     * Mapped query result available for each-query-result-callback({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}).
     *
     * For before and after query execution({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}
     * and {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), this returns {@code null}.
     *
     * @return currently mapped result
     */
    Object getCurrentMappedResult();

    /**
     * Retrieve {@link ValueStore} which is associated to the scope of before/after method execution.
     *
     * Mainly used for passing values between {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)} and
     * {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}.
     *
     * @return value store
     */
    ValueStore getValueStore();

}
