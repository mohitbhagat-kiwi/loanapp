package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.CreditScoreContract;
import com.loanapp.contracts.LoanRequestContract;
import com.loanapp.states.CreditScoreState;
import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class IssueCreditScoreFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private Party requestingBank;
        private String panNumber;

        public Initiator(Party requestingBank, String panNumber) {
            this.requestingBank = requestingBank;
            this.panNumber = panNumber;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Get creditscore from DB based on Pan Number
            final CreditScoreDatabaseService databaseService = getServiceHub().cordaService(CreditScoreDatabaseService.class);
            double creditScore = 0;
            try {
                creditScore = databaseService.queryPanNumberValue(panNumber);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            final CreditScoreState output = new CreditScoreState(new UniqueIdentifier(), requestingBank,getOurIdentity(), creditScore);

            final TransactionBuilder builder = new TransactionBuilder(notary);

            builder.addOutputState(output);
            builder.addCommand(new CreditScoreContract.Commands.Create(),  Arrays.asList(getOurIdentity().getOwningKey(),requestingBank.getOwningKey()));

            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(requestingBank);

            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(ptx, Arrays.asList(otherPartySession)));

            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
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
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                }
            });

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }


}
