package com.fpsweeper.harvest.auth;

import com.fpsweeper.harvest.wallet.UserWallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO returned by GET /auth/me.
 * Includes simulationCreditLimit so the frontend knows the user's virtual
 * credit ceiling without a separate API call.
 */
public class UserMeDto {

    private final UUID   id;
    private final String email;
    private final String role;
    private final String authProvider;
    private final UserWallet wallet;
    private final BigDecimal simulationCreditLimit;

    public UserMeDto(UUID id, String email, String role, String authProvider, UserWallet wallet) {
        this.id                   = id;
        this.email                = email;
        this.role                 = role;
        this.authProvider         = authProvider;
        this.wallet               = wallet;
        this.simulationCreditLimit = new BigDecimal("1000.00");
    }

    public UserMeDto(UUID id, String email, String role, String authProvider,
                     UserWallet wallet, BigDecimal simulationCreditLimit) {
        this.id                   = id;
        this.email                = email;
        this.role                 = role;
        this.authProvider         = authProvider;
        this.wallet               = wallet;
        this.simulationCreditLimit = simulationCreditLimit != null
                ? simulationCreditLimit
                : new BigDecimal("1000.00");
    }

    public UUID   getId()                   { return id; }
    public String getEmail()                { return email; }
    public String getRole()                 { return role; }
    public String getAuthProvider()         { return authProvider; }
    public UserWallet getWallet()           { return wallet; }
    public BigDecimal getSimulationCreditLimit() { return simulationCreditLimit; }
}