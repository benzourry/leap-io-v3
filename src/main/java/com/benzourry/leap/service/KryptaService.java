package com.benzourry.leap.service;

//import com.benzourry.leap.contracts.CertificateRegistry;
//import com.benzourry.leap.model.JalinContractInfo;
//import com.benzourry.leap.model.JalinNetworkConfig;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.KryptaContract;
import com.benzourry.leap.model.KryptaWallet;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.KryptaContractRepository;
import com.benzourry.leap.repository.KryptaWalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class KryptaService {

//    private final JalinNetworkConfigRepository networkRepo;
    private final KryptaWalletRepository walletRepo;

    private final KryptaContractRepository contractRepo;

    private final AppRepository appRepository;

    private final ObjectMapper MAPPER;

    public KryptaService(
//            JalinNetworkConfigRepository networkRepo,
                        KryptaWalletRepository walletRepo,
                        AppRepository appRepository,
                        KryptaContractRepository contractRepo,
                        ObjectMapper MAPPER) {
        this.walletRepo = walletRepo;
        this.appRepository = appRepository;
        this.contractRepo = contractRepo;
        this.MAPPER = MAPPER;
    }

    private final Map<String, Web3j> web3jCache = new ConcurrentHashMap<>();

    public Web3j getOrCreateWeb3(KryptaWallet wallet) {
        if (web3jCache.containsKey(wallet.getRpcUrl())) {
            return web3jCache.get(wallet.getRpcUrl());
        }else{
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(200, TimeUnit.SECONDS)
                    .writeTimeout(200, TimeUnit.SECONDS)
                    .build();
            HttpService httpService = new HttpService(wallet.getRpcUrl(), httpClient, false);
            Web3j web3j = Web3j.build(httpService);
            web3jCache.put(wallet.getRpcUrl(), web3j);
            return web3j;
        }
    }

    private final Map<Long, ArrayNode> abiCache = new ConcurrentHashMap<>();
    private ArrayNode getOrCreateAbi(KryptaContract contract) throws Exception {
        return abiCache.computeIfAbsent(contract.getId(), id -> {
            try {
                return (ArrayNode) MAPPER.readTree(resolveAbi(contract));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static final Map<Long, Map<String, ObjectNode>> fnCache = new ConcurrentHashMap<>();

    private ObjectNode getOrCreateFunctionDef(KryptaContract contract, String functionName) throws Exception {
        Map<String, ObjectNode> fnMap = fnCache.computeIfAbsent(contract.getId(), id -> {
            Map<String, ObjectNode> map = new HashMap<>();
            ArrayNode abiArray;
            try {
                abiArray = getOrCreateAbi(contract);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (JsonNode node : abiArray) {
                if ("function".equals(node.path("type").asText())) {
                    map.put(node.path("name").asText(), (ObjectNode) node);
                }
            }
            return map;
        });

        ObjectNode fnDef = fnMap.get(functionName);
        if (fnDef == null)
            throw new RuntimeException("Function not found in ABI: " + functionName);

        return fnDef;
    }

    private static final Map<Long, TransactionManager> txManagerCache = new ConcurrentHashMap<>();

    private TransactionManager getOrCreateTxManager(KryptaWallet wallet, Web3j web3j) {
        return txManagerCache.computeIfAbsent(wallet.getId(),
                id -> new FastRawTransactionManager(web3j, Credentials.create(wallet.getPrivateKey()), wallet.getChainId()));
    }


    public Object call(Long walletId, String functionName, Map<String, Object> args) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        KryptaContract contract = wallet.getContract();
        if (contract == null)
            throw new RuntimeException("Wallet has no associated contract: " + walletId);

        Web3j web3j = getOrCreateWeb3(wallet);

        TransactionManager txManager = getOrCreateTxManager(wallet, web3j);
        ContractGasProvider gasProvider = new DefaultGasProvider();

        ObjectNode fnDef = getOrCreateFunctionDef(contract, functionName);

        ArrayNode inputs = (ArrayNode) fnDef.path("inputs");
        List<Type> inputParams = new ArrayList<>();

        for (JsonNode input : inputs) {
            String name = input.path("name").asText();
            String solidityType = input.path("type").asText();

            if (!args.containsKey(name)) {
                throw new RuntimeException("Missing argument: " + name + " (type: " + solidityType + ")");
            }

            Object value = args.get(name);
            inputParams.add(convertToWeb3Type(solidityType, value));
        }

        List<TypeReference<?>> outputParams = new ArrayList<>();
        if (fnDef.has("outputs")) {
            ArrayNode outputs = (ArrayNode) fnDef.get("outputs");
            for (JsonNode out : outputs) {
                String outType = out.get("type").asText();
                outputParams.add(TypeReference.makeTypeReference(outType));
            }
        }

        Function function = new Function(functionName, inputParams, outputParams);
        String encodedFunction = FunctionEncoder.encode(function);

        String stateMutability = fnDef.path("stateMutability").asText("");
        boolean isView = "view".equals(stateMutability) || "pure".equals(stateMutability);

        if (isView) {
            org.web3j.protocol.core.methods.request.Transaction ethCallTx =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    wallet.getContractAddress(),      // from
                    wallet.getContractAddress(),      // to (contract)
                    encodedFunction                   // data
                );

            EthCall response = web3j.ethCall(ethCallTx, DefaultBlockParameterName.LATEST).send();

            String value = response.getValue();
            List<Type> decoded = FunctionReturnDecoder.decode(value, function.getOutputParameters());

            if (decoded.isEmpty()) return null;
            if (decoded.size() == 1) return decoded.get(0).getValue();

            return decoded.stream().map(Type::getValue).toList();

        } else {
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

            TransactionReceiptProcessor receiptProcessor =
                    new PollingTransactionReceiptProcessor(web3j, 2000, 30);
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

//            web3j.shutdown();
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

    public Map<String, Object> verify(Long walletId, String txHash) throws Exception {

        KryptaWallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        KryptaContract contract = wallet.getContract();
        if (contract == null) throw new RuntimeException("No contract linked to wallet: " + walletId);

        JsonNode abiSummary = contract.getAbiSummary();
        String abiJson = Files.readString(Paths.get(contract.getAbi())); // full ABI if available

        Web3j web3j = getOrCreateWeb3(wallet);
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("type", "verify");
        result.put("txHash", txHash);

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
        ArrayNode abiArray = (ArrayNode) MAPPER.readTree(abiText);

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
        Web3j web3j = getOrCreateWeb3(wallet);

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
        JsonNode abiNode = MAPPER.readTree(abiJson);

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

        contract.setAbi(finalAbi.toString());
        contract.setBin(finalBin.toString());
        contractRepo.save(contract);

        // Optional: clean up temporary folder
        try (Stream<Path> files = Files.walk(outputDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }

        abiCache.remove(contractId);
        fnCache.remove(contractId);

        System.out.println("✅ Compilation completed successfully for contractId=" + contractId);

        return contract;
    }

    public JsonNode extractAbiSummary(Path abiPath) throws Exception {
        // Parse ABI file
        JsonNode abiArray = MAPPER.readTree(Files.newBufferedReader(abiPath));

        // Create summary JSON structure
        ObjectNode summary = MAPPER.createObjectNode();
        ArrayNode events = MAPPER.createArrayNode();
        ArrayNode functions = MAPPER.createArrayNode();

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

//        if (wallet.getContract() == null) {
//            return initDefaultContract(walletId);
//        }

        KryptaContract contract = wallet.getContract();

        Web3j web3j = getOrCreateWeb3(wallet);// Web3j.build(httpService);
        System.out.println("Connected to Ethereum network: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        String binary = Files.readString(Paths.get(contract.getBin())).trim();
        if (!binary.startsWith("0x")) {
            binary = "0x" + binary;
        }

        TransactionManager txManager = getOrCreateTxManager(wallet, web3j);// new FastRawTransactionManager(web3j, credentials, wallet.getChainId());
//        ContractGasProvider gasProvider = new DefaultGasProvider();

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice(); // dynamic gas price
        BigInteger gasLimit = BigInteger.valueOf(3_000_000L);           // ~3 million gas

        System.out.println("Gas price: " + gasPrice);
        System.out.println("Gas limit: " + gasLimit);
        System.out.println("Binary starts with 0x? " + binary.startsWith("0x"));
        System.out.println("Binary length: " + binary.length());

        EthSendTransaction transactionResponse = txManager.sendTransaction(
                gasPrice,
                gasLimit,
                null,           // to = null means contract creation
                binary,       // compiled bytecode
                BigInteger.ZERO
        );

        if (transactionResponse.hasError()) {
            throw new RuntimeException("RPC Error: " + transactionResponse.getError().getMessage());
        }

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
        return wallet;
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

    @PreDestroy
    public void onShutdown() {
        if(web3jCache !=null) {
            web3jCache.values().forEach(web3 -> {
                try {
                    web3.shutdown();
                } catch (Exception e) {
                    System.err.println("Error shutting down Web3j: " + e.getMessage());
                }
            });
            System.out.println("🛑 KryptaExecutor: all Web3j instances closed.");
        }

        if (abiCache != null) abiCache.clear();

        if (fnCache != null) fnCache.clear();

        if (txManagerCache != null) txManagerCache.clear();

        System.out.println("🛑 KryptaExecutor: all TransactionManagers closed.");
    }
}
