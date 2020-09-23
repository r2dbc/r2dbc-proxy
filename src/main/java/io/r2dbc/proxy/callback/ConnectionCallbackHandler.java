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
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Proxy callback handler for {@link Connection}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class ConnectionCallbackHandler extends CallbackHandlerSupport {

    private final Connection connection;

    private final ConnectionInfo connectionInfo;

    public ConnectionCallbackHandler(Connection connection, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.connection = Assert.requireNonNull(connection, "connection must not be null");
        this.connectionInfo = Assert.requireNonNull(connectionInfo, "connectionInfo must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();

        if ("unwrap".equals(methodName)) {
            return this.connection;
        } else if ("unwrapConnection".equals(methodName)) {
            return this.connection;
        }

        Consumer<MethodExecutionInfo> onComplete = null;

        // since these methods return Publisher<Void> pass the callback for doOnComplete().
        if ("beginTransaction".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().incrementTransactionCount();
            };
        } else if ("commitTransaction".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().incrementCommitCount();
            };
        } else if ("rollbackTransaction".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().incrementRollbackCount();
            };
        } else if ("close".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().setClosed(true);
            };
        }

        if ("createStatement".equals(methodName)) {
            String query = (String) args[0];

            // create a value store
            MutableStatementInfo statementInfo = new MutableStatementInfo();
            statementInfo.setConnectionInfo(this.connectionInfo);
            statementInfo.setOriginalQuery(query);

            String updatedQuery = this.proxyConfig.getBindParameterConverter().onCreateStatement(query, statementInfo);
            statementInfo.setUpdatedQuery(updatedQuery);

            // replace the query
            args[0] = updatedQuery;
            Object result = proceedExecution(method, this.connection, args, this.proxyConfig.getListeners(), this.connectionInfo, null);

            return this.proxyConfig.getProxyFactory().wrapStatement((Statement) result, statementInfo, this.connectionInfo);
        }
        // TODO: createSavepoint, releaseSavepoint, rollbackTransactionToSavepoint

        Object result = proceedExecution(method, this.connection, args, this.proxyConfig.getListeners(), this.connectionInfo, onComplete);

        if ("createBatch".equals(methodName)) {
            return this.proxyConfig.getProxyFactory().wrapBatch((Batch) result, this.connectionInfo);
        }

        return result;
    }

}
