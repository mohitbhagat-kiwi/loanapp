package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanRequestContract;
import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubmitLoanRequestFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private Party borrower;
        private List<Party> lenders;
        private String panNumber;
        private int loanAmount;

        public Initiator(List<Party> lenders, String panNumber, int loanAmount) {
            this.lenders = lenders;
            this.panNumber = panNumber;
            this.loanAmount = loanAmount;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            this.borrower = getOurIdentity();

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            final LoanRequestState output = new LoanRequestState(new UniqueIdentifier(),panNumber,borrower,loanAmount,lenders);

            final TransactionBuilder builder = new TransactionBuilder(notary);

            builder.addOutputState(output);
            builder.addCommand(new LoanRequestContract.Commands.Request(),borrower.getOwningKey());

            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> cpSesions = lenders.stream().map(this::initiateFlow).collect(Collectors.toList());


            return subFlow(new FinalityFlow(ptx, cpSesions));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }
}
