package io.promoengine.step12;

import com.intuit.karate.junit5.Karate;

class PromotionServiceTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("promotion-service").relativeTo(getClass());
    }
}
