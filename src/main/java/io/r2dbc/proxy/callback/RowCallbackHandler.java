/*
 * Copyright 2021 the original author or authors.
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
import io.r2dbc.proxy.core.R2dbcProxyException;
import io.r2dbc.proxy.listener.ResultRowConverter;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Row;

import java.lang.reflect.Method;

/**
 * Proxy callback handler for {@link Row}.
 *
 * @author Tadaya Tsuyukubo
 * @since 0.9.0
 */
public final class RowCallbackHandler extends CallbackHandlerSupport {

    private final Row row;

    private final QueryExecutionInfo queryExecutionInfo;

    /**
     * Callback handler logic for {@link Row}.
     *
     * @param row                row
     * @param queryExecutionInfo query execution info
     * @param proxyConfig        proxy config
     * @throws IllegalArgumentException if {@code row} is {@code null}
     * @throws IllegalArgumentException if {@code queryExecutionInfo} is {@code null}
     * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
     */
    public RowCallbackHandler(Row row, QueryExecutionInfo queryExecutionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.row = Assert.requireNonNull(row, "row must not be null");
        this.queryExecutionInfo = Assert.requireNonNull(queryExecutionInfo, "queryExecutionInfo must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();
        ConnectionInfo connectionInfo = this.queryExecutionInfo.getConnectionInfo();

        if ("unwrap".equals(methodName)) {  // for Wrapped
            return this.row;
        } else if ("unwrapConnection".equals(methodName)) {  // for ConnectionHolder
            return connectionInfo.getOriginalConnection();
        }

        // when converter decides to perform the original call("getOperation.proceed()"), this lambda is called.
        ResultRowConverter.GetOperation onGet = () -> {
            try {
                Object result = proceedExecution(method, this.row, args, this.proxyConfig.getListeners(), connectionInfo, null);
                return result;
            } catch (Throwable throwable) {
                throw new R2dbcProxyException("Failed to perform " + methodName, throwable);
            }
        };

        // callback for "Row#get(...)"
        Object result = this.proxyConfig.getResultRowConverter().onGet((Row) proxy, method, args, onGet);
        return result;
    }

}
