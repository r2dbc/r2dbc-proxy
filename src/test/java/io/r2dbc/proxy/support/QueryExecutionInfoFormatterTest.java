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
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.proxy.test.MockQueryExecutionInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfoFormatterTest {

    @Test
    void batchExecution() {

        ConnectionInfo connectionInfo = MockConnectionInfo.builder().connectionId("conn-id").build();

        // Batch Query
        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .threadName("my-thread")
            .threadId(99L)
            .connectionInfo(connectionInfo)
            .isSuccess(true)
            .executeDuration(Duration.of(35, ChronoUnit.MILLIS))
            .type(ExecutionType.BATCH)
            .batchSize(20)
            .bindingsSize(10)
            .queries(Arrays.asList(new QueryInfo("SELECT A"), new QueryInfo("SELECT B")))
            .build();

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

        ConnectionInfo connectionInfo = MockConnectionInfo.builder().connectionId("conn-id").build();

        // Statement Query
        MockQueryExecutionInfo baseExecInfo = MockQueryExecutionInfo.builder()
            .threadName("my-thread")
            .threadId(99L)
            .connectionInfo(connectionInfo)
            .isSuccess(true)
            .executeDuration(Duration.of(35, ChronoUnit.MILLIS))
            .type(ExecutionType.STATEMENT)
            .batchSize(20)
            .bindingsSize(10)
            .build();

        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();
        QueryExecutionInfo execInfo;
        String result;

        // with index bindings
        execInfo = MockQueryExecutionInfo.builder().from(baseExecInfo)
            .queries(Collections.singletonList(queryWithIndexBindings))
            .build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id" +
            " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
            " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-INDEX\"]" +
            " Bindings:[(100,101,102),(200,null(String),202)]");

        // with identifier bindings
        execInfo = MockQueryExecutionInfo.builder().from(baseExecInfo)
            .queries(Collections.singletonList(queryWithIdBindings))
            .build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id" +
            " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
            " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-IDENTIFIER\"]" +
            " Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Integer),$2=202)]");

        // with no bindings
        execInfo = MockQueryExecutionInfo.builder().from(baseExecInfo)
            .queries(Collections.singletonList(queryWithNoBindings))
            .build();
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

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .threadName("my-thread")
            .threadId(99L)
            .build();


        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Thread:my-thread(99)");
    }

    @Test
    void showConnection() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showConnection();

        ConnectionInfo connectionInfo = MockConnectionInfo.builder().connectionId("99").build();

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .connectionInfo(connectionInfo)
            .build();

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Connection:99");
    }

    @Test
    void showTransactionInfo() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTransaction();

        // 1 transaction, 2 rollback, 3 commit
        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .connectionInfo(MockConnectionInfo.builder()
                .transactionCount(1)
                .rollbackCount(2)
                .commitCount(3)
                .build())
            .build();

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Transaction:{Create:1 Rollback:2 Commit:3}");
    }

    @Test
    void showSuccess() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showSuccess();

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder().isSuccess(true).build();

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Success:True");

        execInfo = MockQueryExecutionInfo.builder().isSuccess(false).build();

        str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Success:False");
    }

    @Test
    void showTime() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTime();

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder().executeDuration(Duration.of(55, ChronoUnit.MILLIS)).build();

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Time:55");
    }

    @Test
    void showType() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showType();

        QueryExecutionInfo execInfo;
        String str;

        execInfo = MockQueryExecutionInfo.builder().type(ExecutionType.BATCH).build();
        str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Type:Batch");

        execInfo = MockQueryExecutionInfo.builder().type(ExecutionType.STATEMENT).build();
        str = formatter.format(execInfo);
        assertThat(str).isEqualTo("Type:Statement");
    }

    @Test
    void showBatchSize() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBatchSize();

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder().batchSize(99).build();

        String str = formatter.format(execInfo);
        assertThat(str).isEqualTo("BatchSize:99");
    }

    @Test
    void showBindingsSize() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindingsSize();

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder().bindingsSize(99).build();

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

        QueryExecutionInfo execInfo;
        String result;


        // with multiple bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Arrays.asList(query1, query2, query3)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Query:[\"QUERY-1\",\"QUERY-2\",\"QUERY-3\"]");

        // with single bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query2)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Query:[\"QUERY-2\"]");

        // with no bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.emptyList()).build();
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


        QueryExecutionInfo execInfo;
        String result;


        // with multiple bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query1)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[(100,101,102),(200,null(String),202),(null(Integer),300,302)]");

        // with single bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query2)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[(200,null(String),202)]");

        // with no bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query3)).build();
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


        QueryExecutionInfo execInfo;
        String result;


        // with multiple bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query1)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Long),$2=202),($0=null(String),$1=300,$2=302)]");

        // with single bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query2)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[($0=200,$1=null(Long),$2=202)]");

        // with no bindings
        execInfo = MockQueryExecutionInfo.builder().queries(Collections.singletonList(query3)).build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Bindings:[]");

    }

    @Test
    void showAll() {
        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();

        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .threadName("")
            .threadId(0L)
            .connectionInfo(MockConnectionInfo.empty())
            .isSuccess(true)
            .executeDuration(Duration.ZERO)
            .type(ExecutionType.BATCH)
            .batchSize(0)
            .bindingsSize(0)
            .queries(Collections.emptyList())
            .build();

        String result = formatter.format(execInfo);
        assertThat(result)
            .containsSubsequence("Thread", "Connection", "Transaction", "Success", "Time", "Type",
                "BatchSize", "BindingsSize", "Query", "Bindings");
    }

    @Test
    void defaultInstance() {
        QueryExecutionInfo queryExecutionInfo = MockQueryExecutionInfo.empty();

        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        String result = formatter.format(queryExecutionInfo);
        assertThat(result).as("Does not generate anything.").isEqualTo("");
    }


    @Test
    void delimiter() {
        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .executeDuration(Duration.ZERO)
            .isSuccess(false)
            .build();

        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
            .showTime()
            .showSuccess()
            .delimiter("ZZZ");

        String result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Time:0ZZZSuccess:False");
    }

    @Test
    void newLine() {
        QueryExecutionInfo execInfo = MockQueryExecutionInfo.builder()
            .executeDuration(Duration.ZERO)
            .isSuccess(false)
            .batchSize(0)
            .build();


        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
            .showTime()
            .newLine()
            .showSuccess()
            .newLine()
            .showBatchSize();

        String lineSeparator = System.lineSeparator();
        String expected = format("Time:0 %sSuccess:False %sBatchSize:0", lineSeparator, lineSeparator);

        String result = formatter.format(execInfo);
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

        QueryExecutionInfo execInfoWithIndexBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithIndexBindings))
            .build();
        QueryExecutionInfo execInfoWithIdentifierBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithIdentifierBindings))
            .build();

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

        QueryExecutionInfo execInfoWithIndexBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithIndexBindings))
            .build();

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

        QueryExecutionInfo execInfoWithIdentifierBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithIdentifierBindings))
            .build();

        String result;

        result = formatter.format(execInfoWithIdentifierBindings);
        assertThat(result).isEqualTo("Bindings:[(FOO)]");
    }

}
