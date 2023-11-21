/*
 * Copyright 2023 the original author or authors.
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
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * Proxy callback handler for {@link Result.RowSegment}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.1
 */
public final class RowSegmentCallbackHandler extends CallbackHandlerSupport {

    private final Result.RowSegment rowSegment;

    private final QueryExecutionInfo queryExecutionInfo;

    /**
     * Callback handler logic for {@link Row}.
     *
     * @param rowSegment         row
     * @param queryExecutionInfo query execution info
     * @param proxyConfig        proxy config
     * @throws IllegalArgumentException if {@code row} is {@code null}
     * @throws IllegalArgumentException if {@code queryExecutionInfo} is {@code null}
     * @throws IllegalArgumentException if {@code proxyConfig} is {@code null}
     */
    public RowSegmentCallbackHandler(Result.RowSegment rowSegment, QueryExecutionInfo queryExecutionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.rowSegment = Assert.requireNonNull(rowSegment, "rowSegment must not be null");
        this.queryExecutionInfo = Assert.requireNonNull(queryExecutionInfo, "queryExecutionInfo must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, @Nullable Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();
        ConnectionInfo connectionInfo = this.queryExecutionInfo.getConnectionInfo();

        if (isCommonMethod(methodName)) {
            return handleCommonMethod(methodName, this.rowSegment, args, connectionInfo.getOriginalConnection());
        }

        Object result = proceedExecution(method, this.rowSegment, args, this.proxyConfig.getListeners(), connectionInfo, null);

        if ("row".equals(methodName)) {
            return this.proxyConfig.getProxyFactory().wrapRow((Row) result, queryExecutionInfo);
        }
        return result;
    }

}
