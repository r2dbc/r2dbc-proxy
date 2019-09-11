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

import io.r2dbc.proxy.listener.BindParameterConverter;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;

/**
 * Hold {@link Statement} related information.
 *
 * @author Tadaya Tsuyukubo
 */
public interface StatementInfo {

    /**
     * Get {@link ConnectionInfo} associated to this {@link Statement}.
     *
     * @return connection info
     */
    ConnectionInfo getConnectionInfo();

    /**
     * Get the sql statement that has originally specified on {@link Connection#createStatement(String)}.
     *
     * @return original sql statement
     */
    String getOriginalQuery();

    /**
     * Get the updated sql statement by {@link BindParameterConverter#onCreateStatement(String, StatementInfo)}.
     *
     * @return updated sql statement
     */
    String getUpdatedQuery();

    /**
     * Retrieve {@link ValueStore} which is associated to the scope of logical statement.
     *
     * <p>Values can be stored or retrieved from this store while statement is available.
     *
     * @return value store
     */
    ValueStore getValueStore();

}
