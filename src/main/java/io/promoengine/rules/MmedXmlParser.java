package io.promoengine.rules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses MMED (legacy) XML promotion files.
 * ONLY used for legacy import — not involved in regular calculation flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MmedXmlParser {

    private final MmlToDrlConverter mmlToDrlConverter;

    public List<Map<String, String>> parseXml(String xmlContent) {
        List<Map<String, String>> promotions = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList promotionNodes = doc.getElementsByTagName("Promotion");
            for (int i = 0; i < promotionNodes.getLength(); i++) {
                Element promoEl = (Element) promotionNodes.item(i);
                String id = getAttr(promoEl, "id", "PROMO_" + i);
                String mml = promoEl.getTextContent().trim();
                String drl = mmlToDrlConverter.convert(id, mml);
                promotions.add(Map.of("promotionId", id, "mml", mml, "drl", drl));
            }
        } catch (Exception e) {
            log.error("Failed to parse MMED XML", e);
            throw new io.promoengine.exception.RuleSetException(99, "Failed to parse MMED XML: " + e.getMessage());
        }
        return promotions;
    }

    private String getAttr(Element el, String attr, String def) {
        String val = el.getAttribute(attr);
        return (val == null || val.isEmpty()) ? def : val;
    }
}
