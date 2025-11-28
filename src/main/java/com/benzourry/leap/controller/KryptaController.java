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

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @GetMapping("contract/{contractId}/compile")
    public KryptaContract compile(@PathVariable("contractId") Long contractId) throws Exception {
        return service.compileSolidity(contractId);
    }

    @GetMapping("wallet/{walletId}/deploy")
    public KryptaWallet deploy(@PathVariable("walletId") Long walletId) throws Exception {
        return service.deployContract(walletId);
    }

    @PostMapping("tx/{walletId}/call/{functionName}")
    public ResponseEntity<?> callContract(
            @PathVariable Long walletId,
            @PathVariable String functionName,
            @RequestBody(required = false) Map<String, Object> args) {

        try {
            if (args == null) args = Map.of();

            Object result = service.call(walletId, functionName, args);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");

            if (result instanceof TransactionReceipt receipt) {
                response.put("type", "transaction");
                response.put("txHash", receipt.getTransactionHash());
                response.put("gasUsed",
                        receipt.getGasUsed() != null ? receipt.getGasUsed().toString() : null);
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

}
