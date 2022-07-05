package com.loanapp.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@InitiatingFlow
    @StartableByRPC
     public class GetAllPanNumberFlow extends FlowLogic<List<Object>>  {

        public GetAllPanNumberFlow() {}

        @Override
        @Suspendable
        public List<Object> call() throws FlowException {

            // Get creditscore from DB based on Pan Number
            final CreditScoreDatabaseService databaseService = getServiceHub().cordaService(CreditScoreDatabaseService.class);
            List<Object> creditScore = new ArrayList<Object>();
            try {
                creditScore = databaseService.queryAllPanNumberValue();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println(creditScore);
            return creditScore;
        }
    }


