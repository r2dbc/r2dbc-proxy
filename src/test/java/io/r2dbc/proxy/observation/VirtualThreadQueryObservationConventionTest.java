/*
 * Copyright 2025 the original author or authors.
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link VirtualThreadQueryObservationConvention}.
 *
 * @author Tadaya Tsuyukubo
 */
class VirtualThreadQueryObservationConventionTest {

    VirtualThreadQueryObservationConvention convention = new VirtualThreadQueryObservationConvention() {

    };

    @Test
    void keyValues() {
        QueryContext context = new QueryContext();
        context.setConnectionName("my-connection");
        context.setThreadName("my-thread");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).containsOnly(KeyValue.of(R2dbcObservationDocumentation.LowCardinalityKeys.CONNECTION.asString(), "my-connection"));
    }

}
