package com.benzourry.leap.controller;

//import com.benzourry.leap.model.JalinContractInfo;
//import com.benzourry.leap.model.JalinNetworkConfig;
import com.benzourry.leap.model.KryptaWalletInfo;
import com.benzourry.leap.service.KryptaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/krypta")
public class KryptaController {


    private final KryptaService service;

    public KryptaController(KryptaService service) {
        this.service = service;
    }

    @GetMapping("{id}")
    public KryptaWalletInfo getWalletInfo(@PathVariable Long id) {
        return service.getWalletInfo(id);
    }

    @GetMapping
    public Page<KryptaWalletInfo> getWalletInfos(@RequestParam Long appId, Pageable pageable) {
        return service.getWalletInfoList(appId, pageable);
    }

    @PostMapping
    public KryptaWalletInfo saveWalletInfo(@RequestBody KryptaWalletInfo walletInfo,
                                           @RequestParam("appId") Long appId,
                                           @RequestParam("email") String email) {
        return service.saveWalletInfo(appId,walletInfo, email);
    }

    @PostMapping("{id}/delete")
    public ResponseEntity<?> deleteWalletInfo(@PathVariable Long id) {
        service.deleteWalletInfo(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("{walletId}/get")
    public Map<String, Object> getValue(
            @PathVariable Long walletId,
            @RequestParam("certId") BigInteger certId
    ) throws Exception {

        String data = service.getValue(walletId, certId);

        Map<String, Object> rval = new HashMap<>();

        rval.put("data", data);

        return rval;
    }

    @GetMapping("{walletId}/verify-hash")
    public Map<String, Object> getValue(
            @PathVariable Long walletId,
            @RequestParam("txHash") String txHash
    ) throws Exception {
        return service.verifyTransaction(walletId, txHash);
    }

    @GetMapping("{walletId}/init-contract")
    public Map<String, Object> initContract(
            @PathVariable Long walletId
    ) throws Exception {
//        String walletPath = "C:/Users/blmrazif/AppData/Local/Ethereum/keystore/UTC--2025-10-20T07-51-18.093647400Z--064338d703ef1948c54dfbe0c83011e4722992d2";
//        String walletPassword = "P@ssw0rd";
//        String binPath = "src/main/resources/contracts/CertificateRegistry.bin";
//        String abiPath = "src/main/resources/contracts/CertificateRegistry.abi";

        String contractAddress = service.initContract(walletId);

        Map<String, Object> rval = new HashMap<>();
        rval.put("contractAddress", contractAddress);


        return rval;
    }

    record DataRegistry(BigInteger dataId,String data) {}
    @PostMapping("{walletId}/add")
    public ResponseEntity<TransactionReceipt> getValue(
            @PathVariable Long walletId,
            @RequestBody DataRegistry dataRegistry
    ) throws Exception {

        return ResponseEntity.ok(service.addValue(walletId, dataRegistry.dataId, dataRegistry.data));
    }


    @GetMapping("/deploy")
    public String deploy() throws Exception {
        String rpcUrl = "http://127.0.0.1:8545";
//        String walletPath = "C:/Users/blmrazif/AppData/Local/Ethereum/keystore/UTC--2025-10-20T07-14-52.327656200Z--0201e53fa6a11171a2ee25d0b8d1444481d22e2b";
        String walletPath = "C:/Users/blmrazif/AppData/Local/Ethereum/keystore/UTC--2025-10-20T07-51-18.093647400Z--064338d703ef1948c54dfbe0c83011e4722992d2";
        String walletPassword = "P@ssw0rd";
        String binPath = "src/main/resources/contracts/CertificateRegistry.bin";
        String abiPath = "src/main/resources/contracts/CertificateRegistry.abi";

        return service.deployContract(rpcUrl, walletPath, walletPassword, binPath, abiPath);
    }

    @GetMapping("/compile-sol")
    public void compile() throws Exception {


        service.compileSolidity();
    }

}
