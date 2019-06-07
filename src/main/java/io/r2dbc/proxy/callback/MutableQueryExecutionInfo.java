/*
 * Copyright 2019 the original author or authors.
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
 *
 */

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.core.ValueStore;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the {@link QueryExecutionInfo}.
 *
 * @author Tadaya Tsuyukubo
 */
final class MutableQueryExecutionInfo implements QueryExecutionInfo {

    private ConnectionInfo connectionInfo;

    private Method method;

    private Object[] methodArgs;

    private Throwable throwable;

    private boolean isSuccess;

    private int batchSize;  // num of Batch#add

    private ExecutionType type;

    private int bindingsSize;  // num of Statement#add

    private Duration executeDuration = Duration.ZERO;

    private String threadName = "";

    private long threadId;

    private ProxyEventType proxyEventType;

    private int currentResultCount;

    private Object currentMappedResult;

    private List<QueryInfo> queries = new ArrayList<>();

    private ValueStore valueStore = ValueStore.create();

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setQueries(List<QueryInfo> queries) {
        this.queries = queries;
    }

    public ExecutionType getType() {
        return type;
    }

    public void setType(ExecutionType type) {
        this.type = type;
    }

    public int getBindingsSize() {
        return bindingsSize;
    }

    public void setBindingsSize(int bindingsSize) {
        this.bindingsSize = bindingsSize;
    }

    public void setExecuteDuration(Duration executeDuration) {
        this.executeDuration = executeDuration;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public void setProxyEventType(ProxyEventType proxyEventType) {
        this.proxyEventType = proxyEventType;
    }

    public void setCurrentResultCount(int currentResultCount) {
        this.currentResultCount = currentResultCount;
    }

    public void setCurrentMappedResult(Object currentResult) {
        this.currentMappedResult = currentResult;
    }

    @Override
    public ValueStore getValueStore() {
        return this.valueStore;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getMethodArgs() {
        return methodArgs;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public List<QueryInfo> getQueries() {
        return this.queries;
    }

    @Override
    public Duration getExecuteDuration() {
        return executeDuration;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public ProxyEventType getProxyEventType() {
        return proxyEventType;
    }

    @Override
    public int getCurrentResultCount() {
        return currentResultCount;
    }

    @Override
    public Object getCurrentMappedResult() {
        return currentMappedResult;
    }

}
