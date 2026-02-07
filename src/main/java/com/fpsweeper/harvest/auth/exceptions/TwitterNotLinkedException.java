package com.fpsweeper.harvest.auth.exceptions;

public class TwitterNotLinkedException extends RuntimeException {
    public TwitterNotLinkedException() {
        super("No Twitter account linked");
    }
}
