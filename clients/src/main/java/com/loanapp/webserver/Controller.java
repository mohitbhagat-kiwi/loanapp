package com.loanapp.webserver;

import com.loanapp.flows.*;
import com.loanapp.states.*;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.OpaqueBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Define your API endpoints here.
 */

@RestController
@CrossOrigin
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
//    @Autowired
//    private CordaRPCOps notaryProxy;
    @Autowired
    private CordaRPCOps brokerProxy;
    @Autowired
    private CordaRPCOps bankAProxy;
    @Autowired
    private CordaRPCOps bankBProxy;
    @Autowired
    private CordaRPCOps creditBureauProxy;
    @Autowired
    private CordaRPCOps evaluationBureauProxy;
    @Autowired
    @Qualifier("brokerProxy")
    private CordaRPCOps activeParty;
    //private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);
    private final Map<String, Class<? extends ContractState>> map= getStateMapData();

    private Map<String, Class<? extends ContractState>> getStateMapData() {
        Map map =  new HashMap<>();//Creating HashMap
        map.put("LoanRequestState", LoanRequestState.class);  //Put elements in Map
        return  map;
    }

    private Class<? extends ContractState> getStateMapData(String stateName) {
        return LoanRequestState.class;
        //return (Class<? extends ContractState>) map.get(stateName).class;
    }

//    public Controller(NodeRPCConnection rpc) {
//        this.proxy = rpc.proxy;
//    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    @GetMapping("loanRequests")
    public APIResponse<List<StateAndRef<LoanRequestState>>> getLoanRequestsList() {
        try{
            List<StateAndRef<LoanRequestState>> statesList = activeParty.vaultQuery(LoanRequestState.class).getStates();
            return APIResponse.success(statesList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("loanQuotes")
    public APIResponse<List<StateAndRef<LoanQuoteState>>> getLoanQuotesList() {
        try{
            List<StateAndRef<LoanQuoteState>> statesList = activeParty.vaultQuery(LoanQuoteState.class).getStates();
            return APIResponse.success(statesList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("evaluationStates")
    public APIResponse<List<StateAndRef<EvaluationState>>> getEvaluationStatesList() {
        try{
            List<StateAndRef<EvaluationState>> statesList = activeParty.vaultQuery(EvaluationState.class).getStates();
            return APIResponse.success(statesList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("evaluationRequestStates")
    public APIResponse<List<StateAndRef<EvaluationRequestState>>> getEvaluationRequestStatesList() {
        try{
            List<StateAndRef<EvaluationRequestState>> statesList = activeParty.vaultQuery(EvaluationRequestState.class).getStates();
            return APIResponse.success(statesList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("creditScoreStates")
    public APIResponse<List<StateAndRef<CreditScoreState>>> getCreditScoreStatesList() {
        try{
            List<StateAndRef<CreditScoreState>> stateList = activeParty.vaultQuery(CreditScoreState.class).getStates();
            return APIResponse.success(stateList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("loanRequest123")
    public APIResponse<List<StateAndRef<LoanRequestState>>> getVaultStateList(String stateName) {
        try{
            List<StateAndRef<LoanRequestState>> auctionList = activeParty.vaultQuery(LoanRequestState.class).getStates();
            return APIResponse.success(auctionList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping(value = "switch-party/{party}")
    public APIResponse<Void> switchParty(@PathVariable String party){
        switch (party){
//            case "notary" :
//                activeParty = notaryProxy;
//                break;
            case "broker" :
                activeParty = brokerProxy;
                break;
            case "bankA" :
                activeParty = bankAProxy;
                break;
            case "bankB" :
                activeParty = bankBProxy;
                break;
            case "creditBureau" :
                activeParty = creditBureauProxy;
                break;
            case "evaluationBureau" :
                activeParty = evaluationBureauProxy;
                break;
            default:
                return APIResponse.error("Unrecognised Party");
        }

        return APIResponse.success();
    }

    @GetMapping("hello")
    public APIResponse<String> getString() {
        try{
            return APIResponse.success("auctionList");
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("requestLoan")
    public APIResponse<Void> requestLoan(@RequestBody Forms.LoanRequestForm loanRequest){
        try{
            List<Party> lenders = new ArrayList<>();
            loanRequest.getLenders().stream().forEach( name ->
                    lenders.add(activeParty.partiesFromName(name, false).iterator().next())
            );
            activeParty.startFlowDynamic(RequestLoanFlow.Initiator.class,
                            lenders,loanRequest.getPanNumber(),loanRequest.getLoanAmount())
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("processLoan")
    public APIResponse<Void> processLoan(@RequestBody Forms.LoanProcessForm processLoanRequest){
        try{
            UUID uuid = UUID.fromString(processLoanRequest.getLoanRequestIdentifier());
            activeParty.startFlowDynamic(ProcessLoanFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid),processLoanRequest.getStatus())
                    .getReturnValue().get();



            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("addPanNumber")
    public APIResponse<Void> addPanNumber(@RequestBody Forms.AddPanNumberForm addPannumberRequest){
        try{
            activeParty.startFlowDynamic(AddPannumberFlow.class,
                            addPannumberRequest.getPanNumber(),addPannumberRequest.getCreditValue())
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("requestCreditScore")
    public APIResponse<Void> requestCreditScore(@RequestBody Forms.RequestCreditScoreForm creditScoreRequest){
        try{
            activeParty.startFlowDynamic(RequestCreditScoreFlow.Initiator.class,
                            creditScoreRequest.getPanNumber())
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("requestEvaluation")
    public APIResponse<Void> requestEvaluation(@RequestBody Forms.RequestEvaluationForm requestEvaluationRequest){
        try{
            UUID uuid = UUID.fromString(requestEvaluationRequest.getLoanRequestIdentifier());
            activeParty.startFlowDynamic(RequestEvaluationFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid))
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("issueEvaluation")
    public APIResponse<Void> issueEvaluation(@RequestBody Forms.IssueEvaluationForm issueEvaluationRequest){
        try{
            UUID uuid = UUID.fromString(issueEvaluationRequest.getEvaluationRequestID());
            activeParty.startFlowDynamic(IssueEvaluationFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid), issueEvaluationRequest.getEvaluationPrice())
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("submitLoanQuote")
    public APIResponse<Void> submitLoanQuote(@RequestBody Forms.SubmitLoanQuoteForm submitLoanQuoteRequest){
        try{
            UUID uuid = UUID.fromString(submitLoanQuoteRequest.getQuoteIdentifier());
            activeParty.startFlowDynamic(SubmitLoanQuoteFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid), submitLoanQuoteRequest.getLoanAmount(), submitLoanQuoteRequest.getTenure(), submitLoanQuoteRequest.getRateofInterest(), submitLoanQuoteRequest.getTransactionFees())
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("approveLoanQuote")
    public APIResponse<Void> approveLoanQuote(@RequestBody Forms.ApproveLoanQuoteForm approveLoanQuoteRequest){
        try{
            UUID uuid = UUID.fromString(approveLoanQuoteRequest.getQuoteId());
            activeParty.startFlowDynamic(ApproveLoanQuoteFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid))
                    .getReturnValue().get();

            return APIResponse.success();
        }catch (ExecutionException e){
            if(e.getCause() != null && e.getCause().getClass().equals(TransactionVerificationException.ContractRejection.class)){
                return APIResponse.error(e.getCause().getMessage());
            }else{
                return APIResponse.error(e.getMessage());
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            return APIResponse.error(e.getMessage());
        }
    }
}