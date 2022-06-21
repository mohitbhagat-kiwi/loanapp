package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.loanapp.contracts.IOUContract;
import com.loanapp.states.IOUState;
import com.loanapp.states.LoanQuoteState;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DisburseLoanFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final UniqueIdentifier loanQuoteId;

        public Initiator(UniqueIdentifier loanQuoteId) {
            this.loanQuoteId = loanQuoteId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            List<StateAndRef<LoanQuoteState>> loanQuoteStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LoanQuoteState.class).getStates();

            StateAndRef<LoanQuoteState> inputStateAndRef = loanQuoteStateAndRefs.stream().filter(loanQuoteStateAndRef -> {
                LoanQuoteState loanQuoteState = loanQuoteStateAndRef.getState().getData();
                return loanQuoteState.getLinearId().equals(loanQuoteId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Loan Bid Not Found"));

            LoanQuoteState loanQuote = inputStateAndRef.getState().getData();
            Long amount = Long.valueOf(loanQuote.getLoanAmount()) * 100;
            AnonymousParty recipient = loanQuote.getBorrower();
            TokenType tokenType = MoneyUtilities.getUSD();
            Amount<IssuedTokenType> currencyAmount= new Amount(amount,new IssuedTokenType(getOurIdentity(),tokenType));
            IOUState iouState = new IOUState(currencyAmount,getOurIdentity(),recipient);

            // Step 2. Create a new issue command.
            // Remember that a command is a CommandData object and a list of CompositeKeys
            final Command<IOUContract.Commands.Issue> issueCommand = new Command<>(
                    new IOUContract.Commands.Issue(), iouState.getParticipants()
                    .stream().map(AbstractParty::getOwningKey)
                    .collect(Collectors.toList()));

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(inputStateAndRef.getState().getNotary());

            // Step 4. Add the iou as an output states, as well as a command to the transaction builder.
            builder.addOutputState(iouState, IOUContract.IOU_CONTRACT_ID);
            builder.addCommand(issueCommand);


            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);


            // Step 6. Collect the other party's signature using the SignTransactionFlow.

            List<FlowSession> sessions = new ArrayList<>();
            sessions.add(initiateFlow(loanQuote.getBorrower()));

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            SignedTransaction iouTx = subFlow(new FinalityFlow(stx, sessions));


            //waitForLedgerCommit(iouTx.getId());
            FungibleToken fungibleToken = new FungibleToken(currencyAmount,recipient,null);

            /* Issue the required amount of the token to the recipient */
            if(iouTx.getId() != null){
                return subFlow(new IssueTokens(ImmutableList.of(fungibleToken)));
            }
            return iouTx;
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
                    requireThat(req -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        req.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}
