package io.promoengine;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "promoengine")
public class PromoEngineProperties {

    private Engine engine = new Engine();
    private Rules rules = new Rules();
    private Tenancy tenancy = new Tenancy();
    private Security security = new Security();
    private S3 s3 = new S3();
    private Data data = new Data();

    @lombok.Data
    public static class Engine {
        private int debugLevel = 0;
        private int nbrDecimals = 3;
    }

    @lombok.Data
    public static class Rules {
        private String s3Bucket = "";
        private String s3Key = "ruleset/ruleset.xml";
        private String expiryCron = "0 0 2 * * *";
        private long compileTimeoutMs = 10000;
    }

    @lombok.Data
    public static class Tenancy {
        private String defaultTenantId = "default";
        private boolean enabled = true;
    }

    @lombok.Data
    public static class Security {
        private String apiKey = "change-me";
        private String headerName = "X-API-Key";
        private java.util.Map<String, String> tenants = new java.util.HashMap<>();
    }

    @lombok.Data
    public static class S3 {
        private String region = "ap-southeast-1";
    }

    @lombok.Data
    public static class Data {
        private String priceIndex = "promoprice";
        private String itemIndex = "promoitem";
        private String customerIndex = "promocustomer";
    }
}
