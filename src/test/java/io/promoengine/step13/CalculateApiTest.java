package io.promoengine.step13;

import com.intuit.karate.junit5.Karate;

class CalculateApiTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("calculate-api", "rules-api").relativeTo(getClass());
    }
}
