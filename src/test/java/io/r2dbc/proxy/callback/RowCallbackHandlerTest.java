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
import io.r2dbc.proxy.listener.ResultRowConverter;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockRow;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
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

        // mock: row.get(10) => "result"
        // Type needs to be "Object" since "row.get(index)" => "row.get(index, Object.class)"
        MockRow mockRow = MockRow.builder().identified(10, Object.class, "result").build();
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        // invoke "row.get(10)"
        Object[] args = new Object[]{10};
        Object result = callback.invoke(mockRow, GET_BY_INDEX_METHOD, args);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void getByIndexWithClass() throws Throwable {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        // mock: row.get(10, String.class) => "result"
        MockRow mockRow = MockRow.builder().identified(10, String.class, "result").build();
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        // invoke "row.get(10, String.class)"
        Object[] args = new Object[]{10, String.class};
        Object result = callback.invoke(mockRow, GET_BY_INDEX_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void getByNameDefaultMethod() throws Throwable {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        // mock: row.get("foo") => "result"
        // Type needs to be "Object" since "row.get(name)" => "row.get(name, Object.class)"
        MockRow mockRow = MockRow.builder().identified("foo", Object.class, "result").build();
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        // invoke row.get("foo")
        Object[] args = new Object[]{"foo"};
        Object result = callback.invoke(mockRow, GET_BY_NAME_METHOD, args);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void getByNameWithClass() throws Throwable {
        ProxyConfig proxyConfig = ProxyConfig.builder().build();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        // mock: row.get("foo", String.class) => "result"
        MockRow mockRow = MockRow.builder().identified("foo", String.class, "result").build();
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        // invoke row.get("foo", String.class)
        Object[] args = new Object[]{"foo", String.class};
        Object result = callback.invoke(mockRow, GET_BY_NAME_WITH_CLASS_METHOD, args);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void resultRowConverter() throws Throwable {
        ResultRowConverter converter = mock(ResultRowConverter.class);

        ProxyConfig proxyConfig = ProxyConfig.builder().resultRowConverter(converter).build();
        QueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        MockRow mockRow = MockRow.empty();
        Object[] args = new Object[]{"foo", String.class};
        Method method = GET_BY_NAME_WITH_CLASS_METHOD;
        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        when(converter.onGet(same(mockRow), same(method), same(args), any())).thenReturn("abc");

        Object result = callback.invoke(mockRow, method, args);

        assertThat(result).isEqualTo("abc");
        verify(converter).onGet(same(mockRow), same(method), same(args), any());
    }

    @Test
    void unwrap() throws Throwable {
        MockRow mockRow = MockRow.empty();
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        Object result = callback.invoke(mockRow, UNWRAP_METHOD, null);
        assertThat(result).isSameAs(mockRow);
    }

    @Test
    void getProxyConfig() throws Throwable {
        MockRow mockRow = MockRow.empty();
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        Object result = callback.invoke(mockRow, GET_PROXY_CONFIG_METHOD, null);
        assertThat(result).isSameAs(proxyConfig);
    }

    @Test
    void rowConverterDoNotCallProceed() throws Throwable {
        ResultRowConverter converter = (proxyRow, method, args, getOperation) -> {
            // do not call "getOperation.proceed()"
            return "foo";
        };

        Row mockRow = mock(Row.class);

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setResultRowConverter(converter);
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        Object[] args = new String[]{"name"};
        Object result = callback.invoke(mockRow, GET_BY_NAME_METHOD, args);

        assertThat(result).isSameAs("foo");
        verifyNoInteractions(mockRow);
    }

    @Test
    void rowConverterCallsAlternativeMethods() throws Throwable {
        ResultRowConverter converter = (proxyRow, method, args, getOperation) -> {
            if ((args[0] instanceof String) && ("full_name".equals(args[0]))) {
                String lastName = proxyRow.get("last_name", String.class);
                String firstName = proxyRow.get("first_name", String.class);
                return lastName + " " + firstName;
            }
            return getOperation.proceed();
        };

        Row mockRow = mock(Row.class);
        when(mockRow.get("first_name", String.class)).thenReturn("first");
        when(mockRow.get("last_name", String.class)).thenReturn("last");

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setResultRowConverter(converter);
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();

        RowCallbackHandler callback = new RowCallbackHandler(mockRow, queryExecutionInfo, proxyConfig);

        Object[] args = new Object[]{"full_name"};
        Object result = callback.invoke(mockRow, GET_BY_NAME_METHOD, args);

        assertThat(result).isEqualTo("last first");
        verify(mockRow).get("first_name", String.class);
        verify(mockRow).get("last_name", String.class);
        verify(mockRow, never()).get("full_name", String.class);
    }

}
