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

package io.r2dbc.proxy.listener;

import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;

/**
 * Keep the last invoked execution.
 *
 * Used for validating last execution.
 *
 * @author Tadaya Tsuyukubo
 */
public class LastExecutionAwareListener implements ProxyExecutionListener {

    private QueryExecutionInfo beforeQueryExecutionInfo;

    private QueryExecutionInfo afterQueryExecutionInfo;

    private QueryExecutionInfo eachQueryResultExecutionInfo;

    private MethodExecutionInfo beforeMethodExecutionInfo;

    private MethodExecutionInfo afterMethodExecutionInfo;

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        this.beforeQueryExecutionInfo = execInfo;
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        this.afterQueryExecutionInfo = execInfo;
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        this.eachQueryResultExecutionInfo = execInfo;
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        this.beforeMethodExecutionInfo = executionInfo;
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        this.afterMethodExecutionInfo = executionInfo;
    }

    /**
     * Get the last used {@link QueryExecutionInfo} in {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}.
     *
     * @return last used {@link QueryExecutionInfo}. Can be {@code null} if not invoked yet.
     */
    public QueryExecutionInfo getBeforeQueryExecutionInfo() {
        return beforeQueryExecutionInfo;
    }

    /**
     * Get the last used {@link QueryExecutionInfo} in {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}.
     *
     * @return last used {@link QueryExecutionInfo}. Can be {@code null} if not invoked yet.
     */
    public QueryExecutionInfo getAfterQueryExecutionInfo() {
        return afterQueryExecutionInfo;
    }

    /**
     * Get the last used {@link QueryExecutionInfo} in {@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}.
     *
     * @return last used {@link QueryExecutionInfo}. Can be {@code null} if not invoked yet.
     */
    public QueryExecutionInfo getEachQueryResultExecutionInfo() {
        return eachQueryResultExecutionInfo;
    }

    /**
     * Get the last used {@link MethodExecutionInfo} in {@link ProxyExecutionListener#beforeMethod(MethodExecutionInfo)}.
     *
     * @return last used {@link MethodExecutionInfo}. Can be {@code null} if not invoked yet.
     */
    public MethodExecutionInfo getBeforeMethodExecutionInfo() {
        return beforeMethodExecutionInfo;
    }

    /**
     * Get the last used {@link MethodExecutionInfo} in {@link ProxyExecutionListener#afterMethod(MethodExecutionInfo)}.
     *
     * @return last used {@link MethodExecutionInfo}. Can be {@code null} if not invoked yet.
     */
    public MethodExecutionInfo getAfterMethodExecutionInfo() {
        return afterMethodExecutionInfo;
    }
}
