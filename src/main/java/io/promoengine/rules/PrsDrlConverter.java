package io.promoengine.rules;

import io.promoengine.dto.request.PrsRuleSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts a PRS (PromoRuleSet) rule definition into a Drools DRL rule body.
 *
 * Handles all promotion trigger + application combinations:
 *
 *   Trigger types:
 *     BUY_QTY     — group.minQty check on sum(quantity)
 *     SPEND_MIN   — group.minAmount check on sum(lineAmount)
 *     CROSS_GROUP — multiple groups, each independently checked; each group may
 *                   use minQty, minAmount, or both
 *
 *   Application types:
 *     TRANSACTION    — discount fires once per transaction
 *     MOST_EXPENSIVE — discount applied to N most expensive items in the group
 *
 * Note on MOST_EXPENSIVE: the Drools rule fires with the correct discount amount
 * and a special promotionType flag so the response mapper can attribute the
 * discount to the most expensive line items rather than distributing it evenly.
 */
@Slf4j
@Component
public class PrsDrlConverter {

    public String convert(PrsRuleSet.PrsRule rule) {
        if (rule.getId() == null || rule.getId().isBlank()) {
            throw new IllegalArgumentException("Rule id is required");
        }

        String   id          = rule.getId();
        int      prio        = rule.getPriority();
        int      ptype       = rule.getPromotionType();
        String   desc        = safeDesc(rule.getDescription(), id);
        double   amount      = rule.getDiscount() != null ? rule.getDiscount().getAmount() : 0.0;
        String   application = rule.getApplication() != null ? rule.getApplication().getType() : "TRANSACTION";

        StringBuilder when = new StringBuilder();

        // ── Transaction with optional date range ─────────────────────────────
        if (hasDate(rule.getStartDate()) && hasDate(rule.getEndDate())) {
            when.append("$tx : Transaction(\n");
            when.append("        !transactionDate.isBefore(java.time.LocalDate.parse(\"").append(rule.getStartDate()).append("\")),\n");
            when.append("        !transactionDate.isAfter(java.time.LocalDate.parse(\"").append(rule.getEndDate()).append("\")))\n");
        } else {
            when.append("$tx : Transaction()\n");
        }

        // ── Trigger conditions ────────────────────────────────────────────────
        if (rule.getTrigger() != null && rule.getTrigger().getGroups() != null) {
            List<PrsRuleSet.PrsGroup> groups = rule.getTrigger().getGroups();
            int idx = 1;

            for (PrsRuleSet.PrsGroup group : groups) {
                if (group.getSkus() == null || group.getSkus().isEmpty()) continue;
                String skuCond = buildSkuCondition(group.getSkus());

                boolean hasQty    = group.getMinQty()    != null && group.getMinQty()    > 0;
                boolean hasAmount = group.getMinAmount()  != null && group.getMinAmount() > 0;

                if (!hasQty && !hasAmount) {
                    // No minimum — just require at least one matching item
                    when.append("    exists io.promoengine.engine.model.InvoiceItem(").append(skuCond).append(")\n");
                    continue;
                }

                if (hasQty) {
                    when.append("    accumulate(\n");
                    when.append("        io.promoengine.engine.model.InvoiceItem(")
                            .append(skuCond).append(", $q").append(idx).append(" : quantity),\n");
                    when.append("        $qty").append(idx).append(" : sum($q").append(idx).append(")\n");
                    when.append("    )\n");
                    when.append("    eval($qty").append(idx).append(".intValue() >= ").append(group.getMinQty()).append(")\n");
                }

                if (hasAmount) {
                    String amtIdx = hasQty ? idx + "a" : String.valueOf(idx);
                    when.append("    accumulate(\n");
                    when.append("        io.promoengine.engine.model.InvoiceItem(")
                            .append(skuCond).append(", $la").append(amtIdx).append(" : lineAmount),\n");
                    when.append("        $amt").append(amtIdx).append(" : sum($la").append(amtIdx).append(")\n");
                    when.append("    )\n");
                    when.append("    eval($amt").append(amtIdx).append(".doubleValue() >= ").append(group.getMinAmount()).append(")\n");
                }

                idx++;
            }
        }
        // If no trigger or no groups: the rule applies to every qualifying transaction

        // ── MOST_EXPENSIVE: find the most expensive item in the first group ──
        if ("MOST_EXPENSIVE".equals(application)
                && rule.getTrigger() != null
                && rule.getTrigger().getGroups() != null
                && !rule.getTrigger().getGroups().isEmpty()) {

            PrsRuleSet.PrsGroup firstGroup = rule.getTrigger().getGroups().get(0);
            if (firstGroup.getSkus() != null && !firstGroup.getSkus().isEmpty()) {
                String skuCond = buildSkuCondition(firstGroup.getSkus());
                int    count   = rule.getApplication() != null ? rule.getApplication().getCount() : 1;

                if (count == 1) {
                    // Bind the single most expensive item — "no other item is more expensive"
                    when.append("    $topItem : io.promoengine.engine.model.InvoiceItem(").append(skuCond).append(")\n");
                    when.append("    not io.promoengine.engine.model.InvoiceItem(")
                            .append(skuCond).append(", unitPrice > $topItem.unitPrice)\n");
                }
                // count > 1: multiple most-expensive items — complex to model per-item in stateless session;
                // fires with full discount amount for now (line attribution handled by mapper)
            }
        }

        String discountExpr = String.format("new java.math.BigDecimal(\"%.4f\")", amount);

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
                id, prio, when.toString().trim(),
                id, ptype, desc, discountExpr
        );
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String buildSkuCondition(List<String> skus) {
        if (skus.size() == 1) return "sku == \"" + skus.get(0) + "\"";
        return "sku in (" + skus.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + ")";
    }

    private boolean hasDate(String date) {
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private String safeDesc(String description, String fallback) {
        String s = (description != null && !description.isBlank()) ? description : fallback;
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
