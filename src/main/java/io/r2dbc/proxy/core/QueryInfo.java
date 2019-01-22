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

import io.r2dbc.proxy.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Hold each query related info.
 *
 * @author Tadaya Tsuyukubo
 */
public class QueryInfo {

    private final String query;

    private final List<Bindings> bindingsList = new ArrayList<>();

    /**
     * Construct the {@code QueryInfo} with query.
     *
     * @param query query
     * @throws IllegalArgumentException if {@code query} is {@code null}
     */
    public QueryInfo(String query) {
        this.query = Assert.requireNonNull(query, "query must not be null");
    }

    /**
     * Get the query.
     *
     * @return query; never {@code null}
     */
    public String getQuery() {
        return this.query;
    }

    /**
     * Get the list of {@link Bindings}.
     *
     * @return list of bindings; never {@code null}
     */
    public List<Bindings> getBindingsList() {
        return this.bindingsList;
    }
}
