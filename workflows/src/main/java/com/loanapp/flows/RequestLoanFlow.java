package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanRequestContract;
import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class RequestLoanFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String> {

        private Party borrower;
        private List<Party> lenders;
        private String panNumber;
        private int loanAmount;
        private String filePath;
        private SecureHash attachmentHash;

        public Initiator(List<Party> lenders, String panNumber, int loanAmount){
            this(lenders,panNumber,loanAmount,"",null);
        }
        public Initiator(List<Party> lenders, String panNumber, int loanAmount, SecureHash attachmentHash){
            this(lenders,panNumber,loanAmount,"",attachmentHash);
        }
        public Initiator(List<Party> lenders, String panNumber, int loanAmount, String filePath){
            this(lenders,panNumber,loanAmount,filePath,null);
        }

        @ConstructorForDeserialization
        public Initiator(List<Party> lenders, String panNumber, int loanAmount, String filePath,SecureHash attachmentHash) {
            this.lenders = lenders;
            this.panNumber = panNumber;
            this.loanAmount = loanAmount;
            this.filePath = filePath;
            this.attachmentHash = attachmentHash;
        }
        private static String uploadAttachment(String path, ServiceHub service, Party whoami, String filename) throws IOException {
            SecureHash attachmentHash = service.getAttachments().importAttachment(
                    new FileInputStream(new File(path)),
                    whoami.toString(),
                    filename
            );

            return attachmentHash.toString();
        }
        @Override
        @Suspendable
        public String call() throws FlowException {
            this.borrower = getOurIdentity();
            //SecureHash attachmentHash = null;
            if(!filePath.isEmpty()){
                try {
                    attachmentHash = SecureHash.parse(uploadAttachment(
                            filePath,
                            getServiceHub(),
                            getOurIdentity(),
                            "testzip")
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            final LoanRequestState output = new LoanRequestState(new UniqueIdentifier(),panNumber,
                    borrower,loanAmount,lenders,attachmentHash == null?"":attachmentHash.toString());

            final TransactionBuilder builder = new TransactionBuilder(notary);

            builder.addOutputState(output);
            builder.addCommand(new LoanRequestContract.Commands.Request(),borrower.getOwningKey());
            if(!filePath.isEmpty()){
                builder.addAttachment(attachmentHash);
            }


            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> cpSesions = lenders.stream().map(this::initiateFlow).collect(Collectors.toList());
            subFlow(new FinalityFlow(ptx, cpSesions));
            return output.getLinearId().toString();
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
