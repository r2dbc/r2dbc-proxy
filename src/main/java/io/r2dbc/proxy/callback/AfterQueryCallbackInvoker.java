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

import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;

/**
 * Invoke {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)} callback.
 * <p>
 * Extracted the logic to call "afterQuery" callback method.
 * This is because gh-94 exhibits the need to put afterQuery callback invocation
 * in both {@link QueryInvocationSubscriber} and {@link ResultInvocationSubscriber}.
 *
 * @author Tadaya Tsuyukubo
 * @see QueryInvocationSubscriber
 * @see ResultCallbackHandler
 */
class AfterQueryCallbackInvoker {

    private final MutableQueryExecutionInfo executionInfo;

    private final QueriesExecutionContext queriesExecutionContext;

    private final ProxyExecutionListener listener;

    public AfterQueryCallbackInvoker(MutableQueryExecutionInfo executionInfo, QueriesExecutionContext queriesExecutionContext, ProxyExecutionListener listener) {
        this.executionInfo = executionInfo;
        this.queriesExecutionContext = queriesExecutionContext;
        this.listener = listener;
    }

    public void afterQuery() {
        this.executionInfo.setExecuteDuration(this.queriesExecutionContext.getElapsedDuration());
        this.executionInfo.setThreadName(Thread.currentThread().getName());
        this.executionInfo.setThreadId(Thread.currentThread().getId());
        this.executionInfo.setCurrentMappedResult(null);
        this.executionInfo.setProxyEventType(ProxyEventType.AFTER_QUERY);

        this.listener.afterQuery(this.executionInfo);
    }

}
