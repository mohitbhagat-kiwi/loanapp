package com.loanapp.flows;
import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.contracts.LoanRequestContract;
import com.loanapp.states.LoanRequestState;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.*;
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow;
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler;
import net.corda.core.contracts.StateAndRef;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RequestLoanFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private String accountName;
        private AnonymousParty borrower;
        private List<Party> lenders;
        private String panNumber;
        private int loanAmount;
        private String filePath;

        public Initiator(String accountName, List<Party> lenders, String panNumber, int loanAmount){
            this(accountName,lenders,panNumber,loanAmount,"");
        }

        @ConstructorForDeserialization
        public Initiator(String accountName,List<Party> lenders, String panNumber, int loanAmount, String filePath) {
            this.accountName = accountName;
            this.lenders = lenders;
            this.panNumber = panNumber;
            this.loanAmount = loanAmount;
            this.filePath = filePath;
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
        public SignedTransaction call() throws FlowException {

//            StateAndRef<AccountInfo> borrowerAc = (StateAndRef<AccountInfo>)subFlow(new AccountInfoByName(accountName)).get(0);
//            PublicKey borrowerKey = getServiceHub().getKeyManagementService().freshKey(borrowerAc.getState().getData().getIdentifier().getId());
//            this.borrower = new AnonymousParty(borrowerKey);

            StateAndRef<AccountInfo> borrowerState = UtilitiesKt.getAccountService(this).accountInfo(accountName).get(0);
            AccountInfo borrowerInfo = borrowerState.getState().getData();
            this.borrower = subFlow(new RequestKeyForAccount(borrowerInfo));

            //this.borrower = getOurIdentity();
            SecureHash attachmentHash = null;
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
                    borrower,loanAmount,lenders,filePath.isEmpty()?"":attachmentHash.toString(),borrowerInfo.getLinearId());

            final TransactionBuilder builder = new TransactionBuilder(notary);

            builder.addOutputState(output);
            builder.addCommand(new LoanRequestContract.Commands.Request(),borrower.getOwningKey());
            if(!filePath.isEmpty()){
                builder.addAttachment(attachmentHash);
            }

            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder,borrower.getOwningKey()    );

            List<FlowSession> cpSesions = lenders.stream().map(this::initiateFlow).collect(Collectors.toList());

            subFlow(new ShareAccountInfoFlow( borrowerState, cpSesions));

            return subFlow(new FinalityFlow(ptx, cpSesions));
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
            subFlow(new ShareAccountInfoHandlerFlow(counterpartySession));

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }
}
