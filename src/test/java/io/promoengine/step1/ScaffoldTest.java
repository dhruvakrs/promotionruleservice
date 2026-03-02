package io.promoengine.step1;

import com.intuit.karate.junit5.Karate;

class ScaffoldTest {

    @Karate.Test
    Karate testScaffold() {
        return Karate.run("scaffold").relativeTo(getClass());
    }
}
