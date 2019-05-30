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
import io.r2dbc.proxy.core.ValueStore;
import io.r2dbc.spi.Connection;

/**
 * Mock implementation of {@link ConnectionInfo} for testing.
 *
 * @author Tadaya Tsuyukubo
 */
public final class MockConnectionInfo implements ConnectionInfo {

    /**
     * Provides a builder for {@link MockConnectionInfo}.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Provide an empty {@link MockConnectionInfo}.
     *
     * @return a {@link MockConnectionInfo}.
     */
    public static MockConnectionInfo empty() {
        return builder().build();
    }

    private Connection originalConnection;

    private String connectionId;

    private boolean isClosed;

    private int transactionCount;

    private int commitCount;

    private int rollbackCount;

    private ValueStore valueStore;

    private MockConnectionInfo(Builder builder) {
        this.originalConnection = builder.originalConnection;
        this.connectionId = builder.connectionId;
        this.isClosed = builder.isClosed;
        this.transactionCount = builder.transactionCount;
        this.commitCount = builder.commitCount;
        this.rollbackCount = builder.rollbackCount;
        this.valueStore = builder.valueStore;
    }

    @Override
    public Connection getOriginalConnection() {
        return this.originalConnection;
    }

    @Override
    public String getConnectionId() {
        return this.connectionId;
    }

    @Override
    public void incrementTransactionCount() {
        // no-op
    }

    @Override
    public void incrementCommitCount() {
        // no-op
    }

    @Override
    public void incrementRollbackCount() {
        // no-op
    }

    @Override
    public int getTransactionCount() {
        return this.transactionCount;
    }

    @Override
    public int getCommitCount() {
        return this.commitCount;
    }

    @Override
    public int getRollbackCount() {
        return this.rollbackCount;
    }

    @Override
    public boolean isClosed() {
        return this.isClosed;
    }

    @Override
    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }

    @Override
    public ValueStore getValueStore() {
        return this.valueStore;
    }

    public static final class Builder {

        private Connection originalConnection;

        private String connectionId;

        private boolean isClosed;

        private int transactionCount;

        private int commitCount;

        private int rollbackCount;

        private ValueStore valueStore = ValueStore.create();

        private Builder() {
        }

        public Builder originalConnection(Connection originalConnection) {
            this.originalConnection = originalConnection;
            return this;
        }

        public Builder connectionId(String connectionId) {
            this.connectionId = connectionId;
            return this;
        }

        public Builder isClosed(boolean isClosed) {
            this.isClosed = isClosed;
            return this;
        }

        public Builder transactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
            return this;
        }

        public Builder commitCount(int commitCount) {
            this.commitCount = commitCount;
            return this;
        }

        public Builder rollbackCount(int rollbackCount) {
            this.rollbackCount = rollbackCount;
            return this;
        }

        public Builder valueStore(ValueStore valueStore) {
            this.valueStore = valueStore;
            return this;
        }

        public MockConnectionInfo build() {
            return new MockConnectionInfo(this);
        }
    }

}
