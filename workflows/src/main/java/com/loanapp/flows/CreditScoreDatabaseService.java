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

import static com.loanapp.flows.Constants.TABLE_NAME;

@CordaService
public class CreditScoreDatabaseService extends DatabaseService {
    public CreditScoreDatabaseService(@NotNull ServiceHub services) throws SQLException {
        super(services);
        this.setUpStorage();
    }

    /**
     * Adds a panNumber and associated value to the table of PanNumberValue values.
     */
    protected void addPanNumberValue(String panNumber, double value) throws SQLException {
        final String query = "insert into " + TABLE_NAME + " values (?, ?)";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, panNumber);
        params.put(2, value);

        executeUpdate(query, params);
        log.info("panNumber " + panNumber + " added to PanNumberValue_values table.");
    }

    /**
     * Retrieves the value of a panNumber in the table of panNumber values.
     */
    protected double queryPanNumberValue(String panNumber) throws SQLException {
        final String query = "select value from " + TABLE_NAME + " where panNumber = ?";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, panNumber);

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
            throw new IllegalArgumentException("panNumber " + panNumber + " not present in database");
        } else if (results.size() > 1) {
            throw new IllegalArgumentException("Error list has more than one element");
        }

        log.info("panNumber " + panNumber + "read from panNumber_values table.");
        return (Double) results.get(0);
    }

    protected List<Object> queryAllPanNumberValue() throws SQLException {
        final String query = "select * from " + TABLE_NAME;
        final Map<Integer, Object> params = new HashMap<>();
        Function<ResultSet, Object> transformer = (it) -> {

            double i = 0;
            String p = "";
            try {
                i = it.getDouble("value");
                p = it.getString("panNumber");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return p + ":" + i ;
        };

        final List<Object> results = executeQuery(query, params, transformer);

        if (results.isEmpty()) {
            throw new IllegalArgumentException("not present in database");
        }

        return results;
    }


    private void setUpStorage() throws SQLException {
        final String query = "create table if not exists " + TABLE_NAME +
                "(panNumber varchar(64), value int)";
        executeUpdate(query, Collections.emptyMap());
        log.info("Created panNumber_values table.");
    }
}
