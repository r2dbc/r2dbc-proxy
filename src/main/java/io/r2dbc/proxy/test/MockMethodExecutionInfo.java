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
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.core.ValueStore;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

/**
 * Mock implementation of {@link MethodExecutionInfo} for testing.
 *
 * @author Tadaya Tsuyukubo
 */
public final class MockMethodExecutionInfo implements MethodExecutionInfo {

    /**
     * Provides a builder for {@link MockMethodExecutionInfo}.
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
    public static MockMethodExecutionInfo empty() {
        return builder().build();
    }

    private final Object target;

    private final Method method;

    private final Object[] methodArgs;

    private final Object result;

    private final Throwable thrown;

    private final ConnectionInfo connectionInfo;

    private final Duration executeDuration;

    private final String threadName;

    private final long threadId;

    private final ProxyEventType proxyEventType;

    private final ValueStore valueStore;

    private MockMethodExecutionInfo(Builder builder) {
        this.target = builder.target;
        this.method = builder.method;
        this.methodArgs = builder.methodArgs;
        this.result = builder.result;
        this.thrown = builder.thrown;
        this.connectionInfo = builder.connectionInfo;
        this.executeDuration = builder.executeDuration;
        this.threadName = builder.threadName;
        this.threadId = builder.threadId;
        this.proxyEventType = builder.proxyEventType;
        this.valueStore = builder.valueStore;
    }

    @Override
    public Object getTarget() {
        return this.target;
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
    public Object getResult() {
        return this.result;
    }

    @Override
    public Throwable getThrown() {
        return this.thrown;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
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
    public ValueStore getValueStore() {
        return this.valueStore;
    }

    public static final class Builder {

        private Object target;

        private Method method;

        private Object[] methodArgs;

        private Object result;

        private Throwable thrown;

        private ConnectionInfo connectionInfo;

        private Duration executeDuration;

        private String threadName;

        private long threadId;

        private ProxyEventType proxyEventType;

        private ValueStore valueStore = ValueStore.create();

        private Builder() {
        }

        public Builder target(Object target) {
            this.target = target;
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

        public Builder result(Object result) {
            this.result = result;
            return this;
        }

        public Builder setThrown(Throwable thrown) {
            this.thrown = thrown;
            return this;
        }

        public Builder connectionInfo(ConnectionInfo connectionInfo) {
            this.connectionInfo = connectionInfo;
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

        public Builder valueStore(ValueStore valueStore) {
            this.valueStore = valueStore;
            return this;
        }

        public Builder customValue(Object key, Object value) {
            this.valueStore.put(key, value);
            return this;
        }

        public Builder customValues(Map<Object, Object> values) {
            this.valueStore.putAll(values);
            return this;
        }

        public MockMethodExecutionInfo build() {
            return new MockMethodExecutionInfo(this);
        }

    }

}
