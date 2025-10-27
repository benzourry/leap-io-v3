package com.benzourry.leap.controller;

//import com.benzourry.leap.model.JalinContractInfo;
//import com.benzourry.leap.model.JalinNetworkConfig;
import com.benzourry.leap.model.KryptaContract;
import com.benzourry.leap.model.KryptaWallet;
import com.benzourry.leap.service.KryptaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/krypta")
public class KryptaController {


    private final KryptaService service;

    public KryptaController(KryptaService service) {
        this.service = service;
    }

    @GetMapping("wallet/{id}")
    public KryptaWallet getWalletInfo(@PathVariable Long id) {
        return service.getWallet(id);
    }

    @GetMapping("wallet")
    public Page<KryptaWallet> getWalletInfos(@RequestParam Long appId, Pageable pageable) {
        return service.getWalletList(appId, pageable);
    }

    @PostMapping("wallet")
    public KryptaWallet saveWalletInfo(@RequestBody KryptaWallet walletInfo,
                                       @RequestParam("appId") Long appId,
                                       @RequestParam("email") String email) {
        return service.saveWallet(appId,walletInfo, email);
    }

    @PostMapping("wallet/{id}/delete")
    public ResponseEntity<?> deleteWalletInfo(@PathVariable Long id) {
        service.deleteWallet(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("contract/{id}")
    public KryptaContract getContractInfo(@PathVariable Long id) {
        return service.getContract(id);
    }

    @GetMapping("contract")
    public Page<KryptaContract> getContractInfos(@RequestParam Long appId, Pageable pageable) {
        return service.getContractList(appId, pageable);
    }

    @PostMapping("contract")
    public KryptaContract saveContractInfo(@RequestBody KryptaContract contractInfo,
                                       @RequestParam("appId") Long appId,
                                       @RequestParam("email") String email) {
        return service.saveContract(appId,contractInfo, email);
    }

    @PostMapping("contract/{id}/delete")
    public ResponseEntity<?> deleteContractInfo(@PathVariable Long id) {
        service.deleteContract(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("wallet/{walletId}/init-contract")
    public KryptaWallet initContract(
            @PathVariable Long walletId
    ) throws Exception {
        return service.initDefaultContract(walletId);
    }

//    @GetMapping("/deploy")
//    public String deploy() throws Exception {
//        String rpcUrl = "http://127.0.0.1:8545";
////        String walletPath = "C:/Users/blmrazif/AppData/Local/Ethereum/keystore/UTC--2025-10-20T07-14-52.327656200Z--0201e53fa6a11171a2ee25d0b8d1444481d22e2b";
//        String walletPath = "C:/Users/blmrazif/AppData/Local/Ethereum/keystore/UTC--2025-10-20T07-51-18.093647400Z--064338d703ef1948c54dfbe0c83011e4722992d2";
//        String walletPassword = "P@ssw0rd";
//        String binPath = "src/main/resources/contracts/CertificateRegistry.bin";
//        String abiPath = "src/main/resources/contracts/CertificateRegistry.abi";
//
//        return service.deployContract(rpcUrl, walletPath, walletPassword, binPath, abiPath);
//    }

    @GetMapping("contract/{contractId}/compile")
    public KryptaContract compile(@PathVariable("contractId") Long contractId) throws Exception {
        return service.compileSolidity(contractId);
    }

    @GetMapping("wallet/{walletId}/deploy")
    public KryptaWallet deploy(@PathVariable("walletId") Long walletId) throws Exception {
        return service.deployContract(walletId);
    }


    @GetMapping("tx/{walletId}/get")
    public Map<String, Object> getValue(
            @PathVariable Long walletId,
            @RequestParam("dataId") BigInteger dataId
    ) throws Exception {

        String data = service.getValue(walletId, dataId);
        Map<String, Object> rval = new HashMap<>();
        rval.put("data", data);
        return rval;
    }

    public record ContractCallRequest(
            List<Object> args,
            String abiPath
    ) {}
    @PostMapping("tx/{walletId}/call/{functionName}")
    public ResponseEntity<?> callContract(
            @PathVariable Long walletId,
            @PathVariable String functionName,
            @RequestBody(required = false) List<Object> args) {
        try {
            if (args == null) args = List.of();

            Object result = service.call(walletId, functionName, args);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");

            if (result instanceof TransactionReceipt receipt) {
                response.put("type", "transaction");
                response.put("txHash", receipt.getTransactionHash());
                response.put("gasUsed", receipt.getGasUsed() != null ? receipt.getGasUsed().toString() : null);
            } else {
                response.put("type", "read");
                response.put("result", result);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }


    @GetMapping("tx/{walletId}/log/{eventName}")
    public Map<String,Object> logsContract(
            @PathVariable Long walletId,
            @PathVariable String eventName
    ) throws Exception {
        Map<String, Object> rVal = new HashMap<>();
        rVal.put("type", "logs");
        rVal.put("eventName", eventName);
        rVal.put("content", service.logs(walletId, eventName));
        return rVal;
    }

    @GetMapping("tx/{walletId}/verify-hash")
    public Map<String, Object> verifyHash(
            @PathVariable Long walletId,
            @RequestParam("txHash") String txHash
    ) throws Exception {
        return service.verify(walletId, txHash);
    }

    record DataRegistry(BigInteger dataId,String data) {}

//    @PostMapping("tx/{walletId}/add")
//    public ResponseEntity<TransactionReceipt> addValue(
//            @PathVariable Long walletId,
//            @RequestBody DataRegistry dataRegistry
//    ) throws Exception {
//
//        return ResponseEntity.ok(service.addValue(walletId, dataRegistry.dataId, dataRegistry.data));
//    }

    @PostMapping("tx/{walletId}/revoke")
    public ResponseEntity<TransactionReceipt> addValue(
            @PathVariable Long walletId,
            @RequestParam("dataId") BigInteger dataId
    ) throws Exception {

        return ResponseEntity.ok(service.revokeValue(walletId, dataId));
    }

}
