package io.r2dbc.proxy.callback;

import org.junit.jupiter.api.Test;

import static io.r2dbc.proxy.callback.ContextViewPropagation.propagate;
import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import io.r2dbc.proxy.core.ValueStore;
import io.r2dbc.proxy.test.MockConnectionInfo;
import io.r2dbc.proxy.test.MockMethodExecutionInfo;
import io.r2dbc.proxy.test.MockStatementInfo;
import static org.assertj.core.api.Assertions.assertThat;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

class ContextViewPropagationTest {

    @Test
    void propagateFromExecutionInfoToConnectionInfo() {
        ContextView contextView = Context.of("key", "value");
        MethodExecutionInfo executionInfo = MockMethodExecutionInfo.builder()
                                                                   .customValue(ContextView.class, contextView)
                                                                   .build();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        propagate(executionInfo, connectionInfo);

        ContextView result = connectionInfo.getValueStore().get(ContextView.class, ContextView.class);
        assertThat(result).isEqualTo(contextView);
    }

    @Test
    void propagateFromExecutionInfoToConnectionInfoEmpty() {
        MethodExecutionInfo executionInfo = MockMethodExecutionInfo.empty();
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        propagate(executionInfo, connectionInfo);
        ContextView result = connectionInfo.getValueStore().get(ContextView.class, ContextView.class);
        assertThat(result).isNull();
    }

    @Test
    void propagateFromConnectionInfoToStatementInfo() {
        ContextView contextView = Context.of("key", "value");
        ValueStore valueStore = ValueStore.create();
        valueStore.put(ContextView.class, contextView);
        ConnectionInfo connectionInfo = MockConnectionInfo.builder()
                                                         .valueStore(valueStore)
                                                         .build();

        StatementInfo statementInfo = MockStatementInfo.empty();
        propagate(connectionInfo, statementInfo);

        ContextView result = statementInfo.getValueStore().get(ContextView.class, ContextView.class);
        assertThat(result).isEqualTo(contextView);
    }

    @Test
    void propagateFromConnectionInfoToStatementInfoEmpty() {
        ConnectionInfo connectionInfo = MockConnectionInfo.empty();
        StatementInfo statementInfo = MockStatementInfo.empty();

        propagate(connectionInfo, statementInfo);
        ContextView result = statementInfo.getValueStore().get(ContextView.class, ContextView.class);
        assertThat(result).isNull();
    }

}