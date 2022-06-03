package com.loanapp.contracts;

import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
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
                final CommandWithParties<CommandData> command = tx.getCommands().get(0);
                if( command.getValue() instanceof  Commands.Request) {
                    final LoanRequestState out = tx.outputsOfType(LoanRequestState.class).get(0);
                    // LoanRequest-specific constraints.
                    require.using("The Loan Amount must be non-negative.", out.getLoanAmount() > 0);
                    require.using("There should be at least 1 lender", out.getLenders().stream().count() > 0);
                    require.using("The Loan Amount must be non-negative.", out.getPanNumber().length() == 6);
                    require.using(String.valueOf(out.getBorrower().getName().getOrganisationUnit().equals("Fintech")), out.getBorrower()
                            .getName().getOrganisationUnit().equals("Fintech"));
                }
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Request implements Commands {}
    }
}
