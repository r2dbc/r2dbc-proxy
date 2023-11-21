/*
 * Copyright 2023 the original author or authors.
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

import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.Wrapped;
import io.r2dbc.spi.test.MockRow;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Test for {@link RowSegmentCallbackHandler}.
 *
 * @author Tadaya Tsuyukubo
 */
public class RowSegmentCallbackHandlerTest {

    private static Method ROW_METHOD = ReflectionUtils.findMethod(Result.RowSegment.class, "row");

    private static Method UNWRAP_METHOD = ReflectionUtils.findMethod(Wrapped.class, "unwrap");

    private static Method GET_PROXY_CONFIG_METHOD = ReflectionUtils.findMethod(ProxyConfigHolder.class, "getProxyConfig");


    @Test
    void row() throws Throwable {
        MockRow mockRow = MockRow.empty();
        Result.RowSegment rowSegment = mock(Result.RowSegment.class);
        when(rowSegment.row()).thenReturn(mockRow);
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = new ProxyConfig();

        RowSegmentCallbackHandler callback = new RowSegmentCallbackHandler(rowSegment, queryExecutionInfo, proxyConfig);
        Object result = callback.invoke(rowSegment, ROW_METHOD, null);

        assertThat(result).isInstanceOf(Row.class).isInstanceOf(Wrapped.class);
        assertThat(((Wrapped<?>) result).unwrap()).isSameAs(mockRow);
    }

    @Test
    void unwrap() throws Throwable {
        MockRow mockRow = MockRow.empty();
        Result.RowSegment rowSegment = mock(Result.RowSegment.class);
        when(rowSegment.row()).thenReturn(mockRow);
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = new ProxyConfig();

        RowSegmentCallbackHandler callback = new RowSegmentCallbackHandler(rowSegment, queryExecutionInfo, proxyConfig);
        Object result = callback.invoke(rowSegment, UNWRAP_METHOD, null);

        assertThat(result).isSameAs(rowSegment);
    }

    @Test
    void unwrapToInvalid() throws Throwable {
        Result.RowSegment rowSegment = mock(Result.RowSegment.class, withSettings().extraInterfaces(Wrapped.class));
        when(((Wrapped<?>) rowSegment).unwrap(String.class)).thenReturn("foo");
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = new ProxyConfig();

        RowSegmentCallbackHandler callback = new RowSegmentCallbackHandler(rowSegment, queryExecutionInfo, proxyConfig);
        Object result = callback.invoke(rowSegment, UNWRAP_METHOD, new Object[]{String.class});

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void getProxyConfig() throws Throwable {
        MockRow mockRow = MockRow.empty();
        Result.RowSegment rowSegment = mock(Result.RowSegment.class);
        when(rowSegment.row()).thenReturn(mockRow);
        MutableQueryExecutionInfo queryExecutionInfo = new MutableQueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(MockConnectionInfo.empty());
        ProxyConfig proxyConfig = new ProxyConfig();

        RowSegmentCallbackHandler callback = new RowSegmentCallbackHandler(rowSegment, queryExecutionInfo, proxyConfig);

        Object result = callback.invoke(mockRow, GET_PROXY_CONFIG_METHOD, null);
        assertThat(result).isSameAs(proxyConfig);
    }

}
