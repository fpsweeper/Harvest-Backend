package com.fpsweeper.harvest.wallet.exceptions;
public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException() {
        super("No wallet linked to this account");
    }

    public WalletNotFoundException(String message) {
        super(message);
    }
}
