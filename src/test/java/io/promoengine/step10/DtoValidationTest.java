package io.promoengine.step10;

import com.intuit.karate.junit5.Karate;

class DtoValidationTest {

    @Karate.Test
    Karate testAll() {
        return Karate.run("dto-validation").relativeTo(getClass());
    }
}
