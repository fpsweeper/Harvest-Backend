package com.fpsweeper.harvest.wallet.exceptions;
public class WalletAlreadyLinkedException extends RuntimeException {
    public WalletAlreadyLinkedException() {
        super("This wallet is already linked to another account");
    }

    public WalletAlreadyLinkedException(String message) {
        super(message);
    }
}