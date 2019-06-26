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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.core.ValueStore;

/**
 * @author Tadaya Tsuyukubo
 */
final class MutableStatementInfo implements StatementInfo {

    private ConnectionInfo connectionInfo;

    private String originalQuery;

    private String updatedQuery;

    private ValueStore valueStore = ValueStore.create();

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public String getOriginalQuery() {
        return this.originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    @Override
    public String getUpdatedQuery() {
        return this.updatedQuery;
    }

    public void setUpdatedQuery(String updatedQuery) {
        this.updatedQuery = updatedQuery;
    }

    @Override
    public ValueStore getValueStore() {
        return this.valueStore;
    }
}
