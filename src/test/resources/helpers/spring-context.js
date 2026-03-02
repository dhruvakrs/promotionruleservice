function fn() {
    var InMemoryRuleServiceHelper = Java.type('io.promoengine.step7.InMemoryRuleServiceHelper');
    var service = new InMemoryRuleServiceHelper();
    return { promotionRuleService: service };
}
