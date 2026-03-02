package io.promoengine.step6;

import com.intuit.karate.junit5.Karate;

class PromotionRuleEngineTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("promotion-rule-engine").relativeTo(getClass());
    }
}
