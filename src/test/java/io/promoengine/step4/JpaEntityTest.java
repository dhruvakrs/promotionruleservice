package io.promoengine.step4;

import com.intuit.karate.junit5.Karate;

class JpaEntityTest {

    @Karate.Test
    Karate testJpaEntities() {
        return Karate.run("jpa-entities").relativeTo(getClass());
    }
}
