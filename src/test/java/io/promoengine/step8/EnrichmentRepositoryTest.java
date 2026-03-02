package io.promoengine.step8;

import com.intuit.karate.junit5.Karate;

class EnrichmentRepositoryTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("enrichment-repository").relativeTo(getClass());
    }
}
