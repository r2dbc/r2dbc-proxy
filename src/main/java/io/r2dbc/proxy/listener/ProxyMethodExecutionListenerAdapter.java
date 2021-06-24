/*
 * Copyright 2020 the original author or authors.
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

import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.Statement;

import java.lang.reflect.Method;

/**
 * Adapter to make {@link ProxyMethodExecutionListener} work as {@link ProxyExecutionListener}.
 *
 * @author Tadaya Tsuyukubo
 * @since 0.8.3
 */
public class ProxyMethodExecutionListenerAdapter implements ProxyExecutionListener {

    private final ProxyMethodExecutionListener delegate;

    public ProxyMethodExecutionListenerAdapter(ProxyMethodExecutionListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        this.delegate.beforeMethod(executionInfo);
        invokeMethodCallback(executionInfo, true);
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        invokeMethodCallback(executionInfo, false);
        this.delegate.afterMethod(executionInfo);
    }

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        this.delegate.beforeQuery(execInfo);
        invokeQueryCallback(execInfo, true);
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        invokeQueryCallback(execInfo, false);
        this.delegate.afterQuery(execInfo);
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        this.delegate.eachQueryResult(execInfo);
    }

    private void invokeMethodCallback(MethodExecutionInfo executionInfo, boolean isBefore) {
        Method method = executionInfo.getMethod();
        String methodName = method.getName();
        Object target = executionInfo.getTarget();

        if (target instanceof ConnectionFactory) {
            // ConnectionFactory methods
            if ("create".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeCreateOnConnectionFactory(executionInfo);
                } else {
                    this.delegate.afterCreateOnConnectionFactory(executionInfo);
                }
            } else if ("getMetadata".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeGetMetadataOnConnectionFactory(executionInfo);
                } else {
                    this.delegate.afterGetMetadataOnConnectionFactory(executionInfo);
                }
            }
        } else if (target instanceof Connection) {
            // Connection methods
            if ("beginTransaction".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeBeginTransactionOnConnection(executionInfo);
                } else {
                    this.delegate.afterBeginTransactionOnConnection(executionInfo);
                }
            } else if ("close".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeCloseOnConnection(executionInfo);
                } else {
                    this.delegate.afterCloseOnConnection(executionInfo);
                }
            } else if ("commitTransaction".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeCommitTransactionOnConnection(executionInfo);
                } else {
                    this.delegate.afterCommitTransactionOnConnection(executionInfo);
                }
            } else if ("createBatch".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeCreateBatchOnConnection(executionInfo);
                } else {
                    this.delegate.afterCreateBatchOnConnection(executionInfo);
                }
            } else if ("createSavepoint".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeCreateSavepointOnConnection(executionInfo);
                } else {
                    this.delegate.afterCreateSavepointOnConnection(executionInfo);
                }
            } else if ("createStatement".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeCreateStatementOnConnection(executionInfo);
                } else {
                    this.delegate.afterCreateStatementOnConnection(executionInfo);
                }
            } else if ("releaseSavepoint".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeReleaseSavepointOnConnection(executionInfo);
                } else {
                    this.delegate.afterReleaseSavepointOnConnection(executionInfo);
                }
            } else if ("rollbackTransaction".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeRollbackTransactionOnConnection(executionInfo);
                } else {
                    this.delegate.afterRollbackTransactionOnConnection(executionInfo);
                }
            } else if ("rollbackTransactionToSavepoint".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeRollbackTransactionToSavepointOnConnection(executionInfo);
                } else {
                    this.delegate.afterRollbackTransactionToSavepointOnConnection(executionInfo);
                }
            } else if ("setTransactionIsolationLevel".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeSetTransactionIsolationLevelOnConnection(executionInfo);
                } else {
                    this.delegate.afterSetTransactionIsolationLevelOnConnection(executionInfo);
                }
            } else if ("validate".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeValidateOnConnection(executionInfo);
                } else {
                    this.delegate.afterValidateOnConnection(executionInfo);
                }
            } else if ("isAutoCommit".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeIsAutoCommitOnConnection(executionInfo);
                } else {
                    this.delegate.afterIsAutoCommitOnConnection(executionInfo);
                }
            } else if ("getTransactionIsolationLevel".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeGetTransactionIsolationLevelOnConnection(executionInfo);
                } else {
                    this.delegate.afterGetTransactionIsolationLevelOnConnection(executionInfo);
                }
            } else if ("setAutoCommit".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeSetAutoCommitOnConnection(executionInfo);
                } else {
                    this.delegate.afterSetAutoCommitOnConnection(executionInfo);
                }
            } else if ("getMetadata".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeGetMetadataOnConnection(executionInfo);
                } else {
                    this.delegate.afterGetMetadataOnConnection(executionInfo);
                }
            }
        } else if (target instanceof Batch) {
            // Batch methods
            if ("add".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeAddOnBatch(executionInfo);
                } else {
                    this.delegate.afterAddOnBatch(executionInfo);
                }
            } else if ("execute".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeExecuteOnBatch(executionInfo);
                } else {
                    this.delegate.afterExecuteOnBatch(executionInfo);
                }
            }
        } else if (target instanceof Statement) {
            // Statement methods
            if ("add".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeAddOnStatement(executionInfo);
                } else {
                    this.delegate.afterAddOnStatement(executionInfo);
                }
            } else if ("bind".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeBindOnStatement(executionInfo);
                } else {
                    this.delegate.afterBindOnStatement(executionInfo);
                }
            } else if ("bindNull".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeBindNullOnStatement(executionInfo);
                } else {
                    this.delegate.afterBindNullOnStatement(executionInfo);
                }
            } else if ("execute".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeExecuteOnStatement(executionInfo);
                } else {
                    this.delegate.afterExecuteOnStatement(executionInfo);
                }
            } else if ("fetchSize".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeFetchSizeOnStatement(executionInfo);
                } else {
                    this.delegate.afterFetchSizeOnStatement(executionInfo);
                }
            } else if ("returnGeneratedValues".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeReturnGeneratedValuesOnStatement(executionInfo);
                } else {
                    this.delegate.afterReturnGeneratedValuesOnStatement(executionInfo);
                }
            }
        } else if (target instanceof Result) {
            if ("getRowsUpdated".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeGetRowsUpdatedOnResult(executionInfo);
                } else {
                    this.delegate.afterGetRowsUpdatedOnResult(executionInfo);
                }
            } else if ("map".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeMapOnResult(executionInfo);
                } else {
                    this.delegate.afterMapOnResult(executionInfo);
                }
            }
        } else if (target instanceof Row) {
            if ("get".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeGetOnRow(executionInfo);
                } else {
                    this.delegate.afterGetOnRow(executionInfo);
                }
            } else if ("getMetadata".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeGetMetadataOnRow(executionInfo);
                } else {
                    this.delegate.afterGetMetadataOnRow(executionInfo);
                }
            }
        }
    }

    private void invokeQueryCallback(QueryExecutionInfo execInfo, boolean isBefore) {
        ExecutionType executionType = execInfo.getType();

        if (executionType == ExecutionType.BATCH) {
            if (isBefore) {
                this.delegate.beforeExecuteOnBatch(execInfo);
            } else {
                this.delegate.afterExecuteOnBatch(execInfo);
            }
        } else {
            if (isBefore) {
                this.delegate.beforeExecuteOnStatement(execInfo);
            } else {
                this.delegate.afterExecuteOnStatement(execInfo);
            }
        }
    }

    public ProxyMethodExecutionListener getDelegate() {
        return this.delegate;
    }

}
