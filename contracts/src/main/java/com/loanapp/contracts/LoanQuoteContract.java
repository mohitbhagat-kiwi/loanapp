package com.loanapp.contracts;

import com.loanapp.states.LoanQuoteState;
import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LoanQuoteContract implements Contract {
    public static final String ID = "com.loanapp.contracts.LoanQuoteContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandData commandData = tx.getCommands().get(0).getValue();
        final LoanQuoteState out = tx.outputsOfType(LoanQuoteState.class).get(0);
        requireThat(require -> {
            require.using("The lender should be a bank", out.getLender()
                    .getName().getOrganisationUnit().equals("Bank"));
            return null;
        });
        if (commandData instanceof LoanQuoteContract.Commands.Process) {
            requireThat(require -> {
                require.using("The Status should be InProcess or Rejected",
                        out.getStatus().equals("InProcess") || out.getStatus().equals("Rejected"));
                return null;
            });
        } else if (commandData instanceof LoanQuoteContract.Commands.Submit) {
            final LoanQuoteState in = tx.inputsOfType(LoanQuoteState.class).get(0);
            requireThat(require -> {
                require.using("The Status should be In Process",
                        in.getStatus().equals("InProcess"));
                return null;
            });
        } else if (commandData instanceof LoanQuoteContract.Commands.Approve) {
            final LoanQuoteState in = tx.inputsOfType(LoanQuoteState.class).get(0);
            requireThat(require -> {
                require.using("The Status should be Submitted",
                        in.getStatus().equals("Submitted"));
                return null;
            });
        } else{
            //Unrecognized Command type
            throw new IllegalArgumentException("Incorrect type of Command");
        }
    }

    public interface Commands extends CommandData {
        class Process implements LoanQuoteContract.Commands {}
        class Submit implements LoanQuoteContract.Commands {}
        class Approve implements LoanQuoteContract.Commands {}
    }
}
