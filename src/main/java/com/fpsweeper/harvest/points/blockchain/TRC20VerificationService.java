package com.fpsweeper.harvest.points.blockchain;

import com.fpsweeper.harvest.points.SupportedChain;
import com.fpsweeper.harvest.points.SupportedChainRepository;
import com.fpsweeper.harvest.points.blockchain.utils.TronAddressUtils;  // ✅ ADD THIS
import com.fpsweeper.harvest.points.dto.TransactionVerificationResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@Service
public class TRC20VerificationService {

    @Autowired
    private SupportedChainRepository chainRepository;

    @Value("${trc20.rpc.url:https://api.trongrid.io}")
    private String rpcUrl;

    @Value("${trc20.usdt.token:TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t}")
    private String usdtTokenAddress;

    @Value("${blockchain.min.confirmations.trc20:19}")
    private int minConfirmations;

    private final OkHttpClient httpClient;

    public TRC20VerificationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Verify TRC20 USDT transaction
     */
    public TransactionVerificationResult verifyTransaction(
            String txHash,
            BigDecimal expectedAmount,
            String expectedRecipient
    ) {
        System.out.println("🔍 Verifying TRC20 transaction: " + txHash);

        try {
            // Get transaction info
            JsonObject txInfo = getTransactionInfo(txHash);

            if (txInfo == null || !txInfo.has("id")) {
                return TransactionVerificationResult.notFound(
                        "Transaction not found on TRC20 (Tron)"
                );
            }

            // Check transaction result
            if (txInfo.has("receipt")) {
                JsonObject receipt = txInfo.getAsJsonObject("receipt");
                String result = receipt.get("result").getAsString();

                if (!"SUCCESS".equals(result)) {
                    return TransactionVerificationResult.failed(
                            "Transaction failed on blockchain: " + result
                    );
                }
            }

            // Get confirmations
            long currentBlock = getCurrentBlockNumber();
            long txBlock = txInfo.get("blockNumber").getAsLong();
            int confirmations = (int) (currentBlock - txBlock);

            // Parse transaction
            String fromAddress = null;
            String toAddress = null;
            BigDecimal amount = null;

            JsonObject transaction = getTransaction(txHash);

            if (transaction != null && transaction.has("raw_data")) {
                JsonObject rawData = transaction.getAsJsonObject("raw_data");

                if (rawData.has("contract")) {
                    JsonArray contracts = rawData.getAsJsonArray("contract");

                    for (int i = 0; i < contracts.size(); i++) {
                        JsonObject contract = contracts.get(i).getAsJsonObject();
                        String type = contract.get("type").getAsString();

                        if ("TriggerSmartContract".equals(type)) {
                            JsonObject parameter = contract.getAsJsonObject("parameter");
                            JsonObject value = parameter.getAsJsonObject("value");

                            // Check contract address
                            String contractAddress = value.get("contract_address").getAsString();
                            String contractAddressBase58 = TronAddressUtils.hexToBase58(contractAddress);  // ✅ UPDATED

                            if (!contractAddressBase58.equals(usdtTokenAddress)) {
                                continue;
                            }

                            // Parse transfer data
                            String data = value.get("data").getAsString();

                            if (!data.startsWith("a9059cbb")) {
                                continue;
                            }

                            // Parse parameters
                            String toHex = data.substring(32, 72);
                            toAddress = TronAddressUtils.hexToBase58(toHex);  // ✅ UPDATED

                            String amountHex = data.substring(72);
                            BigInteger amountRaw = new BigInteger(amountHex, 16);

                            // USDT on Tron has 6 decimals
                            amount = new BigDecimal(amountRaw).divide(new BigDecimal("1000000"));

                            // Get sender
                            fromAddress = TronAddressUtils.hexToBase58(value.get("owner_address").getAsString());  // ✅ UPDATED

                            break;
                        }
                    }
                }
            }

            if (amount == null) {
                return TransactionVerificationResult.failed(
                        "No USDT transfer found in transaction"
                );
            }

            // Validate recipient
            if (expectedRecipient != null) {
                SupportedChain chain = chainRepository.findByChainName("TRC20")
                        .orElseThrow(() -> new RuntimeException("TRC20 chain not configured"));

                String platformWallet = chain.getPlatformWalletAddress();
                if (!toAddress.equals(platformWallet)) {
                    return TransactionVerificationResult.failed(
                            "Transaction sent to wrong address. Expected: " + platformWallet +
                                    ", Got: " + toAddress
                    );
                }
            }

            // Validate amount (1% tolerance)
            BigDecimal tolerance = new BigDecimal("0.01");  // Only 1 cent tolerance for decimals
            BigDecimal difference = amount.subtract(expectedAmount).abs();

            if (difference.compareTo(tolerance) > 0) {
                return TransactionVerificationResult.failed(
                        "Exact amount mismatch. Expected: $" + expectedAmount + ", Got: $" + amount
                );
            }

            // Check confirmations
            if (confirmations < minConfirmations) {
                System.out.println("⏳ TRC20 transaction pending: " + confirmations + "/" + minConfirmations + " confirmations");

                return TransactionVerificationResult.pending(
                        confirmations,
                        minConfirmations,
                        txBlock,
                        fromAddress,
                        amount
                );
            }

            // Fully verified!
            System.out.println("✅ TRC20 transaction verified: " + txHash);
            System.out.println("   From: " + fromAddress);
            System.out.println("   To: " + toAddress);
            System.out.println("   Amount: $" + amount);
            System.out.println("   Confirmations: " + confirmations);

            return TransactionVerificationResult.success(
                    confirmations,
                    txBlock,
                    fromAddress,
                    amount
            );

        } catch (Exception e) {
            System.err.println("❌ Error verifying TRC20 transaction: " + e.getMessage());
            e.printStackTrace();
            return TransactionVerificationResult.failed(
                    "Verification error: " + e.getMessage()
            );
        }
    }

    private JsonObject getTransactionInfo(String txHash) throws IOException {
        String url = rpcUrl + "/wallet/gettransactioninfobyid?value=" + txHash;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("TronGrid request failed: " + response.code());
            }

            String responseBody = response.body().string();
            return JsonParser.parseString(responseBody).getAsJsonObject();
        }
    }

    private JsonObject getTransaction(String txHash) throws IOException {
        String url = rpcUrl + "/wallet/gettransactionbyid?value=" + txHash;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("TronGrid request failed: " + response.code());
            }

            String responseBody = response.body().string();
            return JsonParser.parseString(responseBody).getAsJsonObject();
        }
    }

    private long getCurrentBlockNumber() throws IOException {
        String url = rpcUrl + "/wallet/getnowblock";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("TronGrid request failed: " + response.code());
            }

            String responseBody = response.body().string();
            JsonObject block = JsonParser.parseString(responseBody).getAsJsonObject();

            if (block.has("block_header")) {
                JsonObject header = block.getAsJsonObject("block_header");
                JsonObject rawData = header.getAsJsonObject("raw_data");
                return rawData.get("number").getAsLong();
            }

            return 0;
        }
    }
}