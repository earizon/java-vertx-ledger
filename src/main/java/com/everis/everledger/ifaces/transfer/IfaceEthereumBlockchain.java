
package com.everis.everledger.ifaces.transfer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

public interface IfaceEthereumBlockchain {

    public /*receipt*/String sendTransfer(String walletFilePath, String destinationAddress,
                                          BigInteger amount, Convert.Unit transferUnit);

    public BigInteger getWeiBalance(String wallet, Convert.Unit transferUnit);

}
