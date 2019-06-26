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
import io.r2dbc.proxy.core.ValueStore;

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

    private ValueStore valueStore;


    private MockStatementInfo(Builder builder) {
        this.connectionInfo = builder.connectionInfo;
        this.originalQuery = builder.originalQuery;
        this.updatedQuery = builder.updatedQuery;
        this.valueStore = builder.valueStore;
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
    public ValueStore getValueStore() {
        return this.valueStore;
    }

    public static final class Builder {

        private ConnectionInfo connectionInfo;

        private String originalQuery;

        private String updatedQuery;

        private ValueStore valueStore = ValueStore.create();

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

        public Builder valueStore(ValueStore valueStore) {
            this.valueStore = valueStore;
            return this;
        }

        public MockStatementInfo build() {
            return new MockStatementInfo(this);
        }

    }
}
