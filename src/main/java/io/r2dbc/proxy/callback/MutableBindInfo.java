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

import io.r2dbc.proxy.core.BindInfo;
import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.StatementInfo;

/**
 * @author Tadaya Tsuyukubo
 */
final class MutableBindInfo implements BindInfo {

    private StatementInfo statementInfo;

    private Binding binding;

    @Override
    public StatementInfo getStatementInfo() {
        return this.statementInfo;
    }

    public void setStatementInfo(StatementInfo statementInfo) {
        this.statementInfo = statementInfo;
    }

    @Override
    public Binding getBinding() {
        return this.binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

}
