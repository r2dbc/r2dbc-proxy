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

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Convert {@link MethodExecutionInfo} to {@link String}.
 *
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfoFormatter implements Function<MethodExecutionInfo, String> {

    private static final String DEFAULT_DELIMITER = " ";

    private List<BiConsumer<MethodExecutionInfo, StringBuilder>> consumers = new ArrayList<>();

    private AtomicLong sequenceNumber = new AtomicLong(1);

    // Default consumer to format the MethodExecutionInfo
    private BiConsumer<MethodExecutionInfo, StringBuilder> defaultConsumer = (executionInfo, sb) -> {
        long seq = this.sequenceNumber.getAndIncrement();
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


    public static MethodExecutionInfoFormatter withDefault() {
        MethodExecutionInfoFormatter formatter = new MethodExecutionInfoFormatter();
        formatter.addConsumer(formatter.defaultConsumer);
        return formatter;
    }

    @Override
    public String apply(MethodExecutionInfo executionInfo) {
        return format(executionInfo);
    }

    public String format(MethodExecutionInfo executionInfo) {

        StringBuilder sb = new StringBuilder();

        consumers.forEach(consumer -> {
            consumer.accept(executionInfo, sb);
            sb.append(this.delimiter);
        });

        chompIfEndWith(sb, this.delimiter);

        return sb.toString();

    }

    public MethodExecutionInfoFormatter addConsumer(BiConsumer<MethodExecutionInfo, StringBuilder> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    // TODO: share this with QueryExecutionInfoFormatter
    protected void chompIfEndWith(StringBuilder sb, String s) {
        if (sb.length() < s.length()) {
            return;
        }
        final int startIndex = sb.length() - s.length();
        if (sb.substring(startIndex, sb.length()).equals(s)) {
            sb.delete(startIndex, sb.length());
        }
    }


}
