package io.promoengine.step16;

import com.intuit.karate.junit5.Karate;

class PromoEngineE2ETest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("e2e-suite").relativeTo(getClass());
    }

    @Karate.Test
    Karate runAllFeatures() {
        return Karate.run("classpath:io/promoengine").tags("~@ignore");
    }
}
