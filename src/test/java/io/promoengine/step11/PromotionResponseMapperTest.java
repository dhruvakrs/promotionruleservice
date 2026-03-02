package io.promoengine.step11;

import com.intuit.karate.junit5.Karate;

class PromotionResponseMapperTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("promotion-response-mapper").relativeTo(getClass());
    }
}
