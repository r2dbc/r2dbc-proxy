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

package io.r2dbc.proxy.core;


import io.r2dbc.spi.Connection;

/**
 * Hold {@link Connection} related information.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ConnectionInfo {

    /**
     * Retrieve original {@link Connection}.
     *
     * @return connection
     */
    Connection getOriginalConnection();

    /**
     * Get ID for the connection.
     *
     * @return connection ID
     * @see io.r2dbc.proxy.callback.ConnectionIdManager
     */
    String getConnectionId();

    /**
     * Increment transaction count.
     */
    void incrementTransactionCount();

    /**
     * Increment commit count.
     */
    void incrementCommitCount();

    /**
     * Increment rollback count.
     */
    void incrementRollbackCount();

    /**
     * Returns how many times {@link Connection#beginTransaction()} method is called.
     *
     * @return num of beginTransaction() method being called
     */
    int getTransactionCount();

    /**
     * Returns how many times {@link Connection#commitTransaction()} method is called.
     *
     * @return num of commitTransaction method being called
     */
    int getCommitCount();

    /**
     * Returns how many times {@link Connection#rollbackTransaction()} method is called.
     *
     * @return num of rollback methods being called
     */
    int getRollbackCount();

    /**
     * Returns whether connection is closed or not.
     *
     * @return {@code true} if connection is closed
     */
    boolean isClosed();

    /**
     * Set {@code boolean} to indicate whether the connection is closed or not.
     *
     * @param closed set {@code true} if {@link Connection} is closed
     */
    void setClosed(boolean closed);

    /**
     * Retrieve {@link ValueStore} which is associated to the scope of logical connection.
     *
     * <p>Values can be stored or retrieved from this store while connection is available.
     *
     * @return value store
     */
    ValueStore getValueStore();

}
