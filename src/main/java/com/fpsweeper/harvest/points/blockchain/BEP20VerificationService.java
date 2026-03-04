package com.fpsweeper.harvest.points.blockchain;

import com.fpsweeper.harvest.points.SupportedChain;
import com.fpsweeper.harvest.points.SupportedChainRepository;
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
public class BEP20VerificationService {

    @Autowired
    private SupportedChainRepository chainRepository;

    @Value("${bep20.rpc.url:https://bsc-dataseed.binance.org}")
    private String rpcUrl;

    @Value("${bep20.usdt.token:0x55d398326f99059fF775485246999027B3197955}")
    private String usdtTokenAddress;

    @Value("${blockchain.min.confirmations.bep20:15}")
    private int minConfirmations;

    private final OkHttpClient httpClient;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public BEP20VerificationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Verify BEP20 USDT transaction
     */
    public TransactionVerificationResult verifyTransaction(
            String txHash,
            BigDecimal expectedAmount,
            String expectedRecipient
    ) {
        System.out.println("🔍 Verifying BEP20 transaction: " + txHash);

        try {
            // Get transaction receipt
            JsonObject receipt = getTransactionReceipt(txHash);

            if (receipt == null) {
                return TransactionVerificationResult.notFound(
                        "Transaction not found on BEP20 (BSC)"
                );
            }

            // Check transaction status
            String status = receipt.get("status").getAsString();
            if (!"0x1".equals(status)) {
                return TransactionVerificationResult.failed(
                        "Transaction failed on blockchain"
                );
            }

            // Get current block number
            long currentBlock = getCurrentBlockNumber();
            long txBlock = Long.parseLong(
                    receipt.get("blockNumber").getAsString().substring(2), 16
            );
            int confirmations = (int) (currentBlock - txBlock);

            // Parse logs to find Transfer event
            JsonArray logs = receipt.getAsJsonArray("logs");

            String fromAddress = null;
            String toAddress = null;
            BigDecimal amount = null;

            // Transfer event signature: 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
            String transferEventSignature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

            for (int i = 0; i < logs.size(); i++) {
                JsonObject log = logs.get(i).getAsJsonObject();

                // Check if this is a Transfer event from USDT contract
                String logAddress = log.get("address").getAsString().toLowerCase();
                if (!logAddress.equals(usdtTokenAddress.toLowerCase())) {
                    continue;
                }

                JsonArray topics = log.getAsJsonArray("topics");
                if (topics.size() < 3) continue;

                String eventSig = topics.get(0).getAsString();
                if (!eventSig.equals(transferEventSignature)) continue;

                // Parse Transfer event
                // topics[0] = event signature
                // topics[1] = from address (padded)
                // topics[2] = to address (padded)
                // data = amount

                fromAddress = "0x" + topics.get(1).getAsString().substring(26);
                toAddress = "0x" + topics.get(2).getAsString().substring(26);

                String data = log.get("data").getAsString();
                BigInteger amountWei = new BigInteger(data.substring(2), 16);

                // USDT on BSC has 18 decimals
                amount = new BigDecimal(amountWei).divide(new BigDecimal("1000000000000000000"));

                break; // Found the transfer event
            }

            if (amount == null) {
                return TransactionVerificationResult.failed(
                        "No USDT transfer found in transaction"
                );
            }

            // Validate recipient
            if (expectedRecipient != null) {
                SupportedChain chain = chainRepository.findByChainName("BEP20")
                        .orElseThrow(() -> new RuntimeException("BEP20 chain not configured"));

                String platformWallet = chain.getPlatformWalletAddress().toLowerCase();
                if (!toAddress.toLowerCase().equals(platformWallet)) {
                    return TransactionVerificationResult.failed(
                            "Transaction sent to wrong address. Expected: " + platformWallet +
                                    ", Got: " + toAddress
                    );
                }
            }

            // Validate amount (allow 1% tolerance for rounding)
            BigDecimal tolerance = new BigDecimal("0.01");  // Only 1 cent tolerance for decimals
            BigDecimal difference = amount.subtract(expectedAmount).abs();

            if (difference.compareTo(tolerance) > 0) {
                return TransactionVerificationResult.failed(
                        "Exact amount mismatch. Expected: $" + expectedAmount + ", Got: $" + amount
                );
            }

            // Check confirmations
            if (confirmations < minConfirmations) {
                System.out.println("⏳ BEP20 transaction pending: " + confirmations + "/" + minConfirmations + " confirmations");

                return TransactionVerificationResult.pending(
                        confirmations,
                        minConfirmations,
                        txBlock,
                        fromAddress,
                        amount
                );
            }

            // Fully verified!
            System.out.println("✅ BEP20 transaction verified: " + txHash);
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
            System.err.println("❌ Error verifying BEP20 transaction: " + e.getMessage());
            e.printStackTrace();
            return TransactionVerificationResult.failed(
                    "Verification error: " + e.getMessage()
            );
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
                throw new IOException("RPC request failed: " + response.code());
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
    private long getCurrentBlockNumber() throws IOException {
        String jsonPayload = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_blockNumber\",\"params\":[]}";

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("RPC request failed: " + response.code());
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            String blockNumberHex = jsonResponse.get("result").getAsString();
            return Long.parseLong(blockNumberHex.substring(2), 16);
        }
    }
}