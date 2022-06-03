package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanQuoteContract;
import com.loanapp.states.EvaluationRequestState;
import com.loanapp.states.LoanQuoteState;
import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StaticPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.List;

public class ApproveLoanQuoteFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier quoteId;

        @ConstructorForDeserialization
        public Initiator(UniqueIdentifier quoteId) {
            this.quoteId = quoteId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<LoanQuoteState>> loanQuoteRefs = getServiceHub().getVaultService()
                    .queryBy(LoanQuoteState.class).getStates();

            StateAndRef<LoanQuoteState> loanQuote = loanQuoteRefs.stream().filter(requestStateAndRef -> {
                LoanQuoteState requestState = requestStateAndRef.getState().getData();
                return requestState.getLinearId().equals(quoteId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Loan Quote Not Found"));

            LoanQuoteState inputState = loanQuote.getState().getData();

            final LoanQuoteState output = new LoanQuoteState(
                    inputState.getLoanRequestDetails(),inputState.getLinearId(),
                    inputState.getBorrower(),inputState.getLender(),
                    inputState.getLoanAmount(), inputState.getTenure(), inputState.getRateofInterest(),
                    inputState.getTransactionFees(), "Approved"
            );

            Party notary = loanQuote.getState().getNotary();
            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the inputs and outputs, as well as a command to the transaction builder.
            builder.addInputState(loanQuote);
            builder.addOutputState(output);
            builder.addCommand(new LoanQuoteContract.Commands.Approve(), Arrays.asList(inputState.getBorrower().getOwningKey(),inputState.getLender().getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession cpSession = initiateFlow(inputState.getLender());

            //step 6: collect signatures
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(cpSession)));


            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, Arrays.asList(cpSession)));
        }
    }
    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    /*
                     * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                     * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                     * What if we don’t want to deal with the counterparty in question, or the value is too high,
                     * or we’re not happy with the transaction’s structure? checkTransaction
                     * allows us to define these additional checks. If any of these conditions are not met,
                     * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                     * ----------
                     * For this hello-world cordapp, we will not implement any aditional checks.
                     * */
                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }

}
