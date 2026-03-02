package io.promoengine.step14;

import com.intuit.karate.junit5.Karate;

class SecurityApiTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("security-api").relativeTo(getClass());
    }
}
