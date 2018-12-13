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

package io.r2dbc.proxy.core;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Hold method execution related information.
 *
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfo {

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

    private Map<String, Object> customValues = new HashMap<>();

    /**
     * Store key/value pair.
     *
     * Mainly used for passing values between before and after listener callback.
     *
     * @param key   key
     * @param value value
     */
    public void addCustomValue(String key, Object value) {
        this.customValues.put(key, value);
    }

    public <T> T getCustomValue(String key, Class<T> type) {
        return type.cast(this.customValues.get(key));
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public Duration getExecuteDuration() {
        return executeDuration;
    }

    public void setExecuteDuration(Duration executeDuration) {
        this.executeDuration = executeDuration;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public ProxyEventType getProxyEventType() {
        return proxyEventType;
    }

    public void setProxyEventType(ProxyEventType proxyEventType) {
        this.proxyEventType = proxyEventType;
    }
}
