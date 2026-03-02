package io.promoengine.step7;

import com.intuit.karate.junit5.Karate;

class PromotionRuleServiceTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("promotion-rule-service").relativeTo(getClass());
    }
}
