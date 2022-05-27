package com.loanapp.states;

import com.loanapp.contracts.EvaluationRequestContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(EvaluationRequestContract.class)
public class EvaluationRequestState implements LinearState {

    private final UniqueIdentifier uniqueIdentifier;
    private final String attachmentID;
    private final Party requestingBank;
    private final Party issuer;


    public EvaluationRequestState(UniqueIdentifier uniqueIdentifier, String attachmentID, Party requestingBank,
                                  Party issuer) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.attachmentID = attachmentID;
        this.requestingBank = requestingBank;
        this.issuer = issuer;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(requestingBank,issuer);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }

    public String getAttachmentID() {
        return attachmentID;
    }

    public Party getRequestingBank() {
        return requestingBank;
    }
}
