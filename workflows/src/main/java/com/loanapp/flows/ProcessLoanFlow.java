package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanQuoteContract;
import com.loanapp.states.LoanQuoteState;
import com.loanapp.states.LoanRequestState;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByKey;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.ci.workflows.*;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StaticPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ProcessLoanFlow {
    @InitiatingFlow
    @StartableByRPC
    @StartableByService
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private String status;
        private UniqueIdentifier loanRequestIdentifier;
        private final static Logger log = LoggerFactory.getLogger(Initiator.class);

        public Initiator(UniqueIdentifier loanRequestIdentifier) {
            this.loanRequestIdentifier = loanRequestIdentifier;
            this.status = null;
        }

        @ConstructorForDeserialization
        public Initiator(UniqueIdentifier loanRequestIdentifier,String status) {
            this.loanRequestIdentifier = loanRequestIdentifier;
            this.status = status;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            Party lender = getOurIdentity();
            List<StateAndRef<LoanRequestState>> loanRequestStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LoanRequestState.class).getStates();

            StateAndRef<LoanRequestState> inputStateAndRef = loanRequestStateAndRefs.stream().filter(requestStateAndRef -> {
                LoanRequestState requestState = requestStateAndRef.getState().getData();
                return requestState.getLinearId().equals(loanRequestIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Request Not Found"));
            if(status == null){
                LoanCriteriaDatabaseService databaseService = getServiceHub().cordaService(LoanCriteriaDatabaseService.class);
                double limit = 0;
                try {
                    limit = databaseService.queryLaonCriteriaValue("maxLimit");
                    log.info("limit val "+limit);
                } catch (Exception e) {
                    log.info("db error: " + e.getMessage());
                } finally {
                    status = inputStateAndRef.getState().getData().getLoanAmount() < limit ? "InProcess":"Rejected";
                }
            }

            Party notary = inputStateAndRef.getState().getNotary();


            StateAndRef<AccountInfo> borrowerState = UtilitiesKt.getAccountService(this)
                    .accountInfo(inputStateAndRef.getState().getData().getAccountId().getId());
            AccountInfo borrowerInfo = borrowerState.getState().getData();
            AnonymousParty borrowerAccount = subFlow(new RequestKeyForAccount(borrowerInfo));

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);
            FlowSession cpSession = initiateFlow(borrowerInfo.getHost());

            cpSession.send(inputStateAndRef.getState().getData().getAccountId().getId());
            subFlow(new SyncKeyMappingFlowHandler(cpSession));

            final LoanQuoteState output =LoanQuoteState.Issue(
                    new LinearPointer<>(loanRequestIdentifier, LoanRequestState.class),
                    new UniqueIdentifier(),borrowerAccount,lender,status
            );

            // Step 4. Add the project as an output state, as well as a command to the transaction builder.
            builder.addOutputState(output);
            builder.addCommand(new LoanQuoteContract.Commands.Process(), Arrays.asList(borrowerAccount.getOwningKey(), lender.getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            //step 6: collect signatures
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(cpSession)));


            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, Arrays.asList(cpSession)));
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
            UUID accountId = counterpartySession.receive(UUID.class).unwrap(t->t);
            AccountInfo borrowerAccountInfo = UtilitiesKt.getAccountService(this)
                    .accountInfo(accountId).getState().getData();
            //Generate new keys for buyers and sellers
            AnonymousParty borrowerAccount = subFlow(new RequestKeyForAccount(borrowerAccountInfo));
            subFlow(new SyncKeyMappingFlow(counterpartySession,Arrays.asList(borrowerAccount)));

            //subFlow(new ProvideKeyFlow(counterpartySession));
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
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }
}
