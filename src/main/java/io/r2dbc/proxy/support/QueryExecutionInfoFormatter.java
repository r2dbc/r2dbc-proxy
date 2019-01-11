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

package io.r2dbc.proxy.support;

import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.BindingValue;
import io.r2dbc.proxy.core.BindingValue.NullBindingValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Convert {@link QueryExecutionInfo} to {@code String}.
 *
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfoFormatter implements Function<QueryExecutionInfo, String> {

    private static final String DEFAULT_DELIMITER = " ";

    /**
     * Default implementation for formatting thread info.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onThread = (executionInfo, sb) -> {
        sb.append("Thread:");
        sb.append(executionInfo.getThreadName());
        sb.append("(");
        sb.append(executionInfo.getThreadId());
        sb.append(")");
    };

    /**
     * Default implementation for formatting connection.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onConnection = (executionInfo, sb) -> {
        sb.append("Connection:");
        sb.append(executionInfo.getConnectionInfo().getConnectionId());
    };

    /**
     * Default implementation for formatting transaction releated info.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onTransactionInfo = (executionInfo, sb) -> {
        sb.append("Transaction:");
        ConnectionInfo connectionInfo = executionInfo.getConnectionInfo();
        sb.append("{Create:");
        sb.append(connectionInfo.getTransactionCount());
        sb.append(" Rollback:");
        sb.append(connectionInfo.getRollbackCount());
        sb.append(" Commit:");
        sb.append(connectionInfo.getCommitCount());
        sb.append("}");
    };

    /**
     * Default implementation for formatting success.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onSuccess = (executionInfo, sb) -> {
        sb.append("Success:");
        sb.append(executionInfo.isSuccess() ? "True" : "False");
    };

    /**
     * Default implementation for formatting execution time.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onTime = (executionInfo, sb) -> {
        sb.append("Time:");
        sb.append(executionInfo.getExecuteDuration().toMillis());
    };

    /**
     * Default implementation for formatting execution type.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onType = (executionInfo, sb) -> {
        sb.append("Type:");
        sb.append(executionInfo.getType() == ExecutionType.BATCH ? "Batch" : "Statement");
    };

    /**
     * Default implementation for formatting batch size.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onBatchSize = (executionInfo, sb) -> {
        sb.append("BatchSize:");
        sb.append(executionInfo.getBatchSize());
    };

    /**
     * Default implementation for formatting size of bindings.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onBindingsSize = (executionInfo, sb) -> {
        sb.append("BindingsSize:");
        sb.append(executionInfo.getBindingsSize());
    };

    /**
     * Default implementation for formatting queries.
     */
    private BiConsumer<QueryExecutionInfo, StringBuilder> onQuery = (executionInfo, sb) -> {
        sb.append("Query:[");

        List<QueryInfo> queries = executionInfo.getQueries();
        if (!queries.isEmpty()) {
            String s = queries.stream()
                .map(QueryInfo::getQuery)
                .collect(joining("\",\"", "\"", "\""));
            sb.append(s);
        }

        sb.append("]");
    };

    /**
     * Default implementation for formatting binding value.
     */
    public BiConsumer<BindingValue, StringBuilder> onBindingValue = (bindingValue, sb) -> {
        if (bindingValue instanceof NullBindingValue) {
            Class<?> type = ((NullBindingValue) bindingValue).getType();
            sb.append("null(");
            sb.append(type.getSimpleName());
            sb.append(")");
        } else {
            sb.append(bindingValue.getValue());
        }
    };

    /**
     * Default implementation for formatting bindings by index.
     *
     * generate comma separated values. "val1,val2,val3"
     */
    public BiConsumer<SortedSet<Binding>, StringBuilder> onIndexBindings = (indexBindings, sb) -> {
        String s = indexBindings.stream()
            .map(Binding::getBindingValue)
            .map(bindingValue -> {
                StringBuilder sbuilder = new StringBuilder();
                this.onBindingValue.accept(bindingValue, sbuilder);
                return sbuilder.toString();
            })
            .collect(joining(","));

        sb.append(s);
    };

    /**
     * Default implementation for formatting bindings by identifier.
     *
     * Generate comma separated key-values pair string. "key1=val1,key2=val2,key3=val3"
     */
    public BiConsumer<SortedSet<Binding>, StringBuilder> onIdentifierBindings = (identifierBindings, sb) -> {
        String s = identifierBindings.stream()
            .map(binding -> {
                StringBuilder sbuilder = new StringBuilder();
                sbuilder.append(binding.getKey());
                sbuilder.append("=");
                this.onBindingValue.accept(binding.getBindingValue(), sbuilder);
                return sbuilder.toString();
            })
            .collect(joining(","));
        sb.append(s);
    };

    /**
     * Default implementation for formatting bindings.
     */
    public BiConsumer<QueryExecutionInfo, StringBuilder> onBindings = (executionInfo, sb) -> {
        sb.append("Bindings:[");

        List<QueryInfo> queries = executionInfo.getQueries();
        if (!queries.isEmpty()) {
            String s = queries.stream()
                .map(QueryInfo::getBindingsList)
                .filter(bindings -> !bindings.isEmpty())
                .map(bindings -> bindings.stream()
                    .map(binds -> {
                        StringBuilder sbForBindings = new StringBuilder();
                        SortedSet<Binding> indexBindings = binds.getIndexBindings();
                        if (!indexBindings.isEmpty()) {
                            this.onIndexBindings.accept(indexBindings, sbForBindings);
                        }

                        SortedSet<Binding> identifierBindings = binds.getIdentifierBindings();
                        if (!identifierBindings.isEmpty()) {
                            this.onIdentifierBindings.accept(identifierBindings, sbForBindings);
                        }
                        return sbForBindings.toString();
                    })
                    .collect(joining("),(", "(", ")")))
                .collect(joining(","));
            sb.append(s);
        }

        sb.append("]");
    };

    private BiConsumer<QueryExecutionInfo, StringBuilder> newLine = (executionInfo, sb) -> {
        sb.append(System.lineSeparator());
    };

    private String delimiter = DEFAULT_DELIMITER;

    private List<BiConsumer<QueryExecutionInfo, StringBuilder>> consumers = new ArrayList<>();


    /**
     * Create a {@link QueryExecutionInfoFormatter} which writes out all attributes on {@link QueryExecutionInfo}.
     *
     * @return a formatter
     */
    public static QueryExecutionInfoFormatter showAll() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.addConsumer(formatter.onThread);
        formatter.addConsumer(formatter.onConnection);
        formatter.addConsumer(formatter.onTransactionInfo);
        formatter.addConsumer(formatter.onSuccess);
        formatter.addConsumer(formatter.onTime);
        formatter.addConsumer(formatter.onType);
        formatter.addConsumer(formatter.onBatchSize);
        formatter.addConsumer(formatter.onBindingsSize);
        formatter.addConsumer(formatter.onQuery);
        formatter.addConsumer(formatter.onBindings);
        return formatter;
    }

    /**
     * Register a {@code BiConsumer} that convert {@link QueryExecutionInfo} to {@code String}.
     *
     * @param consumer a {@code BiConsumer} that takes a {@link QueryExecutionInfo} and write to the {@code StringBuilder}.
     * @return this formatter
     */
    public QueryExecutionInfoFormatter addConsumer(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    /**
     * Convert the given {@link QueryExecutionInfo} to {@code String} using registered consumers.
     *
     * @param executionInfo input
     * @return formatted sting
     */
    public String format(QueryExecutionInfo executionInfo) {

        StringBuilder sb = new StringBuilder();

        consumers.forEach(consumer -> {
            consumer.accept(executionInfo, sb);

            // if it is for new line, skip adding delimiter
            if (consumer != this.newLine) {
                sb.append(this.delimiter);
            }
        });

        chompIfEndWith(sb, this.delimiter);

        return sb.toString();

    }

    @Override
    public String apply(QueryExecutionInfo executionInfo) {
        return format(executionInfo);
    }

    protected void chompIfEndWith(StringBuilder sb, String s) {
        if (sb.length() < s.length()) {
            return;
        }
        final int startIndex = sb.length() - s.length();
        if (sb.substring(startIndex, sb.length()).equals(s)) {
            sb.delete(startIndex, sb.length());
        }
    }

    /**
     * Set a delimiter between each consumer
     *
     * @param delimiter delimiter
     * @return formatter
     */
    public QueryExecutionInfoFormatter delimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     *
     * @return
     */
    public QueryExecutionInfoFormatter showThread() {
        this.consumers.add(this.onThread);
        return this;
    }

    public QueryExecutionInfoFormatter showThread(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onThread = consumer;
        return showThread();
    }

    public QueryExecutionInfoFormatter showConnection() {
        this.consumers.add(this.onConnection);
        return this;
    }

    public QueryExecutionInfoFormatter showConnection(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onConnection = consumer;
        return showConnection();
    }

    public QueryExecutionInfoFormatter showTransaction() {
        this.consumers.add(this.onTransactionInfo);
        return this;
    }

    public QueryExecutionInfoFormatter showTransaction(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onTransactionInfo = consumer;
        return showTransaction();
    }

    public QueryExecutionInfoFormatter showSuccess() {
        this.consumers.add(this.onSuccess);
        return this;
    }

    public QueryExecutionInfoFormatter showSuccess(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onSuccess = consumer;
        return showSuccess();
    }

    public QueryExecutionInfoFormatter showTime() {
        this.consumers.add(this.onTime);
        return this;
    }

    public QueryExecutionInfoFormatter showTime(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onTime = consumer;
        return showTime();
    }

    public QueryExecutionInfoFormatter showType() {
        this.consumers.add(this.onType);
        return this;
    }

    public QueryExecutionInfoFormatter showType(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onType = consumer;
        return showType();
    }


    public QueryExecutionInfoFormatter showBatchSize() {
        this.consumers.add(this.onBatchSize);
        return this;
    }

    public QueryExecutionInfoFormatter showBatchSize(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onBatchSize = consumer;
        return showBatchSize();
    }

    public QueryExecutionInfoFormatter showBindingsSize() {
        this.consumers.add(this.onBindingsSize);
        return this;
    }

    public QueryExecutionInfoFormatter showBindingsSize(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onBindingsSize = consumer;
        return showBindingsSize();
    }


    public QueryExecutionInfoFormatter showQuery() {
        this.consumers.add(this.onQuery);
        return this;
    }

    public QueryExecutionInfoFormatter showQuery(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onQuery = consumer;
        return showQuery();
    }


    public QueryExecutionInfoFormatter showBindings() {
        this.consumers.add(this.onBindings);
        return this;
    }

    public QueryExecutionInfoFormatter showBindings(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onBindings = consumer;
        return showBindings();
    }

    /**
     * Change the line
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter newLine() {
        this.consumers.add(this.newLine);
        return this;
    }


    /**
     * Set a consumer for converting {@link BindingValue}.
     *
     * @param onBindingValue bi-consumer for binding value
     * @return formatter
     */
    public QueryExecutionInfoFormatter bindingValue(BiConsumer<BindingValue, StringBuilder> onBindingValue) {
        Assert.requireNonNull(onBindingValue, "onBindingValue must not be null");
        this.onBindingValue = onBindingValue;
        return this;
    }

    /**
     * Set a consumer for converting {@link SortedSet} of {@link Binding} constructed by bind-by-index.
     *
     * @param onIndexBindings bi-consumer for index-bindings
     * @return formatter
     */
    public QueryExecutionInfoFormatter indexBindings(BiConsumer<SortedSet<Binding>, StringBuilder> onIndexBindings) {
        Assert.requireNonNull(onIndexBindings, "onIndexBindings must not be null");
        this.onIndexBindings = onIndexBindings;
        return this;
    }

    /**
     * Set a consumer for converting {@link SortedSet} of {@link Binding} constructed by bind-by-identifier.
     *
     * @param onIdentifierBindings bi-consumer for identifier-bindings
     * @return formatter
     */
    public QueryExecutionInfoFormatter identifierBindings(BiConsumer<SortedSet<Binding>, StringBuilder> onIdentifierBindings) {
        Assert.requireNonNull(onIdentifierBindings, "onIdentifierBindings must not be null");
        this.onIdentifierBindings = onIdentifierBindings;
        return this;
    }

}
