package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.utilities.UntrustworthyData;

public class RequestCreditScoreFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<Void> {

        private String panNumber;

        public Initiator(String panNumber) {
            this.panNumber = panNumber;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            final Party  creditBureau = getServiceHub().getNetworkMapCache().getPeerByLegalName(new CordaX500Name(null,"credit","CreditBureau", "Toronto", null,"CA"));

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(creditBureau);
            otherPartySession.send(panNumber);
            return  null;
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
            UntrustworthyData<String> counterpartyData = counterpartySession.receive(String.class);

            final String panNumber = counterpartyData.unwrap(msg -> {
                assert (msg.length() == 6);
                return msg;
            });

            //Stored the transaction into data base.
            subFlow(new IssueCreditScoreFlow.Initiator(counterpartySession.getCounterparty(), panNumber));
            return null;
        }
    }
}
