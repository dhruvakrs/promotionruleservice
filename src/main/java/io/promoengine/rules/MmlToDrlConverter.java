package io.promoengine.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts MML (Marketing Markup Language) — legacy format — to Drools Rule Language (DRL).
 * Used ONLY for legacy import. All new rules are authored directly in DRL.
 *
 * Two MML variants are supported:
 *
 * ── SIMPLE FORMAT (sample/test XML) ─────────────────────────────────────────
 *   Text key-value pairs inside <Promotion id="..."> elements:
 *     DESCRIPTION: <text>
 *     CONDITION:   CATEGORY=<code> | DEPT=<code>
 *     DISCOUNT:    <amount>
 *     DISCOUNT_TYPE: FIXED | PCT
 *     PRIORITY:    <n>
 *
 * ── REAL MMED FORMAT (QA / production XML) ──────────────────────────────────
 *   Proprietary DSL decoded from mmedCode (Base64 + gzip + Java serial):
 *     PS(N,(ITEM(12,"sku")+...))           — item group definitions
 *     RESTYPE(a,b,c,DISCOUNT,...)          — 4th param is the discount amount
 *     QMIN(qty,0,qty,PS(N))                — minimum quantity per group
 *     DATE_BETWEEN(yyyyMMdd,yyyyMMdd)      — already captured as XML metadata
 *   promotionType comes from the <promotionType> XML element (< 10 = STANDARD, >= 10 = COUPON).
 */
@Slf4j
@Component
public class MmlToDrlConverter {

    // ─── Simple format ────────────────────────────────────────────────────────

    public String convert(String promotionId, String mml) {
        String description   = extractValue(mml, "DESCRIPTION", promotionId);
        String condition     = extractValue(mml, "CONDITION", "");
        String discountStr   = extractValue(mml, "DISCOUNT", "0");
        String discountType  = extractValue(mml, "DISCOUNT_TYPE", "FIXED");
        String priorityStr   = extractValue(mml, "PRIORITY", "10");

        String whenClause    = buildSimpleWhenClause(condition);
        String discountExpr  = buildDiscountExpr(discountStr, discountType);
        int priority         = parseInt(priorityStr, 10);

        return buildDrl(promotionId, priority, whenClause, promotionId, 0, description, discountExpr);
    }

    private String buildSimpleWhenClause(String condition) {
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

    // ─── Real MMED format ─────────────────────────────────────────────────────

    /**
     * Converts real MMED MML (decoded from mmedCode) to DRL.
     *
     * @param promotionId   numeric ID from <promotionID> element
     * @param description   ENG description from <promotionText>
     * @param promotionType from <promotionType> element
     * @param priority      from <priority> element
     * @param startDate     from <startDate> element (yyyy-MM-dd)
     * @param endDate       from <endDate> element (yyyy-MM-dd)
     * @param mml           decoded MML text string
     */
    public String convertMmedMml(String promotionId, String description,
                                  int promotionType, int priority,
                                  String startDate, String endDate,
                                  String mml) {
        double discount = extractResTypeDiscount(mml);
        Map<Integer, List<String>> psSkus = extractPsSkus(mml);
        Map<Integer, Double>       psQmin = extractQminConditions(mml);

        StringBuilder when = new StringBuilder();

        // Transaction with optional date range
        if (hasValidDate(startDate) && hasValidDate(endDate)) {
            when.append("$tx : Transaction(\n");
            when.append("        !transactionDate.isBefore(java.time.LocalDate.parse(\"").append(startDate).append("\")),\n");
            when.append("        !transactionDate.isAfter(java.time.LocalDate.parse(\"").append(endDate).append("\")))\n");
        } else {
            when.append("$tx : Transaction()\n");
        }

        if (!psQmin.isEmpty()) {
            // Buy-N-of-group conditions
            int idx = 1;
            for (Map.Entry<Integer, Double> e : psQmin.entrySet()) {
                List<String> skus = psSkus.getOrDefault(e.getKey(), List.of());
                if (skus.isEmpty()) continue;

                int minQty = (int) Math.ceil(e.getValue());
                String qVar   = "$q"   + idx;
                String qtyVar = "$qty" + idx;

                when.append("    accumulate(\n");
                when.append("        io.promoengine.engine.model.InvoiceItem(")
                        .append(buildSkuCondition(skus)).append(", ").append(qVar).append(" : quantity),\n");
                when.append("        ").append(qtyVar).append(" : sum(").append(qVar).append(")\n");
                when.append("    )\n");
                when.append("    eval(").append(qtyVar).append(".intValue() >= ").append(minQty).append(")\n");
                idx++;
            }
        } else if (!psSkus.isEmpty()) {
            // No quantity minimum — just require at least one matching item
            List<String> allSkus = psSkus.values().stream()
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());
            when.append("    exists io.promoengine.engine.model.InvoiceItem(")
                    .append(buildSkuCondition(allSkus)).append(")\n");
        }
        // If no PS or QMIN info at all, the rule applies to every transaction (Transaction() only)

        String discountExpr = String.format("new java.math.BigDecimal(\"%.2f\")", discount);
        return buildDrl(promotionId, priority, when.toString().trim(),
                promotionId, promotionType, description, discountExpr);
    }

