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

package io.r2dbc.proxy.support;

import io.r2dbc.proxy.core.Binding;
import io.r2dbc.proxy.core.BoundValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.util.Assert;
import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Blob;
import io.r2dbc.spi.Clob;
import io.r2dbc.spi.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Convert {@link QueryExecutionInfo} to {@code String}.
 *
 * <p>Sample usage:
 * <pre>{@code
 *   // convert all info
 *   QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();
 *   String str = formatter.format(queryExecutionInfo);
 *
 *   // customize conversion
 *   QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
 *   formatter.addConsumer((execInfo, sb) -> {
 *     sb.append("MY-QUERY-EXECUTION="); // add prefix
 *   };
 *   formatter.newLine();  // new line
 *   formatter.showSuccess();
 *   formatter.addConsumer((execInfo, sb)  -> {
 *       // custom conversion
 *       sb.append("MY-ID=" + executionInfo.getConnectionInfo().getConnectionId());
 *   });
 *   formatter.showQuery();
 *
 *   // convert it
 *   String str = formatter.format(queryExecutionInfo);
 * }</pre>
 *
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfoFormatter implements Function<QueryExecutionInfo, String> {

    private static final String DEFAULT_DELIMITER = " ";

    /**
     * Default implementation for formatting thread info.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onThread = (executionInfo, sb) -> {
        sb.append("Thread:");
        sb.append(executionInfo.getThreadName());
        sb.append("(");
        sb.append(executionInfo.getThreadId());
        sb.append(")");
    };

    /**
     * Default implementation for formatting connection.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onConnection = (executionInfo, sb) -> {
        sb.append("Connection:");
        sb.append(executionInfo.getConnectionInfo().getConnectionId());
    };

    /**
     * Default implementation for formatting transaction releated info.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onTransactionInfo = (executionInfo, sb) -> {
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
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onSuccess = (executionInfo, sb) -> {
        sb.append("Success:");
        sb.append(executionInfo.isSuccess() ? "True" : "False");
    };

    /**
     * Default implementation for formatting execution time.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onTime = (executionInfo, sb) -> {
        sb.append("Time:");
        sb.append(executionInfo.getExecuteDuration().toMillis());
    };

    /**
     * Default implementation for formatting execution type.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onType = (executionInfo, sb) -> {
        sb.append("Type:");
        sb.append(executionInfo.getType() == ExecutionType.BATCH ? "Batch" : "Statement");
    };

    /**
     * Default implementation for formatting batch size.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onBatchSize = (executionInfo, sb) -> {
        sb.append("BatchSize:");
        sb.append(executionInfo.getBatchSize());
    };

    /**
     * Default implementation for formatting size of bindings.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onBindingsSize = (executionInfo, sb) -> {
        sb.append("BindingsSize:");
        sb.append(executionInfo.getBindingsSize());
    };

    /**
     * Default implementation for formatting queries.
     */
    private final BiConsumer<QueryExecutionInfo, StringBuilder> onQuery = (executionInfo, sb) -> {
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
     * Default implementation for formatting bound value.
     */
    public BiConsumer<BoundValue, StringBuilder> onBoundValue = (boundValue, sb) -> {
        if (boundValue.isNull()) {
            Class<?> type = boundValue.getNullType();
            sb.append("null(");
            sb.append(type.getSimpleName());
            sb.append(")");
        } else {
            Object value = boundValue.getValue();
            if (value instanceof Clob) {
                sb.append("<clob>");
            } else if (value instanceof Blob) {
                sb.append("<blob>");
            } else {
                sb.append(value);
            }
        }
    };

    /**
     * Default implementation for formatting bindings by index.
     *
     * generate comma separated values. "val1,val2,val3"
     */
    public BiConsumer<SortedSet<Binding>, StringBuilder> onIndexBindings = (indexBindings, sb) -> {
        String s = indexBindings.stream()
            .map(Binding::getBoundValue)
            .map(boundValue -> {
                StringBuilder sbuilder = new StringBuilder();
                this.onBoundValue.accept(boundValue, sbuilder);
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
                this.onBoundValue.accept(binding.getBoundValue(), sbuilder);
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

    public QueryExecutionInfoFormatter() {
    }

    private QueryExecutionInfoFormatter(QueryExecutionInfoFormatter formatter) {
        this.newLine = formatter.newLine;
        this.delimiter = formatter.delimiter;
        this.consumers.addAll(formatter.consumers);
    }

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
     * @throws IllegalArgumentException if {@code consumer} is {@code null}
     */
    public QueryExecutionInfoFormatter addConsumer(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");

        this.consumers.add(consumer);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Convert the given {@link QueryExecutionInfo} to {@code String} using registered consumers.
     *
     * @param executionInfo input
     * @return formatted sting
     * @throws IllegalArgumentException if {@code executionInfo} is {@code null}
     */
    public String format(QueryExecutionInfo executionInfo) {
        Assert.requireNonNull(executionInfo, "executionInfo must not be null");

        StringBuilder sb = new StringBuilder();

        this.consumers.forEach(consumer -> {
            consumer.accept(executionInfo, sb);

            // if it is for new line, skip adding delimiter
            if (consumer != this.newLine) {
                sb.append(this.delimiter);
            }
        });

        FormatterUtils.chompIfEndWith(sb, this.delimiter);

        return sb.toString();
    }

    @Override
    public String apply(QueryExecutionInfo executionInfo) {
        return format(executionInfo);
    }

    /**
     * Set a delimiter between each consumer
     *
     * @param delimiter delimiter
     * @return formatter
     * @throws IllegalArgumentException if {@code delimiter} is {@code null}
     */
    public QueryExecutionInfoFormatter delimiter(String delimiter) {
        this.delimiter = Assert.requireNonNull(delimiter, "delimiter must not be null");
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show thread information with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showThread() {
        this.consumers.add(this.onThread);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show connection information with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showConnection() {
        this.consumers.add(this.onConnection);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show transaction information with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showTransaction() {
        this.consumers.add(this.onTransactionInfo);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show query success with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showSuccess() {
        this.consumers.add(this.onSuccess);
        return new QueryExecutionInfoFormatter(this);
    }


    /**
     * Show query execution duration with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showTime() {
        this.consumers.add(this.onTime);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show statement type(batch or statement) with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showType() {
        this.consumers.add(this.onType);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show batch size with default format.
     *
     * i.e. Number of the calls of {@link Batch#add(String)}}.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showBatchSize() {
        this.consumers.add(this.onBatchSize);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show binding size with default format.
     *
     * i.e. Number of the calls of {@link Statement#add()}}.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showBindingsSize() {
        this.consumers.add(this.onBindingsSize);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show query with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showQuery() {
        this.consumers.add(this.onQuery);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Show bindings with default format.
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter showBindings() {
        this.consumers.add(this.onBindings);
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Change the line
     *
     * @return formatter
     */
    public QueryExecutionInfoFormatter newLine() {
        this.consumers.add(this.newLine);
        return new QueryExecutionInfoFormatter(this);
    }


    /**
     * Set a consumer for converting {@link BoundValue}.
     *
     * @param onBoundValue bi-consumer for binding value
     * @return formatter
     * @throws IllegalArgumentException if {@code onBoundValue} is {@code null}
     */
    public QueryExecutionInfoFormatter boundValue(BiConsumer<BoundValue, StringBuilder> onBoundValue) {
        this.onBoundValue = Assert.requireNonNull(onBoundValue, "onBoundValue must not be null");
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Set a consumer for converting {@link SortedSet} of {@link Binding} constructed by bind-by-index.
     *
     * @param onIndexBindings bi-consumer for index-bindings
     * @return formatter
     * @throws IllegalArgumentException if {@code onIndexBindings} is {@code null}
     */
    public QueryExecutionInfoFormatter indexBindings(BiConsumer<SortedSet<Binding>, StringBuilder> onIndexBindings) {
        this.onIndexBindings = Assert.requireNonNull(onIndexBindings, "onIndexBindings must not be null");
        return new QueryExecutionInfoFormatter(this);
    }

    /**
     * Set a consumer for converting {@link SortedSet} of {@link Binding} constructed by bind-by-identifier.
     *
     * @param onIdentifierBindings bi-consumer for identifier-bindings
     * @return formatter
     * @throws IllegalArgumentException if {@code onIdentifierBindings} is {@code null}
     */
    public QueryExecutionInfoFormatter identifierBindings(BiConsumer<SortedSet<Binding>, StringBuilder> onIdentifierBindings) {
        this.onIdentifierBindings = Assert.requireNonNull(onIdentifierBindings, "onIdentifierBindings must not be null");
        return new QueryExecutionInfoFormatter(this);
    }

}
