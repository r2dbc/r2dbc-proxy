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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy callback handler for {@link Result}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ResultCallbackHandler extends CallbackHandlerSupport {

    private Result result;

    private QueryExecutionInfo queryExecutionInfo;

    public ResultCallbackHandler(Result result, QueryExecutionInfo queryExecutionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.result = Assert.requireNonNull(result, "result must not be null");
        this.queryExecutionInfo = Assert.requireNonNull(queryExecutionInfo, "queryExecutionInfo must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();
        ConnectionInfo connectionInfo = this.queryExecutionInfo.getConnectionInfo();

        if ("unwrap".equals(methodName)) {  // for Wrapped
            return this.result;
        } else if ("unwrapConnection".equals(methodName)) {  // for ConnectionHolder
            return connectionInfo.getOriginalConnection();
        }

        Object invocationResult = proceedExecution(method, this.result, args, this.proxyConfig.getListeners(), connectionInfo, null, null);

        if ("map".equals(methodName)) {

            AtomicInteger resultCount = new AtomicInteger(0);

            // add logic to call "listener#eachQueryResult()"
            return Flux.from((Publisher<?>) invocationResult)
                .doOnEach(signal -> {

                    boolean proceed = signal.isOnNext() || signal.isOnError();
                    if (!proceed) {
                        return;
                    }

                    int count = resultCount.incrementAndGet();

                    if (signal.isOnNext()) {
                        Object mappedResult = signal.get();

                        this.queryExecutionInfo.setCurrentResultCount(count);
                        this.queryExecutionInfo.setCurrentMappedResult(mappedResult);
                        this.queryExecutionInfo.setThrowable(null);
                    } else {
                        // onError
                        Throwable thrown = signal.getThrowable();
                        this.queryExecutionInfo.setCurrentResultCount(count);
                        this.queryExecutionInfo.setCurrentMappedResult(null);
                        this.queryExecutionInfo.setThrowable(thrown);

                    }

                    this.queryExecutionInfo.setProxyEventType(ProxyEventType.EACH_QUERY_RESULT);

                    String threadName = Thread.currentThread().getName();
                    long threadId = Thread.currentThread().getId();
                    this.queryExecutionInfo.setThreadName(threadName);
                    this.queryExecutionInfo.setThreadId(threadId);

                    // callback
                    this.proxyConfig.getListeners().eachQueryResult(this.queryExecutionInfo);

                });

        }

        return invocationResult;

    }

}
