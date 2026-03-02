package io.promoengine.step5;

import com.intuit.karate.junit5.Karate;

class OpenSearchDocumentTest {

    @Karate.Test
    Karate testOpenSearchDocuments() {
        return Karate.run("opensearch-documents").relativeTo(getClass());
    }
}
