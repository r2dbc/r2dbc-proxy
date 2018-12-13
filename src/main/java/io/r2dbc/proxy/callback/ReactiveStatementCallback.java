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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.BindingValue;
import io.r2dbc.proxy.core.BindingValue.NullBindingValue;
import io.r2dbc.proxy.core.BindingValue.SimpleBindingValue;
import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Proxy callback for {@link Statement}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveStatementCallback extends CallbackSupport {

    private Statement<?> statement;

    private ConnectionInfo connectionInfo;

    private String query;

    private List<Bindings> bindings = new ArrayList<>();

    private int currentBindingsIndex = 0;

    public ReactiveStatementCallback(Statement<?> statement, String query, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.statement = statement;
        this.query = query;
        this.connectionInfo = connectionInfo;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();

        if ("unwrap".equals(methodName)) {
            return this.statement;
        } else if ("getOriginalConnection".equals(methodName)) {
            return this.connectionInfo.getOriginalConnection();
        }

        Object result = proceedExecution(method, this.statement, args, this.proxyConfig.getListeners(), this.connectionInfo, null, null);

        // add, bind, bindNull, execute
        if ("add".equals(methodName)) {
            this.currentBindingsIndex++;
        } else if ("bind".equals(methodName) || "bindNull".equals(methodName)) {

            if (this.bindings.size() <= this.currentBindingsIndex) {
                this.bindings.add(new Bindings());
            }
            Bindings bindings = this.bindings.get(this.currentBindingsIndex);

            BindingValue bindingValue;
            if ("bind".equals(methodName)) {
                bindingValue = new SimpleBindingValue(args[1]);
            } else {
                bindingValue = new NullBindingValue((Class<?>) args[1]);
            }

            if (args[0] instanceof Integer) {
                bindings.addIndexBinding((int) args[0], bindingValue);
            } else {
                bindings.addIdentifierBinding(args[0], bindingValue);
            }
        } else if ("execute".equals(methodName)) {

            // build QueryExecutionInfo
            QueryInfo queryInfo = new QueryInfo(this.query);
            queryInfo.getBindingsList().addAll(this.bindings);
            List<QueryInfo> queries = Stream.of(queryInfo).collect(toList());

            QueryExecutionInfo execInfo = new QueryExecutionInfo();
            execInfo.setType(ExecutionType.STATEMENT);
            execInfo.setQueries(queries);
            execInfo.setBindingsSize(this.bindings.size());
            execInfo.setMethod(method);
            execInfo.setMethodArgs(args);
            execInfo.setConnectionInfo(this.connectionInfo);

            // API defines "execute()" returns a publisher
            Publisher<? extends Result> publisher = (Publisher<? extends Result>) result;

            return interceptQueryExecution(publisher, execInfo);
        }

        return result;
    }

}
