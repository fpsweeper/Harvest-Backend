package com.fpsweeper.harvest.points.blockchain;

import com.fpsweeper.harvest.points.dto.TransactionVerificationResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@Service
public class ArbitrumVerificationService {

    @Value("${arbitrum.rpc.url}")
    private String rpcUrl;

    @Value("${arbitrum.platform.wallet}")
    private String platformWallet;

    @Value("${arbitrum.usdc.token}")
    private String usdcTokenAddress;

    @Value("${blockchain.min.confirmations.arbitrum:12}")
    private int minConfirmations;

    private final OkHttpClient httpClient;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ArbitrumVerificationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Verify an Arbitrum transaction
     */
    public TransactionVerificationResult verifyTransaction(
            String txHash,
            BigDecimal expectedAmount,
            String expectedRecipient
    ) {
        try {
            System.out.println("🔍 Verifying Arbitrum transaction: " + txHash);

            // Get transaction receipt
            JsonObject receipt = getTransactionReceipt(txHash);

            if (receipt == null) {
                return TransactionVerificationResult.notFound("Transaction not found on Arbitrum");  // ✅ UPDATED
            }

            // Check if transaction succeeded (status = 0x1)
            String status = receipt.get("status").getAsString();
            if (!status.equals("0x1")) {
                return TransactionVerificationResult.failed("Transaction failed on-chain");  // ✅ UPDATED
            }

            // Get block number
            String blockNumberHex = receipt.get("blockNumber").getAsString();
            long blockNumber = Long.parseLong(blockNumberHex.substring(2), 16);

            // Get current block number
            Long currentBlock = getCurrentBlockNumber();
            int confirmations = (int) (currentBlock - blockNumber);

            // Parse logs to find USDC transfer
            JsonArray logs = receipt.getAsJsonArray("logs");
            String fromAddress = null;
            String toAddress = null;
            BigDecimal amount = null;

            // ERC20 Transfer event signature
            String transferEventSignature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

            for (int i = 0; i < logs.size(); i++) {
                JsonObject log = logs.get(i).getAsJsonObject();

                // Check if this is from USDC contract
                String contractAddress = log.get("address").getAsString();
                if (!contractAddress.equalsIgnoreCase(usdcTokenAddress)) {
                    continue;
                }

                JsonArray topics = log.getAsJsonArray("topics");
                if (topics.size() == 0 || !topics.get(0).getAsString().equals(transferEventSignature)) {
                    continue;
                }

                // Extract from and to addresses
                if (topics.size() >= 3) {
                    fromAddress = "0x" + topics.get(1).getAsString().substring(26);
                    toAddress = "0x" + topics.get(2).getAsString().substring(26);
                }

                // Extract amount
                String data = log.get("data").getAsString();
                if (!data.equals("0x")) {
                    BigInteger rawAmount = new BigInteger(data.substring(2), 16);
                    amount = new BigDecimal(rawAmount).divide(new BigDecimal("1000000"));
                }

                // Check if this is to our target wallet
                String targetRecipient = expectedRecipient != null ? expectedRecipient : platformWallet;
                if (toAddress != null && toAddress.equalsIgnoreCase(targetRecipient)) {
                    break;
                }
            }

            // Verify recipient
            String targetRecipient = expectedRecipient != null ? expectedRecipient : platformWallet;
            if (toAddress == null || !toAddress.equalsIgnoreCase(targetRecipient)) {
                return TransactionVerificationResult.failed(  // ✅ UPDATED
                        "Transaction recipient is not expected wallet. Expected: " + targetRecipient + ", Got: " + toAddress
                );
            }

            // Verify amount
            if (amount == null) {
                return TransactionVerificationResult.failed("Could not extract amount from transaction");  // ✅ UPDATED
            }

            // Allow 1% tolerance for amount comparison
            BigDecimal tolerance = new BigDecimal("0.01");  // Only 1 cent tolerance for decimals
            BigDecimal difference = amount.subtract(expectedAmount).abs();

            if (difference.compareTo(tolerance) > 0) {
                return TransactionVerificationResult.failed(
                        "Exact amount mismatch. Expected: $" + expectedAmount + ", Got: $" + amount
                );
            }

            // Check confirmations
            if (confirmations < minConfirmations) {
                System.out.println("⏳ Arbitrum transaction pending: " + confirmations + "/" + minConfirmations + " confirmations");

                return TransactionVerificationResult.pending(  // ✅ UPDATED
                        confirmations,
                        minConfirmations,
                        blockNumber,
                        fromAddress,
                        amount
                );
            }

            // Fully verified!
            System.out.println("✅ Arbitrum transaction verified: " + txHash);
            System.out.println("   From: " + fromAddress);
            System.out.println("   To: " + toAddress);
            System.out.println("   Amount: $" + amount);
            System.out.println("   Confirmations: " + confirmations);

            return TransactionVerificationResult.success(  // ✅ UPDATED
                    confirmations,
                    blockNumber,
                    fromAddress,
                    amount
            );

        } catch (Exception e) {
            System.err.println("❌ Error verifying Arbitrum transaction: " + e.getMessage());
            e.printStackTrace();
            return TransactionVerificationResult.failed("Error: " + e.getMessage());  // ✅ UPDATED
        }
    }

    /**
     * Get transaction receipt via RPC
     */
    private JsonObject getTransactionReceipt(String txHash) throws IOException {
        String jsonPayload = String.format(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getTransactionReceipt\",\"params\":[\"%s\"]}",
                txHash
        );

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("RPC call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("result") && !jsonResponse.get("result").isJsonNull()) {
                return jsonResponse.getAsJsonObject("result");
            }

            return null;
        }
    }

    /**
     * Get current block number
     */
    private Long getCurrentBlockNumber() throws IOException {
        String jsonPayload = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_blockNumber\",\"params\":[]}";

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get block number");
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("result")) {
                String blockHex = jsonResponse.get("result").getAsString();
                return Long.parseLong(blockHex.substring(2), 16);
            }

            return 0L;
        }
    }
}