package com.benzourry.leap.service;

//import com.benzourry.leap.contracts.CertificateRegistry;
//import com.benzourry.leap.model.JalinContractInfo;
//import com.benzourry.leap.model.JalinNetworkConfig;
import com.benzourry.leap.contracts.DataRegistry;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.KryptaWalletInfo;
//import com.benzourry.leap.repository.JalinContractInfoRepository;
//import com.benzourry.leap.repository.JalinNetworkConfigRepository;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.KryptaWalletInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class KryptaService {

//    private final JalinNetworkConfigRepository networkRepo;
    private final KryptaWalletInfoRepository walletRepo;
//    private final JalinContractInfoRepository contractRepo;

    private final AppRepository appRepository;


    public KryptaService(
//            JalinNetworkConfigRepository networkRepo,
                        KryptaWalletInfoRepository walletRepo,
                        AppRepository appRepository
//                        JalinContractInfoRepository contractRepo
    ) {
//        this.networkRepo = networkRepo;
        this.walletRepo = walletRepo;
//        this.contractRepo = contractRepo;
        this.appRepository = appRepository;
    }


    /**
     * Deploy contract to a specified network and return the deployed contract address.
     */
    public String deployContract(String rpcUrl, String walletPath, String walletPassword,
                                 String binPath, String abiPath) throws Exception {

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();
        HttpService httpService = new HttpService(rpcUrl, httpClient, false);

        // 1️⃣ Connect to Ethereum node
        Web3j web3j = Web3j.build(httpService);
        System.out.println("Connected to Ethereum network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        // 2️⃣ Load wallet credentials
        String privateKey = "0xb71c71a67e1177ad4e901695e1b4b9ee17ae16c6668d313eac2f96dbcda3f291";
        Credentials credentials = Credentials.create(privateKey);

//        Credentials credentials = WalletUtils.loadCredentials(walletPassword, new File(walletPath));
//        System.out.println("Using wallet: " + credentials.getAddress());

        // 3️⃣ Read ABI and BIN files
        String binary = Files.readString(Paths.get(binPath));
        String abi = Files.readString(Paths.get(abiPath));

        // 4️⃣ Prepare transaction manager and gas provider
        long chainId = 1337L; // <-- replace with your network chain ID
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);
//        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials);
        ContractGasProvider gasProvider = new DefaultGasProvider();

        // 5️⃣ Deploy the contract
        System.out.println("Deploying contract...");
        String contractAddress = DataRegistry.deploy(
                web3j,
                txManager,    // or credentials
                gasProvider
        ).send().getContractAddress();

        System.out.println("✅ Contract deployed at: " + contractAddress);

        web3j.shutdown();
        return contractAddress;
    }

    /**
     * Deploy contract to a specified network and return the deployed contract address.
     */
    public String initContract(Long walletId) throws Exception {

        KryptaWalletInfo walletInfo = walletRepo.findById(walletId).orElseThrow();

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();

        HttpService httpService = new HttpService(walletInfo.getRpcUrl(), httpClient, false);

        // 1️⃣ Connect to Ethereum node
        Web3j web3j = Web3j.build(httpService);
        System.out.println("Connected to Ethereum network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        // 2️⃣ Load wallet credentials
        Credentials credentials = Credentials.create(walletInfo.getPrivateKey());

        // 3️⃣ Read ABI and BIN files (NOT NEEDED, ALREADY IN WRAPPER CertificateRegistry)
//        String binary = Files.readString(Paths.get(binPath));
//        String abi = Files.readString(Paths.get(abiPath));

        // 4️⃣ Prepare transaction manager and gas provider
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, walletInfo.getChainId());
//        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials);
        ContractGasProvider gasProvider = new DefaultGasProvider();

        // 5️⃣ Deploy the contract
        System.out.println("Deploying contract...");
        String contractAddress = DataRegistry.deploy(
                web3j,
                txManager,    // or credentials
                gasProvider
        ).send().getContractAddress();

        System.out.println("✅ Contract deployed at: " + contractAddress);

        web3j.shutdown();

        walletInfo.setContractAddress(contractAddress);
        walletRepo.save(walletInfo);

        return contractAddress;
    }


    public Web3j createWeb3(KryptaWalletInfo wallet) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();
        HttpService httpService = new HttpService(wallet.getRpcUrl(), httpClient, false);
        return Web3j.build(httpService);
    }

    public Credentials createCredentials(KryptaWalletInfo wallet, String password) throws Exception {
        // Example: decrypt wallet JSON file
        // OR decrypt private key stored in DB
        String decryptedPrivateKey = decrypt(wallet.getPrivateKey(), password);
        return Credentials.create(decryptedPrivateKey);
    }

    private String decrypt(String encrypted, String password) {
        // Replace this with proper AES encryption or use Vault/KMS
        return encrypted; // simplified example
    }



    public String getValue(Long walletId, BigInteger certId) throws Exception {
        // 1. Load from DB
//        JalinContractInfo contractInfo = contractRepo.findById(contractId).orElseThrow();
//        JalinNetworkConfig network = networkRepo.findById(contractInfo.getNetworkId()).orElseThrow();
        KryptaWalletInfo wallet = walletRepo.findById(walletId).orElseThrow();

        // 2. Build Web3j and Credentials
        Web3j web3j = this.createWeb3(wallet);
//        Credentials creds = this.createCredentials(wallet, wallet.getPassword());
        Credentials creds = Credentials.create(wallet.getPrivateKey());

//        long chainId = 1337L; // <-- replace with your network chain ID
        TransactionManager txManager = new RawTransactionManager(web3j, creds, wallet.getChainId());
//        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials);
        ContractGasProvider gasProvider = new DefaultGasProvider();


        // 3. Load contract wrapper dynamically
        var contract = DataRegistry.load(
                wallet.getContractAddress(),
                web3j,
                txManager,
                gasProvider
        );

        // 4. Call contract
        return contract.getData(certId).send();
    }

    public TransactionReceipt addValue(Long walletId, BigInteger certId, String data) throws Exception {

        KryptaWalletInfo wallet = walletRepo.findById(walletId).orElseThrow();

        // 2. Build Web3j and Credentials
        Web3j web3j = this.createWeb3(wallet);
        Credentials creds = Credentials.create(wallet.getPrivateKey());

        TransactionManager txManager = new RawTransactionManager(web3j, creds, wallet.getChainId());
        ContractGasProvider gasProvider = new DefaultGasProvider();

        var contract = DataRegistry.load(
                wallet.getContractAddress(),
                web3j,
                txManager,
                gasProvider
        );

        // 4. Call contract
        return contract.addData(certId, data).send();
    }

    public TransactionReceipt revokeValue(Long walletId, BigInteger certId) throws Exception {
        // 1. Load from DB
//        JalinContractInfo contractInfo = contractRepo.findById(contractId).orElseThrow();
//        JalinNetworkConfig network = networkRepo.findById(contractInfo.getNetworkId()).orElseThrow();
        KryptaWalletInfo wallet = walletRepo.findById(walletId).orElseThrow();

        // 2. Build Web3j and Credentials
        Web3j web3j = this.createWeb3(wallet);
//        Credentials creds = this.createCredentials(wallet, wallet.getPassword());
        Credentials creds = Credentials.create(wallet.getPrivateKey());
        // 3. Load contract wrapper dynamically
        var contract = DataRegistry.load(
                wallet.getContractAddress(),
                web3j,
                creds,
                new DefaultGasProvider()
        );

        // 4. Call contract
        return contract.revokeData(certId).send();
    }

    public Map<String, Object> verifyTransactionOld(Long walletId, String txHash) throws Exception {
        // 1️⃣ Load wallet info from DB (RPC URL, etc.)
        KryptaWalletInfo wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        Web3j web3j = this.createWeb3(wallet);

        System.out.println("Connected to network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        Map<String, Object> result = new LinkedHashMap<>();

        // 3️⃣ Check if transaction exists
        EthTransaction txResponse = web3j.ethGetTransactionByHash(txHash).send();
        if (txResponse.getTransaction().isEmpty()) {
            result.put("status", "NOT_FOUND");
            result.put("message", "Transaction not found in network");
            return result;
        }

        Transaction tx = txResponse.getTransaction().get();
        result.put("from", tx.getFrom());
        result.put("to", tx.getTo());
        result.put("value", tx.getValue());
        result.put("nonce", tx.getNonce());

        // 4️⃣ Wait until transaction is mined
        Optional<TransactionReceipt> receiptOpt;
        int attempts = 0;
        do {
            Thread.sleep(2000);
            receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            attempts++;
        } while (receiptOpt.isEmpty() && attempts < 30); // waits up to ~60s

        if (receiptOpt.isEmpty()) {
            result.put("status", "PENDING");
            result.put("message", "Transaction still pending");
            return result;
        }

        // 5️⃣ Parse receipt
        TransactionReceipt receipt = receiptOpt.get();
        result.put("blockNumber", receipt.getBlockNumber());
        result.put("gasUsed", receipt.getGasUsed());
        result.put("contractAddress", receipt.getContractAddress());
        result.put("txStatus", receipt.getStatus().equals("0x1") ? "SUCCESS" : "FAILED");

        // 6️⃣ Optionally parse logs
        result.put("logsCount", receipt.getLogs().size());

        web3j.shutdown();
        return result;
    }

    public Map<String, Object> verifyTransaction(Long walletId, String txHash) throws Exception {
        // 1️⃣ Load wallet info
        KryptaWalletInfo wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        Web3j web3j = this.createWeb3(wallet);
        System.out.println("Connected to network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        Map<String, Object> result = new LinkedHashMap<>();

        // 2️⃣ Check if transaction exists
        EthTransaction txResponse = web3j.ethGetTransactionByHash(txHash).send();
        if (txResponse.getTransaction().isEmpty()) {
            result.put("status", "NOT_FOUND");
            result.put("message", "Transaction not found in network");
            return result;
        }

        Transaction tx = txResponse.getTransaction().get();
        result.put("from", tx.getFrom());
        result.put("to", tx.getTo());
        result.put("value", tx.getValue());
        result.put("nonce", tx.getNonce());

        // 3️⃣ Wait for the transaction to be mined
        Optional<TransactionReceipt> receiptOpt;
        int attempts = 0;
        do {
            Thread.sleep(2000);
            receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            attempts++;
        } while (receiptOpt.isEmpty() && attempts < 30); // waits ~60s

        if (receiptOpt.isEmpty()) {
            result.put("status", "PENDING");
            result.put("message", "Transaction still pending");
            return result;
        }

        // 4️⃣ Parse receipt
        TransactionReceipt receipt = receiptOpt.get();
        result.put("blockNumber", receipt.getBlockNumber());
        result.put("gasUsed", receipt.getGasUsed());
        result.put("contractAddress", receipt.getContractAddress());
        result.put("txStatus", receipt.getStatus().equals("0x1") ? "SUCCESS" : "FAILED");
        result.put("logsCount", receipt.getLogs().size());

        // 5️⃣ Decode event data (example: CertificateCreated(uint256 certId, string data))
        // 5️⃣ Define your events
        Event eventAdded = new Event("DataAdded",
                Arrays.asList(
                        new TypeReference<Uint256>(true) {},  // indexed certId
                        new TypeReference<Utf8String>() {}     // data
                ));

        Event eventRevoked = new Event("DataRevoked",
                Arrays.asList(
                        new TypeReference<Uint256>(true) {}   // indexed certId
                ));

        String topicAdded = EventEncoder.encode(eventAdded);
        String topicRevoked = EventEncoder.encode(eventRevoked);

        List<Map<String, Object>> decodedEvents = new ArrayList<>();

        for (Log log : receipt.getLogs()) {
            if (log.getTopics().isEmpty()) continue;
            String topic = log.getTopics().get(0);

            if (topic.equals(topicAdded)) {
                // Decode CertificateAdded
                List<Type> nonIndexed = FunctionReturnDecoder.decode(
                        log.getData(), eventAdded.getNonIndexedParameters());
                BigInteger certId = new BigInteger(log.getTopics().get(1).substring(2), 16);
                String data = nonIndexed.isEmpty() ? "" : nonIndexed.get(0).getValue().toString();

                Map<String, Object> e = new LinkedHashMap<>();
                e.put("event", "DataAdded");
                e.put("certId", certId);
                e.put("data", data);
                decodedEvents.add(e);

            } else if (topic.equals(topicRevoked)) {
                // Decode CertificateRevoked
                BigInteger certId = new BigInteger(log.getTopics().get(1).substring(2), 16);
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("event", "DataRevoked");
                e.put("certId", certId);
                decodedEvents.add(e);
            }
        }

        result.put("events", decodedEvents);
        web3j.shutdown();
        return result;
    }

    public KryptaWalletInfo getWalletInfo(Long id) {
        return walletRepo.findById(id).orElseThrow();
    }

    public Page<KryptaWalletInfo> getWalletInfoList(Long appId, Pageable pageable) {
        return walletRepo.findByAppId(appId, pageable);
    }

    public KryptaWalletInfo saveWalletInfo(Long appId, KryptaWalletInfo walletInfo, String email) {
        App app = appRepository.findById(appId).orElseThrow();
        walletInfo.setApp(app);
        walletInfo.setEmail(email);
        return walletRepo.save(walletInfo);
    }

    public void deleteWalletInfo(Long id) {
        walletRepo.deleteById(id);
    }


    @Value("${instance.krypta.solc-path:/usr/local/bin/solcjs}")
    String SOLC_PATH;

    public void compileSolidity() throws Exception {
        // Your Solidity source code as string
        String solidityCode = """
        pragma solidity ^0.8.0;
        contract Hello {
            string public message = "Hello, World!";
            function setMessage(string calldata newMsg) external {
                message = newMsg;
            }
        }
    """;

        // 1️⃣ Save Solidity code to a temp file
        Path tempFile = Files.createTempFile("Hello", ".sol");
        Files.writeString(tempFile, solidityCode);
        Path outputDir = Files.createTempDirectory("solcjs-output");

        // 2️⃣ Run solc to compile the code
        ProcessBuilder pb = new ProcessBuilder(
                SOLC_PATH,
                "--bin",
                "--abi",
                "--output-dir", outputDir.toString(),
                tempFile.toAbsolutePath().toString()
        );

//        Map<String, String> env = pb.environment();
//        String npmPath = "C:\\Users\\blmrazif\\AppData\\Roaming\\npm";
//        env.put("PATH", env.get("PATH") + ";" + npmPath);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 3️⃣ Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Solc compilation failed:\n" + output);
        }


        // Read the generated files (Hello_sol_Hello.abi and Hello_sol_Hello.bin)
        try (Stream<Path> files = Files.list(outputDir)) {
            files.forEach(path -> {
                try {
                    System.out.println("File: " + path);
                    System.out.println(Files.readString(path));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }


        // 4️⃣ Parse JSON using Jackson
        String result = output.toString();
        System.out.println("Full JSON Output:\n" + result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(result);

        JsonNode contracts = root.path("contracts");
        if (contracts.isMissingNode() || !contracts.fieldNames().hasNext()) {
            throw new RuntimeException("No contracts found in compilation output.");
        }

        // 5️⃣ Extract ABI and BIN
        contracts.fields().forEachRemaining(entry -> {
            String contractName = entry.getKey();
            JsonNode contractNode = entry.getValue();

            String abi = contractNode.path("abi").asText();
            String bin = contractNode.path("bin").asText();

            System.out.println("Contract: " + contractName);
            System.out.println("ABI: " + abi);
            System.out.println("BIN: " + bin);
        });
    }
}
