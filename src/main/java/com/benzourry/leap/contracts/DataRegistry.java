package com.benzourry.leap.contracts;

import io.reactivex.Flowable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.10.0.
 */
@SuppressWarnings("rawtypes")
public class DataRegistry extends Contract {
    public static final String BINARY = "6080604052348015600e575f5ffd5b50600180546001600160a01b03191633179055610f7a8061002e5f395ff3fe608060405234801561000f575f5ffd5b5060043610610090575f3560e01c806389a8e2a71161006357806389a8e2a71461010a5780638da5cb5b1461011d5780639b704ed514610130578063adc604fc14610143578063f577a50014610156575f5ffd5b80630178fe3f14610094578063403be57a146100bd57806342413315146100d2578063893d20e8146100e5575b5f5ffd5b6100a76100a2366004610a98565b61018b565b6040516100b49190610add565b60405180910390f35b6100d06100cb366004610a98565b61028a565b005b6100d06100e0366004610af6565b610356565b6001546001600160a01b03165b6040516001600160a01b0390911681526020016100b4565b6100d0610118366004610af6565b61048c565b6001546100f2906001600160a01b031681565b6100d061013e366004610bb5565b610606565b6100d0610151366004610bb5565b610845565b61017b610164366004610a98565b5f9081526020819052604090206001015460ff1690565b60405190151581526020016100b4565b5f8181526020819052604090206001015460609060ff166101ef5760405162461bcd60e51b815260206004820152601960248201527811185d18481b9bdd08199bdd5b99081bdc881a5b9d985b1a59603a1b60448201526064015b60405180910390fd5b5f828152602081905260409020805461020790610c21565b80601f016020809104026020016040519081016040528092919081815260200182805461023390610c21565b801561027e5780601f106102555761010080835404028352916020019161027e565b820191905f5260205f20905b81548152906001019060200180831161026157829003601f168201915b50505050509050919050565b6001546001600160a01b031633146102b45760405162461bcd60e51b81526004016101e690610c59565b5f8181526020819052604090206001015460ff166103145760405162461bcd60e51b815260206004820152601c60248201527f4e6f7420666f756e64206f7220616c7265616479207265766f6b65640000000060448201526064016101e6565b5f81815260208190526040808220600101805460ff191690555182917fadd92423080b84b57082bc6c33cfb57baa48813a739f14a18d52a803767130a991a250565b6001546001600160a01b031633146103805760405162461bcd60e51b81526004016101e690610c59565b5f8381526020819052604090206001015460ff16156103d25760405162461bcd60e51b815260206004820152600e60248201526d416c72656164792065786973747360901b60448201526064016101e6565b6040805160606020601f850181900402820181018352918101838152909182919085908590819085018382808284375f920182905250938552505060016020938401525085815290819052604090208151819061042f9082610ce1565b50602091909101516001909101805460ff191691151591909117905560405183907fb35d322946378a6988952f6ec334d7458ce97c42fbb3ecfc1e00a5062c553a0a9061047f9085908590610dc4565b60405180910390a2505050565b6001546001600160a01b031633146104b65760405162461bcd60e51b81526004016101e690610c59565b5f8381526020819052604090206001015460ff166105125760405162461bcd60e51b815260206004820152601960248201527811185d18481b9bdd08199bdd5b99081bdc881a5b9d985b1a59603a1b60448201526064016101e6565b5f838152602081905260408120805461052a90610c21565b80601f016020809104026020016040519081016040528092919081815260200182805461055690610c21565b80156105a15780601f10610578576101008083540402835291602001916105a1565b820191905f5260205f20905b81548152906001019060200180831161058457829003601f168201915b5050505f8781526020819052604090209293506105c391508490508583610ddf565b50837f70fc7d66686bb9a40f0b9075882d22a152689e4619fc666c06c2b3b51befa3c88285856040516105f893929190610e99565b60405180910390a250505050565b6001546001600160a01b031633146106305760405162461bcd60e51b81526004016101e690610c59565b8281811461067b5760405162461bcd60e51b81526020600482015260186024820152774d69736d617463686564206172726179206c656e6774687360401b60448201526064016101e6565b5f81116106b85760405162461bcd60e51b815260206004820152600b60248201526a08adae0e8f240c4c2e8c6d60ab1b60448201526064016101e6565b6101f48111156106fc5760405162461bcd60e51b815260206004820152600f60248201526e426174636820746f6f206c6172676560881b60448201526064016101e6565b5f5b818110156107bf575f86868381811061071957610719610ec8565b602090810292909201355f818152928390526040909220600101549192505060ff166107785760405162461bcd60e51b815260206004820152600e60248201526d125b9d985b1a590819185d18525960921b60448201526064016101e6565b84848381811061078a5761078a610ec8565b905060200281019061079c9190610edc565b5f838152602081905260409020916107b5919083610ddf565b50506001016106fe565b507fc6c3c4d152afe2fbac65f925cca30424abe942075ae850d9fa5b665effe8bbf585855f8181106107f3576107f3610ec8565b9050602002013586866001856108099190610f1f565b81811061081857610818610ec8565b60408051948552602091820293909301359084015250810183905260600160405180910390a15050505050565b6001546001600160a01b0316331461086f5760405162461bcd60e51b81526004016101e690610c59565b828181146108ba5760405162461bcd60e51b81526020600482015260186024820152774d69736d617463686564206172726179206c656e6774687360401b60448201526064016101e6565b5f81116108f75760405162461bcd60e51b815260206004820152600b60248201526a08adae0e8f240c4c2e8c6d60ab1b60448201526064016101e6565b6101f481111561093b5760405162461bcd60e51b815260206004820152600f60248201526e426174636820746f6f206c6172676560881b60448201526064016101e6565b5f5b81811015610a64575f86868381811061095857610958610ec8565b602090810292909201355f818152928390526040909220600101549192505060ff16156109b85760405162461bcd60e51b815260206004820152600e60248201526d416c72656164792065786973747360901b60448201526064016101e6565b60405180604001604052808686858181106109d5576109d5610ec8565b90506020028101906109e79190610edc565b8080601f0160208091040260200160405190810160405280939291908181526020018383808284375f9201829052509385525050600160209384015250838152908190526040902081518190610a3d9082610ce1565b50602091909101516001918201805460ff191691151591909117905591909101905061093d565b507fe5d34b7eca441afb93faead6f428e4ba647fd16590d94514000a8738ac1106a885855f8181106107f3576107f3610ec8565b5f60208284031215610aa8575f5ffd5b5035919050565b5f81518084528060208401602086015e5f602082860101526020601f19601f83011685010191505092915050565b602081525f610aef6020830184610aaf565b9392505050565b5f5f5f60408486031215610b08575f5ffd5b83359250602084013567ffffffffffffffff811115610b25575f5ffd5b8401601f81018613610b35575f5ffd5b803567ffffffffffffffff811115610b4b575f5ffd5b866020828401011115610b5c575f5ffd5b939660209190910195509293505050565b5f5f83601f840112610b7d575f5ffd5b50813567ffffffffffffffff811115610b94575f5ffd5b6020830191508360208260051b8501011115610bae575f5ffd5b9250929050565b5f5f5f5f60408587031215610bc8575f5ffd5b843567ffffffffffffffff811115610bde575f5ffd5b610bea87828801610b6d565b909550935050602085013567ffffffffffffffff811115610c09575f5ffd5b610c1587828801610b6d565b95989497509550505050565b600181811c90821680610c3557607f821691505b602082108103610c5357634e487b7160e01b5f52602260045260245ffd5b50919050565b6020808252600e908201526d139bdd08185d5d1a1bdc9a5e995960921b604082015260600190565b634e487b7160e01b5f52604160045260245ffd5b601f821115610cdc57805f5260205f20601f840160051c81016020851015610cba5750805b601f840160051c820191505b81811015610cd9575f8155600101610cc6565b50505b505050565b815167ffffffffffffffff811115610cfb57610cfb610c81565b610d0f81610d098454610c21565b84610c95565b6020601f821160018114610d41575f8315610d2a5750848201515b5f19600385901b1c1916600184901b178455610cd9565b5f84815260208120601f198516915b82811015610d705787850151825560209485019460019092019101610d50565b5084821015610d8d57868401515f19600387901b60f8161c191681555b50505050600190811b01905550565b81835281816020850137505f828201602090810191909152601f909101601f19169091010190565b602081525f610dd7602083018486610d9c565b949350505050565b67ffffffffffffffff831115610df757610df7610c81565b610e0b83610e058354610c21565b83610c95565b5f601f841160018114610e3c575f8515610e255750838201355b5f19600387901b1c1916600186901b178355610cd9565b5f83815260208120601f198716915b82811015610e6b5786850135825560209485019460019092019101610e4b565b5086821015610e87575f1960f88860031b161c19848701351681555b505060018560011b0183555050505050565b604081525f610eab6040830186610aaf565b8281036020840152610ebe818587610d9c565b9695505050505050565b634e487b7160e01b5f52603260045260245ffd5b5f5f8335601e19843603018112610ef1575f5ffd5b83018035915067ffffffffffffffff821115610f0b575f5ffd5b602001915036819003821315610bae575f5ffd5b81810381811115610f3e57634e487b7160e01b5f52601160045260245ffd5b9291505056fea26469706673582212204a572fb70c3ab1a1573cefbc3b48c6adbf796aabf228130dbba1556e4af3f90664736f6c634300081d0033";

