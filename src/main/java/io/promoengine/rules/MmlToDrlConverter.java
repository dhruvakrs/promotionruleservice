package io.promoengine.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Converts MML (Marketing Markup Language) — legacy format — to Drools Rule Language (DRL).
 * Used ONLY for legacy MML import (MmedXmlParser). All new rules are authored directly in DRL.
 *
 * MML format supported:
 *   PROMOTION: <id>
 *   DESCRIPTION: <text>
 *   CONDITION: CATEGORY=<code>
 *   DISCOUNT: <amount>
 *   DISCOUNT_TYPE: FIXED|PCT
 *   PRIORITY: <n>
 */
@Slf4j
@Component
public class MmlToDrlConverter {

    public String convert(String promotionId, String mml) {
        String description = extractValue(mml, "DESCRIPTION", promotionId);
        String condition = extractValue(mml, "CONDITION", "");
        String discountStr = extractValue(mml, "DISCOUNT", "0");
        String discountType = extractValue(mml, "DISCOUNT_TYPE", "FIXED");
        String priorityStr = extractValue(mml, "PRIORITY", "10");

        String whenClause = buildWhenClause(condition);
        String discountExpr = buildDiscountExpr(discountStr, discountType);
        int priority = parseInt(priorityStr, 10);

        return String.format(
                "rule \"%s\"%n" +
                "  salience %d%n" +
                "  when%n" +
                "    %s%n" +
                "  then%n" +
                "    results.add(io.promoengine.engine.model.PromotionResult.builder()" +
                ".promotionId(\"%s\").promotionType(0).description(\"%s\")" +
                ".discountAmount(%s).timesApplied(1.0).build());%n" +
                "end",
                promotionId, priority, whenClause, promotionId, description, discountExpr
        );
    }

    private String buildWhenClause(String condition) {
        if (condition.startsWith("CATEGORY=")) {
            String category = condition.substring("CATEGORY=".length()).trim();
            return "$tx : Transaction()\n    exists io.promoengine.engine.model.InvoiceItem(categoryCode == \"" + category + "\")";
        }
        if (condition.startsWith("DEPT=")) {
            String dept = condition.substring("DEPT=".length()).trim();
            return "$tx : Transaction()\n    exists io.promoengine.engine.model.InvoiceItem(departmentCode == \"" + dept + "\")";
        }
        return "$tx : Transaction()";
    }

    private String buildDiscountExpr(String discountStr, String discountType) {
        double amount = parseDouble(discountStr, 0.0);
        return "new java.math.BigDecimal(\"" + String.format("%.2f", amount) + "\")";
    }

    private String extractValue(String mml, String key, String defaultVal) {
        for (String line : mml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key + ":")) {
                return trimmed.substring(key.length() + 1).trim();
            }
        }
        return defaultVal;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
