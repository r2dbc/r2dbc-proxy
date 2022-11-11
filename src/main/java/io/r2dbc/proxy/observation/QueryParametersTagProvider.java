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

import io.r2dbc.proxy.core.Bindings;

import java.util.List;

/**
 * Construct a tag value for query parameters.
 *
 * @author Tadaya Tsuyukubo
 */
public interface QueryParametersTagProvider {

    /**
     * Provide query parameter values.
     *
     * @param bindingsList list of bind operations
     * @return string representation
     */
    String getTagValue(List<Bindings> bindingsList);

}
