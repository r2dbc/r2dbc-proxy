/*
 * Copyright 2018 the original author or authors.
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

package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.BoundValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.core.R2dbcProxyException;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.listener.BindParameterConverter;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Proxy callback handler for {@link Statement}.
 *
 * @author Tadaya Tsuyukubo
 */
public final class StatementCallbackHandler extends CallbackHandlerSupport {

    private final Statement statement;

    private final ConnectionInfo connectionInfo;

    private final StatementInfo statementInfo;

    private final List<Bindings> bindings = new ArrayList<>();

    private int currentBindingsIndex = 0;

    public StatementCallbackHandler(Statement statement, StatementInfo statementInfo, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.statement = Assert.requireNonNull(statement, "statement must not be null");
        this.statementInfo = Assert.requireNonNull(statementInfo, "originalQuery must not be null");
        this.connectionInfo = Assert.requireNonNull(connectionInfo, "connectionInfo must not be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Assert.requireNonNull(proxy, "proxy must not be null");
        Assert.requireNonNull(method, "method must not be null");

        String methodName = method.getName();

        if ("unwrap".equals(methodName)) {
            return this.statement;
        } else if ("unwrapConnection".equals(methodName)) {
            return this.connectionInfo.getOriginalConnection();
        }

        if ("bind".equals(methodName) || "bindNull".equals(methodName)) {

            BoundValue boundValue;
            if ("bind".equals(methodName)) {
                boundValue = BoundValue.value(args[1]);
            } else {
                boundValue = BoundValue.nullValue((Class<?>) args[1]);
            }

            boolean isIndexBinding = args[0] instanceof Integer;

            Binding binding;
            if (isIndexBinding) {
                binding = new Bindings.IndexBinding((int) args[0], boundValue);
            } else {
                binding = new Bindings.IdentifierBinding(args[0], boundValue);
            }

            // when converter decides to perform original binding behavior, this lambda will be called.
            BindParameterConverter.BindOperation onBind = () -> {

                try {
                    proceedExecution(method, this.statement, args, this.proxyConfig.getListeners(), this.connectionInfo, null, null);
                } catch (Throwable throwable) {
                    throw new R2dbcProxyException("Failed to perform " + methodName, throwable);
                }

                if (this.bindings.size() <= this.currentBindingsIndex) {
                    this.bindings.add(new Bindings());
                }

                Bindings bindings = this.bindings.get(this.currentBindingsIndex);
                if (isIndexBinding) {
                    bindings.addIndexBinding((Bindings.IndexBinding) binding);
                } else {
                    bindings.addIdentifierBinding((Bindings.IdentifierBinding) binding);
                }

                return (Statement) proxy;
            };


            MutableBindInfo bindInfo = new MutableBindInfo();
            bindInfo.setStatementInfo(this.statementInfo);
            bindInfo.setBinding(binding);

            // callback for binding operation
            this.proxyConfig.getBindParameterConverter().onBind(bindInfo, (Statement) proxy, onBind);

            return proxy;
        }

        Object result = proceedExecution(method, this.statement, args, this.proxyConfig.getListeners(), this.connectionInfo, null, null);

        // add, bind, bindNull, execute
        if ("add".equals(methodName)) {
            this.currentBindingsIndex++;
            return proxy;
        } else if ("execute".equals(methodName)) {

            // build QueryExecutionInfo
            QueryInfo queryInfo = new QueryInfo(this.statementInfo.getUpdatedQuery()); // TODO: may include original query info
            queryInfo.getBindingsList().addAll(this.bindings);
            List<QueryInfo> queries = Stream.of(queryInfo).collect(toList());

            MutableQueryExecutionInfo execInfo = new MutableQueryExecutionInfo();
            execInfo.setType(ExecutionType.STATEMENT);
            execInfo.setQueries(queries);
            execInfo.setBindingsSize(this.bindings.size());
            execInfo.setMethod(method);
            execInfo.setMethodArgs(args);
            execInfo.setConnectionInfo(this.connectionInfo);

            // API defines "execute()" returns a publisher
            Publisher<? extends Result> publisher = (Publisher<? extends Result>) result;

            return interceptQueryExecution(publisher, execInfo);
        } else if ("returnGeneratedValues".equals(methodName)) {
            return proxy;
        }

        return result;
    }

}
