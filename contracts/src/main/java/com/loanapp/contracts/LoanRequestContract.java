package com.loanapp.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LoanRequestContract implements Contract {

    public static final String ID = "com.loanapp.contracts.LoanRequestContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.Request) {
            requireThat(require -> {
                /*At here, you can structure the rules for creating a project proposal
                 * this verify method makes sure that all proposed projects from the borrower company
                 * are sound, so that banks are not going to waste any time on unqualified project proposals*/
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Request implements Commands {}
    }
}
