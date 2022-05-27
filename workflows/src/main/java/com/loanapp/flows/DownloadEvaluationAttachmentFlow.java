package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.states.EvaluationRequestState;
import net.corda.core.contracts.Attachment;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DownloadEvaluationAttachmentFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String> {
        private final UniqueIdentifier evaluationRequestID;
        private final String path;

        public Initiator(UniqueIdentifier evaluationRequestID, String path) {
            this.evaluationRequestID = evaluationRequestID;
            this.path = path;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            List<StateAndRef<EvaluationRequestState>> EvaluationRequestStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(EvaluationRequestState.class).getStates();

            StateAndRef<EvaluationRequestState> inputStateAndRef = EvaluationRequestStateAndRefs.stream().filter(requestStateAndRef -> {
                EvaluationRequestState requestState = requestStateAndRef.getState().getData();
                return requestState.getLinearId().equals(evaluationRequestID);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Request Not Found"));
            String attachmentID = inputStateAndRef.getState().getData().getAttachmentID();
            Attachment content = getServiceHub().getAttachments().openAttachment(SecureHash.parse(attachmentID));
            try {
                assert content != null;
                //content.extractFile(path, new FileOutputStream(new File(path)));
                InputStream inStream = content.open();
                byte[] buffer = new byte[inStream.available()];
                inStream.read(buffer);
                File targetFile = new File(path);
                FileOutputStream oStream= new FileOutputStream(targetFile);
                oStream.write(buffer);
                inStream.close();
                oStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "Downloaded file from to " + path;
        }
    }

}
