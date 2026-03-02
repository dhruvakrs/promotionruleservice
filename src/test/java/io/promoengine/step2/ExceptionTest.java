package io.promoengine.step2;

import com.intuit.karate.junit5.Karate;

class ExceptionTest {

    @Karate.Test
    Karate testExceptions() {
        return Karate.run("exceptions").relativeTo(getClass());
    }
}
