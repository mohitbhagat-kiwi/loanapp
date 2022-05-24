package com.loanapp.states;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CreditScoreState implements LinearState {
    private UniqueIdentifier uniqueIdentifier;
    private Party requestingBank;
    private Double creditScore;

    public CreditScoreState(UniqueIdentifier uniqueIdentifier, Party requestingBank, Double creditScore) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.requestingBank = requestingBank;
        this.creditScore = creditScore;
    }

    public Double getCreditScore() {
        return creditScore;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(requestingBank);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }
}
