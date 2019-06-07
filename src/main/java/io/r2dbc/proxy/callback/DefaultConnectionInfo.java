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
import io.r2dbc.proxy.core.ValueStore;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Default implementation for {@link ConnectionInfo}.
 *
 * @author Tadaya Tsuyukubo
 */
final class DefaultConnectionInfo implements ConnectionInfo {

    private static final AtomicIntegerFieldUpdater<DefaultConnectionInfo> TRANSACTION_COUNT_INCREMENTER =
        AtomicIntegerFieldUpdater.newUpdater(DefaultConnectionInfo.class, "transactionCount");

    private static final AtomicIntegerFieldUpdater<DefaultConnectionInfo> COMMIT_COUNT_INCREMENTER =
        AtomicIntegerFieldUpdater.newUpdater(DefaultConnectionInfo.class, "commitCount");

    private static final AtomicIntegerFieldUpdater<DefaultConnectionInfo> ROLLBACK_COUNT_INCREMENTER =
        AtomicIntegerFieldUpdater.newUpdater(DefaultConnectionInfo.class, "rollbackCount");


    private Connection originalConnection;

    private String connectionId;

    private AtomicBoolean isClosed = new AtomicBoolean();

    // access via TRANSACTION_COUNT_INCREMENTER
    private volatile int transactionCount = 0;

    // access via COMMIT_COUNT_INCREMENTER
    private volatile int commitCount = 0;

    // access via ROLLBACK_COUNT_INCREMENTER
    private volatile int rollbackCount = 0;

    private ValueStore valueStore = ValueStore.create();

    // TODO: may keep transaction isolation level

    /**
     * Set original {@link Connection}.
     *
     * @param originalConnection original connection
     * @throws IllegalArgumentException if {@code originalConnection} is {@code null}
     */
    public void setOriginalConnection(Connection originalConnection) {
        Assert.requireNonNull(originalConnection, "originalConnection must not be null");

        this.originalConnection = originalConnection;
    }

    /**
     * Set connection ID.
     *
     * @param connectionId connection ID
     * @throws IllegalArgumentException if {@code connectionId} is {@code null}
     */
    public void setConnectionId(String connectionId) {
        Assert.requireNonNull(connectionId, "connectionId must not be null");

        this.connectionId = connectionId;
    }

    @Override
    public void setClosed(boolean closed) {
        this.isClosed.set(closed);
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
        TRANSACTION_COUNT_INCREMENTER.incrementAndGet(this);
    }

    @Override
    public void incrementCommitCount() {
        COMMIT_COUNT_INCREMENTER.incrementAndGet(this);
    }

    @Override
    public void incrementRollbackCount() {
        ROLLBACK_COUNT_INCREMENTER.incrementAndGet(this);
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
        return this.isClosed.get();
    }

    @Override
    public ValueStore getValueStore() {
        return this.valueStore;
    }
}
