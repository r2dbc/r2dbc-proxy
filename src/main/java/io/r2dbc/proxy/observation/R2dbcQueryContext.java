/*
 * Copyright 2022 the original author or authors.
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

package io.r2dbc.proxy.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Observation.Context} for r2dbc query.
 *
 * @author Tadaya Tsuyukubo
 */
public class R2dbcQueryContext extends SenderContext<Object> {

    private String connectionName;

    private String threadName;

    private List<String> queries = new ArrayList<>();

    private List<String> params = new ArrayList<>();

    public R2dbcQueryContext() {
        super((carrier, key, value) -> {
            // no-op setter
        }, Kind.CLIENT);
    }

    public String getConnectionName() {
        return this.connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public List<String> getQueries() {
        return this.queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

    public List<String> getParams() {
        return this.params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }
}
