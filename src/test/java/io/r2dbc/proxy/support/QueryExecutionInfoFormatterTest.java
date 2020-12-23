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

import io.r2dbc.proxy.core.Bindings;
import io.r2dbc.proxy.core.BoundValue;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.ExecutionType;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.proxy.test.MockQueryExecutionInfo;
import io.r2dbc.spi.Blob;
import io.r2dbc.spi.Clob;
import io.r2dbc.spi.Parameter;
import io.r2dbc.spi.Parameters;
import io.r2dbc.spi.R2dbcType;
import io.r2dbc.spi.Type;
import io.r2dbc.spi.test.MockBlob;
import io.r2dbc.spi.test.MockClob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static io.r2dbc.proxy.core.Bindings.indexBinding;
import static io.r2dbc.proxy.core.Bindings.namedBinding;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
        indexBindings1.addIndexBinding(indexBinding(0, BoundValue.value("100")));
        indexBindings1.addIndexBinding(indexBinding(1, BoundValue.value("101")));
        indexBindings1.addIndexBinding(indexBinding(2, BoundValue.value("102")));

        Bindings indexBindings2 = new Bindings();
        indexBindings2.addIndexBinding(indexBinding(2, BoundValue.value("202")));
        indexBindings2.addIndexBinding(indexBinding(1, BoundValue.nullValue(String.class)));
        indexBindings2.addIndexBinding(indexBinding(0, BoundValue.value("200")));


        Bindings idBindings1 = new Bindings();
        idBindings1.addNamedBinding(namedBinding("$0", BoundValue.value("100")));
        idBindings1.addNamedBinding(namedBinding("$1", BoundValue.value("101")));
        idBindings1.addNamedBinding(namedBinding("$2", BoundValue.value("102")));

        Bindings idBindings2 = new Bindings();
        idBindings2.addNamedBinding(namedBinding("$2", BoundValue.value("202")));
        idBindings2.addNamedBinding(namedBinding("$1", BoundValue.nullValue(Integer.class)));
        idBindings2.addNamedBinding(namedBinding("$0", BoundValue.value("200")));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT WITH-INDEX");
        QueryInfo queryWithNamedBindings = new QueryInfo("SELECT WITH-NAME");
        QueryInfo queryWithNoBindings = new QueryInfo("SELECT NO-BINDINGS");

        queryWithIndexBindings.getBindingsList().addAll(Arrays.asList(indexBindings1, indexBindings2));
        queryWithNamedBindings.getBindingsList().addAll(Arrays.asList(idBindings1, idBindings2));

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

        // with named bindings
        execInfo = MockQueryExecutionInfo.builder().from(baseExecInfo)
            .queries(Collections.singletonList(queryWithNamedBindings))
            .build();
        result = formatter.format(execInfo);
        assertThat(result).isEqualTo("Thread:my-thread(99) Connection:conn-id" +
            " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
            " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-NAME\"]" +
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
        bindings1.addIndexBinding(indexBinding(0, BoundValue.value("100")));
        bindings1.addIndexBinding(indexBinding(1, BoundValue.value("101")));
        bindings1.addIndexBinding(indexBinding(2, BoundValue.value("102")));

        Bindings bindings2 = new Bindings();
        bindings2.addIndexBinding(indexBinding(2, BoundValue.value("202")));
        bindings2.addIndexBinding(indexBinding(1, BoundValue.nullValue(String.class)));
        bindings2.addIndexBinding(indexBinding(0, BoundValue.value("200")));

        Bindings bindings3 = new Bindings();
        bindings3.addIndexBinding(indexBinding(1, BoundValue.value("300")));
        bindings3.addIndexBinding(indexBinding(2, BoundValue.value("302")));
        bindings3.addIndexBinding(indexBinding(0, BoundValue.nullValue(Integer.class)));

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
    void showBindingsWithNamedBinding() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Bindings bindings1 = new Bindings();
        bindings1.addNamedBinding(namedBinding("$0", BoundValue.value("100")));
        bindings1.addNamedBinding(namedBinding("$1", BoundValue.value("101")));
        bindings1.addNamedBinding(namedBinding("$2", BoundValue.value("102")));

        Bindings bindings2 = new Bindings();
        bindings2.addNamedBinding(namedBinding("$2", BoundValue.value("202")));
        bindings2.addNamedBinding(namedBinding("$1", BoundValue.nullValue(Long.class)));
        bindings2.addNamedBinding(namedBinding("$0", BoundValue.value("200")));

        Bindings bindings3 = new Bindings();
        bindings3.addNamedBinding(namedBinding("$1", BoundValue.value("300")));
        bindings3.addNamedBinding(namedBinding("$2", BoundValue.value("302")));
        bindings3.addNamedBinding(namedBinding("$0", BoundValue.nullValue(String.class)));

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
        bindingsByIndex.addIndexBinding(indexBinding(0, BoundValue.value("100")));
        bindingsByIndex.addIndexBinding(indexBinding(1, BoundValue.nullValue(Object.class)));

        Bindings bindingsByName = new Bindings();
        bindingsByName.addNamedBinding(namedBinding("$0", BoundValue.value("100")));
        bindingsByName.addNamedBinding(namedBinding("$1", BoundValue.nullValue(Object.class)));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT 1");
        QueryInfo queryWithNamedBindings = new QueryInfo("SELECT 1");

        queryWithIndexBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIndex));
        queryWithNamedBindings.getBindingsList().addAll(Collections.singletonList(bindingsByName));

        QueryExecutionInfo execInfoWithIndexBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithIndexBindings))
            .build();
        QueryExecutionInfo execInfoWithNamedBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithNamedBindings))
            .build();

        String result;

        result = formatter.format(execInfoWithIndexBindings);
        assertThat(result).isEqualTo("Bindings:[(FOO,FOO)]");

        result = formatter.format(execInfoWithNamedBindings);
        assertThat(result).isEqualTo("Bindings:[($0=FOO,$1=FOO)]");
    }

    @Test
    void boundValuePlaceholderForLargeObject() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Blob blob = MockBlob.empty();
        Clob clob = MockClob.empty();

        Bindings bindingsByIndex = new Bindings();
        bindingsByIndex.addIndexBinding(indexBinding(0, BoundValue.value(blob)));
        bindingsByIndex.addIndexBinding(indexBinding(1, BoundValue.value(clob)));

        Bindings bindingsByName = new Bindings();
        bindingsByName.addNamedBinding(namedBinding("$0", BoundValue.value(blob)));
        bindingsByName.addNamedBinding(namedBinding("$1", BoundValue.value(clob)));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT 1");
        QueryInfo queryWithNamedBindings = new QueryInfo("SELECT 1");

        queryWithIndexBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIndex));
        queryWithNamedBindings.getBindingsList().addAll(Collections.singletonList(bindingsByName));

        QueryExecutionInfo execInfoWithIndexBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithIndexBindings))
            .build();
        QueryExecutionInfo execInfoWithNamedBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithNamedBindings))
            .build();

        String result;

        result = formatter.format(execInfoWithIndexBindings);
        assertThat(result).isEqualTo("Bindings:[(<blob>,<clob>)]");

        result = formatter.format(execInfoWithNamedBindings);
        assertThat(result).isEqualTo("Bindings:[($0=<blob>,$1=<clob>)]");
    }

    @ParameterizedTest
    @ArgumentsSource(BoundValueParameterProvider.class)
    void boundValueParameter(Parameter parameter, String expected) {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Bindings bindings = new Bindings();
        bindings.addIndexBinding(indexBinding(0, BoundValue.value(parameter)));

        QueryInfo queryInfo = new QueryInfo("SELECT 1");
        queryInfo.getBindingsList().addAll(Collections.singletonList(bindings));

        QueryExecutionInfo executionInfo = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryInfo))
            .build();

        String result = formatter.format(executionInfo);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void indexBindings() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.indexBindings((bindings, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIndex = new Bindings();
        bindingsByIndex.addIndexBinding(indexBinding(0, BoundValue.value("100")));
        bindingsByIndex.addIndexBinding(indexBinding(1, BoundValue.nullValue(Object.class)));

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
    void namedBindings() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.namedBindings((boundValue, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByNamer = new Bindings();
        bindingsByNamer.addNamedBinding(namedBinding("$0", BoundValue.value("100")));
        bindingsByNamer.addNamedBinding(namedBinding("$1", BoundValue.nullValue(Object.class)));

        QueryInfo queryWithNamedBindings = new QueryInfo("SELECT 1");

        queryWithNamedBindings.getBindingsList().addAll(Collections.singletonList(bindingsByNamer));

        QueryExecutionInfo execInfoWithNamedBindings = MockQueryExecutionInfo.builder()
            .queries(Collections.singletonList(queryWithNamedBindings))
            .build();

        String result;

        result = formatter.format(execInfoWithNamedBindings);
        assertThat(result).isEqualTo("Bindings:[(FOO)]");
    }

    static class BoundValueParameterProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            Blob blob = MockBlob.empty();
            Clob clob = MockClob.empty();
            return Stream.of(
                // in parameters
                arguments(Parameters.in(R2dbcType.BOOLEAN), "Bindings:[(null(in,BOOLEAN))]"),
                arguments(Parameters.in(String.class), "Bindings:[(null(in,String))]"),
                arguments(Parameters.in("FOO"), "Bindings:[(FOO(in,String))]"),
                arguments(Parameters.in(R2dbcType.INTEGER, 10), "Bindings:[(10(in,INTEGER))]"),
                arguments(Parameters.in(R2dbcType.DOUBLE, null), "Bindings:[(null(in,DOUBLE))]"),
                // clob/blob
                arguments(Parameters.in(Clob.class), "Bindings:[(null(in,Clob))]"),
                arguments(Parameters.in(clob), "Bindings:[(<clob>(in,MockClob))]"),
                arguments(Parameters.in(R2dbcType.CLOB, clob), "Bindings:[(<clob>(in,CLOB))]"),
                arguments(Parameters.in(Blob.class), "Bindings:[(null(in,Blob))]"),
                arguments(Parameters.in(blob), "Bindings:[(<blob>(in,MockBlob))]"),
                arguments(Parameters.in(R2dbcType.BLOB, blob), "Bindings:[(<blob>(in,BLOB))]"),
                // custom type
                arguments(Parameters.in(new ClassCustomType()), "Bindings:[(null(in,CustomJavaType))]"),
                arguments(Parameters.in(EnumCustomType.ENUM_CUSTOM_FOO), "Bindings:[(null(in,ENUM_CUSTOM_FOO))]"),
                arguments(Parameters.in(ClassCustomType.class), "Bindings:[(null(in,ClassCustomType))]"),
                arguments(Parameters.in(new ClassCustomType(), "abc"), "Bindings:[(abc(in,CustomJavaType))]"),
                arguments(Parameters.in(EnumCustomType.ENUM_CUSTOM_FOO, "abc"), "Bindings:[(abc(in,ENUM_CUSTOM_FOO))]"),

                // out parameters
                arguments(Parameters.out(R2dbcType.BOOLEAN), "Bindings:[(null(out,BOOLEAN))]"),
                arguments(Parameters.out(String.class), "Bindings:[(null(out,String))]"),
                arguments(Parameters.out("FOO"), "Bindings:[(FOO(out,String))]"),
                arguments(Parameters.out(R2dbcType.INTEGER, 10), "Bindings:[(10(out,INTEGER))]"),
                arguments(Parameters.out(R2dbcType.DOUBLE, null), "Bindings:[(null(out,DOUBLE))]"),
                // clob/blob
                arguments(Parameters.out(Clob.class), "Bindings:[(null(out,Clob))]"),
                arguments(Parameters.out(clob), "Bindings:[(<clob>(out,MockClob))]"),
                arguments(Parameters.out(R2dbcType.CLOB, clob), "Bindings:[(<clob>(out,CLOB))]"),
                arguments(Parameters.out(Blob.class), "Bindings:[(null(out,Blob))]"),
                arguments(Parameters.out(blob), "Bindings:[(<blob>(out,MockBlob))]"),
                arguments(Parameters.out(R2dbcType.BLOB, blob), "Bindings:[(<blob>(out,BLOB))]"),
                // custom type
                arguments(Parameters.out(new ClassCustomType()), "Bindings:[(null(out,CustomJavaType))]"),
                arguments(Parameters.out(EnumCustomType.ENUM_CUSTOM_FOO), "Bindings:[(null(out,ENUM_CUSTOM_FOO))]"),
                arguments(Parameters.out(ClassCustomType.class), "Bindings:[(null(out,ClassCustomType))]"),
                arguments(Parameters.out(new ClassCustomType(), "abc"), "Bindings:[(abc(out,CustomJavaType))]"),
                arguments(Parameters.out(EnumCustomType.ENUM_CUSTOM_FOO, "abc"), "Bindings:[(abc(out,ENUM_CUSTOM_FOO))]")

            );
        }
    }

    static class ClassCustomType implements Type {

        @Override
        public Class<?> getJavaType() {
            return CustomJavaType.class;
        }

        @Override
        public String getName() {
            return "custom-name";
        }

    }

    static class CustomJavaType {

    }

    enum EnumCustomType implements Type {
        ENUM_CUSTOM_FOO(CustomJavaType.class);

        private final Class<?> javaType;

        EnumCustomType(Class<?> javaType) {
            this.javaType = javaType;
        }

        @Override
        public Class<?> getJavaType() {
            return this.javaType;
        }

        @Override
        public String getName() {
            return name();
        }
    }

}
