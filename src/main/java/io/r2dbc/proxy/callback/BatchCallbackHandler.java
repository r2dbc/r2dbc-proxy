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
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Proxy callback handler for {@link Batch}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class BatchCallbackHandler extends CallbackHandlerSupport {

    private final Batch batch;

    private final ConnectionInfo connectionInfo;

    private final List<String> queries = new ArrayList<>();

    public BatchCallbackHandler(Batch batch, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.batch = Assert.requireNonNull(batch, "batch must not be null");
        this.connectionInfo = Assert.requireNonNull(connectionInfo, "connectionInfo must not be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();

        if ("unwrap".equals(methodName)) {
            return this.batch;
        } else if ("unwrapConnection".equals(methodName)) {
            return this.connectionInfo.getOriginalConnection();
        }

        Object result = proceedExecution(method, this.batch, args, this.proxyConfig.getListeners(), this.connectionInfo, null, null);

        if ("add".equals(methodName)) {
            this.queries.add((String) args[0]);
            return proxy;
        } else if ("execute".equals(methodName)) {

            List<QueryInfo> queryInfoList = this.queries.stream()
                .map(QueryInfo::new)
                .collect(toList());

            MutableQueryExecutionInfo execInfo = new MutableQueryExecutionInfo();
            execInfo.setType(ExecutionType.BATCH);
            execInfo.setQueries(queryInfoList);
            execInfo.setBatchSize(this.queries.size());
            execInfo.setMethod(method);
            execInfo.setMethodArgs(args);
            execInfo.setConnectionInfo(this.connectionInfo);

            // API defines "execute()" returns a publisher
            Publisher<? extends Result> publisher = (Publisher<? extends Result>) result;

            return interceptQueryExecution(publisher, execInfo);
        }

        return result;
    }
}
