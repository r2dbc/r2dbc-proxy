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

package io.r2dbc.proxy.test;

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of {@link StatementInfo}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class MockStatementInfo implements StatementInfo {

    /**
     * Provide a builder for {@link MockStatementInfo}.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Provide an empty {@link MockStatementInfo}.
     *
     * @return a {@link MockStatementInfo}
     */
    public static MockStatementInfo empty() {
        return builder().build();
    }

    private ConnectionInfo connectionInfo;

    private String originalQuery;

    private String updatedQuery;

    private Map<String, Object> customValues;


    private MockStatementInfo(Builder builder) {
        this.connectionInfo = builder.connectionInfo;
        this.originalQuery = builder.originalQuery;
        this.updatedQuery = builder.updatedQuery;
        this.customValues = builder.customValues;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    @Override
    public String getOriginalQuery() {
        return this.originalQuery;
    }

    @Override
    public String getUpdatedQuery() {
        return this.updatedQuery;
    }

    @Override
    public void addCustomValue(String key, Object value) {
        Assert.requireNonNull(key, "key must not be null");

        this.customValues.put(key, value);
    }

    @Override
    public <T> T getCustomValue(String key, Class<T> type) {
        Assert.requireNonNull(key, "key must not be null");
        Assert.requireNonNull(type, "type must not be null");

        return type.cast(this.customValues.get(key));
    }

    public static final class Builder {

        private ConnectionInfo connectionInfo;

        private String originalQuery;

        private String updatedQuery;

        private Map<String, Object> customValues = new HashMap<>();

        public ConnectionInfo getConnectionInfo() {
            return this.connectionInfo;
        }

        public Builder connectionInfo(ConnectionInfo connectionInfo) {
            this.connectionInfo = connectionInfo;
            return this;
        }

        public Builder originalQuery(String originalQuery) {
            this.originalQuery = originalQuery;
            return this;
        }

        public Builder updatedQuery(String updatedQuery) {
            this.updatedQuery = updatedQuery;
            return this;
        }

        public Builder customValue(String key, Object value) {
            this.customValues.put(key, value);
            return this;
        }

        public Builder customValues(Map<String, Object> customValues) {
            this.customValues = customValues;
            return this;
        }

        public MockStatementInfo build() {
            return new MockStatementInfo(this);
        }

    }
}
