/*
 * Copyright 2019 the original author or authors.
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
 *
 */

package io.r2dbc.proxy.core;

import io.r2dbc.proxy.listener.ProxyExecutionListener;

/**
 * @author Tadaya Tsuyukubo
 */
public interface StatementInfo {

    ConnectionInfo getConnectionInfo();

    String getOriginalQuery();

    String getUpdatedQuery();

    /**
     * Store key/value pair.
     *
     * Mainly used for passing values between {@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)} and
     * {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}.
     *
     * @param key   key
     * @param value value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    void addCustomValue(String key, Object value);

    /**
     * Retrieve value from key/value store.
     *
     * @param key  key
     * @param type value class
     * @param <T>  return type
     * @return value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    <T> T getCustomValue(String key, Class<T> type);


}
