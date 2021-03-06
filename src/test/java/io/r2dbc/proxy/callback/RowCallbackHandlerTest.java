/*
 * Copyright 2021 the original author or authors.
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

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockRow;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ResultCallbackHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
public class RowCallbackHandlerTest {

    private static Method GET_BY_INDEX_METHOD = ReflectionUtils.findMethod(Row.class, "get", int.class);

    private static Method GET_BY_INDEX_WITH_CLASS_METHOD = ReflectionUtils.findMethod(Row.class, "get", int.class, Class.class);

    private static Method GET_BY_NAME_METHOD = ReflectionUtils.findMethod(Row.class, "get", String.class);

    private static Method GET_BY_NAME_WITH_CLASS_METHOD = ReflectionUtils.findMethod(Row.class, "get", String.class, Class.class);

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    private static Method GET_PROXY_CONFIG_METHOD = ReflectionUtils.findMethod(ProxyConfigHolder.class, "getProxyConfig");


    @Test
    void getByIndexDefaultMethod() throws Throwable {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        // mock: row.get(10) => 100
        // Type needs to be "Object" since "row.get(index)" => "row.get(index, Object.class)"
        MockRow mockRow = MockRow.builder().identified(10, Object.class, 100).build();
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        // invoke "row.get(10)"
        Object[] args = new Object[]{10};
        Object result = callback.invoke(mockRow, GET_BY_INDEX_METHOD, args);

        assertThat(result).isEqualTo(100);
    }

    @Test
    void getByIndexWithClass() throws Throwable {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        // mock: row.get(10, String.class) => 100
        MockRow mockRow = MockRow.builder().identified(10, String.class, 100).build();
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        // invoke "row.get(10)"
        Object[] args = new Object[]{10, String.class};
        Object result = callback.invoke(mockRow, GET_BY_INDEX_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo(100);
    }

    ////  SAMPLE usage

    @Test
    void intToInteger() throws Throwable {
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        proxyConfig.setResultRowConverter((proxyRow, args, getOperation) -> {
            // if "row.get(int, int.class)"
            if (args.length == 2 && ((Class<?>) args[1]).isPrimitive()) {
                // instead, call "row.get(int, Integer.class)"
                return proxyRow.get((int) args[0], Integer.class);
            }
            return getOperation.proceed();
        });

        // mock: row.get(10, String.class) => 100
        Row row = mock(Row.class);
        when(row.get(1, Integer.class)).thenReturn(100);

        RowCallbackHandler callback = new RowCallbackHandler(row, queryExecutionInfo, proxyConfig);

        // invoke "row.get(10, int.class)"
        Object[] args = new Object[]{1, int.class};
        Object result = callback.invoke(row, GET_BY_INDEX_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo(100);

        verify(row, never()).get(1, int.class);
        verify(row).get(1, Integer.class);

    }

    @Test
    void toMultipleGets() throws Throwable {
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        proxyConfig.setResultRowConverter((proxyRow, args, getOperation) -> {
            if ("fullName".equals(args[0])) {
                String firstName = proxyRow.get("firstName", String.class);
                String lastName = proxyRow.get("lastName", String.class);
                return firstName + " " + lastName;
            }
            return getOperation.proceed();   // invoke original method
        });

        // mock: row.get(10, String.class) => 100
        Row row = mock(Row.class);
        when(row.get("firstName", String.class)).thenReturn("first");
        when(row.get("lastName", String.class)).thenReturn("last");

        RowCallbackHandler callback = new RowCallbackHandler(row, queryExecutionInfo, proxyConfig);

        // invoke "row.get("fullName", String.class)"
        Object[] args = new Object[]{"fullName", String.class};
        Object result = callback.invoke(row, GET_BY_NAME_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo("first last");

        verify(row, never()).get("fullName", String.class);
        verify(row).get("firstName", String.class);
        verify(row).get("lastName", String.class);
    }

    @Test
    void ignoreGet() throws Throwable {
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        proxyConfig.setResultRowConverter((proxyRow, args, getOperation) -> {
            if ("ignore".equals(args[0])) {
                return "oops";
            }
            return getOperation.proceed();   // invoke original method
        });

        // mock: row.get(10, String.class) => 100
        Row row = mock(Row.class);
        RowCallbackHandler callback = new RowCallbackHandler(row, queryExecutionInfo, proxyConfig);

        // invoke "row.get("fullName", String.class)"
        Object[] args = new Object[]{"ignore", String.class};
        Object result = callback.invoke(row, GET_BY_NAME_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo("oops");

        verifyNoInteractions(row);
    }

    @Test
    void convertResult() throws Throwable {
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        proxyConfig.setResultRowConverter((proxyRow, args, getOperation) -> {
            Object result = getOperation.proceed();  // invoke original method
            if (result == null && args.length == 2 && ((Class<?>) args[1]).isPrimitive()) {
                return 999;
            }
            return result;
        });

        // mock: row.get(10, String.class) => 100
        Row row = mock(Row.class);
        when(row.get("column", int.class)).thenReturn(null);
        RowCallbackHandler callback = new RowCallbackHandler(row, queryExecutionInfo, proxyConfig);

        // invoke "row.get("column", int.class)"
        Object[] args = new Object[]{"column", int.class};
        Object result = callback.invoke(row, GET_BY_NAME_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo(999);

        verify(row).get("column", int.class);
    }

}
