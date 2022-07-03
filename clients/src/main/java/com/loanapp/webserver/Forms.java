package com.loanapp.webserver;

import java.util.List;

public class Forms {
    public static class LoanRequestForm {

        private List<String> lenders;
        private String panNumber;
        private int loanAmount;

        private String attachmentId;

        public List<String> getLenders() {
            return lenders;
        }
        public String getPanNumber() {
            return panNumber;
        }
        public int getLoanAmount() {
            return loanAmount;
        }
        public String getAttachmentId() {
            return attachmentId;
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

        public void setAttachmentId(String attachmentId) {
            this.attachmentId = attachmentId;
        }
    }

    public static class LoanProcessForm {
        private String loanRequestIdentifier;
        private String status;
        public String getLoanRequestIdentifier() {
            return loanRequestIdentifier;
        }
        public String getStatus() { return status; }
        public void setLoanRequestIdentifier(String loanRequestIdentifier) {
            this.loanRequestIdentifier = loanRequestIdentifier;
        }
        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class AddPanNumberForm {
        private String panNumber;
        private int creditValue;

        public String getPanNumber() {
            return panNumber;
        }
        public int getCreditValue() {
            return creditValue;
        }

        public void setPanNumber(String panNumber) {
            this.panNumber = panNumber;
        }
        public void setCreditValue(int creditValue) {
            this.creditValue = creditValue;
        }
    }

    public static class RequestCreditScoreForm {
        private String panNumber;
        public String getPanNumber() {
            return panNumber;
        }
        public void setPanNumber(String panNumber) {
            this.panNumber = panNumber;
        }
    }

    public static class RequestEvaluationForm {
        private String loanRequestIdentifier;
        public String getLoanRequestIdentifier() {
            return loanRequestIdentifier;
        }
        public void setLoanRequestIdentifier(String loanRequestIdentifier) {
            this.loanRequestIdentifier = loanRequestIdentifier;
        }
    }

    public static class IssueEvaluationForm {
        private String evaluationRequestID;
        private int evaluationPrice;
        public String getEvaluationRequestID() {
            return evaluationRequestID;
        }
        public int getEvaluationPrice() {
            return evaluationPrice;
        }
        public void setEvaluationRequestID(String panNumber) {
            this.evaluationRequestID = panNumber;
        }
        public void setEvaluationPrice(int evaluationPrice) {
            this.evaluationPrice = evaluationPrice;
        }
    }

    public static class SubmitLoanQuoteForm {
        private String quoteIdentifier;
        private int loanAmount;
        private int tenure;
        private double rateofInterest;
        private int transactionFees;
        public String getQuoteIdentifier() {
            return quoteIdentifier;
        }
        public int getLoanAmount() {
            return loanAmount;
        }
        public int getTenure() {
            return tenure;
        }
        public double getRateofInterest() {
            return rateofInterest;
        }
        public int getTransactionFees() {
            return transactionFees;
        }
        public void setQuoteIdentifier(String quoteIdentifier) {
            this.quoteIdentifier = quoteIdentifier;
        }
        public void setLoanAmount(int loanAmount) {
            this.loanAmount = loanAmount;
        }
        public void setTenure(int tenure) {
            this.tenure = tenure;
        }
        public void setRateofInterest(double rateofInterest) {
            this.rateofInterest = rateofInterest;
        }
        public void setTransactionFees(int transactionFees) {
            this.transactionFees = transactionFees;
        }
    }

    public static class ApproveLoanQuoteForm {
        private String quoteId;
        public String getQuoteId() {
            return quoteId;
        }
        public void setQuoteId(String loanRequestIdentifier) {
            this.quoteId = loanRequestIdentifier;
        }
    }
    public static class DownloadAttachmentForm {
        private String evaluationRequestID;
        public String getEvaluationRequestID() {
            return evaluationRequestID;
        }
        public void setEvaluationRequestID(String panNumber) {
            this.evaluationRequestID = panNumber;
        }
    }

    public static class LoginForm{
        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        private String username;
        private String password;
    }
}
