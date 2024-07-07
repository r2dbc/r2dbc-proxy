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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

import java.util.HashSet;
import java.util.Set;

/**
 * Default {@link ObservationConvention} for R2DBC query execution.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1.0
 */
public interface QueryObservationConvention extends ObservationConvention<QueryContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof QueryContext;
    }

    @Override
    default String getName() {
        return "r2dbc.query";
    }

    @Override
    default String getContextualName(QueryContext context) {
        return "query";
    }

    @Override
    default KeyValues getLowCardinalityKeyValues(QueryContext context) {
        Set<KeyValue> keyValues = new HashSet<>();
        keyValues.add(KeyValue.of(R2dbcObservationDocumentation.LowCardinalityKeys.CONNECTION, context.getConnectionName()));
        return KeyValues.of(keyValues);
    }

    @Override
    default KeyValues getHighCardinalityKeyValues(QueryContext context) {
        Set<KeyValue> keyValues = new HashSet<>();
        for (int i = 0; i < context.getQueries().size(); i++) {
            String key = context.getQueries().get(i);
            String queryKey = String.format(R2dbcObservationDocumentation.HighCardinalityKeys.QUERY.asString(), i);
            keyValues.add(KeyValue.of(queryKey, key));
        }
        // params could be empty when "includeParameterValues=false" in the listener.
        for (int i = 0; i < context.getParams().size(); i++) {
            String params = context.getParams().get(i);
            String key = String.format(R2dbcObservationDocumentation.HighCardinalityKeys.QUERY_PARAMETERS.asString(), i);
            keyValues.add(KeyValue.of(key, params));
        }
        keyValues.add(KeyValue.of(R2dbcObservationDocumentation.HighCardinalityKeys.THREAD, context.getThreadName()));
        return KeyValues.of(keyValues);
    }
}
