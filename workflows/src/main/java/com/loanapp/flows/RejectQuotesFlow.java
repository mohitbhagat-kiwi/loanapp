package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanQuoteContract;
import com.loanapp.states.LoanQuoteState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RejectQuotesFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        

        private UniqueIdentifier approvedQuoteId;
        public Initiator(UniqueIdentifier approvedQuoteId) {
            this.approvedQuoteId = approvedQuoteId;
        }


        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            List<StateAndRef<LoanQuoteState>> loanQuoteRefs = getServiceHub().getVaultService()
                    .queryBy(LoanQuoteState.class).getStates();

            StateAndRef<LoanQuoteState> loanQuote = loanQuoteRefs.stream().filter(requestStateAndRef -> {
                LoanQuoteState quoteState = requestStateAndRef.getState().getData();
                return quoteState.getLinearId().equals(approvedQuoteId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Loan Quote Not Found"));
            LoanQuoteState inputState = loanQuote.getState().getData();

            List<StateAndRef<LoanQuoteState>> loanQuotes = loanQuoteRefs.stream().filter(requestStateAndRef -> {
                if(!requestStateAndRef.equals(loanQuote)) {
                    LoanQuoteState quoteState = requestStateAndRef.getState().getData();
                    return quoteState.getLoanRequestDetails().equals(inputState.getLoanRequestDetails());
                }
                else return false;
            }).collect(Collectors.toList());


            List<PublicKey> signers = new ArrayList<>();
            signers.add(getOurIdentity().getOwningKey());

            TransactionBuilder transactionBuilder = new TransactionBuilder(loanQuotes.get(0).getState().getNotary());
            loanQuotes.stream().forEach(quote -> {
                transactionBuilder.addInputState(quote);
//                LoanQuoteState inp = quote.getState().getData();
//                transactionBuilder.addOutputState(new LoanQuoteState(inp.getLoanRequestDetails(),
//                        inp.getLinearId(),inp.getBorrower(),inp.getLender(),inp.getLoanAmount(),inp.getTenure(),
//                        inp.getRateofInterest(),inp.getTransactionFees(),inp.getStatus()));
//                signers.add(inp.getLender().getOwningKey());
            });
            transactionBuilder.addCommand(new LoanQuoteContract.Commands.Reject(),signers);

            // Verify the transaction
            transactionBuilder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction stx = getServiceHub().signInitialTransaction(transactionBuilder);

            List<FlowSession> all = new ArrayList<>();
            loanQuotes.stream().forEach(quote -> {
                all.add(initiateFlow(quote.getState().getData().getLender()));
            });

            return subFlow(new FinalityFlow(stx, all));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //Stored the transaction into data base.
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }

}
