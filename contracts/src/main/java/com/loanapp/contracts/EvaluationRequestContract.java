package com.loanapp.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class EvaluationRequestContract implements Contract {

    public final static String ID = "com.loanapp.contracts.EvaluationRequestContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }

    public interface Commands extends CommandData {
        class Create implements EvaluationRequestContract.Commands {}
    }
}
