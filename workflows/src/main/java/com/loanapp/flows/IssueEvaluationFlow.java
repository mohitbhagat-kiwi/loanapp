package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.EvaluationContract;
import com.loanapp.states.EvaluationRequestState;
import com.loanapp.states.EvaluationState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.List;

public class IssueEvaluationFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String>{

        private final UniqueIdentifier evaluationRequestID;
        private final int evaluationPrice;
        private Party issuer;
        private Party requstingBank;

        public Initiator(UniqueIdentifier evaluationRequestID, int evaluationPrice) {
            this.evaluationRequestID = evaluationRequestID;
            this.evaluationPrice = evaluationPrice;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            this.issuer = getOurIdentity();
            List<StateAndRef<EvaluationRequestState>> EvaluationRequestStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(EvaluationRequestState.class).getStates();

            StateAndRef<EvaluationRequestState> inputStateAndRef = EvaluationRequestStateAndRefs.stream().filter(requestStateAndRef -> {
                EvaluationRequestState requestState = requestStateAndRef.getState().getData();
                return requestState.getLinearId().equals(evaluationRequestID);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Request Not Found"));
            requstingBank = inputStateAndRef.getState().getData().getRequestingBank();
            EvaluationState output = new EvaluationState(new UniqueIdentifier(),requstingBank,issuer,evaluationPrice);

            final Party notary = inputStateAndRef.getState().getNotary();
            TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addCommand(new EvaluationContract.Commands.Issue(),
                    requstingBank.getOwningKey(),issuer.getOwningKey());
            builder.addInputState(inputStateAndRef);
            builder.addOutputState(output,EvaluationContract.ID);
            builder.verify(getServiceHub());

            // self signing
            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // counter parties signing
            FlowSession session = initiateFlow(requstingBank);
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            subFlow(new FinalityFlow(fullySignedTransaction, Arrays.asList(session)));
            return output.getLinearId().toString();
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
