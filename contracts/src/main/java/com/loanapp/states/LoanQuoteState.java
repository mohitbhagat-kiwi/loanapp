package com.loanapp.states;

import com.loanapp.contracts.LoanQuoteContract;
import com.loanapp.contracts.LoanRequestContract;
import kotlin.jvm.JvmStatic;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(LoanQuoteContract.class)
public class LoanQuoteState implements LinearState {

    private LinearPointer<LoanRequestState> loanRequestDetails;
    private UniqueIdentifier uniqueIdentifier;

    private Party borrower;
    private Party lender;
    private int loanAmount;
    private int tenure;
    private double rateofInterest;
    private int transactionFees;
    private String status;

    private LoanQuoteState(LinearPointer<LoanRequestState> loanRequestDetails,
                          UniqueIdentifier uniqueIdentifier, Party borrower, Party lender,String status){
        this.loanRequestDetails = loanRequestDetails;
        this.uniqueIdentifier = uniqueIdentifier;
        this.lender = lender;
        this.borrower = borrower;
        this.status = status;
    }

    @JvmStatic
    public static LoanQuoteState Issue(LinearPointer<LoanRequestState> loanRequestDetails,
                                UniqueIdentifier uniqueIdentifier, Party borrower, Party lender,String status){
        return new LoanQuoteState(loanRequestDetails,uniqueIdentifier, borrower, lender,status);
    }

    @ConstructorForDeserialization
    public LoanQuoteState(LinearPointer<LoanRequestState> loanRequestDetails,
                            UniqueIdentifier uniqueIdentifier, Party borrower, Party lender,
                          int loanAmount, int tenure, double rateofInterest, int transactionFees,
                          String status){
        this.loanRequestDetails = loanRequestDetails;
        this.uniqueIdentifier = uniqueIdentifier;
        this.lender = lender;
        this.borrower = borrower;
        this.loanAmount = loanAmount;
        this.tenure = tenure;
        this.rateofInterest = rateofInterest;
        this.transactionFees = transactionFees;
        this.status = status;
    }

    public Party getBorrower() { return borrower; }
    public Party getLender() { return lender; }
    public int getLoanAmount() {
        return loanAmount;
    }
    public int getTenure() { return tenure; }
    public double getRateofInterest() { return rateofInterest; }

    public int getTransactionFees() { return transactionFees; }

    public String getStatus() { return status; }

    public LinearPointer<LoanRequestState> getLoanRequestDetails() {
        return loanRequestDetails;
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
