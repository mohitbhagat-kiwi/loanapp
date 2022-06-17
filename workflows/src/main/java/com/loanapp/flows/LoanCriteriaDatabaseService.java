package com.loanapp.flows;

import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.loanapp.flows.Constants.LOAN_CRITERIA_TABLE_NAME;

@CordaService
public class LoanCriteriaDatabaseService extends DatabaseService {
    public LoanCriteriaDatabaseService(@NotNull ServiceHub services) throws SQLException {
        super(services);
        this.setUpStorage();
    }

    /**
     * Adds a criteria and associated value to the table of loanCriteria_values.
     */
    protected void addLoanCiteriaValue(String criteria, double value) throws SQLException {
        final String query = "insert into " + LOAN_CRITERIA_TABLE_NAME + " values (?, ?)";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, criteria);
        params.put(2, value);

        executeUpdate(query, params);
        log.info("criteria " + criteria + " added to loanCriteria_values table.");
    }

    /**
     * Retrieves the value of a criteria in the table of loanCriteria_values.
     */
    protected double queryLaonCriteriaValue(String criteria) throws SQLException {
        final String query = "select value from " + LOAN_CRITERIA_TABLE_NAME + " where criteria = ?";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, criteria);

        Function<ResultSet, Object> transformer = (it) -> {
            double i = 0;
            try {
                i = it.getDouble("value");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return i;
        };

        final List<Object> results = executeQuery(query, params, transformer);

        if (results.isEmpty()) {
            throw new IllegalArgumentException("criteria " + criteria + " not present in database");
        } else if (results.size() > 1) {
            throw new IllegalArgumentException("Error list has more than one element");
        }

        log.info("criteria " + criteria + "read from loanCriteria_values table.");
        return (Double) results.get(0);
    }

    private void setUpStorage() throws SQLException {
        final String query = "create table if not exists " + LOAN_CRITERIA_TABLE_NAME +
                "(criteria varchar(64), value DOUBLE PRECISION)";
        executeUpdate(query, Collections.emptyMap());
        log.info("Created loanCriteria_values table.");
    }
}
