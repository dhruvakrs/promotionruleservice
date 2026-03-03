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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Parses MMED (legacy) XML promotion files.
 *
 * Supports two formats:
 *  1. Real QA format — root element ns3:mmed with namespace, promotions as ns3:promotion elements,
 *     mmedCode is Base64+gzip+Java-serialized MML string.
 *  2. Simple sample format — root element Promotions, each Promotion has id attribute and
 *     text-content key-value MML (for testing / samples only).
 *
 * ONLY used for legacy import — not involved in regular calculation flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MmedXmlParser {

    private static final String MMED_NS = "http://www.capgemini.com/rm3/common/mmed";

    private final MmlToDrlConverter mmlToDrlConverter;

    public List<Map<String, String>> parseXml(String xmlContent) {
        List<Map<String, String>> promotions = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList ns3Promotions = doc.getElementsByTagNameNS(MMED_NS, "promotion");
            if (ns3Promotions.getLength() > 0) {
                parseNs3Format(ns3Promotions, promotions);
            } else {
                parseSimpleFormat(doc, promotions);
            }
        } catch (Exception e) {
            log.error("Failed to parse MMED XML", e);
            throw new io.promoengine.exception.RuleSetException(99, "Failed to parse MMED XML: " + e.getMessage());
        }
        return promotions;
    }

    // ─── Real MMED format (ns3:promotion elements) ────────────────────────────

    private void parseNs3Format(NodeList nodes, List<Map<String, String>> result) {
        log.info("Parsing real MMED format: {} promotion elements", nodes.getLength());
        int skipped = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);

            String promotionId = getText(el, "promotionID");
            if (promotionId == null || promotionId.isEmpty()) { skipped++; continue; }

            if ("true".equalsIgnoreCase(getText(el, "deletePromotion"))) { skipped++; continue; }

            int promotionType = parseInt(getText(el, "promotionType"), 0);
            int priority      = parseInt(getText(el, "priority"), 10);
            String startDate  = getText(el, "startDate");
            String endDate    = getText(el, "endDate");
            String description = extractDescription(el, promotionId);
            String mmedCode    = getText(el, "mmedCode");

            try {
                String mml = decodeMmedCode(mmedCode);
                String drl = mmlToDrlConverter.convertMmedMml(
                        promotionId, description, promotionType, priority, startDate, endDate, mml);
                result.add(Map.of(
                        "promotionId",   promotionId,
                        "drl",           drl,
                        "mml",           mml.substring(0, Math.min(200, mml.length())),
                        "promotionType", String.valueOf(promotionType),
                        "priority",      String.valueOf(priority)
                ));
            } catch (Exception e) {
                log.warn("Skipping promotion {} (decode/convert failed): {}", promotionId, e.getMessage());
                skipped++;
            }
        }
        log.info("Parsed {} promotions, skipped {}", result.size(), skipped);
    }

    /**
     * Decodes a mmedCode value:
     *   Base64 → gzip decompress → strip Java serialization header → MML text.
     *
     * Java serialization header for a plain String:
     *   0xAC 0xED  — magic
     *   0x00 0x05  — version 5
     *   0x74       — TC_STRING
     *   [2 bytes]  — string length
     *   [N bytes]  — string content (ISO-8859-1)
     */
    private String decodeMmedCode(String mmedCode) throws Exception {
        if (mmedCode == null || mmedCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty mmedCode");
        }
        // Base64 decode (pad to multiple-of-4 just in case)
        String padded = mmedCode.trim();
        int rem = padded.length() % 4;
        if (rem != 0) padded = padded + "=".repeat(4 - rem);
        byte[] gzipped = Base64.getDecoder().decode(padded);

        // Gzip decompress
        byte[] raw;
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gzipped));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gis.transferTo(bos);
            raw = bos.toByteArray();
        }

        // Strip Java serialization header if present
        if (raw.length > 7
                && raw[0] == (byte) 0xAC && raw[1] == (byte) 0xED  // magic
                && raw[4] == 0x74) {                                  // TC_STRING
            int strLen = ((raw[5] & 0xFF) << 8) | (raw[6] & 0xFF);
            int available = raw.length - 7;
            return new String(raw, 7, Math.min(strLen, available), StandardCharsets.ISO_8859_1);
        }
        return new String(raw, StandardCharsets.ISO_8859_1);
    }

    // ─── Simple sample format (<Promotion id="..."> text content) ─────────────

    private void parseSimpleFormat(Document doc, List<Map<String, String>> result) {
        log.info("Parsing simple MMED sample format");
        NodeList promotionNodes = doc.getElementsByTagName("Promotion");
        for (int i = 0; i < promotionNodes.getLength(); i++) {
            Element promoEl = (Element) promotionNodes.item(i);
            String id  = getAttr(promoEl, "id", "PROMO_" + i);
            String mml = promoEl.getTextContent().trim();
            String drl = mmlToDrlConverter.convert(id, mml);
            result.add(Map.of("promotionId", id, "mml", mml, "drl", drl));
        }
    }

    // ─── XML helpers ──────────────────────────────────────────────────────────

    private String extractDescription(Element el, String fallback) {
        NodeList texts = el.getElementsByTagName("promotionText");
        for (int i = 0; i < texts.getLength(); i++) {
            Element textEl = (Element) texts.item(i);
            if ("ENG".equals(getText(textEl, "languageCd"))) {
                String desc = getText(textEl, "description");
                if (!desc.isEmpty()) return desc;
            }
        }
        if (texts.getLength() > 0) {
            String desc = getText((Element) texts.item(0), "description");
            if (!desc.isEmpty()) return desc;
        }
        return fallback;
    }

    private String getText(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent().trim();
    }

    private String getAttr(Element el, String attr, String def) {
        String val = el.getAttribute(attr);
        return (val == null || val.isEmpty()) ? def : val;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s == null ? "" : s.trim()); }
        catch (Exception e) { return def; }
    }
}
