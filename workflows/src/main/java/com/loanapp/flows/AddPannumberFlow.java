package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;

import java.sql.SQLException;

@InitiatingFlow
@StartableByRPC
public class AddPannumberFlow extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final String panNumber;
    private final Integer value;

    public AddPannumberFlow(String panNumber, Integer value) {
        this.panNumber = panNumber;
        this.value = value;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        final CreditScoreDatabaseService databaseService = getServiceHub().cordaService(CreditScoreDatabaseService.class);
        // BE CAREFUL when accessing the node's database in flows:
        // 1. The operation must be executed in a BLOCKING way. Flows don't
        //    currently support suspending to await a database operation's
        //    response
        // 2. The operation must be idempotent. If the flow fails and has to
        //    restart from a checkpoint, the operation will also be replayed
        try {
            databaseService.addPanNumberValue(panNumber, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