    public static final String FUNC_ADDDATA = "addData";

    public static final String FUNC_ADDDATABATCH = "addDataBatch";

    public static final String FUNC_GETDATA = "getData";

    public static final String FUNC_GETOWNER = "getOwner";

    public static final String FUNC_ISVALID = "isValid";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_REVOKEDATA = "revokeData";

    public static final String FUNC_UPDATEDATA = "updateData";

    public static final String FUNC_UPDATEDATABATCH = "updateDataBatch";

    public static final Event BATCHDATAADDED_EVENT = new Event("BatchDataAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event BATCHDATAUPDATED_EVENT = new Event("BatchDataUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event DATAADDED_EVENT = new Event("DataAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event DATAREVOKED_EVENT = new Event("DataRevoked", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event DATAUPDATED_EVENT = new Event("DataUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
    ;

    @Deprecated
    protected DataRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DataRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DataRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DataRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<BatchDataAddedEventResponse> getBatchDataAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BATCHDATAADDED_EVENT, transactionReceipt);
        ArrayList<BatchDataAddedEventResponse> responses = new ArrayList<BatchDataAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BatchDataAddedEventResponse typedResponse = new BatchDataAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.startIndex = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.endIndex = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.count = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BatchDataAddedEventResponse getBatchDataAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BATCHDATAADDED_EVENT, log);
        BatchDataAddedEventResponse typedResponse = new BatchDataAddedEventResponse();
        typedResponse.log = log;
        typedResponse.startIndex = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.endIndex = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.count = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<BatchDataAddedEventResponse> batchDataAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBatchDataAddedEventFromLog(log));
    }

    public Flowable<BatchDataAddedEventResponse> batchDataAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BATCHDATAADDED_EVENT));
        return batchDataAddedEventFlowable(filter);
    }

    public static List<BatchDataUpdatedEventResponse> getBatchDataUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BATCHDATAUPDATED_EVENT, transactionReceipt);
        ArrayList<BatchDataUpdatedEventResponse> responses = new ArrayList<BatchDataUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BatchDataUpdatedEventResponse typedResponse = new BatchDataUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.startIndex = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.endIndex = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.count = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BatchDataUpdatedEventResponse getBatchDataUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BATCHDATAUPDATED_EVENT, log);
        BatchDataUpdatedEventResponse typedResponse = new BatchDataUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.startIndex = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.endIndex = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.count = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<BatchDataUpdatedEventResponse> batchDataUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBatchDataUpdatedEventFromLog(log));
    }

    public Flowable<BatchDataUpdatedEventResponse> batchDataUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BATCHDATAUPDATED_EVENT));
        return batchDataUpdatedEventFlowable(filter);
    }

    public static List<DataAddedEventResponse> getDataAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DATAADDED_EVENT, transactionReceipt);
        ArrayList<DataAddedEventResponse> responses = new ArrayList<DataAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DataAddedEventResponse typedResponse = new DataAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.dataId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.data = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DataAddedEventResponse getDataAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DATAADDED_EVENT, log);
        DataAddedEventResponse typedResponse = new DataAddedEventResponse();
        typedResponse.log = log;
        typedResponse.dataId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.data = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<DataAddedEventResponse> dataAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDataAddedEventFromLog(log));
    }

    public Flowable<DataAddedEventResponse> dataAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DATAADDED_EVENT));
        return dataAddedEventFlowable(filter);
    }

    public static List<DataRevokedEventResponse> getDataRevokedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DATAREVOKED_EVENT, transactionReceipt);
        ArrayList<DataRevokedEventResponse> responses = new ArrayList<DataRevokedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DataRevokedEventResponse typedResponse = new DataRevokedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.dataId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DataRevokedEventResponse getDataRevokedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DATAREVOKED_EVENT, log);
        DataRevokedEventResponse typedResponse = new DataRevokedEventResponse();
        typedResponse.log = log;
        typedResponse.dataId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<DataRevokedEventResponse> dataRevokedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDataRevokedEventFromLog(log));
    }

    public Flowable<DataRevokedEventResponse> dataRevokedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DATAREVOKED_EVENT));
        return dataRevokedEventFlowable(filter);
    }

    public static List<DataUpdatedEventResponse> getDataUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DATAUPDATED_EVENT, transactionReceipt);
        ArrayList<DataUpdatedEventResponse> responses = new ArrayList<DataUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DataUpdatedEventResponse typedResponse = new DataUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.dataId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.oldData = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newData = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DataUpdatedEventResponse getDataUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DATAUPDATED_EVENT, log);
        DataUpdatedEventResponse typedResponse = new DataUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.dataId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.oldData = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newData = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<DataUpdatedEventResponse> dataUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDataUpdatedEventFromLog(log));
    }

    public Flowable<DataUpdatedEventResponse> dataUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DATAUPDATED_EVENT));
        return dataUpdatedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addData(BigInteger dataId, String data) {
        final Function function = new Function(
                FUNC_ADDDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(dataId), 
                new org.web3j.abi.datatypes.Utf8String(data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> addDataBatch(List<BigInteger> dataIds, List<String> dataList) {
        final Function function = new Function(
                FUNC_ADDDATABATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(dataIds, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                        org.web3j.abi.datatypes.Utf8String.class,
                        org.web3j.abi.Utils.typeMap(dataList, org.web3j.abi.datatypes.Utf8String.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> getData(BigInteger dataId) {
        final Function function = new Function(FUNC_GETDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(dataId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> getOwner() {
        final Function function = new Function(FUNC_GETOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isValid(BigInteger dataId) {
        final Function function = new Function(FUNC_ISVALID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(dataId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeData(BigInteger dataId) {
        final Function function = new Function(
                FUNC_REVOKEDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(dataId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateData(BigInteger dataId, String newData) {
        final Function function = new Function(
                FUNC_UPDATEDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(dataId), 
                new org.web3j.abi.datatypes.Utf8String(newData)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateDataBatch(List<BigInteger> dataIds, List<String> newDataList) {
        final Function function = new Function(
                FUNC_UPDATEDATABATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(dataIds, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                        org.web3j.abi.datatypes.Utf8String.class,
                        org.web3j.abi.Utils.typeMap(newDataList, org.web3j.abi.datatypes.Utf8String.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static DataRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DataRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DataRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DataRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DataRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DataRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DataRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DataRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<DataRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DataRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DataRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DataRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DataRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DataRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class BatchDataAddedEventResponse extends BaseEventResponse {
        public BigInteger startIndex;

        public BigInteger endIndex;

        public BigInteger count;
    }

    public static class BatchDataUpdatedEventResponse extends BaseEventResponse {
        public BigInteger startIndex;

        public BigInteger endIndex;

        public BigInteger count;
    }

    public static class DataAddedEventResponse extends BaseEventResponse {
        public BigInteger dataId;

        public String data;
    }

    public static class DataRevokedEventResponse extends BaseEventResponse {
        public BigInteger dataId;
    }

    public static class DataUpdatedEventResponse extends BaseEventResponse {
        public BigInteger dataId;

        public String oldData;

        public String newData;
    }
}
