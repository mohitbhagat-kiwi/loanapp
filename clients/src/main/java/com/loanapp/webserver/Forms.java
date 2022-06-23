package com.loanapp.webserver;

import java.util.List;

public class Forms {
    public static class LoanRequestForm {

        private List<String> lenders;
        private String panNumber;
        private int loanAmount;

        public List<String> getLenders() {
            return lenders;
        }

        public String getPanNumber() {
            return panNumber;
        }

        public int getLoanAmount() {
            return loanAmount;
        }

        public void setLenders(List<String> lenders) {
            this.lenders = lenders;
        }

        public void setPanNumber(String panNumber) {
            this.panNumber = panNumber;
        }

        public void setLoanAmount(int loanAmount) {
            this.loanAmount = loanAmount;
        }
    }
}
