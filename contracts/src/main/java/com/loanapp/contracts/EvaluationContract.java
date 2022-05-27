package com.loanapp.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class EvaluationContract implements Contract {
    public final static String ID = "com.loanapp.contracts.EvaluationContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }
    public interface Commands extends CommandData {
        class Issue implements EvaluationContract.Commands {}
    }
}
