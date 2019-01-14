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

import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.BoundValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfoFormatterTest {

    @Test
    void batchExecution() {

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(connectionInfo.getConnectionId()).thenReturn("conn-id");

        // Batch Query
        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getThreadName()).thenReturn("my-thread");
        when(execInfo.getThreadId()).thenReturn(99L);
        when(execInfo.getConnectionInfo()).thenReturn(connectionInfo);
        when(execInfo.isSuccess()).thenReturn(true);
        when(execInfo.getExecuteDuration()).thenReturn(Duration.of(35, ChronoUnit.MILLIS));
        when(execInfo.getType()).thenReturn(ExecutionType.BATCH);
        when(execInfo.getBatchSize()).thenReturn(20);
        when(execInfo.getBindingsSize()).thenReturn(10);
        when(execInfo.getQueries()).thenReturn(Arrays.asList(new QueryInfo("SELECT A"), new QueryInfo("SELECT B")));

        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();

        String result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id " +
            "Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35 " +
            "Type:Batch BatchSize:20 BindingsSize:10 " +
            "Query:[\"SELECT A\",\"SELECT B\"] Bindings:[]");
    }

    @Test
    void statementExecution() {

        Bindings indexBindings1 = new Bindings();
        indexBindings1.addIndexBinding(0, BoundValue.value("100"));
        indexBindings1.addIndexBinding(1, BoundValue.value("101"));
        indexBindings1.addIndexBinding(2, BoundValue.value("102"));

        Bindings indexBindings2 = new Bindings();
        indexBindings2.addIndexBinding(2, BoundValue.value("202"));
        indexBindings2.addIndexBinding(1, BoundValue.nullValue(String.class));
        indexBindings2.addIndexBinding(0, BoundValue.value("200"));


        Bindings idBindings1 = new Bindings();
        idBindings1.addIdentifierBinding("$0", BoundValue.value("100"));
        idBindings1.addIdentifierBinding("$1", BoundValue.value("101"));
        idBindings1.addIdentifierBinding("$2", BoundValue.value("102"));

        Bindings idBindings2 = new Bindings();
        idBindings2.addIdentifierBinding("$2", BoundValue.value("202"));
        idBindings2.addIdentifierBinding("$1", BoundValue.nullValue(Integer.class));
        idBindings2.addIdentifierBinding("$0", BoundValue.value("200"));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT WITH-INDEX");
        QueryInfo queryWithIdBindings = new QueryInfo("SELECT WITH-IDENTIFIER");
        QueryInfo queryWithNoBindings = new QueryInfo("SELECT NO-BINDINGS");

        queryWithIndexBindings.getBindingsList().addAll(Arrays.asList(indexBindings1, indexBindings2));
        queryWithIdBindings.getBindingsList().addAll(Arrays.asList(idBindings1, idBindings2));

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(connectionInfo.getConnectionId()).thenReturn("conn-id");

        // Statement Query
        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getThreadName()).thenReturn("my-thread");
        when(execInfo.getThreadId()).thenReturn(99L);
        when(execInfo.getConnectionInfo()).thenReturn(connectionInfo);
        when(execInfo.isSuccess()).thenReturn(true);
        when(execInfo.getExecuteDuration()).thenReturn(Duration.of(35, ChronoUnit.MILLIS));
        when(execInfo.getType()).thenReturn(ExecutionType.STATEMENT);
        when(execInfo.getBatchSize()).thenReturn(20);
        when(execInfo.getBindingsSize()).thenReturn(10);


        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();
        String result;

        // with index bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(queryWithIndexBindings));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id" +
            " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
            " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-INDEX\"]" +
            " Bindings:[(100,101,102),(200,null(String),202)]");

        // with identifier bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(queryWithIdBindings));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id" +
            " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
            " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-IDENTIFIER\"]" +
            " Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Integer),$2=202)]");

        // with no bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(queryWithNoBindings));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id" +
            " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
            " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT NO-BINDINGS\"]" +
            " Bindings:[]");

    }


    @Test
    void showThread() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showThread();

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getThreadName()).thenReturn("my-thread");
        when(execInfo.getThreadId()).thenReturn(99L);

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Thread:my-thread(99)");
    }

    @Test
    void showConnection() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showConnection();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(connectionInfo.getConnectionId()).thenReturn("99");

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getConnectionInfo()).thenReturn(connectionInfo);

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Connection:99");
    }

    @Test
    void showTransactionInfo() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTransaction();

        // 1 transaction, 2 rollback, 3 commit
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(connectionInfo.getTransactionCount()).thenReturn(1);
        when(connectionInfo.getRollbackCount()).thenReturn(2);
        when(connectionInfo.getCommitCount()).thenReturn(3);

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getConnectionInfo()).thenReturn(connectionInfo);

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Transaction:{Create:1 Rollback:2 Commit:3}");
    }

    @Test
    void showSuccess() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showSuccess();

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.isSuccess()).thenReturn(true);

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Success:True");

        when(execInfo.isSuccess()).thenReturn(false);

        str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Success:False");
    }

    @Test
    void showTime() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTime();

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getExecuteDuration()).thenReturn(Duration.of(55, ChronoUnit.MILLIS));

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Time:55");
    }

    @Test
    void showType() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showType();

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        String str;

        when(execInfo.getType()).thenReturn(ExecutionType.BATCH);
        str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Type:Batch");

        when(execInfo.getType()).thenReturn(ExecutionType.STATEMENT);
        str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Type:Statement");
    }

    @Test
    void showBatchSize() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBatchSize();

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getBatchSize()).thenReturn(99);

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("BatchSize:99");
    }

    @Test
    void showBindingsSize() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindingsSize();

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        when(execInfo.getBindingsSize()).thenReturn(99);

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("BindingsSize:99");
    }

    @Test
    void showQuery() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showQuery();

        QueryInfo query1 = new QueryInfo("QUERY-1");
        QueryInfo query2 = new QueryInfo("QUERY-2");
        QueryInfo query3 = new QueryInfo("QUERY-3");

        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        String result;


        // with multiple bindings
        when(execInfo.getQueries()).thenReturn(Arrays.asList(query1, query2, query3));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Query:[\"QUERY-1\",\"QUERY-2\",\"QUERY-3\"]");

        // with single bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Query:[\"QUERY-2\"]");

        // with no bindings
        when(execInfo.getQueries()).thenReturn(Collections.emptyList());
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Query:[]");

    }

    @Test
    void showBindingsWithIndexBinding() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Bindings bindings1 = new Bindings();
        bindings1.addIndexBinding(0, BoundValue.value("100"));
        bindings1.addIndexBinding(1, BoundValue.value("101"));
        bindings1.addIndexBinding(2, BoundValue.value("102"));

        Bindings bindings2 = new Bindings();
        bindings2.addIndexBinding(2, BoundValue.value("202"));
        bindings2.addIndexBinding(1, BoundValue.nullValue(String.class));
        bindings2.addIndexBinding(0, BoundValue.value("200"));

        Bindings bindings3 = new Bindings();
        bindings3.addIndexBinding(1, BoundValue.value("300"));
        bindings3.addIndexBinding(2, BoundValue.value("302"));
        bindings3.addIndexBinding(0, BoundValue.nullValue(Integer.class));

        QueryInfo query1 = new QueryInfo("SELECT 1");  // will have 3 bindings
        QueryInfo query2 = new QueryInfo("SELECT 1");  // will have 1 bindings
        QueryInfo query3 = new QueryInfo("SELECT 1");  // will have empty bindings

        query1.getBindingsList().addAll(Arrays.asList(bindings1, bindings2, bindings3));
        query2.getBindingsList().addAll(Arrays.asList(bindings2));


        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        String result;


        // with multiple bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query1));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[(100,101,102),(200,null(String),202),(null(Integer),300,302)]");

        // with single bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[(200,null(String),202)]");

        // with no bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query3));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[]");

    }

    @Test
    void showBindingsWithIdentifierBinding() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Bindings bindings1 = new Bindings();
        bindings1.addIdentifierBinding("$0", BoundValue.value("100"));
        bindings1.addIdentifierBinding("$1", BoundValue.value("101"));
        bindings1.addIdentifierBinding("$2", BoundValue.value("102"));

        Bindings bindings2 = new Bindings();
        bindings2.addIdentifierBinding("$2", BoundValue.value("202"));
        bindings2.addIdentifierBinding("$1", BoundValue.nullValue(Long.class));
        bindings2.addIdentifierBinding("$0", BoundValue.value("200"));

        Bindings bindings3 = new Bindings();
        bindings3.addIdentifierBinding("$1", BoundValue.value("300"));
        bindings3.addIdentifierBinding("$2", BoundValue.value("302"));
        bindings3.addIdentifierBinding("$0", BoundValue.nullValue(String.class));

        QueryInfo query1 = new QueryInfo("SELECT 1");  // will have 3 bindings
        QueryInfo query2 = new QueryInfo("SELECT 1");  // will have 1 bindings
        QueryInfo query3 = new QueryInfo("SELECT 1");  // will have empty bindings

        query1.getBindingsList().addAll(Arrays.asList(bindings1, bindings2, bindings3));
        query2.getBindingsList().addAll(Arrays.asList(bindings2));


        QueryExecutionInfo execInfo = mock(QueryExecutionInfo.class);
        String result;


        // with multiple bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query1));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Long),$2=202),($0=null(String),$1=300,$2=302)]");

        // with single bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[($0=200,$1=null(Long),$2=202)]");

        // with no bindings
        when(execInfo.getQueries()).thenReturn(Collections.singletonList(query3));
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[]");

    }

    @Test
    void showAll() {
        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();

        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        QueryExecutionInfo queryExecutionInfo = mock(QueryExecutionInfo.class);
        when(queryExecutionInfo.getThreadName()).thenReturn("");
        when(queryExecutionInfo.getThreadId()).thenReturn(0L);
        when(queryExecutionInfo.getConnectionInfo()).thenReturn(connectionInfo);
        when(queryExecutionInfo.isSuccess()).thenReturn(true);
        when(queryExecutionInfo.getExecuteDuration()).thenReturn(Duration.ZERO);
        when(queryExecutionInfo.getType()).thenReturn(ExecutionType.BATCH);
        when(queryExecutionInfo.getBatchSize()).thenReturn(0);
        when(queryExecutionInfo.getBindingsSize()).thenReturn(0);
        when(queryExecutionInfo.getQueries()).thenReturn(Collections.emptyList());
        when(queryExecutionInfo.getBindingsSize()).thenReturn(0);

        String result = formatter.format(queryExecutionInfo);
        assertThat(result)
            .containsSubsequence("Thread", "Connection", "Transaction", "Success", "Time", "Type",
                "BatchSize", "BindingsSize", "Query", "Bindings");
    }

    @Test
    void defaultInstance() {
        QueryExecutionInfo queryExecutionInfo = mock(QueryExecutionInfo.class);

        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        String result = formatter.format(queryExecutionInfo);
        assertThat(result).isEqualTo("").as("Does not generate anything.");
    }


    @Test
    void delimiter() {
        QueryExecutionInfo queryExecutionInfo = mock(QueryExecutionInfo.class);
        when(queryExecutionInfo.getExecuteDuration()).thenReturn(Duration.ZERO);
        when(queryExecutionInfo.isSuccess()).thenReturn(false);

        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
            .showTime()
            .showSuccess()
            .delimiter("ZZZ");

        String result = formatter.format(queryExecutionInfo);
        assertThat(result).isEqualTo("Time:0ZZZSuccess:False");
    }

    @Test
    void newLine() {
        QueryExecutionInfo queryExecutionInfo = mock(QueryExecutionInfo.class);
        when(queryExecutionInfo.getExecuteDuration()).thenReturn(Duration.ZERO);
        when(queryExecutionInfo.isSuccess()).thenReturn(false);
        when(queryExecutionInfo.getBatchSize()).thenReturn(0);

        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
            .showTime()
            .newLine()
            .showSuccess()
            .newLine()
            .showBatchSize();

        String lineSeparator = System.lineSeparator();
        String expected = format("Time:0 %sSuccess:False %sBatchSize:0", lineSeparator, lineSeparator);

        String result = formatter.format(queryExecutionInfo);
        assertThat(result).isEqualTo(expected);
    }


    @Test
    void boundValue() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.boundValue((boundValue, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIndex = new Bindings();
        bindingsByIndex.addIndexBinding(0, BoundValue.value("100"));
        bindingsByIndex.addIndexBinding(1, BoundValue.nullValue(Object.class));

        Bindings bindingsByIdentifier = new Bindings();
        bindingsByIdentifier.addIdentifierBinding("$0", BoundValue.value("100"));
        bindingsByIdentifier.addIdentifierBinding("$1", BoundValue.nullValue(Object.class));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT 1");
        QueryInfo queryWithIdentifierBindings = new QueryInfo("SELECT 1");

        queryWithIndexBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIndex));
        queryWithIdentifierBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIdentifier));

        QueryExecutionInfo execInfoWithIndexBindings = mock(QueryExecutionInfo.class);
        QueryExecutionInfo execInfoWithIdentifierBindings = mock(QueryExecutionInfo.class);
        when(execInfoWithIndexBindings.getQueries()).thenReturn(Collections.singletonList(queryWithIndexBindings));
        when(execInfoWithIdentifierBindings.getQueries()).thenReturn(Collections.singletonList(queryWithIdentifierBindings));

        String result;

        result = formatter.format(execInfoWithIndexBindings);
        assertThat(result).isEqualTo("Bindings:[(FOO,FOO)]");

        result = formatter.format(execInfoWithIdentifierBindings);
        assertThat(result).isEqualTo("Bindings:[($0=FOO,$1=FOO)]");
    }

    @Test
    void indexBindings() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.indexBindings((bindings, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIndex = new Bindings();
        bindingsByIndex.addIndexBinding(0, BoundValue.value("100"));
        bindingsByIndex.addIndexBinding(1, BoundValue.nullValue(Object.class));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT 1");

        queryWithIndexBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIndex));

        QueryExecutionInfo execInfoWithIndexBindings = mock(QueryExecutionInfo.class);
        when(execInfoWithIndexBindings.getQueries()).thenReturn(Collections.singletonList(queryWithIndexBindings));

        String result;

        result = formatter.format(execInfoWithIndexBindings);
        assertThat(result).isEqualTo("Bindings:[(FOO)]");

    }

    @Test
    void identifierBindings() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.identifierBindings((boundValue, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIdentifier = new Bindings();
        bindingsByIdentifier.addIdentifierBinding("$0", BoundValue.value("100"));
        bindingsByIdentifier.addIdentifierBinding("$1", BoundValue.nullValue(Object.class));

        QueryInfo queryWithIdentifierBindings = new QueryInfo("SELECT 1");

        queryWithIdentifierBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIdentifier));

        QueryExecutionInfo execInfoWithIdentifierBindings = mock(QueryExecutionInfo.class);
        when(execInfoWithIdentifierBindings.getQueries()).thenReturn(Collections.singletonList(queryWithIdentifierBindings));

        String result;

        result = formatter.format(execInfoWithIdentifierBindings);
        assertThat(result).isEqualTo("Bindings:[(FOO)]");
    }

}
