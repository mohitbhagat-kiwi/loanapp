package com.loanapp.webserver;

import com.loanapp.states.LoanRequestState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
//    @Autowired
//    private CordaRPCOps notaryProxy;
//    @Autowired
//    private CordaRPCOps brokerProxy;
//    @Autowired
//    private CordaRPCOps bankAProxy;
//    @Autowired
//    private CordaRPCOps bankBProxy;
//    @Autowired
//    private CordaRPCOps creditBureauProxy;
//    @Autowired
//    private CordaRPCOps evaluationBureauProxy;
//    @Autowired
//    @Qualifier("notaryProxy")
//    private CordaRPCOps activeParty;
    private final CordaRPCOps proxy;
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

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    @GetMapping("loanRequests")
    public APIResponse<List<StateAndRef<LoanRequestState>>> getLoanRequestsList() {
        try{
            List<StateAndRef<LoanRequestState>> auctionList = proxy.vaultQuery(LoanRequestState.class).getStates();
            return APIResponse.success(auctionList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("loanRequest123")
    public APIResponse<List<StateAndRef<LoanRequestState>>> getVaultStateList(String stateName) {
        try{
            List<StateAndRef<LoanRequestState>> auctionList = proxy.vaultQuery(LoanRequestState.class).getStates();
            return APIResponse.success(auctionList);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping(value = "switch-party/{party}")
    public APIResponse<Void> switchParty(@PathVariable String party){
//        switch (party){
//            case "notary" :
//                activeParty = notaryProxy;
//                break;
//            case "broker" :
//                activeParty = brokerProxy;
//                break;
//            case "bankA" :
//                activeParty = bankAProxy;
//                break;
//            case "bankB" :
//                activeParty = bankBProxy;
//                break;
//            case "creditBureau" :
//                activeParty = creditBureauProxy;
//                break;
//            case "evaluationBureau" :
//                activeParty = evaluationBureauProxy;
//                break;
//            default:
//                return APIResponse.error("Unrecognised Party");
//        }

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
}