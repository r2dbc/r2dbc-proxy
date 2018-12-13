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


import io.r2dbc.spi.Connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Connection} related information.
 *
 * @author Tadaya Tsuyukubo
 */
public class ConnectionInfo {

    private Connection originalConnection;

    private String connectionId;

    private AtomicBoolean isClosed = new AtomicBoolean();

    private AtomicInteger transactionCount = new AtomicInteger();

    private AtomicInteger commitCount = new AtomicInteger();

    private AtomicInteger rollbackCount = new AtomicInteger();

    // TODO: may keep transaction isolation level

    public Connection getOriginalConnection() {
        return this.originalConnection;
    }

    public void setOriginalConnection(Connection originalConnection) {
        this.originalConnection = originalConnection;
    }

    public String getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * Increment transaction count.
     */
    public void incrementTransactionCount() {
        this.transactionCount.incrementAndGet();
    }

    /**
     * Increment commit count.
     */
    public void incrementCommitCount() {
        this.commitCount.incrementAndGet();
    }

    /**
     * Increment rollback count.
     */
    public void incrementRollbackCount() {
        this.rollbackCount.incrementAndGet();
    }


    /**
     * Returns how many times {@link Connection#beginTransaction()} method is called.
     *
     * @return num of beginTransaction() method being called
     */
    public int getTransactionCount() {
        return this.transactionCount.get();
    }

    /**
     * Returns how many times {@link Connection#commitTransaction()} method is called.
     *
     * @return num of commitTransaction method being called
     */
    public int getCommitCount() {
        return this.commitCount.get();
    }

    /**
     * Returns how many times {@link Connection#rollbackTransaction()} method is called.
     *
     * @return num of rollback methods being called
     */
    public int getRollbackCount() {
        return this.rollbackCount.get();
    }

    /**
     * Returns whether connection is closed or not.
     *
     * @return {@code true} if connection is closed
     */
    public boolean isClosed() {
        return this.isClosed.get();
    }

    /**
     * Indicate connection is closed.
     *
     * @param closed closed
     */
    public void setClosed(boolean closed) {
        this.isClosed.set(closed);
    }

}
