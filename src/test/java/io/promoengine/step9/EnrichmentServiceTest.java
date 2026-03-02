package io.promoengine.step9;

import com.intuit.karate.junit5.Karate;

class EnrichmentServiceTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("enrichment-service").relativeTo(getClass());
    }
}
