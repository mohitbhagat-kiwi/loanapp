# Loan Cordapp

This is a sample Cordapp which demonstrate a high level Loan (secured and unsecured) application scenario on a Corda network.

This cordapp provides a loan service where a loan applicant via a Broker request multiple retail banks for a quote.
The retail banks depending on their lending criteria processes or reject a loan application. Once processed the bank further request credit score and collateral evaluation from Credit and Evaluation Bureaus respectively.
After examining the Credit score and collateral banks can submit their quote against the applicants request, which is sent for broker's approval.

# Usage

## Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)

```
./gradlew clean deployNodes
```

Then type: (to run the nodes)

```
./build/nodes/runnodes
```

## Interacting with the CorDapp

The Broker starts the `RequestLoanFlow` in order to request loan from a group of
lenders(retail banks).

Go to Rrokers's terminal and run the below command.
Add the path of collateral doc(optional)

```
flow start RequestLoanFlow lenders: [BankA, BankB], panNumber: “abc123”, loanAmount: 100000, filePath: “D:\\Corda\\samples-java\\Features\\attachment-sendfile\\test.zip”
```

Validate the Loan request is created and shared with the lenders successfully by running the vaultQuery command in each
lender's terminal and borrower's terminal.

```
run vaultQuery contractStateType: com.loanapp.states.LoanRequestState
```

-----------to do-------------------
Once the lenders have verified the project details and done their due deligence, they could submit bids for loan.

Goto BankOfAshu's terminal and run the below command. The project-id can be found using the vaultQuery command shown earlier.

```
start SubmitLoanBidFlow borrower: PeterCo, loanAmount: 8000000, tenure: 5, rateofInterest: 4.0, transactionFees: 20000, projectIdentifier: <project_id>
```

Validate the loanBid is submitted successfully by running the vaultQuery command below:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.LoanBidState
```

Now the borrower can inspect the loan terms and approve the loan bid, to start the syndication process.

Go to PeterCo's terminal and run the below command. The loanbid-identifier can be found using the vaultQuery command used earlier.

```
start ApproveLoanBidFlow bidIdentifier: <loanbid-identifier>
```

One the loan bid has been approved by the borrower, the lender can start the process of creating the syndicate by
acting as the lead bank and approach participating bank for funds.

Goto BankOfAshu's terminal and run the below command.

```
start CreateSyndicateFlow participantBanks: [BankOfSneha, BankOfTom], projectIdentifier: <project-identifier>, loanDetailIdentifier: <loanbid-identifier>
```

Verify the syndicate is created using the below command:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.SyndicateState
```

On receiving the syndicate creation request, participating banks could verify the project and loan terms and submit
bids for the amount of fund they wish to lend by using the below flow in BankOfSneha or BankOfTom node.

```
start SyndicateBidFlow$Initiator syndicateIdentifier: <syndicate-id>, bidAmount: <lending-amount>
```

Verify the syndicate bid is successfully created using the below command:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.SyndicateBidState
```

The lead bank on receiving bids from participating banks could approve the bid using the below flow command.

```
start ApproveSyndicateBidFlow bidIdentifier: <sydicatebid-id>
```
