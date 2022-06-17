package com.loanapp.flows;

import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@CordaService
public class LoanProcessingService  extends SingletonSerializeAsToken {
    private final static Logger log = LoggerFactory.getLogger(LoanProcessingService.class);
    private final static Executor executor = Executors.newFixedThreadPool(8);
    private final AppServiceHub serviceHub;

    public LoanProcessingService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        processLoan();
        log.info("Tracking new Payment Request");
    }

    private void processLoan() {
        Party ourIdentity = ourIdentity();

        serviceHub.getVaultService().trackBy(LoanRequestState.class).getUpdates().subscribe(
                update -> {
                    update.getProduced().forEach(
                            message -> {

                                TransactionState<LoanRequestState> state = message.getState();
                                if (ourIdentity.getName().getOrganisationUnit().equals("Bank")
                                ) {
                                    executor.execute(() -> {
                                        try {
                                            serviceHub.startFlow(new ProcessLoanFlow.Initiator(
                                                    state.getData().getLinearId()));
                                        }
                                        catch (Exception e) {
                                            log.error("flow error " + e.getMessage());
                                        }
                                    });
                                }
                            }
                    );
                }
        );
    }

    private Party ourIdentity() {
        return serviceHub.getMyInfo().getLegalIdentities().get(0);
    }

}
