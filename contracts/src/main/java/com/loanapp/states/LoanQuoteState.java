package com.loanapp.states;

import com.loanapp.contracts.LoanQuoteContract;
import com.loanapp.contracts.LoanRequestContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(LoanQuoteContract.class)
public class LoanQuoteState implements LinearState {
    private LinearPointer<LoanRequestState> loanRequestDetails;
    private UniqueIdentifier uniqueIdentifier;
    private Party borrower;
    private Party lender;

    public LoanQuoteState(LinearPointer<LoanRequestState> loanRequestDetails,
                            UniqueIdentifier uniqueIdentifier){
        this.loanRequestDetails = loanRequestDetails;
        this.uniqueIdentifier = uniqueIdentifier;
    }
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }
}