    // ─── MML parsing helpers ──────────────────────────────────────────────────

    /**
     * Extracts the discount amount from RESTYPE(a,b,c,DISCOUNT,...).
     * The 4th parameter (index 3) is the discount value.
     */
    private double extractResTypeDiscount(String mml) {
        Pattern p = Pattern.compile("RESTYPE\\(([^)]+)\\)");
        Matcher m = p.matcher(mml);
        if (m.find()) {
            String[] params = m.group(1).split(",");
            if (params.length >= 4) {
                try { return Double.parseDouble(params[3].trim()); }
                catch (NumberFormatException ignored) {}
            }
        }
        return 0.0;
    }

    /**
     * Extracts PS group → SKU list mapping from PS(N,(ITEM(12,"sku")+...)) definitions.
     * Only captures PS groups before "RESULT IS" (the conditions block).
     * Only captures ITEM(12,"sku") — type 12 = item number / SKU.
     */
    private Map<Integer, List<String>> extractPsSkus(String mml) {
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        // Work only in the PS definitions section (before "RESULT IS")
        String psPart = mml.contains("RESULT IS") ? mml.substring(0, mml.indexOf("RESULT IS")) : mml;

        // Find each PS(N, and then extract the parenthesised item list
        Pattern psStart = Pattern.compile("PS\\((\\d+),\\(");
        Matcher m = psStart.matcher(psPart);
        while (m.find()) {
            int psNum = parseInt(m.group(1), -1);
            if (psNum < 0) continue;

            // Walk forward to find matching closing paren
            int start = m.end();
            int depth = 1;
            int pos   = start;
            while (pos < psPart.length() && depth > 0) {
                char c = psPart.charAt(pos++);
                if (c == '(') depth++;
                else if (c == ')') depth--;
            }
            String items = psPart.substring(start, pos - 1);
            List<String> skus = extractItemSkus(items);
            if (!skus.isEmpty()) result.put(psNum, skus);
        }
        return result;
    }

    /**
     * Extracts SKUs from a list like: ITEM(12,"108389")+ITEM(12,"217612")
     * Only ITEM type 12 = specific article/SKU number.
     */
    private List<String> extractItemSkus(String items) {
        List<String> skus = new ArrayList<>();
        Pattern p = Pattern.compile("ITEM\\(12,\"([^\"]+)\"\\)");
        Matcher m = p.matcher(items);
        while (m.find()) skus.add(m.group(1));
        return skus;
    }

    /**
     * Extracts QMIN(qty,...,PS(N)) conditions → Map<psNum, minQty>.
     * Looks in the IF clause of the MML.
     */
    private Map<Integer, Double> extractQminConditions(String mml) {
        Map<Integer, Double> result = new LinkedHashMap<>();
        Pattern p = Pattern.compile("QMIN\\(([\\d.]+),[^,]+,[^,]+,PS\\((\\d+)\\)\\)");
        Matcher m = p.matcher(mml);
        while (m.find()) {
            double qty  = parseDouble(m.group(1), 1.0);
            int    psNum = parseInt(m.group(2), -1);
            if (psNum >= 0) result.put(psNum, qty);
        }
        return result;
    }

    /**
     * Builds a Drools SKU constraint string:
     *   1 SKU  → sku == "sku1"
     *   N SKUs → sku in ("sku1", "sku2", ...)
     */
    private String buildSkuCondition(List<String> skus) {
        if (skus.isEmpty()) return "eval(true)";
        if (skus.size() == 1) return "sku == \"" + skus.get(0) + "\"";
        return "sku in (" + skus.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + ")";
    }

    // ─── DRL builder ──────────────────────────────────────────────────────────

    private String buildDrl(String ruleName, int priority, String whenClause,
                             String promotionId, int promotionType,
                             String description, String discountExpr) {
        String safeDesc = description == null ? promotionId
                : description.replace("\\", "\\\\").replace("\"", "\\\"");
        return String.format(
                "rule \"%s\"%n" +
                "  salience %d%n" +
                "  when%n" +
                "    %s%n" +
                "  then%n" +
                "    results.add(io.promoengine.engine.model.PromotionResult.builder()" +
                ".promotionId(\"%s\").promotionType(%d).description(\"%s\")" +
                ".discountAmount(%s).timesApplied(1.0).build());%n" +
                "end",
                ruleName, priority, whenClause,
                promotionId, promotionType, safeDesc, discountExpr
        );
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private String extractValue(String mml, String key, String defaultVal) {
        for (String line : mml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key + ":")) {
                return trimmed.substring(key.length() + 1).trim();
            }
        }
        return defaultVal;
    }

    private boolean hasValidDate(String date) {
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s == null ? "" : s.trim()); }
        catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s == null ? "" : s.trim()); }
        catch (Exception e) { return def; }
    }
}
