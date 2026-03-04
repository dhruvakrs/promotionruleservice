package io.promoengine.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * PromoRuleSet (PRS) — portable, human-readable promotion rule file format.
 *
 * Replaces the proprietary MMED XML for new rule generation workflows.
 * Backward compatible: old MMED XML endpoint remains active.
 *
 * Supports 7 real-world promotion scenarios (derived from QA data analysis):
 *   1. BUY_QTY   → buy min N items → discount on transaction
 *   2. BUY_QTY   → buy min N items → discount on most expensive item(s)
 *   3. SPEND_MIN → spend min ฿X   → discount on transaction
 *   4. CROSS_GROUP → multiple groups each with independent qty/amount conditions
 *   5. Mixed     → one group spend + one group qty (hybrid cross-group)
 *
 * Delta support:
 *   action=upsert (default) — create if new, update if version is newer than DB
 *   action=delete           — deactivate the rule
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "PromoRuleSet (PRS) — delta-aware promotion rule bundle")
public class PrsRuleSet {

    @Schema(description = "File format identifier", example = "prs")
    private String format = "prs";

    @Schema(description = "PRS schema version", example = "1.1")
    private String version = "1.1";

    @Schema(description = "ISO-8601 timestamp when the file was generated", example = "2026-03-03T10:00:00Z")
    private String generatedAt;

    @Schema(description = "Override tenant for all rules in this file. Falls back to API key tenant if absent.")
    private String tenantId;

    @Schema(description = "List of promotion rules to import")
    private List<PrsRule> rules;

    // ─── Rule ────────────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "A single promotion rule in PRS format")
    public static class PrsRule {

        @Schema(description = "Unique promotion ID", example = "100556820", requiredMode = Schema.RequiredMode.REQUIRED)
        private String id;

        @Schema(description = "Rule version from the source system. Used for delta detection — rules with same or lower version than DB are skipped.", example = "3")
        private int version = 1;

        @Schema(description = "Human-readable description", example = "MakroPRO_MB03")
        private String description;

        @Schema(description = "Promotion type code. Values < 10 = STANDARD; >= 10 = COUPON/LOYALTY. Common: 3=buy-most-exp, 20=multi-sku-buy, 21=multi-group-buy, 22=buy-N-most-exp, 30=spend-min, 31=cross-group-spend", example = "3")
        private int promotionType = 0;

        @Schema(description = "Drools salience — higher fires first", example = "40")
        private int priority = 10;

        @Schema(description = "Promotion start date (yyyy-MM-dd). Rule will not fire before this date.", example = "2023-01-06")
        private String startDate;

        @Schema(description = "Promotion end date (yyyy-MM-dd). Rule will not fire after this date.", example = "2043-12-31")
        private String endDate;

        @Schema(description = "Delta action: 'upsert' (default) creates/updates the rule; 'delete' deactivates it.", example = "upsert", allowableValues = {"upsert", "delete"})
        private String action = "upsert";

        @Schema(description = "Trigger conditions — what the customer must do to qualify")
        private PrsTrigger trigger;

        @Schema(description = "Where the discount is applied")
        private PrsApplication application;

        @Schema(description = "Discount definition")
        private PrsDiscount discount;

        @Schema(description = "If true, the promotion fires multiple times per transaction (once per qualifying set of items)", example = "true")
        private boolean multipleFires = false;

        @Schema(description = "If true, this promotion also triggers a loyalty-points calculation", example = "true")
        private boolean loyaltyPoints = false;
    }

    // ─── Trigger ─────────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Trigger — defines what the customer must purchase or spend to qualify")
    public static class PrsTrigger {

        @Schema(description = "Trigger type hint (informational — DRL is driven by group conditions):\n" +
                "BUY_QTY    — one group, qty-based condition\n" +
                "SPEND_MIN  — one group, amount-based condition\n" +
                "CROSS_GROUP — multiple groups, ALL must be satisfied independently",
                example = "BUY_QTY",
                allowableValues = {"BUY_QTY", "SPEND_MIN", "CROSS_GROUP"})
        private String type;

        @Schema(description = "One or more item groups. For CROSS_GROUP all groups must be satisfied simultaneously.")
        private List<PrsGroup> groups;
    }

    // ─── Group ───────────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "An item group within a trigger. At least one of minQty or minAmount must be set.")
    public static class PrsGroup {

        @Schema(description = "SKUs (article numbers) that qualify for this group", example = "[\"824216\", \"108389\"]")
        private List<String> skus;

        @Schema(description = "Minimum total quantity of items from this group that must be in the cart. Mutually exclusive groups: each SKU only counts toward the group it belongs to.", example = "10")
        private Integer minQty;

        @Schema(description = "Minimum total spend (sum of lineAmount) on items from this group. Used for SPEND_MIN and CROSS_GROUP spend triggers.", example = "185.00")
        private Double minAmount;
    }

    // ─── Application ─────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Defines where the discount is applied once the trigger is satisfied")
    public static class PrsApplication {

        @Schema(description = "TRANSACTION — discount added once to the transaction total.\n" +
                "MOST_EXPENSIVE — discount applied to the N most expensive items in the qualifying group.",
                example = "MOST_EXPENSIVE",
                allowableValues = {"TRANSACTION", "MOST_EXPENSIVE"})
        private String type = "TRANSACTION";

        @Schema(description = "For MOST_EXPENSIVE: how many of the most expensive items to apply the discount to", example = "1")
        private int count = 1;
    }

    // ─── Discount ────────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Discount specification")
    public static class PrsDiscount {

        @Schema(description = "FIXED — subtract a fixed baht amount. PCT — subtract a percentage of the qualifying amount (reserved).",
                example = "FIXED",
                allowableValues = {"FIXED", "PCT"})
        private String type = "FIXED";

        @Schema(description = "Discount value. For FIXED: amount in baht. For PCT: percentage 0–100.", example = "20.00")
        private double amount;
    }
}
