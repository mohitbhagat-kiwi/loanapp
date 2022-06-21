package com.loanapp.states;

import com.loanapp.contracts.IOUContract;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import net.corda.core.contracts.*;
import net.corda.core.identity.*;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.ImmutableList;

import java.util.*;

@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState {

    public final Amount<IssuedTokenType> amount;
    public final Party lender;
    public final AnonymousParty borrower;
    public final Amount<IssuedTokenType> paid;
    private final UniqueIdentifier linearId;

    // Private constructor used only for copying a State object
    @ConstructorForDeserialization
    private IOUState(Amount<IssuedTokenType> amount, Party lender, AnonymousParty borrower, Amount<IssuedTokenType> paid, UniqueIdentifier linearId){
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    public IOUState(Amount<IssuedTokenType> amount, Party lender, AnonymousParty borrower) {
        this(amount, lender, borrower, new Amount<>(0, amount.getToken()), new UniqueIdentifier());
    }

    public Amount<IssuedTokenType> getAmount() {
        return amount;
    }

    public Party getLender() {
        return lender;
    }

    public AnonymousParty getBorrower() {
        return borrower;
    }

    public Amount<IssuedTokenType> getPaid() {
        return paid;
    }
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return  ImmutableList.of(lender, borrower);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current states with a newly specified lender. For use when transferring.
     * - [copy] creates a copy of the states using the internal copy constructor ensuring the LinearId is preserved.
     */
    public IOUState pay(Amount<IssuedTokenType> amountToPay) {
        Amount<IssuedTokenType> newAmountPaid = this.paid.plus(amountToPay);
        return new IOUState(amount, lender, borrower, newAmountPaid, linearId);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, paid, linearId);
    }

    public IOUState copy(Amount<IssuedTokenType> amount, Party lender, AnonymousParty borrower, Amount<IssuedTokenType> paid) {
        return new IOUState(amount, lender, borrower, paid, this.getLinearId());
    }
}
