package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.EvaluationRequestContract;
import com.loanapp.states.EvaluationRequestState;
import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class RequestEvaluationFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier loanRequestIdentifier;

        public Initiator(UniqueIdentifier loanRequestIdentifier) {
            this.loanRequestIdentifier = loanRequestIdentifier;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<LoanRequestState>> loanRequestStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LoanRequestState.class).getStates();

            StateAndRef<LoanRequestState> inputStateAndRef = loanRequestStateAndRefs.stream().filter(requestStateAndRef -> {
                LoanRequestState requestState = requestStateAndRef.getState().getData();
                return requestState.getLinearId().equals(loanRequestIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Request Not Found"));
            LoanRequestState requestState = inputStateAndRef.getState().getData();

            if(requestState.getAttachmentId().isEmpty())
                throw new IllegalArgumentException("Attachment Not Found");

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final Party evaluationBureau = getServiceHub().getNetworkMapCache().getPeerByLegalName(new CordaX500Name("evaluationBureau", "Toronto", "CA"));
            EvaluationRequestState output = new EvaluationRequestState(new UniqueIdentifier(),requestState.getAttachmentId(),
                    getOurIdentity(),evaluationBureau);

            TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addCommand(new EvaluationRequestContract.Commands.Create(), getOurIdentity().getOwningKey(), evaluationBureau.getOwningKey());
            builder.addOutputState(output, EvaluationRequestContract.ID);
            builder.addAttachment(SecureHash.parse(requestState.getAttachmentId()));
            builder.verify(getServiceHub());

            // self signing
            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // counter parties signing
            FlowSession session = initiateFlow(evaluationBureau);
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            return  subFlow(new FinalityFlow(fullySignedTransaction, Arrays.asList(session)));
        }
    }
    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{
        private final FlowSession counterPartySession;

        public Responder(FlowSession counterPartySession) {
            this.counterPartySession = counterPartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Responder flow logic goes here.
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterPartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    if (stx.getTx().getAttachments().isEmpty())
                        throw new FlowException("No Jar was being sent");
                }
            });

            subFlow(new ReceiveFinalityFlow(counterPartySession, signedTransaction.getId()));
            return null;
        }
    }
}
