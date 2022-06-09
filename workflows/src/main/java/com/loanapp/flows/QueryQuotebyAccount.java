package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.loanapp.states.LoanQuoteState;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@StartableByRPC
public class QueryQuotebyAccount extends FlowLogic<List<UniqueIdentifier>> {

    final String name;

    public QueryQuotebyAccount(String name) {
        this.name = name;
    }


    @Override
    @Suspendable
    public List<UniqueIdentifier> call() throws FlowException {
        AccountInfo myAccount = UtilitiesKt.getAccountService(this).accountInfo(name).get(0).getState().getData();
        UUID id = myAccount.getIdentifier().getId();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(id));

        List<StateAndRef<LoanQuoteState>> quoteStateAndRefs =  getServiceHub().getVaultService().queryBy(LoanQuoteState.class,criteria).getStates();
        if(quoteStateAndRefs.size()==0){
            return Collections.emptyList();
        }
        return quoteStateAndRefs.stream().map(ref -> ref.getState().getData().getLinearId()
        ).collect(Collectors.toList());
    }
}
