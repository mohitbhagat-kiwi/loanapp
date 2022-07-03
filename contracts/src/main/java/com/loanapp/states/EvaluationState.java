package com.loanapp.states;

import com.loanapp.contracts.EvaluationContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(EvaluationContract.class)
public class EvaluationState implements LinearState {
    private final UniqueIdentifier uniqueIdentifier;
    private final Party requestingBank;
    private final Party issuer;
    private final int evaluation;

    public EvaluationState(UniqueIdentifier uniqueIdentifier, Party requestingBank, Party issuer, int evaluation) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.requestingBank = requestingBank;
        this.issuer = issuer;
        this.evaluation = evaluation;
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


    public Party getRequestingBank() {
        return requestingBank;
    }

    public Party getIssuer() {
        return issuer;
    }
    public int getEvaluation() {
        return evaluation;
    }
}
