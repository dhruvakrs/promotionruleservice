package io.promoengine.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String apiKey) {
        super("No tenant found for API key: " + apiKey);
    }
}
