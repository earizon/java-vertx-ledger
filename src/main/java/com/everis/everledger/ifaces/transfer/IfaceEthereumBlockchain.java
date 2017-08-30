
package com.everis.everledger.ifaces.transfer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;

public interface IfaceEthereumBlockchain {
    /*
     * Any request to the network will asynchronously return
     * some meta-data (tx receipt, block-number, contract-address, error,...)
     */
    public interface IfaceMetadata {
        String getValue(String key);
    }
    public interface BlockchainMetaData extends IfaceMetadata {
    };

    public interface TransferMinedData extends IfaceMetadata {
        String getReceipt();
        String getBlockNumber();
    };
    public interface TransferError     extends IfaceMetadata {};

    public interface ContractData      extends IfaceMetadata {};
    public interface ContractError     extends IfaceMetadata {};

    public interface EventData         extends IfaceMetadata {};


    // Prototype IfaceEthereumBlockchain
    public interface BlockchainListener {
        void onTransferMined(TransferMinedData data );
        void onTransferError(TransferError     data );

        void onContractCreated(ContractData    data );
        void onContractError  (ContractError   data );

        void onEventReceived  (EventData       data );
    }

    public /*receipt*/String submitTransferToNetworkAndGetReceipt();

        /**
         * @param owner_credentials User sending the TX to create the contract (and so expending its credits)
         * @param contractData      Will be initialized asyncrhonously after correct deployment
         * @throws IOException
         */
        public void deployNewContract(Credentials owner_credentials, BlockchainMetaData contractData) throws IOException;

        public ContractData getExistingContract(String contractAddress, Credentials msg_sender);

        /**
         * @param metadata Metadata must have been initialized, either async. at deployNewContract
         *        or synchronously for previously existing contracts
         * @param idx1 free format index 1
         * @param idx2 free format index 2
         * @param idx3 free format index 3
         * @param sLog free format String
         */
        public CompletableFuture<String> doSendLog2Blockchain(final ContractMetadata metadata, final String request_id,
                                                              final Uint256 idx1, final Uint256 idx2, final Uint256 idx3, final String sLog);

        public BigInteger getMsgSenderBalance(IfaceMetadata metadata);

        /**
         * Get IfaceMetadata for blockchain
         * @param sChain Ethereum (and potentially other blockchains) require
         * @return
         */
        public IfaceMetadata getMetadata(String sChain);
}
