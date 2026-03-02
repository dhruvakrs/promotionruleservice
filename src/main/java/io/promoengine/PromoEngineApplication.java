package io.promoengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(PromoEngineProperties.class)
@EnableScheduling
public class PromoEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromoEngineApplication.class, args);
    }
}
