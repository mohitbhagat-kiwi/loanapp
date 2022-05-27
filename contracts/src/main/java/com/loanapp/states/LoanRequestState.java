package com.loanapp.states;

import com.loanapp.contracts.LoanRequestContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(LoanRequestContract.class)
public class LoanRequestState implements LinearState {


    private Party borrower;
    private List<Party> lenders;
    private String panNumber;
    private int loanAmount;
    private String attachmentId;
    private UniqueIdentifier uniqueIdentifier;

    public LoanRequestState(UniqueIdentifier uniqueIdentifier, String panNumber, Party borrower,
                         int loanAmount, List<Party> lenders,String attachmentId) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.panNumber = panNumber;
        this.borrower = borrower;
        this.loanAmount = loanAmount;
        this.lenders = lenders;
        this.attachmentId = attachmentId;
    }
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> particiants = new ArrayList<>();
        particiants.add(borrower);
        particiants.addAll(lenders);
        return particiants;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public Party getBorrower() {
        return borrower;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public List<Party> getLenders() {
        return lenders;
    }

    public String getAttachmentId() { return attachmentId; }
}
