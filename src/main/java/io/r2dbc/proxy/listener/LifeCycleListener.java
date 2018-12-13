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

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;

/**
 * Provides callback for each SPI.
 *
 * @author Tadaya Tsuyukubo
 * @see LifeCycleExecutionListener
 */
public interface LifeCycleListener {

    //
    // for ConnectionFactory
    //

    default void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeGetMetadataOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterGetMetadataOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // for Connection
    //

    default void beforeBeginTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterBeginTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeCommitTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterCommitTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeCreateBatchOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterCreateBatchOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeCreateSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterCreateSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeCreateStatementOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterCreateStatementOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeReleaseSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterReleaseSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeRollbackTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterRollbackTransactionOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeRollbackTransactionToSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterRollbackTransactionToSavepointOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeSetTransactionIsolationLevelOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterSetTransactionIsolationLevelOnConnection(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // for Batch
    //

    default void beforeAddOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterAddOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeExecuteOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterExecuteOnBatch(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // for Statement
    //

    default void beforeAddOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterAddOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeBindOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterBindOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeBindNullOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterBindNullOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void beforeExecuteOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    default void afterExecuteOnStatement(MethodExecutionInfo methodExecutionInfo) {
    }

    //
    // For Result
    //
    default void beforeGetRowsUpdatedOnResult(MethodExecutionInfo methodExecutioninfo) {
    }

    default void afterGetRowsUpdatedOnResult(MethodExecutionInfo methodExecutioninfo) {
    }

    default void beforeMapOnResult(MethodExecutionInfo methodExecutioninfo) {
    }

    default void afterMapOnResult(MethodExecutionInfo methodExecutioninfo) {
    }

    //
    // For query execution
    //

    default void beforeExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
    }

    default void afterExecuteOnBatch(QueryExecutionInfo queryExecutionInfo) {
    }

    default void beforeExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
    }

    default void afterExecuteOnStatement(QueryExecutionInfo queryExecutionInfo) {
    }

    //
    // processing query result
    //

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
