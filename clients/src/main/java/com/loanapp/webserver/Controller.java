package com.loanapp.webserver;

import com.loanapp.flows.*;
import com.loanapp.states.*;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.OpaqueBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.net.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

    @GetMapping("getAllPanNumber")
    public APIResponse<List<Object>> getAllPanNumber(){
        try{
            List<Object> res = activeParty.startFlowDynamic(GetAllPanNumberFlow.class)
                    .getReturnValue().get();
            return APIResponse.success(res);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("getBrokerDashBoard")
    public APIResponse<LinkedHashMap<String, Integer>> getBrokerDashBoard() {
        int totalLoanAmount = 0;
        int inProcessStatus = 0;
        int rejectedStatus = 0;
        int submittedStatus = 0;
        int approvedStatus = 0;
        try{
            List<StateAndRef<LoanRequestState>> loanRequestStateList = activeParty.vaultQuery(LoanRequestState.class).getStates();
            for ( int i =0 ; i< loanRequestStateList.size() ; i++) {
                totalLoanAmount += loanRequestStateList.get(i).getState().getData().getLoanAmount();
            }

            List<StateAndRef<LoanQuoteState>> loanQuoteStatesList = activeParty.vaultQuery(LoanQuoteState.class).getStates();
            for ( int i =0 ; i< loanQuoteStatesList.size() ; i++) {
                if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("Submitted")) {
                    submittedStatus++;
                } else if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("InProcess")) {
                    inProcessStatus++;
                }else if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("Rejected")) {
                    rejectedStatus++;
                }else if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("Approved")) {
                    approvedStatus++;
                }
            }

            LinkedHashMap<String, Integer> finalResult = new LinkedHashMap<>();
            finalResult.put("totalLoanRequest",loanRequestStateList.size());
            finalResult.put("totalLoanAmount",totalLoanAmount);
            finalResult.put("totalQuoteReceived",loanQuoteStatesList.size());
            finalResult.put("totalPendingApproval",submittedStatus);
            finalResult.put("totalProcessApproval",inProcessStatus);
            finalResult.put("totalRejectedApproval",rejectedStatus);
            finalResult.put("totalApprovedApproval",approvedStatus);

            return APIResponse.success(finalResult);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("getBankDashBoard")
    public APIResponse<LinkedHashMap<String, Integer>> getBankDashBoard() {
        int totalLoanAmountQuoted = 0;
        int inProcessStatus = 0;
        int rejectedStatus = 0;
        int submittedStatus = 0;
        int approvedStatus = 0;
        try{
            List<StateAndRef<LoanRequestState>> loanRequestStateList = activeParty.vaultQuery(LoanRequestState.class).getStates();

            List<StateAndRef<EvaluationRequestState>> evaluationRequestStatesList = activeParty.vaultQuery(EvaluationRequestState.class).getStates();

            List<StateAndRef<EvaluationState>> evaluationStatesList = activeParty.vaultQuery(EvaluationState.class).getStates();

            List<StateAndRef<CreditScoreState>> creditScoreStateList = activeParty.vaultQuery(CreditScoreState.class).getStates();

            List<StateAndRef<LoanQuoteState>> loanQuoteStatesList = activeParty.vaultQuery(LoanQuoteState.class).getStates();

            for ( int i =0 ; i< loanQuoteStatesList.size() ; i++) {
                if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("Submitted")) {
                    submittedStatus++;
                } else if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("InProcess")) {
                    inProcessStatus++;
                }else if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("Rejected")) {
                    rejectedStatus++;
                }else if ( loanQuoteStatesList.get(i).getState().getData().getStatus().equalsIgnoreCase("Approved")) {
                    approvedStatus++;
                }
                totalLoanAmountQuoted += loanQuoteStatesList.get(i).getState().getData().getLoanAmount();
            }
            LinkedHashMap<String, Integer> finalResult = new LinkedHashMap<>();
            finalResult.put("totalLoanRequest",loanRequestStateList.size());
            finalResult.put("totalQuoteReceived",loanQuoteStatesList.size());
            finalResult.put("totalSubmittedApproval",submittedStatus);
            finalResult.put("totalProcessApproval",inProcessStatus);
            finalResult.put("totalRejectedApproval",rejectedStatus);
            finalResult.put("totalApprovedApproval",approvedStatus);
            finalResult.put("totalEvaluationRequest",evaluationRequestStatesList.size());
            finalResult.put("totalEvaluation",evaluationStatesList.size());
            finalResult.put("totalCreditScore",creditScoreStateList.size());
            finalResult.put("totalLoanAmountQuoted",totalLoanAmountQuoted);

            return APIResponse.success(finalResult);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("getCreditBureauDashBoard")
    public APIResponse<LinkedHashMap<String, Integer>> getCreditBureauDashBoard() {

        try{
            List<Object> res = activeParty.startFlowDynamic(GetAllPanNumberFlow.class).getReturnValue().get();

            List<StateAndRef<CreditScoreState>> creditScoreStateList = activeParty.vaultQuery(CreditScoreState.class).getStates();

            LinkedHashMap<String, Integer> finalResult = new LinkedHashMap<>();
            finalResult.put("totalPanCards",res.size());
            finalResult.put("totalCreditScore",creditScoreStateList.size());

            return APIResponse.success(finalResult);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("getEvaluationBureauDashBoard")
    public APIResponse<LinkedHashMap<String, Integer>> getEvaluationBureauDashBoard() {

        try{
            List<StateAndRef<EvaluationRequestState>> evaluationRequestStatesList = activeParty.vaultQuery(EvaluationRequestState.class).getStates();

            List<StateAndRef<EvaluationState>> evaluationStatesList = activeParty.vaultQuery(EvaluationState.class).getStates();

            LinkedHashMap<String, Integer> finalResult = new LinkedHashMap<>();
            finalResult.put("totalEvaluationRequest",evaluationRequestStatesList.size());
            finalResult.put("totalEvaluation",evaluationStatesList.size());

            return APIResponse.success(finalResult);
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
    public APIResponse<String> requestLoan(@RequestBody Forms.LoanRequestForm loanRequest){
        try{
            List<Party> lenders = new ArrayList<>();
            String result = null;
            loanRequest.getLenders().stream().forEach( name ->
                     lenders.add(activeParty.partiesFromName(name, false).iterator().next())
            );
            if(loanRequest.getAttachmentId()!= "" && loanRequest.getAttachmentId() != null){
                result =  activeParty.startFlowDynamic(RequestLoanFlow.Initiator.class,
                                lenders,loanRequest.getPanNumber(),loanRequest.getLoanAmount()
                                ,"",SecureHash.parse(loanRequest.getAttachmentId()))
                        .getReturnValue().get();
            }else {

                result =  activeParty.startFlowDynamic(RequestLoanFlow.Initiator.class,
                                lenders,loanRequest.getPanNumber(),loanRequest.getLoanAmount())
                        .getReturnValue().get();
            }

            return APIResponse.success(result);
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

    @RequestMapping(path = "requestLoanFile", method = RequestMethod.POST,
            consumes = {"multipart/form-data"})
    public APIResponse<Void> requestLoanFile(@RequestPart List<String> lenders, @RequestPart String panNumber,
                                             @RequestPart int loanAmount,@RequestPart MultipartFile file){
        try{
            List<Party> lenderList = new ArrayList<>();
            lenders.stream().forEach( name ->
                    lenderList.add(activeParty.partiesFromName(name, false).iterator().next())
            );
            SecureHash attachmentHash = null;
            if(file != null){
                String filename = file.getOriginalFilename();
                String nodeOrg = activeParty.nodeInfo().getLegalIdentities().get(0).getName().getOrganisation();
                attachmentHash=activeParty.uploadAttachment(
                        file.getInputStream());
            }
            activeParty.startFlowDynamic(RequestLoanFlow.Initiator.class,
                            lenderList,panNumber,loanAmount, "",attachmentHash)
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
    public APIResponse<String> processLoan(@RequestBody Forms.LoanProcessForm processLoanRequest){
        try{
            String result = null;
            UUID uuid = UUID.fromString(processLoanRequest.getLoanRequestIdentifier());
            result = activeParty.startFlowDynamic(ProcessLoanFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid),processLoanRequest.getStatus())
                    .getReturnValue().get();

            return APIResponse.success(result);
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
    public APIResponse<String> requestCreditScore(@RequestBody Forms.RequestCreditScoreForm creditScoreRequest){
        try{
            String result = null;
            result = activeParty.startFlowDynamic(RequestCreditScoreFlow.Initiator.class,
                            creditScoreRequest.getPanNumber())
                    .getReturnValue().get();

            return APIResponse.success(result);
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
    public APIResponse<String> requestEvaluation(@RequestBody Forms.RequestEvaluationForm requestEvaluationRequest){
        try{
            String result = null;
            UUID uuid = UUID.fromString(requestEvaluationRequest.getLoanRequestIdentifier());
            result = activeParty.startFlowDynamic(RequestEvaluationFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid))
                    .getReturnValue().get();

            return APIResponse.success(result);
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
    public APIResponse<String> issueEvaluation(@RequestBody Forms.IssueEvaluationForm issueEvaluationRequest){
        try{
            String result = null;
            UUID uuid = UUID.fromString(issueEvaluationRequest.getEvaluationRequestID());
            result = activeParty.startFlowDynamic(IssueEvaluationFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid), issueEvaluationRequest.getEvaluationPrice())
                    .getReturnValue().get();

            return APIResponse.success(result);
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
    public APIResponse<String> submitLoanQuote(@RequestBody Forms.SubmitLoanQuoteForm submitLoanQuoteRequest){
        try{
            String result = null;
            UUID uuid = UUID.fromString(submitLoanQuoteRequest.getQuoteIdentifier());
            result = activeParty.startFlowDynamic(SubmitLoanQuoteFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid), submitLoanQuoteRequest.getLoanAmount(), submitLoanQuoteRequest.getTenure(), submitLoanQuoteRequest.getRateofInterest(), submitLoanQuoteRequest.getTransactionFees())
                    .getReturnValue().get();

            return APIResponse.success(result);
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
    public APIResponse<String> approveLoanQuote(@RequestBody Forms.ApproveLoanQuoteForm approveLoanQuoteRequest){
        try{
            String result = null;
            UUID uuid = UUID.fromString(approveLoanQuoteRequest.getQuoteId());
           result = activeParty.startFlowDynamic(ApproveLoanQuoteFlow.Initiator.class,
                            new UniqueIdentifier(null,uuid))
                    .getReturnValue().get();

            return APIResponse.success(result);
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

    @RequestMapping(path = "upload/{lenders}/{panNumber}/{loanAmount}", method = RequestMethod.POST,
            consumes = {"multipart/form-data"})
    public ResponseEntity<String> upload(@RequestPart MultipartFile file, @PathVariable String lenders,
                                         @PathVariable String panNumber,@PathVariable int loanAmount) throws IOException, ExecutionException, InterruptedException {

            List<Party> lenderList = new ArrayList<>();
            Arrays.asList(lenders.split(",")).stream().forEach( name ->
                    lenderList.add(activeParty.partiesFromName(name, false).iterator().next())
            );
            SecureHash attachmentHash = null;
            if(file != null){
                String filename = file.getOriginalFilename();
                String nodeOrg = activeParty.nodeInfo().getLegalIdentities().get(0).getName().getOrganisation();
                attachmentHash=activeParty.uploadAttachment(
                        file.getInputStream());
            }
            activeParty.startFlowDynamic(RequestLoanFlow.Initiator.class,
                            lenderList,panNumber,loanAmount, "",attachmentHash)
                    .getReturnValue().get();

            return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - ");
    }

    @PostMapping(path = "uploadTest/{lenders}/{panNumber}/{loanAmount}")
    public ResponseEntity<String> uploadTest(@PathVariable String lenders,
                                         @PathVariable String panNumber,@PathVariable int loanAmount) throws IOException, ExecutionException, InterruptedException {
        try {
            // APPROACH ONE

//            String oppath = "/home/kiwitech/Desktop";
//            URLConnection conn = new URL("https://dxvsxo399v3w9.cloudfront.net/3mhn%2Ffile%2F0f441dc45cdfabf4437f7a1b424b19c9_test.zip?response-content-disposition=attachment%3Bfilename%3D%22test.zip%22%3B&response-content-encoding=binary&Expires=1657275979&Signature=DaIBdDDoh9d1YbFSNaZtl1gCxIxCj3snJ2tYVzAcepmxSAtjFqqXmhUV8Ipe~basN~8iEmBHZ430~eqVBayYQqZO~2lc5lqq4h3i074HuBERB4IDEXzs9O9yj4cmaVrm7rZ~4PyKOZfOzF~cMZhoCGlJ2KyuKHcRKivrNmYroWo9Xat9deEYjIaPXDCdHU4~Du5KzBRJ2HLhXAePWUtXHrJkozaFKfDPbgsjtt81Nam5YGxj4wQTHz1mjuWPaQCa-5eeArwpa0~QlS0sekTLXk9PvCcAotUvYazz1KNquHi7Kh0OnOL85Hop-d1OnNjJXfWJYXFLUf0rMd~j1TJnXw__&Key-Pair-Id=APKAJT5WQLLEOADKLHBQ").openConnection();
//            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
//            conn.setRequestProperty("content-type", "binary/data");
//            InputStream in = conn.getInputStream();
//            FileOutputStream out = new FileOutputStream(oppath + "tmp.zip");
//
//            byte[] b = new byte[1024];
//            int count;
//
//            while ((count = in.read(b)) > 0) {
//                out.write(b, 0, count);
//            }
//            out.close();
//            in.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

            // APPROACH TWO
            String fileURL = "https://dxvsxo399v3w9.cloudfront.net/3mhn%2Ffile%2F0f441dc45cdfabf4437f7a1b424b19c9_test.zip?response-content-disposition=attachment%3Bfilename%3D%22test.zip%22%3B&response-content-encoding=binary&Expires=1657300392&Signature=BUPcDb5pspv6KHPHkY2Hx1ExqjL4eq47f3a-rjsQobjVMl0cmgHXGnHC0s1n7~iDzHpxpcoj2K1oouDPiWpUGn-Aozbf~1gepzEb9v8Xajk2OeV-kScmsPhXjL9vYRWvOxaPIqjbv4m1ztX81LyK3n2yMyo6gm7ZrsUcJj1SyBlFIrxDonKxjc-b35CX4wosj36aArgWgVKlB9UTzBoloQy9DiFHWn9p~WnFPw0s8O5L9ftj~aineqRPGhMpEBHOvWzzHGmJyTVrKG6Vq51lBl0SU5CmH2n7EDZEXhmVZfQ47EbiyO8lMuYI69IbCe4nrvl3iOnVpYRdJRbKqHG2dg__&Key-Pair-Id=APKAJT5WQLLEOADKLHBQ";
            int BUFFER_SIZE = 4096;
            String saveDir = "/home/kiwitech/Desktop";
            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");
                String contentType = httpConn.getContentType();
                int contentLength = httpConn.getContentLength();

                if (disposition != null) {
                    // extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10,
                                disposition.length() - 1);
                    }
                } else {
                    // extracts file name from URL
                    fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                            fileURL.length());
                }

                System.out.println("Content-Type = " + contentType);
                System.out.println("Content-Disposition = " + disposition);
                System.out.println("Content-Length = " + contentLength);
                System.out.println("fileName = " + fileName);

                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();
                String saveFilePath = saveDir + File.separator + fileName;

                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                System.out.println("File downloaded");
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            }
            httpConn.disconnect();

        } catch (IOException e) {
        e.getMessage();
        }
        return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - ");

    }

    @PostMapping("download-attachment")
    public ResponseEntity<Resource> download(@RequestParam String evaluationRequestID){
        UUID uuid = UUID.fromString(evaluationRequestID);
        try {
            String attachmentId = activeParty.startFlowDynamic(DownloadEvaluationAttachmentFlow.Initiator.class,
                    new UniqueIdentifier(null,uuid)).getReturnValue().get();
            InputStreamResource ip = new InputStreamResource(activeParty.openAttachment(SecureHash.parse(attachmentId)));
            return ResponseEntity.ok().header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"attachment.zip\""
            ).body(ip);

        }catch (Exception e){
            return (ResponseEntity<Resource>) ResponseEntity.notFound();
        }

    }

    @PostMapping("login")
    public APIResponse<Void> approveLoanQuote(@RequestBody Forms.LoginForm request){
        if(request.getUsername() == "kiwitech" && request.getPassword() == "admin@123"){
            return APIResponse.success();
        }
        else return APIResponse.error("Invalid Credentials");
    }

    @GetMapping("loanQuotesData")
    public APIResponse<Quotesdata> getLoanQuotesData() {
        try{
            List<StateAndRef<LoanQuoteState>> quotesList = activeParty.vaultQuery(LoanQuoteState.class).getStates();
            List<StateAndRef<LoanRequestState>> statesList = activeParty.vaultQuery(LoanRequestState.class).getStates();
//            quotesList.stream().forEach(quote -> statesList.stream().filter(req ->
//                    req.getState().getData().getLinearId()
//                            .equals(quote.getState().getData().getLoanRequestDetails()
//                                    .resolve(activeParty))));
            //List<GetQuoteDataFlow.Quotesdata> result = activeParty.startFlowDynamic(GetQuoteDataFlow.Initiator.class,statesList).getReturnValue().get();


            return APIResponse.success(new Quotesdata(statesList,quotesList));
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    public class Quotesdata{
        public List<StateAndRef<LoanRequestState>> requests;
        public List<StateAndRef<LoanQuoteState>> quotes;

        public Quotesdata(List<StateAndRef<LoanRequestState>> requests, List<StateAndRef<LoanQuoteState>> quotes) {
            this.requests = requests;
            this.quotes = quotes;
        }
    }


}