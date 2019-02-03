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
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.proxy.test.MockMethodExecutionInfo;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfoFormatterTest {

    @Test
    void withDefault() {

        // String#indexOf(int) method
        Method method = ReflectionUtils.findMethod(String.class, "indexOf", int.class);

        Long target = 100L;

        MethodExecutionInfo executionInfo = MockMethodExecutionInfo.builder()
            .threadId(5L)
            .connectionInfo(MockConnectionInfo.builder().connectionId("ABC").build())
            .executeDuration(Duration.of(23, ChronoUnit.MILLIS))
            .method(method)
            .target(target)
            .build();

        MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();
        String result = formatter.format(executionInfo);

        assertThat(result).isEqualTo("  1: Thread:5 Connection:ABC Time:23  Long#indexOf()");

        // second time should increase the sequence
        result = formatter.format(executionInfo);
        assertThat(result).isEqualTo("  2: Thread:5 Connection:ABC Time:23  Long#indexOf()");

    }

    @Test
    void nullConnectionId() {

        // connection id is null for before execution of "ConnectionFactory#create"
        Method method = ReflectionUtils.findMethod(ConnectionFactory.class, "create");

        Long target = 100L;

        // null ConnectionInfo
        MethodExecutionInfo executionInfo = MockMethodExecutionInfo.builder()
            .threadId(5L)
            .connectionInfo(null)
            .executeDuration(Duration.of(23, ChronoUnit.MILLIS))
            .method(method)
            .target(target)
            .build();

        MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();
        String result = formatter.format(executionInfo);

        assertThat(result).isEqualTo("  1: Thread:5 Connection:n/a Time:23  Long#create()");

        // null ConnectionId
        ConnectionInfo connectionInfo = MockConnectionInfo.builder().connectionId(null).build();

        executionInfo = MockMethodExecutionInfo.builder()
            .threadId(5L)
            .connectionInfo(connectionInfo)
            .executeDuration(Duration.of(23, ChronoUnit.MILLIS))
            .method(method)
            .target(target)
            .build();

        result = formatter.format(executionInfo);

        assertThat(result).isEqualTo("  2: Thread:5 Connection:n/a Time:23  Long#create()");
    }

    @Test
    void customConsumer() {

        MethodExecutionInfo methodExecutionInfo = MockMethodExecutionInfo.empty();

        MethodExecutionInfoFormatter formatter = new MethodExecutionInfoFormatter();
        formatter.addConsumer((executionInfo, sb) -> {
            sb.append("ABC");
        });
        String result = formatter.format(methodExecutionInfo);

        assertThat(result).isEqualTo("ABC");

    }

}
