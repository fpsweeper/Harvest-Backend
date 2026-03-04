package com.fpsweeper.harvest.points;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChainService {

    @Autowired
    private SupportedChainRepository chainRepository;

    /**
     * Get all active chains
     */
    public List<SupportedChain> getActiveChains() {
        return chainRepository.findByIsActive(true);
    }

    /**
     * Get chain by name
     */
    public Optional<SupportedChain> getChainByName(String chainName) {
        return chainRepository.findByChainName(chainName.toUpperCase());
    }

    /**
     * Check if chain is supported and active
     */
    public boolean isChainSupported(String chainName) {
        return chainRepository.findByChainNameAndIsActive(chainName.toUpperCase(), true)
                .isPresent();
    }

    /**
     * Get platform wallet address for chain
     */
    public Optional<String> getPlatformWallet(String chainName) {
        return chainRepository.findByChainNameAndIsActive(chainName.toUpperCase(), true)
                .map(SupportedChain::getPlatformWalletAddress);
    }
}