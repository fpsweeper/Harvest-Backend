package com.fpsweeper.harvest.points.blockchain;

import com.fpsweeper.harvest.points.dto.TransactionVerificationResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class SolanaVerificationService {

    @Value("${solana.rpc.url}")
    private String rpcUrl;

    @Value("${solana.platform.wallet}")
    private String platformWallet;

    @Value("${solana.usdc.token}")
    private String usdcTokenAddress;

    @Value("${blockchain.min.confirmations.solana:32}")
    private int minConfirmations;

    private final OkHttpClient httpClient;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public SolanaVerificationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Verify a Solana transaction
     */
    /**
     * Verify a Solana transaction
     */
    public TransactionVerificationResult verifyTransaction(
            String signature,
            BigDecimal expectedAmount,
            String expectedRecipient
    ) {
        try {
            System.out.println("🔍 Verifying Solana transaction: " + signature);

            // Fetch transaction from Solana
            JsonObject txData = getTransaction(signature);

            if (txData == null) {
                return TransactionVerificationResult.notFound("Transaction not found on Solana");
            }

            // Check if transaction succeeded
            JsonObject meta = txData.getAsJsonObject("meta");
            if (meta == null || !meta.get("err").isJsonNull()) {
                return TransactionVerificationResult.failed("Transaction failed on-chain");
            }

            // Get block information
            Long blockNumber = txData.has("slot") ? txData.get("slot").getAsLong() : null;

            // Get current slot to calculate confirmations
            Long currentSlot = getCurrentSlot();
            Integer confirmations = (blockNumber != null && currentSlot != null)
                    ? (int)(currentSlot - blockNumber)
                    : 0;

            // Parse transaction to find USDC transfer
            JsonObject transaction = txData.getAsJsonObject("transaction");
            JsonObject message = transaction.getAsJsonObject("message");
            JsonArray accountKeys = message.getAsJsonArray("accountKeys");
            JsonArray instructions = message.getAsJsonArray("instructions");

            // Find SPL token transfer instruction
            String fromAddress = null;
            String toAddress = null;
            BigDecimal amount = null;

            // Try parsed instructions first
            for (JsonElement instructionElement : instructions) {
                JsonObject instruction = instructionElement.getAsJsonObject();

                if (!instruction.has("parsed")) {
                    continue;
                }

                JsonObject parsed = instruction.getAsJsonObject("parsed");

                // Check for transfer or transferChecked
                if (!parsed.has("type")) continue;

                String type = parsed.get("type").getAsString();
                if (!type.equals("transfer") && !type.equals("transferChecked")) {
                    continue;
                }

                JsonObject info = parsed.getAsJsonObject("info");

                // Extract source and destination
                String source = info.has("source") ? info.get("source").getAsString() : null;
                String destination = info.has("destination") ? info.get("destination").getAsString() : null;

                // Get amount
                if (info.has("amount")) {
                    long rawAmount = Long.parseLong(info.get("amount").getAsString());
                    amount = new BigDecimal(rawAmount).divide(new BigDecimal("1000000"));
                } else if (info.has("tokenAmount")) {
                    JsonObject tokenAmount = info.getAsJsonObject("tokenAmount");
                    amount = new BigDecimal(tokenAmount.get("uiAmountString").getAsString());
                }

                // For SPL tokens, we need to find the OWNER of the destination token account
                // Check postTokenBalances to find the actual wallet owners
                if (meta.has("postTokenBalances")) {
                    JsonArray postBalances = meta.getAsJsonArray("postTokenBalances");

                    for (JsonElement balanceElement : postBalances) {
                        JsonObject balance = balanceElement.getAsJsonObject();
                        String account = balance.get("accountIndex").getAsString();

                        // Find the account that matches our destination
                        int accountIndex = Integer.parseInt(account);
                        String accountAddress = accountKeys.get(accountIndex).getAsJsonObject().get("pubkey").getAsString();

                        if (accountAddress.equals(destination)) {
                            // This is our destination token account
                            // Get the owner (the actual wallet)
                            if (balance.has("owner")) {
                                toAddress = balance.get("owner").getAsString();
                            }
                        } else if (accountAddress.equals(source)) {
                            // This is our source token account
                            if (balance.has("owner")) {
                                fromAddress = balance.get("owner").getAsString();
                            }
                        }
                    }
                }

                // If we found both addresses and amount, break
                if (toAddress != null && fromAddress != null && amount != null) {
                    break;
                }
            }

            // Fallback: try to get addresses from preTokenBalances and postTokenBalances
            if ((fromAddress == null || toAddress == null) && meta.has("preTokenBalances") && meta.has("postTokenBalances")) {
                JsonArray preBalances = meta.getAsJsonArray("preTokenBalances");
                JsonArray postBalances = meta.getAsJsonArray("postTokenBalances");

                // Find accounts with balance changes
                for (int i = 0; i < preBalances.size(); i++) {
                    JsonObject preBal = preBalances.get(i).getAsJsonObject();

                    for (int j = 0; j < postBalances.size(); j++) {
                        JsonObject postBal = postBalances.get(j).getAsJsonObject();

                        if (preBal.get("accountIndex").getAsInt() == postBal.get("accountIndex").getAsInt()) {
                            long preAmount = Long.parseLong(preBal.getAsJsonObject("uiTokenAmount").get("amount").getAsString());
                            long postAmount = Long.parseLong(postBal.getAsJsonObject("uiTokenAmount").get("amount").getAsString());

                            if (postAmount > preAmount) {
                                // This account received tokens
                                toAddress = postBal.has("owner") ? postBal.get("owner").getAsString() : null;

                                if (amount == null) {
                                    long diff = postAmount - preAmount;
                                    amount = new BigDecimal(diff).divide(new BigDecimal("1000000"));
                                }
                            } else if (preAmount > postAmount) {
                                // This account sent tokens
                                fromAddress = preBal.has("owner") ? preBal.get("owner").getAsString() : null;
                            }
                        }
                    }
                }
            }

            System.out.println("🔍 Parsed transaction:");
            System.out.println("   From: " + fromAddress);
            System.out.println("   To: " + toAddress);
            System.out.println("   Amount: $" + amount);

            // Verify recipient
            String targetRecipient = expectedRecipient != null ? expectedRecipient : platformWallet;
            if (toAddress == null || !toAddress.equals(targetRecipient)) {
                return TransactionVerificationResult.failed(
                        "Transaction recipient is not expected wallet. Expected: " + targetRecipient + ", Got: " + toAddress
                );
            }

            // Verify amount
            if (amount == null) {
                return TransactionVerificationResult.failed("Could not extract amount from transaction");
            }

            // Allow 0.01 tolerance for amount comparison
            BigDecimal tolerance = new BigDecimal("0.01");
            BigDecimal difference = amount.subtract(expectedAmount).abs();

            if (difference.compareTo(tolerance) > 0) {
                return TransactionVerificationResult.failed(
                        "Exact amount mismatch. Expected: $" + expectedAmount + ", Got: $" + amount
                );
            }

            // Check confirmations
            if (confirmations < minConfirmations) {
                System.out.println("⏳ Solana transaction pending: " + confirmations + "/" + minConfirmations + " confirmations");

                return TransactionVerificationResult.pending(
                        confirmations,
                        minConfirmations,
                        blockNumber,
                        fromAddress,
                        amount
                );
            }

            // Fully verified!
            System.out.println("✅ Solana transaction verified: " + signature);
            System.out.println("   From: " + fromAddress);
            System.out.println("   To: " + toAddress);
            System.out.println("   Amount: $" + amount);
            System.out.println("   Confirmations: " + confirmations);

            return TransactionVerificationResult.success(
                    confirmations,
                    blockNumber,
                    fromAddress,
                    amount
            );

        } catch (Exception e) {
            System.err.println("❌ Error verifying Solana transaction: " + e.getMessage());
            e.printStackTrace();
            return TransactionVerificationResult.failed("Error verifying transaction: " + e.getMessage());
        }
    }

    /**
     * Get transaction from Solana RPC
     */
    private JsonObject getTransaction(String signature) throws IOException {
        String jsonPayload = String.format(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getTransaction\",\"params\":[\"%s\",{\"encoding\":\"jsonParsed\",\"maxSupportedTransactionVersion\":0}]}",
                signature
        );

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
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
     * Get current slot (for calculating confirmations)
     */
    private Long getCurrentSlot() throws IOException {
        String jsonPayload = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getSlot\"}";

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get current slot");
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("result")) {
                return jsonResponse.get("result").getAsLong();
            }

            return 0L;
        }
    }
}