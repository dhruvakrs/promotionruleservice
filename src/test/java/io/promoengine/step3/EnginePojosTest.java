package io.promoengine.step3;

import com.intuit.karate.junit5.Karate;

class EnginePojosTest {

    @Karate.Test
    Karate testEnginePojos() {
        return Karate.run("engine-pojos").relativeTo(getClass());
    }
}
