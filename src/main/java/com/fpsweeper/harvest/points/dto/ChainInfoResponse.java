package com.fpsweeper.harvest.points.dto;

import java.math.BigDecimal;

public class ChainInfoResponse {

    private String chainName;
    private String platformWallet;
    private BigDecimal minDepositUsd;
    private String usdcTokenAddress;

    // Constructors
    public ChainInfoResponse() {}

    public ChainInfoResponse(String chainName, String platformWallet,
                             BigDecimal minDepositUsd, String usdcTokenAddress) {
        this.chainName = chainName;
        this.platformWallet = platformWallet;
        this.minDepositUsd = minDepositUsd;
        this.usdcTokenAddress = usdcTokenAddress;
    }

    // Getters and Setters
    public String getChainName() { return chainName; }
    public void setChainName(String chainName) { this.chainName = chainName; }

    public String getPlatformWallet() { return platformWallet; }
    public void setPlatformWallet(String platformWallet) {
        this.platformWallet = platformWallet;
    }

    public BigDecimal getMinDepositUsd() { return minDepositUsd; }
    public void setMinDepositUsd(BigDecimal minDepositUsd) {
        this.minDepositUsd = minDepositUsd;
    }

    public String getUsdcTokenAddress() { return usdcTokenAddress; }
    public void setUsdcTokenAddress(String usdcTokenAddress) {
        this.usdcTokenAddress = usdcTokenAddress;
    }
}