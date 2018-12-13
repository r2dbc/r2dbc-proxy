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

import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.Result;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hold query execution related information.
 *
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfo {

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

    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Contains an exception thrown while query was executed.
     * Contains value only when an exception has thrown, otherwise {@code null}.
     *
     * @param throwable an error thrown while executing a query
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * Indicate whether the query execution was successful or not.
     * Contains valid value only after the query execution.
     *
     * @return true when query has successfully executed
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Returns list of {@link QueryInfo}.
     *
     * @return list of queries. This will NOT return null.
     */
    public List<QueryInfo> getQueries() {
        return this.queries;
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

    /**
     * Time that took queries to execute.
     *
     * @return query execution duration
     */
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

    /**
     * Represent Nth {@link io.r2dbc.spi.Result}.
     *
     * On each query result callback({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}),
     * this value indicates Nth {@link Result} starting from 1.
     * (1st query result, 2nd query result, 3rd, 4th,...).
     *
     * This returns 0 for before query execution({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}).
     * For after query execution({@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), this returns
     * total number of {@link io.r2dbc.spi.Result} returned by this query execution.
     *
     * @return Nth number of query result
     */
    public int getCurrentResultCount() {
        return currentResultCount;
    }

    public void setCurrentResultCount(int currentResultCount) {
        this.currentResultCount = currentResultCount;
    }

    /**
     * Mapped query result available for each-query-result-callback({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}).
     *
     * For before and after query execution({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}
     * and {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), this returns {@code null}.
     *
     * @return currently mapped result
     */
    public Object getCurrentMappedResult() {
        return currentMappedResult;
    }

    public void setCurrentMappedResult(Object currentResult) {
        this.currentMappedResult = currentResult;
    }
}
