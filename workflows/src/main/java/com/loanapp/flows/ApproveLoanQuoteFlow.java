package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanQuoteContract;
import com.loanapp.states.EvaluationRequestState;
import com.loanapp.states.LoanQuoteState;
import com.loanapp.states.LoanRequestState;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StaticPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                LoanQuoteState quoteState = requestStateAndRef.getState().getData();
                return quoteState.getLinearId().equals(quoteId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Loan Quote Not Found"));

            LoanQuoteState inputState = loanQuote.getState().getData();
            LoanRequestState loanRequestState = inputState.getLoanRequestDetails().resolve(getServiceHub())
                    .getState().getData();
            StateAndRef<AccountInfo> borrowerState = UtilitiesKt.getAccountService(this)
                    .accountInfo(loanRequestState.getAccountId().getId());
            AccountInfo borrowerInfo = borrowerState.getState().getData();
            AnonymousParty borrowerAccount = subFlow(new RequestKeyForAccount(borrowerInfo));

            List<StateAndRef<LoanQuoteState>> otherQuotes = loanQuoteRefs.stream().filter(requestStateAndRef -> {
                if(!requestStateAndRef.equals(loanQuote)) {
                    LoanQuoteState quoteState = requestStateAndRef.getState().getData();
                    return quoteState.getLoanRequestDetails().equals(inputState.getLoanRequestDetails());
                }
                else return false;
            }).collect(Collectors.toList());

            final LoanQuoteState output = new LoanQuoteState(
                    inputState.getLoanRequestDetails(),inputState.getLinearId(),
                    borrowerAccount,inputState.getLender(),
                    inputState.getLoanAmount(), inputState.getTenure(), inputState.getRateofInterest(),
                    inputState.getTransactionFees(), "Approved"
            );



            Party notary = loanQuote.getState().getNotary();
            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the inputs and outputs, as well as a command to the transaction builder.
            builder.addInputState(loanQuote);
            //otherQuotes.stream().forEach(quote -> builder.addInputState(quote));
            builder.addOutputState(output);
            builder.addCommand(new LoanQuoteContract.Commands.Approve(), Arrays.asList(borrowerAccount.getOwningKey(),inputState.getLender().getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder,Arrays.asList(borrowerAccount.getOwningKey(),getOurIdentity().getOwningKey()));

            List<FlowSession> allSessions = new ArrayList<>();
            //add session for approved bank
            FlowSession approvedSession = initiateFlow(inputState.getLender());
            approvedSession.send(true);
            allSessions.add(approvedSession);


            //List<FlowSession> cpSesions =  loanRequestState.getLenders().stream().map(this::initiateFlow).collect(Collectors.toList());
            //FlowSession cpSession = initiateFlow(inputState.getLender());

            //step 6: collect signatures for approved bank
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(approvedSession)));

            //step 7: add session for rejected bank

//            otherQuotes.stream().forEach(quote ->{
//                getLogger().info(quote.getState().getData().getLender().toString());
//                FlowSession rejectSession = initiateFlow(quote.getState().getData().getLender());
//                rejectSession.send(false);
//                allSessions.add(rejectSession);
//            });


            // Step 8. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, allSessions));
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
            boolean flag = counterpartySession.receive(Boolean.class).unwrap(it -> it);
            if(flag){
                SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                    @Suspendable
                    @Override
                    protected void checkTransaction(SignedTransaction stx) throws FlowException {

                    }
                });
            }
            //Stored the transaction into data base.
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }

}
