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
public class AddLoanCriteriaFlow extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final String loanCriteria;
    private final Integer value;

    public AddLoanCriteriaFlow(String loanCriteria, Integer value) {
        this.loanCriteria = loanCriteria;
        this.value = value;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        final LoanCriteriaDatabaseService databaseService = getServiceHub().cordaService(LoanCriteriaDatabaseService.class);
        // BE CAREFUL when accessing the node's database in flows:
        // 1. The operation must be executed in a BLOCKING way. Flows don't
        //    currently support suspending to await a database operation's
        //    response
        // 2. The operation must be idempotent. If the flow fails and has to
        //    restart from a checkpoint, the operation will also be replayed
        try {
            databaseService.addLoanCiteriaValue(loanCriteria, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
