package com.fpsweeper.harvest.auth.exceptions;

public class TwitterAlreadyLinkedException extends RuntimeException {
    public TwitterAlreadyLinkedException() {
        super("This Twitter account is already linked to another user");
    }

    public TwitterAlreadyLinkedException(String message) {
        super(message);
    }
}