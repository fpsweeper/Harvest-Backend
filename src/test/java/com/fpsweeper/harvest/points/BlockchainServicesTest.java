package com.fpsweeper.harvest.points;

import com.fpsweeper.harvest.points.blockchain.SolanaVerificationService;
import com.fpsweeper.harvest.points.blockchain.ArbitrumVerificationService;
import com.fpsweeper.harvest.points.dto.TransactionVerificationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
public class BlockchainServicesTest {

    @Autowired
    private SolanaVerificationService solanaService;

    @Autowired
    private ArbitrumVerificationService arbitrumService;

    @Test
    public void testServicesLoaded() {
        // Just verify the services can be autowired
        assert solanaService != null;
        assert arbitrumService != null;
        System.out.println("✅ Blockchain services loaded successfully");
    }
}