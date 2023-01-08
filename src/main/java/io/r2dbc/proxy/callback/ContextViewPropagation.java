package io.r2dbc.proxy.callback;

import io.r2dbc.proxy.core.ConnectionInfo;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.StatementInfo;
import reactor.util.context.ContextView;

class ContextViewPropagation {

    private ContextViewPropagation() {
    }

    static void propagate(MethodExecutionInfo executionInfo, ConnectionInfo connectionInfo) {
        ContextView contextView = executionInfo.getValueStore().get(ContextView.class, ContextView.class);

        if (contextView != null) {
            connectionInfo.getValueStore().put(ContextView.class, contextView);
        }
    }

    static void propagate(ConnectionInfo connectionInfo, StatementInfo statementInfo) {
        ContextView contextView = connectionInfo.getValueStore().get(ContextView.class, ContextView.class);
        if (contextView != null) {
            statementInfo.getValueStore().put(ContextView.class, contextView);
        }
    }

}
