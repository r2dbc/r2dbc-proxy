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

package io.r2dbc.proxy.test;

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
import java.util.Map;

/**
 * Mock implementation of {@link QueryExecutionInfo} for testing.
 *
 * @author Tadaya Tsuyukubo
 */
public final class MockQueryExecutionInfo implements QueryExecutionInfo {

    /**
     * Provides a builder for {@link MockQueryExecutionInfo}.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Provide an empty {@link MockMethodExecutionInfo}.
     *
     * @return a {@link MockMethodExecutionInfo}.
     */
    public static MockQueryExecutionInfo empty() {
        return builder().build();
    }

    private ConnectionInfo connectionInfo;

    private Method method;

    private Object[] methodArgs;

    private Throwable throwable;

    private boolean isSuccess;

    private int batchSize;

    private ExecutionType type;

    private int bindingsSize;

    private Duration executeDuration;

    private String threadName;

    private long threadId;

    private ProxyEventType proxyEventType;

    private int currentResultCount;

    private Object currentMappedResult;

    private List<QueryInfo> queries;

    private ValueStore valueStore;

    private MockQueryExecutionInfo(Builder builder) {
        this.connectionInfo = builder.connectionInfo;
        this.method = builder.method;
        this.methodArgs = builder.methodArgs;
        this.throwable = builder.throwable;
        this.isSuccess = builder.isSuccess;
        this.batchSize = builder.batchSize;
        this.type = builder.type;
        this.bindingsSize = builder.bindingsSize;
        this.executeDuration = builder.executeDuration;
        this.threadName = builder.threadName;
        this.threadId = builder.threadId;
        this.proxyEventType = builder.proxyEventType;
        this.currentResultCount = builder.currentResultCount;
        this.currentMappedResult = builder.currentMappedResult;
        this.queries = builder.queries;
        this.valueStore = builder.valueStore;
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public Object[] getMethodArgs() {
        return this.methodArgs;
    }

    @Override
    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    @Override
    public boolean isSuccess() {
        return this.isSuccess;
    }

    @Override
    public int getBatchSize() {
        return this.batchSize;
    }

    @Override
    public List<QueryInfo> getQueries() {
        return this.queries;
    }

    @Override
    public ExecutionType getType() {
        return this.type;
    }

    @Override
    public int getBindingsSize() {
        return this.bindingsSize;
    }

    @Override
    public Duration getExecuteDuration() {
        return this.executeDuration;
    }

    @Override
    public String getThreadName() {
        return this.threadName;
    }

    @Override
    public long getThreadId() {
        return this.threadId;
    }

    @Override
    public ProxyEventType getProxyEventType() {
        return this.proxyEventType;
    }

    @Override
    public int getCurrentResultCount() {
        return this.currentResultCount;
    }

    @Override
    public Object getCurrentMappedResult() {
        return this.currentMappedResult;
    }

    @Override
    public ValueStore getValueStore() {
        return this.valueStore;
    }

    public static final class Builder {

        private ConnectionInfo connectionInfo;

        private Method method;

        private Object[] methodArgs;

        private Throwable throwable;

        private boolean isSuccess;

        private int batchSize;

        private ExecutionType type;

        private int bindingsSize;

        private Duration executeDuration;

        private String threadName;

        private long threadId;

        private ProxyEventType proxyEventType;

        private int currentResultCount;

        private Object currentMappedResult;

        private List<QueryInfo> queries = new ArrayList<>();

        private ValueStore valueStore = ValueStore.create();


        public Builder from(MockQueryExecutionInfo mock) {
            this.connectionInfo = mock.connectionInfo;
            this.method = mock.method;
            this.methodArgs = mock.methodArgs;
            this.throwable = mock.throwable;
            this.isSuccess = mock.isSuccess;
            this.batchSize = mock.batchSize;
            this.type = mock.type;
            this.bindingsSize = mock.bindingsSize;
            this.executeDuration = mock.executeDuration;
            this.threadName = mock.threadName;
            this.threadId = mock.threadId;
            this.proxyEventType = mock.proxyEventType;
            this.currentResultCount = mock.currentResultCount;
            this.currentMappedResult = mock.currentMappedResult;
            this.queries = mock.queries;
            this.valueStore = mock.valueStore;
            return this;
        }

        public Builder connectionInfo(ConnectionInfo connectionInfo) {
            this.connectionInfo = connectionInfo;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public Builder methodArgs(Object[] methodArgs) {
            this.methodArgs = methodArgs;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder isSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder type(ExecutionType type) {
            this.type = type;
            return this;
        }

        public Builder bindingsSize(int bindingsSize) {
            this.bindingsSize = bindingsSize;
            return this;
        }

        public Builder executeDuration(Duration executeDuration) {
            this.executeDuration = executeDuration;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder threadId(long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder proxyEventType(ProxyEventType proxyEventType) {
            this.proxyEventType = proxyEventType;
            return this;
        }

        public Builder currentResultCount(int currentResultCount) {
            this.currentResultCount = currentResultCount;
            return this;
        }

        public Builder currentMappedResult(Object currentMappedResult) {
            this.currentMappedResult = currentMappedResult;
            return this;
        }

        public Builder queryInfo(QueryInfo queryInfo) {
            this.queries.add(queryInfo);
            return this;
        }

        public Builder queries(List<QueryInfo> queries) {
            this.queries = queries;
            return this;
        }

        public Builder valueStore(ValueStore valueStore) {
            this.valueStore = valueStore;
            return this;
        }

        public Builder customValue(Object key, Object value) {
            this.valueStore.put(key, value);
            return this;
        }

        public Builder customValues(Map<Object, Object> customValues) {
            this.valueStore.putAll(customValues);
            return this;
        }

        public MockQueryExecutionInfo build() {
            return new MockQueryExecutionInfo(this);
        }
    }

}
