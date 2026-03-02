package io.promoengine.step15;

import com.intuit.karate.junit5.Karate;

class ExceptionHandlerTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("exception-handler").relativeTo(getClass());
    }
}
