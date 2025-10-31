package com.benzourry.leap.service;

//import com.benzourry.leap.contracts.CertificateRegistry;
//import com.benzourry.leap.model.JalinContractInfo;
//import com.benzourry.leap.model.JalinNetworkConfig;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.contracts.DataRegistry;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.KryptaContract;
import com.benzourry.leap.model.KryptaWallet;
//import com.benzourry.leap.repository.JalinContractInfoRepository;
//import com.benzourry.leap.repository.JalinNetworkConfigRepository;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.KryptaContractRepository;
import com.benzourry.leap.repository.KryptaWalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class KryptaService {

//    private final JalinNetworkConfigRepository networkRepo;
    private final KryptaWalletRepository walletRepo;
    private final KryptaContractRepository contractRepo;

    private final AppRepository appRepository;


    public KryptaService(
//            JalinNetworkConfigRepository networkRepo,
                        KryptaWalletRepository walletRepo,
                        AppRepository appRepository,
                        KryptaContractRepository contractRepo
    ) {
        this.walletRepo = walletRepo;
        this.appRepository = appRepository;
        this.contractRepo = contractRepo;
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
    public KryptaWallet initDefaultContract(Long walletId) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId).orElseThrow();

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();

        HttpService httpService = new HttpService(wallet.getRpcUrl(), httpClient, false);

        // 1️⃣ Connect to Ethereum node
        Web3j web3j = Web3j.build(httpService);
        System.out.println("Connected to Ethereum network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        // 2️⃣ Load wallet credentials
        Credentials credentials = Credentials.create(wallet.getPrivateKey());

        // 3️⃣ Read ABI and BIN files (NOT NEEDED, ALREADY IN WRAPPER CertificateRegistry)
//        String binary = Files.readString(Paths.get(binPath));
//        String abi = Files.readString(Paths.get(abiPath));

        // 4️⃣ Prepare transaction manager and gas provider
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, wallet.getChainId());
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

        wallet.setContractAddress(contractAddress);
        walletRepo.save(wallet);

        return wallet;
    }


    public Web3j createWeb3(KryptaWallet wallet) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();
        HttpService httpService = new HttpService(wallet.getRpcUrl(), httpClient, false);
        return Web3j.build(httpService);
    }

    public Credentials createCredentials(KryptaWallet wallet, String password) throws Exception {
        // Example: decrypt wallet JSON file
        // OR decrypt private key stored in DB
        String decryptedPrivateKey = decrypt(wallet.getPrivateKey(), password);
        return Credentials.create(decryptedPrivateKey);
    }

    private String decrypt(String encrypted, String password) {
        // Replace this with proper AES encryption or use Vault/KMS
        return encrypted; // simplified example
    }

    /****
    public TransactionReceipt callContractFunctionDynamic(
            Long walletId,
            String functionName,
            List<Object> args
    ) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        String abiText = resolveAbi(wallet.getContract());

        if (wallet.getContract() == null) {
            throw new RuntimeException("Wallet has no associated contract: " + walletId);
        }

        KryptaContract contract = wallet.getContract();

        // 1️⃣ Setup Web3j and credentials
        Web3j web3j = this.createWeb3(wallet);
        Credentials credentials = Credentials.create(wallet.getPrivateKey());

        TransactionManager txManager = new FastRawTransactionManager(web3j, credentials, wallet.getChainId());
        ContractGasProvider gasProvider = new DefaultGasProvider();

        // 2️⃣ Parse ABI using Jackson
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode abiArray = (ArrayNode) mapper.readTree(abiText);

        ObjectNode fnDef = null;
        for (JsonNode node : abiArray) {
            if (node.has("type") && "function".equals(node.get("type").asText())
                    && functionName.equals(node.path("name").asText())) {
                fnDef = (ObjectNode) node;
                break;
            }
        }

        if (fnDef == null)
            throw new RuntimeException("Function not found in ABI: " + functionName);

        // 3️⃣ Convert inputs based on ABI types
        ArrayNode inputs = (ArrayNode) fnDef.get("inputs");
        if (inputs.size() != args.size())
            throw new RuntimeException("Argument count mismatch: expected " + inputs.size() + " got " + args.size());

        List<Type> inputParams = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String solidityType = inputs.get(i).get("type").asText();
            Object value = args.get(i);
            inputParams.add(convertToWeb3Type(solidityType, value));
        }

        // 4️⃣ Parse outputs (optional)
        List<TypeReference<?>> outputParams = new ArrayList<>();
        if (fnDef.has("outputs")) {
            ArrayNode outputs = (ArrayNode) fnDef.get("outputs");
            for (JsonNode out : outputs) {
                String outType = out.get("type").asText();
                outputParams.add(TypeReference.makeTypeReference(outType));
            }
        }

        // 5️⃣ Encode and send transaction
        Function function = new Function(functionName, inputParams, outputParams);
        String encodedFunction = FunctionEncoder.encode(function);

        EthSendTransaction txResponse = txManager.sendTransaction(
                gasProvider.getGasPrice(functionName),
                gasProvider.getGasLimit(functionName),
                wallet.getContractAddress(),
                encodedFunction,
                BigInteger.ZERO
        );

        String txHash = txResponse.getTransactionHash();
        if (txHash == null) {
            throw new RuntimeException("Transaction failed: " + txResponse.getError().getMessage());
        }

        System.out.println("📨 Sent tx [" + functionName + "] → " + txHash);

        // 6️⃣ Wait for transaction receipt
        TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 2000, 30);
        TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

        System.out.println("✅ Tx complete. Gas used: " + receipt.getGasUsed());
        web3j.shutdown();
        return receipt;
    }

     **/



    public Object call(Long walletId, String functionName, List<Object> args) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        KryptaContract contract = wallet.getContract();
        if (contract == null)
            throw new RuntimeException("Wallet has no associated contract: " + walletId);

        String abiText = resolveAbi(contract);

        // 1️⃣ Setup Web3j and credentials
        Web3j web3j = this.createWeb3(wallet);
        Credentials credentials = Credentials.create(wallet.getPrivateKey());
        TransactionManager txManager = new FastRawTransactionManager(web3j, credentials, wallet.getChainId());
        ContractGasProvider gasProvider = new DefaultGasProvider();

        // 2️⃣ Parse ABI
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode abiArray = (ArrayNode) mapper.readTree(abiText);

        ObjectNode fnDef = null;
        for (JsonNode node : abiArray) {
            if ("function".equals(node.path("type").asText()) &&
                    functionName.equals(node.path("name").asText())) {
                fnDef = (ObjectNode) node;
                break;
            }
        }

        if (fnDef == null)
            throw new RuntimeException("Function not found in ABI: " + functionName);

        // 3️⃣ Build input parameters
        ArrayNode inputs = (ArrayNode) fnDef.path("inputs");
        if (inputs.size() != args.size())
            throw new RuntimeException("Argument count mismatch: expected " + inputs.size() + " got " + args.size());

        List<Type> inputParams = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String solidityType = inputs.get(i).get("type").asText();
            Object value = args.get(i);
            inputParams.add(convertToWeb3Type(solidityType, value));
        }

        // 4️⃣ Build output types
        List<TypeReference<?>> outputParams = new ArrayList<>();
        if (fnDef.has("outputs")) {
            ArrayNode outputs = (ArrayNode) fnDef.get("outputs");
            for (JsonNode out : outputs) {
                String outType = out.get("type").asText();
                outputParams.add(TypeReference.makeTypeReference(outType));
            }
        }

        // 5️⃣ Build function
        Function function = new Function(functionName, inputParams, outputParams);
        String encodedFunction = FunctionEncoder.encode(function);

        // 6️⃣ Detect if function is read-only
        String stateMutability = fnDef.path("stateMutability").asText("");
        boolean isView = "view".equals(stateMutability) || "pure".equals(stateMutability);

        if (isView) {

            org.web3j.protocol.core.methods.request.Transaction ethCallTx = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    wallet.getContractAddress(),      // from
                    wallet.getContractAddress(),    // to (contract)
                    encodedFunction                 // data
            );
            // ✅ READ CALL (eth_call)
            EthCall response = web3j.ethCall(
                    ethCallTx,
                    DefaultBlockParameterName.LATEST
            ).send();

            String value = response.getValue();
            List<Type> decoded = FunctionReturnDecoder.decode(value, function.getOutputParameters());

            web3j.shutdown();
            if (decoded.isEmpty()) return null;
            if (decoded.size() == 1) return decoded.get(0).getValue();

            return decoded.stream().map(Type::getValue).toList();

        } else {
            // 🧾 WRITE CALL (sendTransaction)
            EthSendTransaction txResponse = txManager.sendTransaction(
                    gasProvider.getGasPrice(functionName),
                    gasProvider.getGasLimit(functionName),
                    wallet.getContractAddress(),
                    encodedFunction,
                    BigInteger.ZERO
            );

            if (txResponse.hasError())
                throw new RuntimeException("Transaction failed: " + txResponse.getError().getMessage());

            String txHash = txResponse.getTransactionHash();
            System.out.println("📨 Sent tx [" + functionName + "] → " + txHash);

            TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 2000, 30);
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

            web3j.shutdown();
            System.out.println("✅ Tx complete. Gas used: " + receipt.getGasUsed());
            return receipt;
        }
    }

    private String resolveAbi(KryptaContract contract) throws IOException {
        if (contract!=null && contract.getAbi() != null && !contract.getAbi().isBlank()) {
//            return contract.getAbi();
            return Files.readString(Paths.get(contract.getAbi()));
        }

        // fallback to default ABI in resources
        try (InputStream is = getClass().getResourceAsStream("/contracts/DataRegistry.abi")) {
            if (is == null) {
                throw new FileNotFoundException("Default ABI not found in resources/contracts/DataRegistry.abi");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Type convertToWeb3Type(String solidityType, Object value) {
        switch (solidityType) {
            case "uint256":
            case "uint":
                return new Uint256(new BigInteger(value.toString()));
            case "int256":
            case "int":
                return new Int256(new BigInteger(value.toString()));
            case "address":
                return new Address(value.toString());
            case "bool":
                return new Bool(Boolean.parseBoolean(value.toString()));
            case "string":
                return new Utf8String(value.toString());
            case "bytes32":
                return new Bytes32(Arrays.copyOf(value.toString().getBytes(), 32));
            default:
                if (solidityType.endsWith("[]")) {
                    String elementType = solidityType.replace("[]", "");
                    List<?> list = (List<?>) value;
                    List<Type> converted = new ArrayList<>();
                    for (Object o : list) {
                        converted.add(convertToWeb3Type(elementType, o));
                    }
                    return new DynamicArray<>(converted);
                }
                throw new RuntimeException("Unsupported Solidity type: " + solidityType);
        }
    }

    public String getValue(Long walletId, BigInteger dataId) throws Exception {
        // 1. Load from DB
//        JalinContractInfo contractInfo = contractRepo.findById(contractId).orElseThrow();
//        JalinNetworkConfig network = networkRepo.findById(contractInfo.getNetworkId()).orElseThrow();
        KryptaWallet wallet = walletRepo.findById(walletId).orElseThrow();

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
        return contract.getData(dataId).send();
    }

    public TransactionReceipt addValue(Long walletId, BigInteger dataId, String data) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId).orElseThrow();

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
        return contract.addData(dataId, data).send();
    }

    public TransactionReceipt revokeValue(Long walletId, BigInteger dataId) throws Exception {
        // 1. Load from DB
//        JalinContractInfo contractInfo = contractRepo.findById(contractId).orElseThrow();
//        JalinNetworkConfig network = networkRepo.findById(contractInfo.getNetworkId()).orElseThrow();
        KryptaWallet wallet = walletRepo.findById(walletId).orElseThrow();

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
        return contract.revokeData(dataId).send();
    }

    public Map<String, Object> verify(Long walletId, String txHash) throws Exception {
        // 1️⃣ Load wallet + contract
        KryptaWallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        KryptaContract contract = wallet.getContract();
        if (contract == null) throw new RuntimeException("No contract linked to wallet: " + walletId);

        JsonNode abiSummary = contract.getAbiSummary();
        String abiJson = Files.readString(Paths.get(contract.getAbi())); // full ABI if available

        Web3j web3j = this.createWeb3(wallet);
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("type", "verify");
        result.put("txHash", txHash);

        // 2️⃣ Fetch transaction
        EthTransaction txResponse = web3j.ethGetTransactionByHash(txHash).send();
        if (txResponse.getTransaction().isEmpty()) {
            result.put("status", "NOT_FOUND");
            return result;
        }

        Transaction tx = txResponse.getTransaction().get();
        result.put("from", tx.getFrom());
        result.put("to", tx.getTo());
        result.put("value", tx.getValue());
        result.put("nonce", tx.getNonce());

        // 3️⃣ Wait for receipt
        Optional<TransactionReceipt> receiptOpt;
        int attempts = 0;
        do {
            Thread.sleep(2000);
            receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            attempts++;
        } while (receiptOpt.isEmpty() && attempts < 30);

        if (receiptOpt.isEmpty()) {
            result.put("status", "PENDING");
            return result;
        }

        TransactionReceipt receipt = receiptOpt.get();
        result.put("blockNumber", receipt.getBlockNumber());
        result.put("gasUsed", receipt.getGasUsed());
        result.put("txStatus", receipt.getStatus().equals("0x1") ? "SUCCESS" : "FAILED");
        result.put("logsCount", receipt.getLogs().size());

        // 4️⃣ Decode events dynamically
        List<Map<String, Object>> decodedEvents = new ArrayList<>();

        if (abiJson != null && !abiJson.isBlank()) {
            decodedEvents = decodeEventsFromAbi(abiJson, receipt);
        } else if (abiSummary != null && abiSummary.has("events")) {
            // Fallback: only match by event name presence in logs (simplified)
            List<String> eventNames = new ArrayList<>();
            abiSummary.get("events").forEach(node -> eventNames.add(node.asText()));
            for (Log log : receipt.getLogs()) {
                if (log.getTopics().isEmpty()) continue;
                String topic = log.getTopics().get(0);
                for (String ev : eventNames) {
                    if (topic.contains(ev)) { // rough match
                        Map<String, Object> e = new LinkedHashMap<>();
                        e.put("event", ev);
                        e.put("raw", log);
                        decodedEvents.add(e);
                    }
                }
            }
        }

        result.put("events", decodedEvents);
        web3j.shutdown();
        return result;
    }

    public List<Map<String, Object>> logs(Long walletId, String eventName) throws Exception {
        KryptaWallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        KryptaContract contract = wallet.getContract();
        if (contract == null)
            throw new RuntimeException("Wallet has no associated contract: " + walletId);

        // 1️⃣ Load ABI
        String abiText = resolveAbi(contract);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode abiArray = (ArrayNode) mapper.readTree(abiText);

        // 2️⃣ Find the event definition in ABI
        ObjectNode eventDef = null;
        for (JsonNode node : abiArray) {
            if ("event".equals(node.path("type").asText()) &&
                    eventName.equals(node.path("name").asText())) {
                eventDef = (ObjectNode) node;
                break;
            }
        }

        if (eventDef == null)
            throw new RuntimeException("Event not found in ABI: " + eventName);

        // 3️⃣ Build event parameter types
        ArrayNode inputs = (ArrayNode) eventDef.path("inputs");
        List<TypeReference<?>> parameters = new ArrayList<>();

        for (JsonNode input : inputs) {
            String type = input.get("type").asText();
            boolean indexed = input.has("indexed") && input.get("indexed").asBoolean();
            parameters.add(TypeReference.makeTypeReference(type, indexed, false));
        }

        Event event = new Event(eventName, parameters);
        String eventTopic = EventEncoder.encode(event);

        // 4️⃣ Query all logs for this event
        Web3j web3j = this.createWeb3(wallet);

        org.web3j.protocol.core.methods.request.EthFilter filter = new org.web3j.protocol.core.methods.request.EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                wallet.getContractAddress()
        ).addSingleTopic(eventTopic);

        EthLog ethLog = web3j.ethGetLogs(filter).send();

        List<Map<String, Object>> decodedEvents = new ArrayList<>();

        // 5️⃣ Decode logs
        for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
            Log log = (Log) logResult.get();
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("event", eventName);
            e.put("txHash", log.getTransactionHash());
            e.put("blockNumber", log.getBlockNumber());
            e.put("logIndex", log.getLogIndex());

            // Separate indexed vs non-indexed params
            List<TypeReference<Type>> indexedParams = new ArrayList<>();
            List<TypeReference<Type>> nonIndexedParams = new ArrayList<>();

            for (TypeReference<?> param : event.getParameters()) {
                if (param.isIndexed()) indexedParams.add((TypeReference<Type>) param);
                else nonIndexedParams.add((TypeReference<Type>) param);
            }

            // Decode indexed params
            int topicIndex = 1;
            for (TypeReference<Type> param : indexedParams) {
                if (topicIndex >= log.getTopics().size()) break;
                String topicValue = log.getTopics().get(topicIndex++);
                String typeName = param.getType().getTypeName();
                String processedTypeName = typeName.substring(typeName.lastIndexOf('.') + 1);

                Object decodedValue = switch (processedTypeName) {
                    case "Uint256", "Int256" -> new BigInteger(topicValue.substring(2), 16);
                    case "Address" -> "0x" + topicValue.substring(26);
                    default -> topicValue;
                };
                e.put(processedTypeName, decodedValue);
            }

            // Decode non-indexed params
            List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), nonIndexedParams);
            for (int i = 0; i < nonIndexedValues.size(); i++) {
                e.put("param" + (i + 1), nonIndexedValues.get(i).getValue());
            }

            decodedEvents.add(e);
        }

        web3j.shutdown();
        return decodedEvents;
    }

    private List<Map<String, Object>> decodeEventsFromAbi(String abiJson, TransactionReceipt receipt) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode abiNode = mapper.readTree(abiJson);

        Map<String, Event> eventDefinitions = new HashMap<>();
        for (JsonNode item : abiNode) {
            if ("event".equals(item.get("type").asText())) {
                String eventName = item.get("name").asText();
                List<TypeReference<?>> params = new ArrayList<>();
                for (JsonNode input : item.get("inputs")) {
                    boolean indexed = input.has("indexed") && input.get("indexed").asBoolean();
                    String type = input.get("type").asText();
                    TypeReference<?> ref = TypeReference.makeTypeReference(type, indexed, true);
                    params.add(ref);
                }
                eventDefinitions.put(EventEncoder.encode(new Event(eventName, params)),
                        new Event(eventName, params));
            }
        }

        List<Map<String, Object>> decodedEvents = new ArrayList<>();
        for (Log log : receipt.getLogs()) {
            if (log.getTopics().isEmpty()) continue;
            String topic0 = log.getTopics().get(0);
            Event event = eventDefinitions.get(topic0);
            if (event == null) continue;

            List<Type> indexedValues = new ArrayList<>();
            for (int i = 0; i < event.getIndexedParameters().size(); i++) {
                if (log.getTopics().size() > i + 1) {
                    Type val = FunctionReturnDecoder.decodeIndexedValue(
                            log.getTopics().get(i + 1),
                            event.getIndexedParameters().get(i)
                    );
                    indexedValues.add(val);
                }
            }

            List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("event", event.getName());
            e.put("indexed", extractValues(indexedValues));
            e.put("data", extractValues(nonIndexedValues));
            decodedEvents.add(e);
        }
        return decodedEvents;
    }

    private List<Object> extractValues(List<Type> values) {
        List<Object> result = new ArrayList<>();
        for (Type v : values) {
            result.add(v.getValue());
        }
        return result;
    }

    public KryptaWallet getWallet(Long id) {
        return walletRepo.findById(id).orElseThrow();
    }

    public Page<KryptaWallet> getWalletList(Long appId, Pageable pageable) {
        return walletRepo.findByAppId(appId, pageable);
    }

    public KryptaWallet saveWallet(Long appId, KryptaWallet walletInfo, String email) {
        App app = appRepository.findById(appId).orElseThrow();
        walletInfo.setApp(app);
        walletInfo.setEmail(email);
        return walletRepo.save(walletInfo);
    }

    public void deleteWallet(Long id) {
        walletRepo.deleteById(id);
    }


    @Value("${instance.krypta.solc-path:/usr/local/bin/solcjs}")
    String SOLC_PATH;

    public KryptaContract compileSolidity(Long contractId) throws Exception {
        KryptaContract contract = contractRepo.findById(contractId).orElseThrow();

        // ✅ Define safe and unique paths
        Path baseDir = Paths.get(Constant.UPLOAD_ROOT_DIR, "contracts");
        Files.createDirectories(baseDir);

        String uniquePrefix = "contract_" + contractId + "_" + System.currentTimeMillis();
        Path tempSolFile = baseDir.resolve(uniquePrefix + ".sol");
        Path outputDir = baseDir.resolve(uniquePrefix + "_out");
        Files.createDirectories(outputDir);

        // 1️⃣ Write Solidity source
        Files.writeString(tempSolFile, contract.getSol());

        // 2️⃣ Compile with solcjs
        ProcessBuilder pb = new ProcessBuilder(
                SOLC_PATH,
                "--bin",
                "--abi",
                "--output-dir", outputDir.toString(),
                tempSolFile.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 3️⃣ Capture compiler output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("❌ Solc compilation failed:\n" + output);
        }

        // 4️⃣ Find generated files
        Path abiPath = null;
        Path binPath = null;
        try (Stream<Path> files = Files.list(outputDir)) {
            for (Path path : files.toList()) {
                String name = path.getFileName().toString();
                if (name.endsWith(".abi")) abiPath = path;
                if (name.endsWith(".bin")) binPath = path;
            }
        }

        if (abiPath == null || binPath == null) {
            throw new RuntimeException("❌ Missing .abi or .bin output files!");
        }

        // 5️⃣ Copy to permanent filenames
        String baseName = "contract_" + contractId;
        Path finalAbi = baseDir.resolve(baseName + ".abi");
        Path finalBin = baseDir.resolve(baseName + ".bin");

        Files.copy(abiPath, finalAbi, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(binPath, finalBin, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("✅ ABI saved: " + finalAbi.toAbsolutePath());
        System.out.println("✅ BIN saved: " + finalBin.toAbsolutePath());

        JsonNode abiSummary = extractAbiSummary(finalAbi);
        contract.setAbiSummary(abiSummary);



        // 6️⃣ Optionally update DB record for retrieval
        contract.setAbi(finalAbi.toString());
        contract.setBin(finalBin.toString());
        contractRepo.save(contract);

        // Optional: clean up temporary folder
        try (Stream<Path> files = Files.walk(outputDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }

        System.out.println("✅ Compilation completed successfully for contractId=" + contractId);

        return contract;
    }


//    private Map<String, Object> extractAbiSummary(Path abiPath) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode abiArray = mapper.readTree(Files.readString(abiPath));
//
//        List<String> events = new ArrayList<>();
//        List<ObjectNode> functions = new ArrayList<>();
//
//        for (JsonNode node : abiArray) {
//            String type = node.path("type").asText();
//            if ("event".equals(type)) {
//                events.add(node.path("name").asText());
//            } else if ("function".equals(type)) {
//                functions.add((ObjectNode) node);
//            }
//        }
//
//        Map<String, Object> result = new LinkedHashMap<>();
//        result.put("events", events);
//        result.put("functions", functions);
//        return result;
//    }

    public JsonNode extractAbiSummary(Path abiPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Parse ABI file
        JsonNode abiArray = mapper.readTree(Files.newBufferedReader(abiPath));

        // Create summary JSON structure
        ObjectNode summary = mapper.createObjectNode();
        ArrayNode events = mapper.createArrayNode();
        ArrayNode functions = mapper.createArrayNode();

        for (JsonNode node : abiArray) {
            String type = node.path("type").asText();
            if ("event".equals(type)) {
                events.add(node.path("name").asText());
            } else if ("function".equals(type)) {
                functions.add(node); // ✅ Keep full function definition
            }
        }

        summary.set("events", events);
        summary.set("functions", functions);

        return summary;
    }

    public KryptaWallet deployContract(Long walletId) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId).orElseThrow();

        if (wallet.getContract() == null) {
            return initDefaultContract(walletId);
        }

        KryptaContract contract = wallet.getContract();

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();
        HttpService httpService = new HttpService(wallet.getRpcUrl(), httpClient, false);

        // 1️⃣ Connect to Ethereum node
        Web3j web3j = Web3j.build(httpService);
        System.out.println("Connected to Ethereum network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        // 2️⃣ Load wallet credentials
        Credentials credentials = Credentials.create(wallet.getPrivateKey());

        // 3️⃣ Read .bin file content
//        String binary = Files.readString(Paths.get(contract.getBin()));
//        if (binary.startsWith("0x")) {
//            binary = binary.substring(2);
//        }
        String binary = Files.readString(Paths.get(contract.getBin())).trim();
        if (!binary.startsWith("0x")) {
            binary = "0x" + binary;
        }

//        System.out.println("Binary: " + binary);

        TransactionManager txManager = new FastRawTransactionManager(web3j, credentials, wallet.getChainId());
        ContractGasProvider gasProvider = new DefaultGasProvider();

        // 6️⃣ Send deployment transaction
        EthSendTransaction transactionResponse = txManager.sendTransaction(
                gasProvider.getGasPrice(""),
                gasProvider.getGasLimit(""),
                null,           // to = null means contract creation
                binary,       // compiled bytecode
                BigInteger.ZERO
        );

        String txHash = transactionResponse.getTransactionHash();
        System.out.println("📦 Deployment tx sent: " + txHash);

        TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(
                web3j,
                2000,   // polling interval (ms)
                15      // max attempts
        );

        TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

        String contractAddress = receipt.getContractAddress();
        System.out.println("✅ Deployed contract address: " + contractAddress);


        wallet.setContractAddress(contractAddress);
        walletRepo.save(wallet);


        web3j.shutdown();
        return wallet;
    }




    public String deployContractFromAbiBin(String rpcUrl, String privateKey, String abiPath, String binPath) throws Exception {
        // 1️⃣ Connect to node
        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        System.out.println("Connected: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        // 2️⃣ Load credentials
        Credentials credentials = Credentials.create(privateKey);

        // 3️⃣ Read ABI and BIN
        String abi = Files.readString(Paths.get(abiPath));
        String bin = Files.readString(Paths.get(binPath));

        // 4️⃣ Create Transaction Manager & Gas Provider
        long chainId = 1337L; // replace as needed
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);
        DefaultGasProvider gasProvider = new DefaultGasProvider();

        // 5️⃣ Deploy contract manually
        EthSendTransaction transactionResponse = txManager.sendTransaction(
                gasProvider.getGasPrice(""),  // gas price
                gasProvider.getGasLimit(""),  // gas limit
                "",                           // to (empty for contract deployment)
                bin,                          // contract bytecode
                BigInteger.ZERO               // value (ETH to send)
        );

        String txHash = transactionResponse.getTransactionHash();
        System.out.println("⛓️  Deployment tx hash: " + txHash);

        // 6️⃣ Wait for receipt
        TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                .send()
                .getTransactionReceipt()
                .orElseThrow(() -> new RuntimeException("No receipt yet. Wait a few seconds."));

        String contractAddress = receipt.getContractAddress();
        System.out.println("✅ Contract deployed at: " + contractAddress);

        web3j.shutdown();
        return contractAddress;
    }


    public KryptaContract getContract(Long id) {
        return contractRepo.findById(id).orElseThrow();
    }

    public Page<KryptaContract> getContractList(Long appId, Pageable pageable) {
        return contractRepo.findByAppId(appId, pageable);
    }

    public KryptaContract saveContract(Long appId, KryptaContract contractInfo, String email) {
        App app = appRepository.findById(appId).orElseThrow();
        contractInfo.setApp(app);
        contractInfo.setEmail(email);
        return contractRepo.save(contractInfo);
    }

    public void deleteContract(Long id) {
        contractRepo.deleteById(id);
    }
}
