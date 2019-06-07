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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Convert {@link MethodExecutionInfo} to {@link String}.
 *
 * <p>Sample usage:
 * <pre>{@code
 *   MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();
 *
 *   ProxyConnectionFactory.builder(connectionFactory)
 *     .onAfterMethod(execInfo ->
 *        execInfo.map(methodExecutionFormatter::format)  // convert
 *          .doOnNext(System.out::println)  // print out to sysout
 *          .subscribe())
 *     .create();
 * }</pre>
 *
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfoFormatter implements Function<MethodExecutionInfo, String> {

    private static final String DEFAULT_DELIMITER = " ";

    private static final AtomicLongFieldUpdater<MethodExecutionInfoFormatter> SEQUENCE_NUMBER_INCREMENTER =
        AtomicLongFieldUpdater.newUpdater(MethodExecutionInfoFormatter.class, "sequenceNumber");

    private final List<BiConsumer<MethodExecutionInfo, StringBuilder>> consumers = new ArrayList<>();

    // access via SEQUENCE_NUMBER_INCREMENTER
    private volatile long sequenceNumber = 0;


    // Default consumer to format the MethodExecutionInfo
    private BiConsumer<MethodExecutionInfo, StringBuilder> defaultConsumer = (executionInfo, sb) -> {
        long seq = SEQUENCE_NUMBER_INCREMENTER.incrementAndGet(this);
        long executionTime = executionInfo.getExecuteDuration().toMillis();
        String targetClass = executionInfo.getTarget().getClass().getSimpleName();
        String methodName = executionInfo.getMethod().getName();
        long threadId = executionInfo.getThreadId();

        ConnectionInfo connectionInfo = executionInfo.getConnectionInfo();

        String connectionId = (connectionInfo == null || connectionInfo.getConnectionId() == null) ? "n/a" : connectionInfo.getConnectionId();

        sb.append(String.format("%3d: Thread:%d Connection:%s Time:%d  %s#%s()",
            seq, threadId, connectionId, executionTime, targetClass, methodName));
    };

    private String delimiter = DEFAULT_DELIMITER;


    /**
     * Create a {@link MethodExecutionInfoFormatter} with default consumers that format the {@link MethodExecutionInfo}.
     *
     * @return a formatter
     */
    public static MethodExecutionInfoFormatter withDefault() {
        MethodExecutionInfoFormatter formatter = new MethodExecutionInfoFormatter();
        formatter.addConsumer(formatter.defaultConsumer);
        return formatter;
    }

    public MethodExecutionInfoFormatter() {
    }

    private MethodExecutionInfoFormatter(MethodExecutionInfoFormatter formatter) {
        this.delimiter = formatter.delimiter;
        this.consumers.addAll(formatter.consumers);
    }

    @Override
    public String apply(MethodExecutionInfo executionInfo) {
        Assert.requireNonNull(executionInfo, "executionInfo must not be null");

        return format(executionInfo);
    }

    /**
     * Convert the given {@link MethodExecutionInfo} to {@code String} using registered consumers.
     *
     * @param executionInfo input
     * @return formatted sting
     * @throws IllegalArgumentException if {@code executionInfo} is {@code null}
     */
    public String format(MethodExecutionInfo executionInfo) {
        Assert.requireNonNull(executionInfo, "executionInfo must not be null");

        StringBuilder sb = new StringBuilder();

        this.consumers.forEach(consumer -> {
            consumer.accept(executionInfo, sb);
            sb.append(this.delimiter);
        });

        FormatterUtils.chompIfEndWith(sb, this.delimiter);

        return sb.toString();
    }

    /**
     * Register a consumer that converts {@link MethodExecutionInfo} to a {@code String}.
     *
     * @param consumer a {@code BiConsumer} that takes a {@link MethodExecutionInfo} and write to the {@code StringBuilder}.
     * @return this formatter
     * @throws IllegalArgumentException if {@code consumer} is {@code null}
     */
    public MethodExecutionInfoFormatter addConsumer(BiConsumer<MethodExecutionInfo, StringBuilder> consumer) {
        Assert.requireNonNull(consumer, "consumer must not be null");

        this.consumers.add(consumer);
        return new MethodExecutionInfoFormatter(this);
    }

}
