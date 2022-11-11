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
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter;

import java.util.List;

/**
 * @author Tadaya Tsuyukubo
 */
public class DefaultQueryParametersTagProvider implements QueryParametersTagProvider {

    private final QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();

    public String getTagValue(List<Bindings> bindingsList) {
        StringBuilder sb = new StringBuilder();

        sb.append("(");
        for (Bindings bindings : bindingsList) {
            if (!bindings.getIndexBindings().isEmpty()) {
                this.formatter.onIndexBindings.accept(bindings.getIndexBindings(), sb);
                sb.append(",");
            } else if (!bindings.getNamedBindings().isEmpty()) {
                this.formatter.onNamedBindings.accept(bindings.getNamedBindings(), sb);
                sb.append(",");
            }
        }
        chompIfEndWith(sb, ',');
        sb.append(")");
        return sb.toString();
    }

    protected void chompIfEndWith(StringBuilder sb, char c) {
        final int lastCharIndex = sb.length() - 1;
        if (lastCharIndex >= 0 && sb.charAt(lastCharIndex) == c) {
            sb.deleteCharAt(lastCharIndex);
        }
    }
}
