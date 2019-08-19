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

import java.util.function.BiFunction;

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.ValidationDepth;

/**
 * Provides callback methods for each SPI call.
 *
 * @author Tadaya Tsuyukubo
 * @see LifeCycleExecutionListener
 */
public interface LifeCycleListener {

    //
    // for ConnectionFactory
    //

    /**
     * Callback that is invoked <em>before</em> {@link ConnectionFactory#create()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link ConnectionFactory#create()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link ConnectionFactory#getMetadata()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeGetMetadataOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link ConnectionFactory#getMetadata()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterGetMetadataOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // for Connection
    //

    /**
     * Callback that is invoked <em>before</em> {@link Connection#beginTransaction()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeBeginTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#beginTransaction()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterBeginTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#close()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#close()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#commitTransaction()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeCommitTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#commitTransaction()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterCommitTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#createBatch()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeCreateBatchOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#createBatch()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterCreateBatchOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#createSavepoint(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeCreateSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#createSavepoint(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterCreateSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#createStatement(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeCreateStatementOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#createStatement(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterCreateStatementOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#releaseSavepoint(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeReleaseSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#releaseSavepoint(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterReleaseSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#rollbackTransaction()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeRollbackTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#rollbackTransaction()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterRollbackTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#rollbackTransactionToSavepoint(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeRollbackTransactionToSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#rollbackTransactionToSavepoint(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterRollbackTransactionToSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#setTransactionIsolationLevel(IsolationLevel)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeSetTransactionIsolationLevelOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#setTransactionIsolationLevel(IsolationLevel)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterSetTransactionIsolationLevelOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#validate(ValidationDepth)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeValidateOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#validate(ValidationDepth)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterValidateOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#isAutoCommit()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeIsAutoCommitOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#isAutoCommit()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterIsAutoCommitOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#getTransactionIsolationLevel()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeGetTransactionIsolationLevelOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#getTransactionIsolationLevel()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterGetTransactionIsolationLevelOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Connection#setAutoCommit(boolean)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeSetAutoCommitOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Connection#setAutoCommit(boolean)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterSetAutoCommitOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // for Batch
    //

    /**
     * Callback that is invoked <em>before</em> {@link Batch#add(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeAddOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Batch#add(String)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterAddOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Batch#execute()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeExecuteOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Batch#execute()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterExecuteOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // for Statement
    //

    /**
     * Callback that is invoked <em>before</em> {@link Statement#add()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeAddOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Statement#add()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterAddOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Statement#bind(int, int)} and its overloads are called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeBindOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Statement#bind(int, int)} and its overloads are called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterBindOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Statement#bindNull(int, Class)} and its overloads are called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeBindNullOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Statement#bindNull(int, Class)} and its overloads are called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterBindNullOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Statement#execute()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeExecuteOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Statement#execute()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterExecuteOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Statement#fetchSize(int)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeFetchSizeOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Statement#fetchSize(int)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterFetchSizeOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Statement#returnGeneratedValues(String...)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeReturnGeneratedValuesOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Statement#returnGeneratedValues(String...)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterReturnGeneratedValuesOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // For Result
    //

    /**
     * Callback that is invoked <em>before</em> {@link Result#getRowsUpdated()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeGetRowsUpdatedOnResult(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Result#getRowsUpdated()} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterGetRowsUpdatedOnResult(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>before</em> {@link Result#map(BiFunction)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void beforeMapOnResult(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Callback that is invoked <em>after</em> {@link Result#map(BiFunction)} is called.
     *
     * @param methodExecutionInfo the current method execution info; never {@code null}.
     */
    default void afterMapOnResult(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // For query execution
    //

    /**
     * Query execution callback that is invoked <em>before</em> {@link Batch#execute()} is called.
     *
     * @param queryExecutionInfo the current query execution info; never {@code null}.
     */
    default void beforeExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
    }

    /**
     * Query execution callback that is invoked <em>after</em> {@link Batch#execute()} is called.
     *
     * @param queryExecutionInfo the current query execution info; never {@code null}.
     */
    default void afterExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
    }

    /**
     * Query execution callback that is invoked <em>before</em> {@link Statement#execute()} is called.
     *
     * @param queryExecutionInfo the current query execution info; never {@code null}.
     */
    default void beforeExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
    }

    /**
     * Query execution callback that is invoked <em>after</em> {@link Statement#execute()} is called.
     *
     * @param queryExecutionInfo the current query execution info; never {@code null}.
     */
    default void afterExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
    }

    //
    // processing query result
    //

    /**
     * Query result processing callback that is invoked on each query result while processed by {@link Result#map(BiFunction)}.
     *
     * @param queryExecutionInfo the current query execution info; never {@code null}.
     */
    default void onEachQueryResult(QueryExecutionInfo queryExecutionInfo) {

    }

    //
    // For every method
    //

    /**
     * Called at every method invocation.
     *
     * When any methods on proxied classes are called, this callback is called first. Then, corresponding
     * beforeXxxOnYyy callback will be called.
     *
     * Analogous to {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)}
     *
     * @param methodExecutionInfo method execution info
     */
    default void beforeMethod(MethodExecutionInfo methodExecutionInfo) {
    }

    /**
     * Called at every method invocation.
     *
     * When any methods on proxied classes are called and after actual method is invoked, corresponding
     * afterXxxOnYyy callback is called, then this callback method will be invoked.
     *
     * Analogous to {@link ProxyExecutionListener#afterMethod(MethodExecutionInfo)}
     *
     * @param methodExecutionInfo method execution info
     */
    default void afterMethod(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // For every query
    //

    /**
     * Called before execution of query.
     *
     * When query is executed, this callback method is called first, then {@link #beforeExecuteOnStatement(QueryExecutionInfo)}
     * or {@link #beforeExecuteOnBatch(QueryExecutionInfo)} will be called.
     *
     * Analogous to {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}
     *
     * @param queryExecutionInfo query execution info
     */
    default void beforeQuery(QueryExecutionInfo queryExecutionInfo) {
    }

    /**
     * Called after execution of query.
     *
     * When query is executed, after original method is called, then {@link #afterExecuteOnStatement(QueryExecutionInfo)}
     * or {@link #afterExecuteOnBatch(QueryExecutionInfo)}, then this method is invoked.
     *
     * Analogous to {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}
     *
     * @param queryExecutionInfo query execution info
     */
    default void afterQuery(QueryExecutionInfo queryExecutionInfo) {
    }

}
