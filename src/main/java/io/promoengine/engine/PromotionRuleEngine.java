package io.promoengine.engine;

import io.promoengine.engine.model.PromotionResult;
import io.promoengine.enrichment.EnrichedTransaction;
import io.promoengine.exception.RuleCompilationException;
import io.promoengine.rules.RuleDefinition;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PromotionRuleEngine {

    private static final String DRL_HEADER =
            "package io.promoengine.rules;\n" +
            "import io.promoengine.engine.model.InvoiceItem;\n" +
            "import io.promoengine.engine.model.Transaction;\n" +
            "import io.promoengine.engine.model.Customer;\n" +
            "import io.promoengine.engine.model.PromotionResult;\n" +
            "import java.math.BigDecimal;\n" +
            "import java.util.List;\n" +
            "global List results;\n\n";

    private final String tenantId;
    private final AtomicReference<KieContainer> kieContainerRef = new AtomicReference<>();

    public PromotionRuleEngine(String tenantId) {
        this.tenantId = tenantId;
    }

    public void reload(List<RuleDefinition> rules) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        for (RuleDefinition rule : rules) {
            String drl = DRL_HEADER + rule.getRuleBody();
            kfs.write("src/main/resources/rules/" + rule.getPromotionId() + ".drl",
                    ks.getResources().newByteArrayResource(drl.getBytes()));
        }

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        Results results = kieBuilder.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            String errors = results.getMessages(Message.Level.ERROR).toString();
            throw new RuleCompilationException(tenantId, errors);
        }

        KieContainer newContainer = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        kieContainerRef.set(newContainer);
        log.info("[{}] Rules reloaded: {} active rules", tenantId, rules.size());
    }

    public CalculationResult calculate(EnrichedTransaction tx) {
        KieContainer container = kieContainerRef.get();
        StatelessKieSession session = container.newStatelessKieSession();
        List<PromotionResult> promotionResults = new ArrayList<>();
        session.setGlobal("results", promotionResults);
        session.execute(EngineFactBuilder.build(tx));
        return new CalculationResult(promotionResults, tx.getSkippedItems());
    }
}
