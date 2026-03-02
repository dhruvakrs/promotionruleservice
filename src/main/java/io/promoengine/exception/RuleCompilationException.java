package io.promoengine.exception;

public class RuleCompilationException extends RuntimeException {

    private final String tenantId;
    private final String drlErrors;

    public RuleCompilationException(String tenantId, String drlErrors) {
        super("DRL compilation failed for tenant [" + tenantId + "]: " + drlErrors);
        this.tenantId = tenantId;
        this.drlErrors = drlErrors;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDrlErrors() {
        return drlErrors;
    }
}
