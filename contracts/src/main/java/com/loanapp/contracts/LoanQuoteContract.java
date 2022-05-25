package com.loanapp.contracts;

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
        if (commandData instanceof LoanQuoteContract.Commands.Process) {
            requireThat(require -> {
                /*At here, you can structure the rules for creating a project proposal
                 * this verify method makes sure that all proposed projects from the borrower company
                 * are sound, so that banks are not going to waste any time on unqualified project proposals*/
                return null;
            });
        } else if (commandData instanceof LoanQuoteContract.Commands.Submit) {
            requireThat(require -> {
                /*At here, you can structure the rules for creating a project proposal
                 * this verify method makes sure that all proposed projects from the borrower company
                 * are sound, so that banks are not going to waste any time on unqualified project proposals*/
                return null;
            });
        } else if (commandData instanceof LoanQuoteContract.Commands.Approve) {
            requireThat(require -> {
                /*At here, you can structure the rules for creating a project proposal
                 * this verify method makes sure that all proposed projects from the borrower company
                 * are sound, so that banks are not going to waste any time on unqualified project proposals*/
                return null;
            });
        } else{
            //Unrecognized Command type
            throw new IllegalArgumentException("Incorrect type of AppleStamp Commands");
        }
    }

    public interface Commands extends CommandData {
        class Process implements LoanQuoteContract.Commands {}
        class Submit implements LoanQuoteContract.Commands {}
        class Approve implements LoanQuoteContract.Commands {}
    }
}
