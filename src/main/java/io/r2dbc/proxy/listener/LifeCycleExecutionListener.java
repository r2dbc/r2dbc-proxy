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

import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;

import java.lang.reflect.Method;

/**
 * Provides explicit callbacks on all SPI invocations and query executions on given {@link LifeCycleListener}.
 *
 * @author Tadaya Tsuyukubo
 * @see LifeCycleListener
 */
public final class LifeCycleExecutionListener implements ProxyExecutionListener {

    private final LifeCycleListener delegate;

    public static LifeCycleExecutionListener of(LifeCycleListener lifeCycleListener) {
        Assert.requireNonNull(lifeCycleListener, "lifeCycleListener must not be null");
        return new LifeCycleExecutionListener(lifeCycleListener);
    }

    private LifeCycleExecutionListener(LifeCycleListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        this.delegate.beforeMethod(executionInfo);
        methodCallback(executionInfo, true);
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        methodCallback(executionInfo, false);
        this.delegate.afterMethod(executionInfo);
    }

    private void methodCallback(MethodExecutionInfo executionInfo, boolean isBefore) {
        Method method = executionInfo.getMethod();
        String methodName = method.getName();
        Class<?> methodDeclaringClass = method.getDeclaringClass();

        if (ConnectionFactory.class.equals(methodDeclaringClass)) {
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
        } else if (Connection.class.equals(methodDeclaringClass)) {
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
            }
        } else if (Batch.class.equals(methodDeclaringClass)) {
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
        } else if (Statement.class.equals(methodDeclaringClass)) {
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
            } else if ("returnGeneratedValues".equals(methodName)) {
                if (isBefore) {
                    this.delegate.beforeReturnGeneratedValuesOnStatement(executionInfo);
                } else {
                    this.delegate.afterReturnGeneratedValuesOnStatement(executionInfo);
                }
            }
        } else if (Result.class.equals(methodDeclaringClass)) {
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
        }

    }

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        this.delegate.beforeQuery(execInfo);
        queryCallback(execInfo, true);
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        queryCallback(execInfo, false);
        this.delegate.afterQuery(execInfo);
    }

    private void queryCallback(QueryExecutionInfo execInfo, boolean isBefore) {
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

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        this.delegate.onEachQueryResult(execInfo);
    }
}
