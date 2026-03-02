package io.promoengine.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
public class EngineConfig {

    private final TenantEngineRegistry registry;

    public EngineConfig(TenantEngineRegistry registry) {
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("PromoEngine ready. TenantEngineRegistry initialized.");
    }
}
