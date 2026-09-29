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
public class CertificateRegistry extends Contract {
    public static final String BINARY = "6080604052348015600e575f5ffd5b50600180546001600160a01b031916331790556106d58061002e5f395ff3fe608060405234801561000f575f5ffd5b5060043610610055575f3560e01c806306f5497e1461005957806331494bbb1461006e5780638da5cb5b14610097578063ccb1173b146100c2578063f577a500146100d5575b5f5ffd5b61006c610067366004610443565b61010a565b005b61008161007c366004610443565b61020d565b60405161008e919061045a565b60405180910390f35b6001546100aa906001600160a01b031681565b6040516001600160a01b03909116815260200161008e565b61006c6100d03660046104a3565b610317565b6100fa6100e3366004610443565b5f9081526020819052604090206001015460ff1690565b604051901515815260200161008e565b6001546001600160a01b0316331461015a5760405162461bcd60e51b815260206004820152600e60248201526d139bdd08185d5d1a1bdc9a5e995960921b60448201526064015b60405180910390fd5b5f8181526020819052604090206001015460ff166101cb5760405162461bcd60e51b815260206004820152602860248201527f4365727469666963617465206e6f7420666f756e64206f7220616c7265616479604482015267081c995d9bdad95960c21b6064820152608401610151565b5f81815260208190526040808220600101805460ff191690555182917fefa6c5f47ac2523bb4db18032377bf7fdce0fa9d86eddcae1ca9bba38be615d791a250565b5f8181526020819052604090206001015460609060ff1661027c5760405162461bcd60e51b815260206004820152602360248201527f436572746966696361746520697320696e76616c6964206f72206e6f7420666f6044820152621d5b9960ea1b6064820152608401610151565b5f828152602081905260409020805461029490610560565b80601f01602080910402602001604051908101604052809291908181526020018280546102c090610560565b801561030b5780601f106102e25761010080835404028352916020019161030b565b820191905f5260205f20905b8154815290600101906020018083116102ee57829003601f168201915b50505050509050919050565b6001546001600160a01b031633146103625760405162461bcd60e51b815260206004820152600e60248201526d139bdd08185d5d1a1bdc9a5e995960921b6044820152606401610151565b5f8281526020819052604090206001015460ff16156103b85760405162461bcd60e51b8152602060048201526012602482015271105b1c9958591e481c9959da5cdd195c995960721b6044820152606401610151565b60408051808201825282815260016020808301919091525f8581529081905291909120815181906103e990826105e4565b50602091909101516001909101805460ff191691151591909117905560405182907fb3f7a51d97edfec0a93ec31952a01e523033a70f92f1cfae631a92c02f5306d09061043790849061045a565b60405180910390a25050565b5f60208284031215610453575f5ffd5b5035919050565b602081525f82518060208401528060208501604085015e5f604082850101526040601f19601f83011684010191505092915050565b634e487b7160e01b5f52604160045260245ffd5b5f5f604083850312156104b4575f5ffd5b82359150602083013567ffffffffffffffff8111156104d1575f5ffd5b8301601f810185136104e1575f5ffd5b803567ffffffffffffffff8111156104fb576104fb61048f565b604051601f8201601f19908116603f0116810167ffffffffffffffff8111828210171561052a5761052a61048f565b604052818152828201602001871015610541575f5ffd5b816020840160208301375f602083830101528093505050509250929050565b600181811c9082168061057457607f821691505b60208210810361059257634e487b7160e01b5f52602260045260245ffd5b50919050565b601f8211156105df57805f5260205f20601f840160051c810160208510156105bd5750805b601f840160051c820191505b818110156105dc575f81556001016105c9565b50505b505050565b815167ffffffffffffffff8111156105fe576105fe61048f565b6106128161060c8454610560565b84610598565b6020601f821160018114610644575f831561062d5750848201515b5f19600385901b1c1916600184901b1784556105dc565b5f84815260208120601f198516915b828110156106735787850151825560209485019460019092019101610653565b508482101561069057868401515f19600387901b60f8161c191681555b50505050600190811b0190555056fea2646970667358221220c62683e3ca3d7c5346f2963c5e86cfce3dbd4499db893ca017a67553aa7b844464736f6c634300081d0033";

    public static final String FUNC_ADDCERTIFICATE = "addCertificate";

    public static final String FUNC_GETCERTIFICATEDATA = "getCertificateData";

    public static final String FUNC_ISVALID = "isValid";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_REVOKECERTIFICATE = "revokeCertificate";

    public static final Event CERTIFICATEADDED_EVENT = new Event("CertificateAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event CERTIFICATEREVOKED_EVENT = new Event("CertificateRevoked", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    @Deprecated
    protected CertificateRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CertificateRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CertificateRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CertificateRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<CertificateAddedEventResponse> getCertificateAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CERTIFICATEADDED_EVENT, transactionReceipt);
        ArrayList<CertificateAddedEventResponse> responses = new ArrayList<CertificateAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CertificateAddedEventResponse typedResponse = new CertificateAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.certId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.data = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CertificateAddedEventResponse getCertificateAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CERTIFICATEADDED_EVENT, log);
        CertificateAddedEventResponse typedResponse = new CertificateAddedEventResponse();
        typedResponse.log = log;
        typedResponse.certId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.data = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CertificateAddedEventResponse> certificateAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCertificateAddedEventFromLog(log));
    }

    public Flowable<CertificateAddedEventResponse> certificateAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CERTIFICATEADDED_EVENT));
        return certificateAddedEventFlowable(filter);
    }

    public static List<CertificateRevokedEventResponse> getCertificateRevokedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CERTIFICATEREVOKED_EVENT, transactionReceipt);
        ArrayList<CertificateRevokedEventResponse> responses = new ArrayList<CertificateRevokedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CertificateRevokedEventResponse typedResponse = new CertificateRevokedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.certId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CertificateRevokedEventResponse getCertificateRevokedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CERTIFICATEREVOKED_EVENT, log);
        CertificateRevokedEventResponse typedResponse = new CertificateRevokedEventResponse();
        typedResponse.log = log;
        typedResponse.certId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CertificateRevokedEventResponse> certificateRevokedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCertificateRevokedEventFromLog(log));
    }

    public Flowable<CertificateRevokedEventResponse> certificateRevokedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CERTIFICATEREVOKED_EVENT));
        return certificateRevokedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addCertificate(BigInteger certId, String data) {
        final Function function = new Function(
                FUNC_ADDCERTIFICATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(certId), 
                new org.web3j.abi.datatypes.Utf8String(data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> getCertificateData(BigInteger certId) {
        final Function function = new Function(FUNC_GETCERTIFICATEDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(certId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isValid(BigInteger certId) {
        final Function function = new Function(FUNC_ISVALID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(certId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeCertificate(BigInteger certId) {
        final Function function = new Function(
                FUNC_REVOKECERTIFICATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(certId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static CertificateRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CertificateRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CertificateRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CertificateRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CertificateRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CertificateRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CertificateRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CertificateRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CertificateRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CertificateRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CertificateRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CertificateRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class CertificateAddedEventResponse extends BaseEventResponse {
        public BigInteger certId;

        public String data;
    }

    public static class CertificateRevokedEventResponse extends BaseEventResponse {
        public BigInteger certId;
    }
}
