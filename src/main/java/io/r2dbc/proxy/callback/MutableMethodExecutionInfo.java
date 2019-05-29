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
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.ProxyEventType;
import io.r2dbc.proxy.core.ValueStore;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Default implementation of the {@link MethodExecutionInfo}.
 *
 * @author Tadaya Tsuyukubo
 */
final class MutableMethodExecutionInfo implements MethodExecutionInfo {

    private Object target;

    private Method method;

    private Object[] methodArgs;

    private Object result;

    private Throwable thrown;

    private ConnectionInfo connectionInfo;

    private Duration executeDuration = Duration.ZERO;

    private String threadName;

    private long threadId;

    private ProxyEventType proxyEventType;

    private ValueStore valueStore = ValueStore.create();

    public void setTarget(Object target) {
        this.target = target;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
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

    @Override
    public Object getTarget() {
        return target;
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
    public Object getResult() {
        return result;
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
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
    public ValueStore getValueStore() {
        return this.valueStore;
    }

}
