package com.fpsweeper.harvest.wallet.exceptions;
public class InvalidWalletAddressException extends RuntimeException {
    public InvalidWalletAddressException() {
        super("Invalid Solana wallet address");
    }

    public InvalidWalletAddressException(String message) {
        super(message);
    }
}
