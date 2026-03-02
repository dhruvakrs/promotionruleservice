# CLAUDE.md — promoengine-service

Implementation guide for Claude Code. Every instruction here is binding.

---

## ⚠ First Action Before Any Implementation

Delete all existing wrong scaffolding — it uses the wrong package and naming:

```bash
rm -rf src/main/java/com/
rm -rf lib/
# Then rewrite pom.xml and application.yml per specs below
```

Do NOT use or import any files from `com.capgemini.rm3.promotion.*`. Delete them first.

---

## Technology Stack

| Technology | Version | Usage |
|-----------|---------|-------|
| **Spring Boot** | **3.2.2** | **Core framework — all layers** |
| Java | 17 | Runtime |
| Maven | 3.9+ | Build |
| Drools | 8.44.0.Final | Promotion rule engine (replaces proprietary JAR) |
| Spring Data Elasticsearch | (boot-managed) | OpenSearch client |
| Spring Data JPA | (boot-managed) | Oracle DB + rule persistence |
| Oracle JDBC | ojdbc11 | Database driver |
| AWS SDK v2 | 2.21.42 | S3 bulk import only |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI |
| Lombok | (boot-managed) | Boilerplate |
| **Karate** | **1.4.1** | **All tests — unit + API + E2E** |
| JUnit 5 | (boot-managed) | Karate test runner |
| Mockito | (boot-managed) | Mocking in Java-interop Karate tests |
| Packaging | JAR (not WAR) | Spring Boot embedded Tomcat 10 |

> **Yes — this is a Spring Boot 3.2.2 application.** Spring Boot manages the entire runtime: embedded Tomcat, JPA, OpenSearch client, security, actuator, and test context.

---

## Product Identity — Absolute Rules

| Rule | Value |
|------|-------|
| Product name | PromoEngine |
| Maven groupId | `io.promoengine` |
| Maven artifactId | `promoengine-service` |
| Root Java package | `io.promoengine` |
| Spring config prefix | `promoengine` |
| Env var prefix | `PE_` |
| OpenSearch index — prices | `promoprice` |
| OpenSearch index — items | `promoitem` |
| OpenSearch index — customers | `promocustomer` |
| Proprietary JAR | **NOT USED** — replaced by Drools 8.x |
| `lib/` directory | **DOES NOT EXIST** |

**NEVER use in any file:** `rm3`, `RM3`, `capgemini`, `Capgemini`, `makro`, `Makro`, `mmed`, `MMED` (except in `MmedXmlParser` and `MmlToDrlConverter` — legacy import only).

---

## Build & Test Commands

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Run ALL tests (Karate + JUnit 5)
mvn test

# Run one Karate runner class
mvn test -Dtest=PromotionRuleEngineTest

# Run one Karate feature file (via runner)
mvn test -Dtest=CalculateApiTest

# Run Karate in a specific env
mvn test -Dkarate.env=docker
```

---

## Maven pom.xml — Complete Specification

```xml
<groupId>io.promoengine</groupId>
<artifactId>promoengine-service</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>jar</packaging>
<name>promoengine-service</name>
<description>PromoEngine — Cloud-native promotion calculation engine for retail SaaS</description>

<properties>
    <java.version>17</java.version>
    <drools.version>8.44.0.Final</drools.version>
    <aws.sdk.version>2.21.42</aws.sdk.version>
    <springdoc.version>2.3.0</springdoc.version>
    <karate.version>1.4.1</karate.version>
</properties>

<!-- KIE BOM — aligns all Drools module versions -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.kie</groupId>
            <artifactId>kie-bom</artifactId>
            <version>${drools.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Runtime dependencies -->
spring-boot-starter-web
spring-boot-starter-actuator
spring-boot-starter-validation
spring-boot-configuration-processor (optional)
spring-boot-starter-data-jpa
ojdbc11 (runtime)
spring-boot-starter-data-elasticsearch
software.amazon.awssdk:s3:${aws.sdk.version}
org.drools:drools-engine          (version from BOM)
org.drools:drools-compiler        (version from BOM)
org.kie:kie-api                   (version from BOM)
springdoc-openapi-starter-webmvc-ui:${springdoc.version}
lombok (optional)
jakarta.xml.bind:jakarta.xml.bind-api   ← ONLY for MmedXmlParser
com.sun.xml.bind:jaxb-impl:4.0.4       ← ONLY for MmedXmlParser

<!-- Test dependencies -->
<dependency>
    <groupId>io.karatelabs</groupId>
    <artifactId>karate-junit5</artifactId>
    <version>${karate.version}</version>
    <scope>test</scope>
</dependency>
spring-boot-starter-test (test)   ← includes Mockito, AssertJ

<!-- NO proprietary JAR. NO lib/ directory. -->
```

---

## Package Structure

```
io.promoengine
├── PromoEngineApplication.java
├── PromoEngineProperties.java          @ConfigurationProperties(prefix="promoengine")
│
├── config/
│   ├── SecurityConfig.java
│   ├── OpenSearchConfig.java
│   └── AwsConfig.java
│
├── security/
│   ├── ApiKeyAuthFilter.java           OncePerRequestFilter
│   └── TenantResolver.java             API key → tenantId
│
├── engine/
│   ├── model/
│   │   ├── InvoiceItem.java
│   │   ├── Transaction.java
│   │   ├── Customer.java
│   │   └── PromotionResult.java
│   ├── EngineFactBuilder.java
│   ├── PromotionRuleEngine.java        AtomicReference<KieBase>
│   ├── TenantEngineRegistry.java       ConcurrentHashMap<tenantId, PromotionRuleEngine>
│   ├── EngineConfig.java
│   ├── CalculationResult.java
│   ├── LineCalculationResult.java
│   └── PromotionCalculationResult.java
│
├── rules/
│   ├── PromotionRuleService.java
│   ├── PromotionPresetService.java
│   ├── PromotionRuleLoader.java
│   ├── RuleDefinition.java
│   ├── RuleExpiryScheduler.java
│   ├── S3RuleImporter.java
│   ├── MmedXmlParser.java
│   └── MmlToDrlConverter.java
│
├── enrichment/
│   ├── EnrichmentService.java
│   ├── EnrichedTransaction.java
│   ├── EnrichedItem.java
│   ├── PriceData.java
│   ├── ItemPropertyData.java
│   ├── CustomerData.java
│   ├── price/   (PriceRepository, OpenSearchPriceRepository, DatabasePriceRepository)
│   ├── item/    (ItemRepository, OpenSearchItemRepository, DatabaseItemRepository)
│   └── customer/(CustomerRepository, OpenSearchCustomerRepository, DatabaseCustomerRepository)
│
├── domain/jpa/
│   ├── PromotionRuleEntity.java
│   ├── PromotionPresetEntity.java
│   ├── PromotionRuleHistoryEntity.java
│   ├── PromotionRuleRepository.java
│   ├── PromotionPresetRepository.java
│   └── PromotionRuleHistoryRepository.java
│
├── dto/
│   ├── request/  (CalculateRequest, OrderItem, CreateRuleRequest, UpdateRuleRequest, RuleImportRequest)
│   └── response/ (CalculateResponse, OrderSummary, CalculatedItem, AppliedPromotion,
│                  PromotionSummary, PromotionRuleResponse, RuleImportResponse,
│                  VersionResponse, ErrorResponse)
│
├── mapper/
│   └── PromotionResponseMapper.java
│
├── service/
│   └── PromotionService.java
│
├── controller/
│   ├── PromotionController.java
│   ├── PromotionRuleController.java
│   ├── PromotionPresetController.java
│   ├── RuleImportController.java
│   └── VersionController.java
│
└── exception/
    ├── RuleCompilationException.java
    ├── EngineInitException.java
    ├── RuleSetException.java
    ├── TenantNotFoundException.java
    ├── EnrichmentException.java
    └── GlobalExceptionHandler.java
```

---

## Karate Test Structure

```
src/test/
├── java/io/promoengine/
│   ├── step2/  ExceptionTest.java
│   ├── step3/  EnginePojosTest.java
│   ├── step4/  JpaEntityTest.java
│   ├── step5/  OpenSearchDocumentTest.java
│   ├── step6/  PromotionRuleEngineTest.java
│   ├── step7/  PromotionRuleServiceTest.java
│   ├── step8/  EnrichmentRepositoryTest.java
│   ├── step9/  EnrichmentServiceTest.java
│   ├── step10/ DtoValidationTest.java
│   ├── step11/ PromotionResponseMapperTest.java
│   ├── step12/ PromotionServiceTest.java
│   ├── step13/ CalculateApiTest.java
│   │           RulesApiTest.java
│   │           PresetsApiTest.java
│   │           VersionApiTest.java
│   ├── step14/ SecurityApiTest.java
│   ├── step15/ ExceptionHandlerTest.java
│   └── step16/ PromoEngineE2ETest.java
│
└── resources/io/promoengine/
    ├── karate-config.js
    ├── step2/  exceptions.feature
    ├── step3/  engine-pojos.feature
    ├── step4/  jpa-entities.feature
    ├── step5/  opensearch-documents.feature
    ├── step6/  promotion-rule-engine.feature
    ├── step7/  promotion-rule-service.feature
    ├── step8/  enrichment-repository.feature
    ├── step9/  enrichment-service.feature
    ├── step10/ dto-validation.feature
    ├── step11/ promotion-response-mapper.feature
    ├── step12/ promotion-service.feature
    ├── step13/ calculate-api.feature
    │           rules-api.feature
    │           presets-api.feature
    │           version-api.feature
    ├── step14/ security-api.feature
    ├── step15/ exception-handler.feature
    └── step16/ e2e-suite.feature
```

**`karate-config.js`** — must be the first file created in `src/test/resources/io/promoengine/`:

```javascript
function fn() {
    var env = karate.env || 'local';
    var config = {
        baseUrl:  'http://localhost:8080/promotionengine/api',
        apiKey:   'test-key',
        tenantId: 'default'
    };
    if (env === 'docker') {
        config.baseUrl = 'http://localhost:8080/promotionengine/api';
    }
    if (env === 'staging') {
        config.baseUrl = karate.properties['staging.url'];
        config.apiKey  = karate.properties['staging.key'];
    }
    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 30000);
    return config;
}
```

**JUnit 5 Karate runner pattern** (use for every step):

```java
@Karate.Test
Karate testAll() {
    return Karate.run("stepN/feature-name").relativeTo(getClass());
}
```

---

## 17-Step Implementation Order

Each step must **compile AND pass its Karate tests** before the next step begins.

---

### Step 1 — Clean Scaffold

**Build:**
- Delete `src/main/java/com/` and `lib/`
- Write fresh `pom.xml` with coords `io.promoengine:promoengine-service:1.0.0-SNAPSHOT`
- Include Drools 8.x KIE BOM, Karate 1.4.1, Spring Boot 3.2.2, no proprietary JAR
- Write `PromoEngineApplication.java` in `io.promoengine`
- Write `PromoEngineProperties.java` (`@ConfigurationProperties(prefix="promoengine")`)
- Write `application.yml` (see Configuration section)
- Write `karate-config.js` in `src/test/resources/io/promoengine/`

**Test — `step1/scaffold.feature`:**

```gherkin
Feature: Application scaffold

  Scenario: Actuator health endpoint responds
    Given url baseUrl
    When path '/actuator/health'
    And method GET
    Then status 200
    And match response.status == 'UP'

  Scenario: Swagger UI is accessible
    Given url baseUrl
    When path '/swagger-ui.html'
    And method GET
    Then status 200
```

Run: `mvn spring-boot:run` in background, then `mvn test -Dtest=ScaffoldTest`.

---

### Step 2 — Exception Classes

**Build:**
- `RuleCompilationException(String tenantId, String drlErrors)` → HTTP 422
- `EngineInitException(String message, Throwable cause)` → HTTP 500
- `RuleSetException(int errorCode, String message)` → HTTP 400/422
- `TenantNotFoundException(String apiKey)` → HTTP 401
- `EnrichmentException(String message, Throwable cause)` → HTTP 500
- Do NOT add `@ResponseStatus` — `GlobalExceptionHandler` handles HTTP mapping

**Test — `step2/exceptions.feature`:**

```gherkin
Feature: Exception classes

  Scenario: RuleCompilationException carries tenantId and error details
    * def RuleCompilationException = Java.type('io.promoengine.exception.RuleCompilationException')
    * def ex = new RuleCompilationException('tenant-a', 'syntax error on line 3')
    * match ex.message contains 'syntax error on line 3'

  Scenario: RuleSetException exposes errorCode
    * def RuleSetException = Java.type('io.promoengine.exception.RuleSetException')
    * def ex = new RuleSetException(14, 'DRL compilation failed')
    * match ex.errorCode == 14
    * match ex.message == 'DRL compilation failed'

  Scenario: TenantNotFoundException includes the bad API key
    * def TenantNotFoundException = Java.type('io.promoengine.exception.TenantNotFoundException')
    * def ex = new TenantNotFoundException('bad-key-xyz')
    * match ex.message contains 'bad-key-xyz'

  Scenario: EnrichmentException wraps cause
    * def EnrichmentException = Java.type('io.promoengine.exception.EnrichmentException')
    * def cause = new java.lang.RuntimeException('ES timeout')
    * def ex = new EnrichmentException('Price lookup failed', cause)
    * match ex.message == 'Price lookup failed'
    * match ex.cause.message == 'ES timeout'
```

---

### Step 3 — Engine POJO Model

**Build:** Four POJOs in `io.promoengine.engine.model` — no JAR, no JAXB.

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceItem {
    private String sku;
    private String storeCode;
    private String categoryCode;
    private String subcategoryCode;
    private String departmentCode;
    private boolean foodItem;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;    // unitPrice × quantity
    private int quantity;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Transaction {
    private String storeId;
    private String customerId;
    private LocalDate transactionDate;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer {
    private String customerId;
    private String customerGroupCode;
    private String customerTypeCode;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionResult {
    private String promotionId;
    private int promotionType;           // <10=STANDARD, >=10=COUPON
    private String description;
    private double timesApplied;
    private BigDecimal discountAmount;
    private BigDecimal lineDiscountAmount;
    private List<String> prerequisites;
}
```

Also write: `CalculationResult`, `LineCalculationResult`, `PromotionCalculationResult`.

**Test — `step3/engine-pojos.feature`:**

```gherkin
Feature: Engine POJO model

  Scenario: InvoiceItem builder sets all fields correctly
    * def InvoiceItem = Java.type('io.promoengine.engine.model.InvoiceItem')
    * def item = InvoiceItem.builder()
        .sku('12345678')
        .storeCode('0101')
        .categoryCode('ELEC')
        .unitPrice(java.math.BigDecimal.valueOf(49.99))
        .lineAmount(java.math.BigDecimal.valueOf(99.98))
        .quantity(2)
        .foodItem(false)
        .build()
    * match item.sku == '12345678'
    * match item.quantity == 2
    * match item.foodItem == false
    * match item.lineAmount.doubleValue() == 99.98

  Scenario: Transaction builder sets header fields
    * def Transaction = Java.type('io.promoengine.engine.model.Transaction')
    * def tx = Transaction.builder()
        .storeId('0101')
        .customerId('CUST001')
        .transactionDate(java.time.LocalDate.of(2024, 1, 15))
        .build()
    * match tx.storeId == '0101'
    * match tx.customerId == 'CUST001'

  Scenario: PromotionResult promotionType < 10 maps to STANDARD
    * def PromotionResult = Java.type('io.promoengine.engine.model.PromotionResult')
    * def result = PromotionResult.builder()
        .promotionId('PROMO01')
        .promotionType(0)
        .discountAmount(java.math.BigDecimal.valueOf(10.00))
        .timesApplied(1.0)
        .build()
    * match result.promotionType < 10
    * match result.promotionId == 'PROMO01'

  Scenario: CalculationResult holds results and skippedItems
    * def CalculationResult = Java.type('io.promoengine.engine.CalculationResult')
    * def skipped = ['UNKNOWN01', 'UNKNOWN02']
    * def result = new CalculationResult([], skipped)
    * match result.skippedItems.size() == 2
    * match result.skippedItems contains 'UNKNOWN01'
```

---

### Step 4 — JPA Entities

**Build:**
- `PromotionRuleEntity` — `promotion_rule` table. Key fields: `id`, `tenantId`, `ruleBody` (CLOB — DRL text), `status` (default `'DRAFT'`), `version`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`
- `PromotionPresetEntity` — `promotion_preset` table. Fields: `id`, `tenantId`, `ruleBody` (CLOB), `status`
- `PromotionRuleHistoryEntity` — `promotion_rule_history` table. Fields: `historyId` (sequence PK), `ruleId`, `version`, `ruleBody`, `status`, `changedAt`, `changedBy`, `changeReason`
- Repositories: `PromotionRuleRepository`, `PromotionPresetRepository`, `PromotionRuleHistoryRepository`

**Test — `step4/jpa-entities.feature`:**

```gherkin
Feature: JPA entity defaults and structure

  Scenario: PromotionRuleEntity defaults status to DRAFT
    * def entity = new (Java.type('io.promoengine.domain.jpa.PromotionRuleEntity'))()
    * entity.setPromotionId('TEST001')
    * entity.setTenantId('default')
    * entity.setRuleBody('rule "test" when then end')
    * match entity.getStatus() == 'DRAFT'
    * match entity.getVersion() == 1

  Scenario: PromotionRuleEntity ruleBody stores DRL text
    * def entity = new (Java.type('io.promoengine.domain.jpa.PromotionRuleEntity'))()
    * def drl = 'rule "10% Off" when then end'
    * entity.setRuleBody(drl)
    * match entity.getRuleBody() == drl

  Scenario: PromotionRuleHistoryEntity captures version
    * def entity = new (Java.type('io.promoengine.domain.jpa.PromotionRuleHistoryEntity'))()
    * entity.setRuleId('TEST001')
    * entity.setVersion(3)
    * entity.setChangeReason('Manual deactivation')
    * match entity.getVersion() == 3
    * match entity.getChangeReason() == 'Manual deactivation'
```

---

### Step 5 — OpenSearch Document Models

**Build:**
- `PriceDocument` — `@Document(indexName = "#{@promoEngineProperties.data.priceIndex}")`
- `ItemDocument` — `@Document(indexName = "#{@promoEngineProperties.data.itemIndex}")`
- `CustomerDocument` — `@Document(indexName = "#{@promoEngineProperties.data.customerIndex}")`

**Test — `step5/opensearch-documents.feature`:**

```gherkin
Feature: OpenSearch document models

  Scenario: PriceDocument builder sets all enrichment fields
    * def PriceDocument = Java.type('io.promoengine.enrichment.price.PriceDocument')
    * def doc = PriceDocument.builder()
        .sku('12345678')
        .storeCode('0101')
        .unitPrice(java.math.BigDecimal.valueOf(49.99))
        .categoryCode('ELEC')
        .foodItem(false)
        .build()
    * match doc.sku == '12345678'
    * match doc.storeCode == '0101'
    * match doc.unitPrice.doubleValue() == 49.99
    * match doc.foodItem == false

  Scenario: ItemDocument captures product classification fields
    * def ItemDocument = Java.type('io.promoengine.enrichment.item.ItemDocument')
    * def doc = ItemDocument.builder()
        .sku('12345678')
        .categoryCode('ELEC')
        .subcategoryCode('TV')
        .departmentCode('D01')
        .foodItem(false)
        .build()
    * match doc.categoryCode == 'ELEC'
    * match doc.subcategoryCode == 'TV'

  Scenario: CustomerDocument captures segment fields
    * def CustomerDocument = Java.type('io.promoengine.enrichment.customer.CustomerDocument')
    * def doc = CustomerDocument.builder()
        .customerId('CUST001')
        .customerGroupCode('VIP')
        .customerTypeCode('B2C')
        .build()
    * match doc.customerGroupCode == 'VIP'
```

---

### Step 6 — Engine Layer (Most Critical)

**Build:**
- `EngineFactBuilder` — `static List<Object> build(EnrichedTransaction tx)`
- `PromotionRuleEngine` — `AtomicReference<KieBase>`, `reload()`, `calculate()`
- `TenantEngineRegistry` — `ConcurrentHashMap<tenantId, PromotionRuleEngine>`, `getEngine()`, `reloadTenant()`
- `EngineConfig` — `@Bean` that calls `TenantEngineRegistry` at startup

**Test — `step6/promotion-rule-engine.feature`:**

```gherkin
Feature: PromotionRuleEngine — Drools calculation

  Background:
    * def PromotionRuleEngine = Java.type('io.promoengine.engine.PromotionRuleEngine')
    * def RuleDefinition = Java.type('io.promoengine.rules.RuleDefinition')
    * def InvoiceItem = Java.type('io.promoengine.engine.model.InvoiceItem')
    * def Transaction = Java.type('io.promoengine.engine.model.Transaction')
    * def EnrichedTransaction = Java.type('io.promoengine.enrichment.EnrichedTransaction')
    * def BigDecimal = Java.type('java.math.BigDecimal')
    * def validDrl =
    """
    package io.promoengine.rules;
    import io.promoengine.engine.model.*;
    import java.math.BigDecimal;
    import java.util.List;
    global List results;

    rule "10% Off Electronics"
      salience 10
      when
        $tx : Transaction()
        exists InvoiceItem(categoryCode == "ELEC")
      then
        results.add(PromotionResult.builder()
          .promotionId("ELEC10PCT")
          .promotionType(0)
          .description("10% off electronics")
          .discountAmount(new BigDecimal("10.00"))
          .timesApplied(1.0)
          .build());
    end
    """

  Scenario: Engine compiles valid DRL and returns PromotionResult
    * def engine = new PromotionRuleEngine('test-tenant')
    * def rule = RuleDefinition.builder().promotionId('ELEC10PCT').ruleBody(validDrl).priority(10).build()
    * engine.reload([rule])
    * def tx = EnrichedTransaction.builder()
        .storeId('0101')
        .customerId('CUST001')
        .transactionDate(java.time.LocalDate.now())
        .enrichedItems([InvoiceItem.builder().sku('99999').categoryCode('ELEC').unitPrice(BigDecimal.valueOf(100)).lineAmount(BigDecimal.valueOf(100)).quantity(1).storeCode('0101').build()])
        .skippedItems([])
        .build()
    * def result = engine.calculate(tx)
    * match result.promotionResults.size() == 1
    * match result.promotionResults[0].promotionId == 'ELEC10PCT'
    * match result.promotionResults[0].discountAmount.doubleValue() == 10.00
    * match result.skippedItems.size() == 0

  Scenario: Engine rejects invalid DRL with RuleCompilationException
    * def engine = new PromotionRuleEngine('test-tenant')
    * def badRule = RuleDefinition.builder().promotionId('BAD').ruleBody('this is not valid DRL at all').priority(10).build()
    * def fn = function() { engine.reload([badRule]) }
    * def result = karate.attempt(fn)
    * match result != null
    * match result.class.name == 'io.promoengine.exception.RuleCompilationException'

  Scenario: AtomicReference reload is zero-downtime — old KieBase still serves in-flight calls
    * def engine = new PromotionRuleEngine('test-tenant')
    * def rule1 = RuleDefinition.builder().promotionId('R1').ruleBody(validDrl).priority(10).build()
    * engine.reload([rule1])
    * def rule2 = RuleDefinition.builder().promotionId('R2').ruleBody(validDrl).priority(5).build()
    * engine.reload([rule1, rule2])
    # Engine should now have 2 rules — reload succeeded
    * def result = engine.calculate(tx)
    * match result.promotionResults.size() >= 1

  Scenario: EngineFactBuilder produces correct InvoiceItem facts
    * def EngineFactBuilder = Java.type('io.promoengine.engine.EngineFactBuilder')
    * def facts = EngineFactBuilder.build(tx)
    * match facts.size() >= 2
    # Must contain at least Transaction fact + InvoiceItem facts
    * def txFact = karate.filter(facts, function(f){ return f.class.simpleName == 'Transaction' })
    * match txFact.size() == 1
    * match txFact[0].storeId == '0101'

  Scenario: TenantEngineRegistry returns same engine instance for same tenantId
    * def TenantEngineRegistry = Java.type('io.promoengine.engine.TenantEngineRegistry')
    # Registry is a Spring bean — test via Spring context if available
    # Otherwise test isolation via separate engine instances
    * def e1 = new PromotionRuleEngine('tenant-a')
    * def e2 = new PromotionRuleEngine('tenant-b')
    * e1.reload([rule1])
    * e2.reload([rule2])
    # Each tenant has its own rule set
    * def r1 = e1.calculate(tx)
    * def r2 = e2.calculate(tx)
    * match r1.promotionResults[0].promotionId == 'R1'
```

---

### Step 7 — Promotion Rule Management Layer

**Build:**
- `PromotionRuleLoader` — `List<RuleDefinition> loadActiveRules(String tenantId)`
- `PromotionRuleService` — `create`, `activate` (validate DRL first), `deactivate`, `rollback`, `testRule`, `getHistory`
- `RuleExpiryScheduler` — `@Scheduled(cron = "${promoengine.rules.expiry-cron}")`
- `MmedXmlParser` + `MmlToDrlConverter` (legacy import only)

**Test — `step7/promotion-rule-service.feature`:**

```gherkin
Feature: Promotion rule service

  Background:
    * def PromotionRuleService = Java.type('io.promoengine.rules.PromotionRuleService')
    * def validDrl = read('classpath:fixtures/valid-rule.drl')
    * def invalidDrl = 'not valid drl syntax ##!!'

  Scenario: Create rule saves with DRAFT status
    # Use Spring Boot test context via Karate Spring runner or mock via Java interop
    * def service = karate.call('classpath:helpers/spring-context.js').promotionRuleService
    * def req = { promotionId: 'TEST001', ruleBody: #(validDrl), priority: 10, description: 'Test rule' }
    * def result = service.create(req, 'default')
    * match result.status == 'DRAFT'
    * match result.promotionId == 'TEST001'

  Scenario: Activate rule with invalid DRL throws RuleCompilationException
    * def service = karate.call('classpath:helpers/spring-context.js').promotionRuleService
    * def req = { promotionId: 'BADRL01', ruleBody: #(invalidDrl), priority: 10 }
    * def created = service.create(req, 'default')
    * def fn = function() { service.activate(created.id, 'default') }
    * def result = karate.attempt(fn)
    * match result.class.name == 'io.promoengine.exception.RuleCompilationException'

  Scenario: Activate valid rule updates status to ACTIVE
    * def service = karate.call('classpath:helpers/spring-context.js').promotionRuleService
    * def req = { promotionId: 'VALIDRL01', ruleBody: #(validDrl), priority: 10 }
    * def created = service.create(req, 'default')
    * def activated = service.activate(created.id, 'default')
    * match activated.status == 'ACTIVE'

  Scenario: Deactivate active rule sets status to INACTIVE
    * def service = karate.call('classpath:helpers/spring-context.js').promotionRuleService
    * def activated = service.activate('VALIDRL01', 'default')
    * def deactivated = service.deactivate('VALIDRL01', 'default')
    * match deactivated.status == 'INACTIVE'

  Scenario: Rule history records every status change
    * def service = karate.call('classpath:helpers/spring-context.js').promotionRuleService
    * def history = service.getHistory('VALIDRL01', 'default')
    * match history.size() >= 2
    * match history[0].status == 'DRAFT'
    * match history[1].status == 'ACTIVE'

  Scenario: MmlToDrlConverter produces valid DRL from simple MML
    * def MmlToDrlConverter = Java.type('io.promoengine.rules.MmlToDrlConverter')
    * def converter = new MmlToDrlConverter()
    * def mml = read('classpath:fixtures/sample-mml.txt')
    * def drl = converter.convert('PROMO01', mml)
    * match drl contains 'rule "PROMO01"'
    * match drl contains 'then'
    * match drl contains 'end'
```

---

### Step 8 — Enrichment Repositories

**Build:**
- `OpenSearchPriceRepository` — primary, queries `promoprice` index: (1) sku+storeCode, (2) sku+`999999`
- `DatabasePriceRepository` — Oracle JPA fallback
- Same pattern for item and customer repositories

**Test — `step8/enrichment-repository.feature`:**

```gherkin
Feature: Enrichment repository fallback chain

  Background:
    * def OpenSearchPriceRepository = Java.type('io.promoengine.enrichment.price.OpenSearchPriceRepository')

  Scenario: Price found for sku + storeCode in primary index
    # Mocked via Mockito in Java helper — inject mock ES client
    * def repo = karate.call('classpath:helpers/mock-price-repo.js').withPrice('12345', '0101', 49.99)
    * def result = repo.findPrice('12345', '0101')
    * match result.isPresent() == true
    * match result.get().unitPrice.doubleValue() == 49.99

  Scenario: Price falls back to sentinel store 999999 when store-specific price missing
    * def repo = karate.call('classpath:helpers/mock-price-repo.js').withSentinelPrice('12345', 39.99)
    * def result = repo.findPrice('12345', '9999')
    * match result.isPresent() == true
    * match result.get().unitPrice.doubleValue() == 39.99

  Scenario: Price returns empty when not in ES or DB
    * def repo = karate.call('classpath:helpers/mock-price-repo.js').withNoPrice('UNKNOWN01')
    * def result = repo.findPrice('UNKNOWN01', '0101')
    * match result.isPresent() == false

  Scenario: Item properties resolved from promoitem index
    * def repo = karate.call('classpath:helpers/mock-item-repo.js').withItem('12345', 'ELEC', 'TV', 'D01', false)
    * def result = repo.findProperties('12345')
    * match result.isPresent() == true
    * match result.get().categoryCode == 'ELEC'
    * match result.get().subcategoryCode == 'TV'
    * match result.get().foodItem == false

  Scenario: Customer data resolved from promocustomer index
    * def repo = karate.call('classpath:helpers/mock-customer-repo.js').withCustomer('CUST001', 'VIP', 'B2C')
    * def result = repo.findCustomer('CUST001')
    * match result.isPresent() == true
    * match result.get().customerGroupCode == 'VIP'
```

---

### Step 9 — EnrichmentService

**Build:**
- Parallel `CompletableFuture.allOf()` for price + item + customer
- Items with no price → `skippedItems` list (never silently dropped)
- `EnrichedTransaction` assembled from all three results

**Test — `step9/enrichment-service.feature`:**

```gherkin
Feature: EnrichmentService

  Background:
    * def EnrichmentService = Java.type('io.promoengine.enrichment.EnrichmentService')
    * def CalculateRequest = Java.type('io.promoengine.dto.request.CalculateRequest')
    * def OrderItem = Java.type('io.promoengine.dto.request.OrderItem')

  Scenario: All prices resolved — skippedItems is empty
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js')
        .withPrices({ '12345': 49.99, '67890': 29.99 })
        .withAllItems()
        .build()
    * def request = { storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 2}, {sku: '67890', quantity: 1}] }
    * def tx = service.enrich(request)
    * match tx.skippedItems.size() == 0
    * match tx.enrichedItems.size() == 2

  Scenario: Item with no price goes to skippedItems
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js')
        .withPrices({ '12345': 49.99 })
        .withNoPriceFor('UNKNOWN01')
        .withAllItems()
        .build()
    * def request = { storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 1}, {sku: 'UNKNOWN01', quantity: 1}] }
    * def tx = service.enrich(request)
    * match tx.skippedItems contains 'UNKNOWN01'
    * match tx.enrichedItems.size() == 1
    * match tx.enrichedItems[0].sku == '12345'

  Scenario: Sentinel store 999999 used for price fallback
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js')
        .withSentinelPrice('12345', 39.99)
        .withAllItems()
        .build()
    * def request = { storeId: '9999', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 1}] }
    * def tx = service.enrich(request)
    * match tx.enrichedItems.size() == 1
    * match tx.enrichedItems[0].unitPrice.doubleValue() == 39.99
    * match tx.skippedItems.size() == 0

  Scenario: EnrichedItem lineAmount = unitPrice x quantity
    * def service = karate.call('classpath:helpers/enrichment-service-mock.js')
        .withPrices({ '12345': 50.00 })
        .withAllItems()
        .build()
    * def request = { storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 3}] }
    * def tx = service.enrich(request)
    * match tx.enrichedItems[0].lineAmount.doubleValue() == 150.00
```

---

### Step 10 — Request / Response DTOs

**Build:**
- `CalculateRequest` — `@NotBlank storeId`, `@NotNull customerId`, `@NotNull transactionDate`, `@NotEmpty items`
- `OrderItem` — `@NotBlank sku`, `@Min(1) quantity`
- `CalculateResponse`, `OrderSummary`, `CalculatedItem`, `AppliedPromotion`, `PromotionSummary`, `ErrorResponse`

**Test — `step10/dto-validation.feature`:**

```gherkin
Feature: DTO validation

  Background:
    * def CalculateRequest = Java.type('io.promoengine.dto.request.CalculateRequest')
    * def Validator = Java.type('io.promoengine.helper.BeanValidationHelper')

  Scenario: Valid CalculateRequest passes validation
    * def req = { storeId: '0101', customerId: '123456', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 2}] }
    * def violations = Validator.validate(req)
    * match violations.size() == 0

  Scenario: Missing storeId fails validation
    * def req = { customerId: '123456', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 2}] }
    * def violations = Validator.validate(req)
    * match violations.size() >= 1
    * match violations[0].propertyPath contains 'storeId'

  Scenario: Empty items list fails validation
    * def req = { storeId: '0101', customerId: '123456', transactionDate: '2024-01-15', items: [] }
    * def violations = Validator.validate(req)
    * match violations.size() >= 1

  Scenario: Item quantity zero fails validation
    * def req = { storeId: '0101', customerId: '123456', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 0}] }
    * def violations = Validator.validate(req)
    * match violations.size() >= 1
    * match violations[0].propertyPath contains 'quantity'

  Scenario: CalculateResponse serializes to correct JSON shape
    * def CalculateResponse = Java.type('io.promoengine.dto.response.CalculateResponse')
    * def resp = CalculateResponse.builder()
        .requestId('uuid-001')
        .storeId('0101')
        .skippedItems(['UNKNOWN01'])
        .build()
    * def json = karate.toJson(resp)
    * match json.requestId == 'uuid-001'
    * match json.skippedItems == ['UNKNOWN01']
```

---

### Step 11 — PromotionResponseMapper

**Build:**
- Manual mapper (no MapStruct)
- `promotionType < 10` → `"STANDARD"`, `>= 10` → `"COUPON"`
- All monetary: `BigDecimal.setScale(3, RoundingMode.HALF_UP)`
- `subtotal` = sum of `lineAmount`, `totalDiscount` = sum of discounts, `total` = subtotal − totalDiscount
- `standardDiscount` / `couponDiscount` split per line

**Test — `step11/promotion-response-mapper.feature`:**

```gherkin
Feature: PromotionResponseMapper

  Background:
    * def PromotionResponseMapper = Java.type('io.promoengine.mapper.PromotionResponseMapper')
    * def mapper = new PromotionResponseMapper()

  Scenario: Monetary values rounded to 3 decimal places
    * def result = karate.call('classpath:helpers/mapper-test-builder.js')
        .withDiscount(10.12345678)
        .build()
    * def response = mapper.toResponse(result.calculationResult, result.request, result.enrichedTransaction)
    * match response.summary.totalDiscount.toString() == '10.123'

  Scenario: promotionType 0 maps to STANDARD in response
    * def result = karate.call('classpath:helpers/mapper-test-builder.js').withStandardPromotion().build()
    * def response = mapper.toResponse(result.calculationResult, result.request, result.enrichedTransaction)
    * match response.promotions[0].type == 'STANDARD'

  Scenario: promotionType 10 maps to COUPON in response
    * def result = karate.call('classpath:helpers/mapper-test-builder.js').withCouponPromotion().build()
    * def response = mapper.toResponse(result.calculationResult, result.request, result.enrichedTransaction)
    * match response.promotions[0].type == 'COUPON'

  Scenario: summary.total = subtotal minus totalDiscount
    * def result = karate.call('classpath:helpers/mapper-test-builder.js')
        .withSubtotal(200.0)
        .withDiscount(30.0)
        .build()
    * def response = mapper.toResponse(result.calculationResult, result.request, result.enrichedTransaction)
    * match response.summary.subtotal.doubleValue() == 200.0
    * match response.summary.totalDiscount.doubleValue() == 30.0
    * match response.summary.total.doubleValue() == 170.0

  Scenario: skippedItems propagated from EnrichedTransaction
    * def result = karate.call('classpath:helpers/mapper-test-builder.js')
        .withSkippedItems(['UNKNOWN01', 'UNKNOWN02'])
        .build()
    * def response = mapper.toResponse(result.calculationResult, result.request, result.enrichedTransaction)
    * match response.skippedItems == ['UNKNOWN01', 'UNKNOWN02']
```

---

### Step 12 — PromotionService

**Build:**
```java
ResponseEntity<CalculateResponse> calculate(CalculateRequest request, String tenantId) {
    EnrichedTransaction tx = enrichmentService.enrich(request);
    PromotionRuleEngine engine = tenantEngineRegistry.getEngine(tenantId);
    CalculationResult result = engine.calculate(tx);
    CalculateResponse response = responseMapper.toResponse(result, request, tx);

    HttpStatus status = tx.getSkippedItems().isEmpty()
        ? HttpStatus.OK               // 200 — all items calculated
        : HttpStatus.PARTIAL_CONTENT; // 206 — some skipped
    return ResponseEntity.status(status).body(response);
}
```

No pool acquire/release — `StatelessKieSession` is disposed inside `engine.calculate()`.

**Test — `step12/promotion-service.feature`:**

```gherkin
Feature: PromotionService orchestration

  Background:
    * def service = karate.call('classpath:helpers/promotion-service-mock.js').build()

  Scenario: All items calculated returns CalculationResult with no skippedItems
    * def request = { storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 2}] }
    * def response = service.calculate(request, 'default')
    * match response.statusCodeValue == 200
    * match response.body.skippedItems.size() == 0

  Scenario: Some items skipped returns 206 Partial Content
    * def request = { storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 1}, {sku: 'UNKNOWN', quantity: 1}] }
    * def response = service.calculate(request, 'default')
    * match response.statusCodeValue == 206
    * match response.body.skippedItems contains 'UNKNOWN'

  Scenario: Enrichment failure throws EnrichmentException
    * def badService = karate.call('classpath:helpers/promotion-service-mock.js').withEnrichmentFailure().build()
    * def fn = function() { badService.calculate({ storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 1}] }, 'default') }
    * def result = karate.attempt(fn)
    * match result.class.name == 'io.promoengine.exception.EnrichmentException'

  Scenario: Unknown tenant throws TenantNotFoundException
    * def fn = function() { service.calculate({ storeId: '0101', customerId: 'CUST001', transactionDate: '2024-01-15', items: [{sku: '12345', quantity: 1}] }, 'unknown-tenant') }
    * def result = karate.attempt(fn)
    * match result.class.name == 'io.promoengine.exception.TenantNotFoundException'
```

---

### Step 13 — Controllers

**Build:**
- `PromotionController` — `POST /api/v1/promotions/calculate`
- `PromotionRuleController` — full CRUD + lifecycle
- `PromotionPresetController` — preset CRUD
- `RuleImportController` — file upload + S3 trigger
- `VersionController` — `GET /api/v1/version`

**Test — `step13/calculate-api.feature`:** (Karate HTTP — server must be running)

```gherkin
Feature: POST /api/v1/promotions/calculate

  Background:
    * url baseUrl
    * header X-API-Key = apiKey
    * def validRequest =
    """
    {
      "requestId": "test-001",
      "storeId": "0101",
      "customerId": "123456",
      "transactionDate": "2024-01-15",
      "items": [
        { "sku": "12345678", "quantity": 2 },
        { "sku": "98765432", "quantity": 1 }
      ]
    }
    """

  Scenario: Full calculation returns 200 with correct response shape
    Given path '/api/v1/promotions/calculate'
    And request validRequest
    When method POST
    Then status 200
    And match response.requestId == 'test-001'
    And match response.storeId == '0101'
    And match response.summary != null
    And match response.summary.subtotal >= 0
    And match response.summary.total >= 0
    And match response.items != null
    And match response.promotions != null
    And match response.skippedItems == []
    And match response.calculatedAt != null

  Scenario: Unknown SKU appears in skippedItems with 206 status
    Given path '/api/v1/promotions/calculate'
    And request
    """
    {
      "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15",
      "items": [{"sku": "DOESNOTEXIST999", "quantity": 1}]
    }
    """
    When method POST
    Then status 206
    And match response.skippedItems contains 'DOESNOTEXIST999'
    And match response.items.size() == 0

  Scenario: Missing storeId returns 400
    Given path '/api/v1/promotions/calculate'
    And request { "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 1}] }
    When method POST
    Then status 400
    And match response.errorCode != null

  Scenario: Empty items array returns 400
    Given path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [] }
    When method POST
    Then status 400

  Scenario: item quantity 0 returns 400
    Given path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 0}] }
    When method POST
    Then status 400
```

**Test — `step13/rules-api.feature`:**

```gherkin
Feature: Promotion Rule CRUD API

  Background:
    * url baseUrl
    * header X-API-Key = apiKey

  Scenario: Create rule saves as DRAFT
    Given path '/api/v1/rules'
    And request { "promotionId": "TEST-K01", "ruleBody": "rule 'test' when then end", "priority": 10, "description": "Karate test rule" }
    When method POST
    Then status 201
    And match response.status == 'DRAFT'
    And match response.promotionId == 'TEST-K01'
    * def ruleId = response.id

  Scenario: List rules returns created rule
    Given path '/api/v1/rules'
    When method GET
    Then status 200
    And match response.content != null

  Scenario: Activate rule with invalid DRL returns 422
    Given path '/api/v1/rules'
    And request { "promotionId": "BAD-DRL-01", "ruleBody": "NOT VALID DRL !@#$", "priority": 5, "description": "Bad rule" }
    When method POST
    Then status 201
    * def badId = response.id
    Given path '/api/v1/rules/' + badId + '/activate'
    When method POST
    Then status 422
    And match response.errorCode == 14

  Scenario: Deactivate rule sets status to INACTIVE
    Given path '/api/v1/rules/' + ruleId + '/deactivate'
    When method POST
    Then status 200
    And match response.status == 'INACTIVE'

  Scenario: Get rule history shows all status changes
    Given path '/api/v1/rules/' + ruleId + '/history'
    When method GET
    Then status 200
    And match response.size() >= 1

  Scenario: Version endpoint returns product info
    Given path '/api/v1/version'
    When method GET
    Then status 200
    And match response.version != null
    And match response.droolsVersion == '8.44.0.Final'
```

---

### Step 14 — Security

**Build:**
- `ApiKeyAuthFilter` — reads `X-API-Key`, resolves `tenantId` via `TenantResolver`
- Whitelist: `/actuator/health`, `/actuator/info`, `/swagger-ui.html`, `/v3/api-docs/**`
- `SecurityConfig` — stateless, no CSRF

**Test — `step14/security-api.feature`:**

```gherkin
Feature: API Key security

  Background:
    * url baseUrl

  Scenario: Request with valid API key succeeds
    Given header X-API-Key = apiKey
    And path '/api/v1/version'
    When method GET
    Then status 200

  Scenario: Request with no API key returns 401
    Given path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 1}] }
    When method POST
    Then status 401
    And match response.errorCode != null

  Scenario: Request with wrong API key returns 401
    Given header X-API-Key = 'wrong-key-12345'
    And path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 1}] }
    When method POST
    Then status 401

  Scenario: Health check accessible without API key
    Given path '/actuator/health'
    When method GET
    Then status 200
    And match response.status == 'UP'

  Scenario: Swagger UI accessible without API key
    Given path '/swagger-ui.html'
    When method GET
    Then status 200

  Scenario: Rule endpoint blocked without API key
    Given path '/api/v1/rules'
    When method GET
    Then status 401
```

---

### Step 15 — GlobalExceptionHandler

**Build:**
```java
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RuleCompilationException.class) // 422, errorCode=14
    @ExceptionHandler(TenantNotFoundException.class)  // 401, errorCode=50
    @ExceptionHandler(EnrichmentException.class)      // 500, errorCode=99
    @ExceptionHandler(EngineInitException.class)      // 500, errorCode=41
    @ExceptionHandler(MethodArgumentNotValidException.class) // 400, errorCode=0
    @ExceptionHandler(Exception.class)                // 500, errorCode=99
    // All → ErrorResponse { errorCode, message }
    // Log full stack trace for 5xx; compact message for 4xx
}
```

**Test — `step15/exception-handler.feature`:**

```gherkin
Feature: GlobalExceptionHandler — standard error response shapes

  Background:
    * url baseUrl
    * header X-API-Key = apiKey

  Scenario: Validation error returns 400 with errorCode and message
    Given path '/api/v1/promotions/calculate'
    And request { "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 1}] }
    When method POST
    Then status 400
    And match response.errorCode != null
    And match response.message != null

  Scenario: Invalid DRL activation returns 422 with errorCode 14
    Given path '/api/v1/rules'
    And request { "promotionId": "ERR-TEST-01", "ruleBody": "INVALID !!", "priority": 5, "description": "error test" }
    When method POST
    Then status 201
    * def id = response.id
    Given path '/api/v1/rules/' + id + '/activate'
    When method POST
    Then status 422
    And match response.errorCode == 14
    And match response.message != null

  Scenario: Missing API key returns 401 with errorCode 50
    Given path '/api/v1/rules'
    When method GET
    Then status 401
    And match response.errorCode == 50

  Scenario: Error response never exposes stack trace to client
    Given path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "TRIGGER-ERROR", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 1}] }
    When method POST
    # Whatever status, body should NOT contain java stack trace
    * def body = response
    * match karate.toString(body) !contains 'at io.promoengine'
    * match karate.toString(body) !contains 'Exception'
```

---

### Step 16 — Full Karate E2E Test Suite

**Build:** `src/test/java/io/promoengine/step16/PromoEngineE2ETest.java` — runs all feature files as a single suite.

```java
@Karate.Test
Karate runAll() {
    return Karate.run("classpath:io/promoengine").tags("~@ignore");
}
```

**Test — `step16/e2e-suite.feature`:**

```gherkin
Feature: PromoEngine end-to-end regression suite

  Background:
    * url baseUrl
    * header X-API-Key = apiKey

  # ─── RULE LIFECYCLE ───────────────────────────────────────────────
  Scenario: Full rule lifecycle — create → activate → calculate → deactivate
    # 1. Create rule
    Given path '/api/v1/rules'
    And request
    """
    {
      "promotionId": "E2E-RULE-01",
      "priority": 10,
      "description": "E2E 10% off all items",
      "ruleBody": "package io.promoengine.rules; import io.promoengine.engine.model.*; import java.math.BigDecimal; import java.util.List; global List results; rule \"E2E 10% Off\" salience 10 when $tx : Transaction() then results.add(PromotionResult.builder().promotionId(\"E2E-RULE-01\").promotionType(0).description(\"10% off\").discountAmount(new BigDecimal(\"10.00\")).timesApplied(1.0).build()); end"
    }
    """
    When method POST
    Then status 201
    And match response.status == 'DRAFT'
    * def ruleId = response.id

    # 2. Activate rule
    Given path '/api/v1/rules/' + ruleId + '/activate'
    When method POST
    Then status 200
    And match response.status == 'ACTIVE'

    # 3. Calculate — rule should fire
    Given path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345678", "quantity": 1}] }
    When method POST
    Then status 200
    And match response.promotions[?(@.promotionId == 'E2E-RULE-01')].size() >= 0
    # (Promotion fires if price is found for this SKU)

    # 4. Deactivate rule
    Given path '/api/v1/rules/' + ruleId + '/deactivate'
    When method POST
    Then status 200
    And match response.status == 'INACTIVE'

    # 5. History shows full lifecycle
    Given path '/api/v1/rules/' + ruleId + '/history'
    When method GET
    Then status 200
    And match response.size() >= 2

  # ─── MULTI-TENANT ISOLATION ─────────────────────────────────────
  Scenario: Rules from tenant-a do not fire for tenant-b
    # tenant-a creates a rule
    * header X-API-Key = karate.properties['tenant.a.key'] || apiKey
    Given path '/api/v1/rules'
    And request { "promotionId": "TENANT-A-RULE", "priority": 10, "description": "Tenant A only", "ruleBody": "..." }
    When method POST
    Then status 201
    # tenant-b calculates — should not see tenant-a's rule
    * header X-API-Key = karate.properties['tenant.b.key'] || apiKey
    Given path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345678", "quantity": 1}] }
    When method POST
    Then status 200
    And match response.promotions[?(@.promotionId == 'TENANT-A-RULE')].size() == 0

  # ─── CONCURRENT REQUESTS ─────────────────────────────────────────
  Scenario: Engine handles concurrent calculation requests
    * def calcFn = function(i) {
        var result = karate.call('classpath:io/promoengine/step13/calculate-api.feature@happy-path');
        return result.responseStatus;
      }
    * def results = karate.repeat(5, calcFn)
    * match each results == 200

  # ─── ROLLBACK ────────────────────────────────────────────────────
  Scenario: Rule rollback restores previous version
    # Create and activate v1
    Given path '/api/v1/rules'
    And request { "promotionId": "ROLLBACK-TEST", "priority": 10, "description": "v1", "ruleBody": "..." }
    When method POST
    Then status 201
    * def ruleId = response.id
    Given path '/api/v1/rules/' + ruleId + '/activate'
    When method POST
    Then status 200

    # Update to v2
    Given path '/api/v1/rules/' + ruleId
    And request { "ruleBody": "... v2 ...", "description": "v2" }
    When method PUT
    Then status 200
    And match response.version == 2

    # Rollback to v1
    Given path '/api/v1/rules/' + ruleId + '/rollback?version=1'
    When method POST
    Then status 200
    And match response.description == 'v1'
    And match response.version == 1

  # ─── VERSION CHECK ───────────────────────────────────────────────
  Scenario: Version endpoint confirms Drools version
    Given path '/api/v1/version'
    When method GET
    Then status 200
    And match response.version != null
    And match response.droolsVersion == '8.44.0.Final'
    And match response.builtAt != null
```

---

### Step 17 — Dockerfile

**Build:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/promoengine-service-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s \
  CMD wget -q -O- http://localhost:8080/promotionengine/api/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Test — `step17/docker-health.feature`:** (set `karate.env=docker`, run after `docker run`)

```gherkin
Feature: Docker container health check

  Background:
    * url baseUrl   # resolves to Docker host from karate-config.js

  Scenario: Container starts and health endpoint returns UP
    Given path '/actuator/health'
    When method GET
    Then status 200
    And match response.status == 'UP'

  Scenario: Calculate endpoint responds inside container
    Given header X-API-Key = apiKey
    And path '/api/v1/promotions/calculate'
    And request { "storeId": "0101", "customerId": "123456", "transactionDate": "2024-01-15", "items": [{"sku": "12345", "quantity": 1}] }
    When method POST
    # 200 or 206 both acceptable (depends on whether test data exists)
    Then assert responseStatus == 200 || responseStatus == 206
```

---

## Business Rules — Must Preserve

| Rule | Detail |
|------|--------|
| No-price items → `skippedItems` | Never silently drop; always explicit in response |
| Sentinel store fallback | Price: store → storeCode `999999` → Oracle DB |
| `promotionType < 10` | STANDARD |
| `promotionType >= 10` | COUPON |
| Monetary rounding | 3 decimal places, `RoundingMode.HALF_UP`, always `BigDecimal` |
| `lineAmount` formula | `unitPrice × quantity` (price from enrichment, never from request) |
| Tenant isolation | API key → tenantId; all DB + engine operations scoped to tenantId |
| Rule never hard-deleted | DRAFT → ACTIVE → INACTIVE; full history table |
| 206 on partial result | `HTTP 206 Partial Content` when `skippedItems[]` is non-empty |
| DRL validated before activate | Compile test KieBase; reject 422 + errorCode 14 if errors |

---

## Configuration — application.yml (complete)

```yaml
server:
  port: 8080
  servlet:
    context-path: ${PE_CONTEXT_PATH:/promotionengine/api}
    # Legacy cutover: set PE_CONTEXT_PATH=/promotionservice/calculateinvoicecore

spring:
  application:
    name: promoengine-service
  datasource:
    url: ${PE_DB_URL}
    username: ${PE_DB_USERNAME}
    password: ${PE_DB_PASSWORD}
    driver-class-name: ${PE_DB_DRIVER:oracle.jdbc.OracleDriver}
    hikari:
      minimum-idle: ${PE_DB_MIN_IDLE:2}
      maximum-pool-size: ${PE_DB_MAX_POOL:10}
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
  elasticsearch:
    uris: ${PE_OPENSEARCH_URI}
    username: ${PE_OPENSEARCH_USERNAME:}
    password: ${PE_OPENSEARCH_PASSWORD:}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

springdoc:
  swagger-ui:
    path: /swagger-ui.html

promoengine:
  engine:
    debug-level: ${PE_ENGINE_DEBUG:0}
    nbr-decimals: ${PE_ENGINE_DECIMALS:3}
  rules:
    s3-bucket: ${PE_S3_BUCKET:}
    s3-key: ${PE_S3_KEY:ruleset/ruleset.xml}
    expiry-cron: ${PE_EXPIRY_CRON:0 0 2 * * *}
    compile-timeout-ms: ${PE_RULE_COMPILE_TIMEOUT_MS:10000}
  tenancy:
    default-tenant-id: ${PE_DEFAULT_TENANT:default}
    enabled: ${PE_MULTI_TENANT:true}
  security:
    api-key: ${PE_API_KEY:change-me}
    header-name: X-API-Key
    tenants:
      ${PE_API_KEY:change-me}: ${PE_DEFAULT_TENANT:default}
  s3:
    region: ${PE_AWS_REGION:ap-southeast-1}
  data:
    price-index: ${PE_PRICE_INDEX:promoprice}
    item-index: ${PE_ITEM_INDEX:promoitem}
    customer-index: ${PE_CUSTOMER_INDEX:promocustomer}

logging:
  level:
    io.promoengine: ${PE_LOG_LEVEL:INFO}
```

---

## Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| 0 | 200/206 | Success |
| 14 | 422 | DRL compilation error |
| 16 | 400 | No active rules for tenant |
| 41 | 500 | Drools engine init failed |
| 50 | 401 | Invalid or missing API key |
| 99 | 500 | Unexpected error |

---

## Coding Conventions

- **DTOs/POJOs:** `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- **JPA Entities:** `@Getter @Setter @Builder` — NOT `@Data` (breaks equals/hashCode with JPA)
- **No MapStruct** — write `PromotionResponseMapper` manually (non-trivial calculation logic)
- **BigDecimal** for all monetary values — never `double` or `float`
- **`@Transactional`** on all service methods that write to DB
- **Constructor injection only** — never `@Autowired` field injection
- **JAXB** imports appear ONLY in `MmedXmlParser.java` — nowhere else

---

## DRL Rule Format — Reference

```drl
package io.promoengine.rules;

import io.promoengine.engine.model.InvoiceItem;
import io.promoengine.engine.model.Transaction;
import io.promoengine.engine.model.Customer;
import io.promoengine.engine.model.PromotionResult;
import java.math.BigDecimal;
import java.util.List;

global List<PromotionResult> results;

rule "Example — 10% Off Electronics Over $100"
    salience 10
    when
        $tx : Transaction()
        accumulate(
            InvoiceItem(categoryCode == "ELEC", $amount : lineAmount),
            $total : sum($amount)
        )
        eval($total.doubleValue() > 100.0)
    then
        results.add(PromotionResult.builder()
            .promotionId("ELEC10PCT")
            .promotionType(0)
            .description("10% off electronics over $100")
            .discountAmount($total.multiply(new BigDecimal("0.10")))
            .timesApplied(1.0)
            .build());
end
```

The `package`, `import`, and `global` header is **prepended automatically** by `PromotionRuleEngine.reload()` — callers store only the `rule "..." ... end` block in the DB.

---

## REST API Contract (Full)

### POST /api/v1/promotions/calculate — Request

```json
// Headers:
// Content-Type: application/json
// X-API-Key: <key>

{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "storeId": "0101",
  "customerId": 123456,
  "transactionDate": "2024-01-15",
  "items": [
    { "sku": "12345678", "quantity": 2 },
    { "sku": "98765432", "quantity": 1 },
    { "sku": "UNKNOWN99", "quantity": 1 }
  ]
}
```

### POST /api/v1/promotions/calculate — Response (200 OK / 206 Partial Content)

`200 OK` when all items calculated. `206 Partial Content` when `skippedItems[]` non-empty.

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "storeId": "0101",
  "calculatedAt": "2024-01-15T10:30:00Z",
  "summary": {
    "subtotal":      299.700,
    "totalDiscount":  30.000,
    "total":         269.700
  },
  "items": [
    {
      "sku": "12345678",
      "quantity": 2,
      "unitPrice": 99.900,
      "subtotal": 199.800,
      "standardDiscount": 20.000,
      "couponDiscount": 0.000,
      "finalAmount": 179.800,
      "appliedPromotions": [
        { "promotionId": "PROMO001", "type": "STANDARD", "discountAmount": 20.000 }
      ]
    },
    {
      "sku": "98765432",
      "quantity": 1,
      "unitPrice": 99.900,
      "subtotal": 99.900,
      "standardDiscount": 10.000,
      "couponDiscount": 0.000,
      "finalAmount": 89.900,
      "appliedPromotions": []
    }
  ],
  "promotions": [
    {
      "promotionId": "PROMO001",
      "type": "STANDARD",
      "description": "Buy 2 electronics — 10% off",
      "timesApplied": 1.0,
      "discountAmount": 20.000,
      "prerequisites": []
    }
  ],
  "skippedItems": ["UNKNOWN99"]
}
```

### Field Glossary — Legacy to PromoEngine

| Legacy Field (SOAP/JAXB) | PromoEngine JSON Field          | Description                      |
|--------------------------|---------------------------------|----------------------------------|
| `Inv`                    | `requestId`                     | Transaction identifier           |
| `Sto`                    | `storeId`                       | Store code                       |
| `NTot`                   | `summary.subtotal`              | Total before discounts           |
| `DTot`                   | `summary.totalDiscount`         | Sum of all discounts             |
| `ITot`                   | `summary.total`                 | Final payable amount             |
| `Lin.Art`                | `items[].sku`                   | Article SKU number               |
| `Lin.NAm`                | `items[].subtotal`              | Net amount for this line         |
| `Lin.NPr`                | `items[].unitPrice`             | Unit price                       |
| `Lin.SDi`                | `items[].standardDiscount`      | Standard promotion discount      |
| `Lin.CDi`                | `items[].couponDiscount`        | Coupon discount                  |
| `Lin.SPr`                | `items[].finalAmount`           | Line amount after discounts      |
| `Lin.Pro[SPI]`           | `items[].appliedPromotions[] type=STANDARD` | Standard promotion  |
| `Lin.Pro[CPI]`           | `items[].appliedPromotions[] type=COUPON`   | Coupon promotion    |
| `Res.Tim`                | `promotions[].timesApplied`     | Times promotion fired            |
| `Res.Txt`                | `promotions[].description`      | Promotion description text       |
| `Res.Dis`                | `promotions[].discountAmount`   | Total discount for this promo    |
| `Res.Pre`                | `promotions[].prerequisites[]`  | Required prerequisite items      |
| *(silent drop)*          | `skippedItems[]`                | Items with no price — explicit   |

---

## Zero-XML Transaction Flow (Section 4)

```
POST /api/v1/promotions/calculate
Content-Type: application/json
X-API-Key: <key>
Body: CalculateRequest (JSON)
          │
          ▼  PromotionController
CalculateRequest { storeId, customerId, transactionDate, items[{sku, qty}] }
          │
          ▼  PromotionService (orchestrator)
          │
    ┌─────┴─────────────────────────────────────┐
    │  PARALLEL (CompletableFuture)              │
    │  ├─ PriceService.fetchPrices(skus,store)   │  OpenSearch promoprice → DB fallback
    │  ├─ ItemService.fetchProperties(skus)      │  OpenSearch promoitem  → DB fallback
    │  └─ CustomerService.fetchCustomer(custId)  │  OpenSearch promocustomer → DB fallback
    └─────┬─────────────────────────────────────┘
          │
          ▼  EnrichedTransaction assembled
    {
      priceMap:     Map<sku → PriceData>
      propertyMap:  Map<sku → ItemPropertyData>
      customerData: CustomerData
      skippedItems: [skus with no price found]    ← explicit, not silently dropped
    }
          │
          ▼  TenantEngineRegistry.getEngine(tenantId)
    PromotionRuleEngine (Drools — thread-safe KieBase, no pool acquisition needed)
          │
          ▼  PromotionRuleEngine.calculate(EnrichedTransaction)
    ┌─────────────────────────────────────────────────────────────┐
    │  session = kieBase.newStatelessKieSession()                 │
    │  results = new ArrayList<PromotionResult>()                 │
    │  session.setGlobal("results", results)                      │
    │                                                             │
    │  facts = EngineFactBuilder.build(tx)                        │
    │    → Transaction( storeId, customerId, transactionDate )    │
    │    → Customer( customerId, customerGroupCode )              │
    │    → InvoiceItem( sku, storeCode, categoryCode,            │
    │                   unitPrice, lineAmount, quantity, ... )    │
    │      (one per enriched item — skippedItems excluded)        │
    │                                                             │
    │  session.execute(facts)                                     │
    │    → DRL rules fire, populate 'results' global             │
    │    → List<PromotionResult> (typed Java POJOs)              │
    │  session discarded — KieBase reference released             │
    └─────────────────────────────────────────────────────────────┘
          │
    CalculationResult {
      promotionResults: List<PromotionResult>
      lineResults:      Map<sku → LineResult>
      skippedItems:     List<String>
    }
          │
          ▼  PromotionResponseMapper.toResponse(result, request)
    CalculateResponse (JSON) ← rounded to 3dp, STANDARD/COUPON enum
          │
          ▼
    200 OK or 206 Partial Content
```

---

## Data Enrichment Layer (Section 9)

### Price Lookup — 3-Step Fallback

| Step | Source                              | Query                  | On Miss        |
|------|-------------------------------------|------------------------|----------------|
| 1    | OpenSearch index `promoprice`       | sku + storeId          | Try step 2     |
| 2    | OpenSearch index `promoprice`       | sku + sentinel `999999`| Try step 3     |
| 3    | Oracle DB (price table)             | item_no + store_code   | Add to skipped |

Steps 1 and 2 run **in parallel** via `CompletableFuture`. Step 3 only if both miss.

### Item Properties — 2-Step Fallback

| Step | Source                       | Data Retrieved                                          |
|------|------------------------------|---------------------------------------------------------|
| 1    | OpenSearch index `promoitem` | subGroupNo, groupNo, deptNo, foodIndicator, custom props|
| 2    | Oracle DB (item property)    | Same fields — SQL fallback                              |

### Customer Data — 2-Step Fallback

| Step | Source                           | Data Retrieved                               |
|------|----------------------------------|----------------------------------------------|
| 1    | OpenSearch index `promocustomer` | customerGroupNo, customerTypeNo, cardTypeNo  |
| 2    | Oracle DB (customer property)    | Same fields — SQL fallback                   |

### EnrichmentService Code Pattern

```java
// io.promoengine.enrichment.EnrichmentService
EnrichedTransaction enrich(CalculateRequest req) {
    List<String> skus = req.getItems().stream().map(OrderItem::getSku).toList();

    var pricesFuture     = CompletableFuture.supplyAsync(() -> priceRepository.findPrices(skus, req.getStoreId()));
    var propertiesFuture = CompletableFuture.supplyAsync(() -> itemRepository.findProperties(skus));
    var customerFuture   = CompletableFuture.supplyAsync(() -> customerRepository.findCustomer(req.getCustomerId()));

    CompletableFuture.allOf(pricesFuture, propertiesFuture, customerFuture).join();

    Map<String, PriceData>        prices     = pricesFuture.join();
    Map<String, ItemPropertyData> properties = propertiesFuture.join();
    Optional<CustomerData>        customer   = customerFuture.join();

    List<EnrichedItem> enrichedItems = new ArrayList<>();
    List<String>       skippedItems  = new ArrayList<>();

    for (OrderItem item : req.getItems()) {
        if (prices.containsKey(item.getSku())) {
            enrichedItems.add(EnrichedItem.from(item, prices.get(item.getSku()), properties.get(item.getSku())));
        } else {
            skippedItems.add(item.getSku());  // explicit — not silently dropped
        }
    }

    return EnrichedTransaction.builder()
        .storeId(req.getStoreId())
        .customerId(req.getCustomerId())
        .transactionDate(req.getTransactionDate())
        .enrichedItems(enrichedItems)
        .customerData(customer.orElse(null))
        .skippedItems(skippedItems)
        .build();
}
```

---

## Promotion Rule Architecture (Section 10)

### 3-Tier Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  TIER 1 — SOURCE OF TRUTH: Database                                 │
│  promotion_rule table: one row per promotion rule                   │
│    rule_body column: DRL text (Drools Rule Language)                │
│  promotion_rule_history: full version history                       │
│  promotion_preset table: preset definitions                         │
│                                                                     │
│  Rule lifecycle:  DRAFT → ACTIVE → INACTIVE (never deleted)         │
│  S3 is IMPORT ONLY — initial bulk load or migration                 │
└───────────────────────┬─────────────────────────────────────────────┘
                        │  on startup / on rule change
                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  TIER 2 — COMPILED RULE SET: JVM Heap                               │
│  PromotionRuleLoader: DB → List<RuleDefinition>                     │
│  DRL text → KieFileSystem → KieBuilder → KieBase                    │
│                                                                     │
│  KieBase: compiled, immutable, thread-safe                          │
│  Reload: compile new KieBase → AtomicReference.set(newBase)         │
│    Downtime = 0. In-flight sessions complete with old KieBase.      │
└───────────────────────┬─────────────────────────────────────────────┘
                        │  one per tenant
                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  TIER 3 — CALCULATION: TenantEngineRegistry                         │
│  ConcurrentHashMap<tenantId, PromotionRuleEngine>                   │
│  Per calculation: session = kieBase.newStatelessKieSession()         │
│    → session.execute(facts)  → results list populated by DRL rules  │
│    → session discarded (lightweight, no pooling needed)             │
└─────────────────────────────────────────────────────────────────────┘
```

### Rule Lifecycle States

| State      | Meaning                                             | In Engine? |
|------------|-----------------------------------------------------|------------|
| `DRAFT`    | Created but not yet active. Testable via dry-run API.| No         |
| `ACTIVE`   | Loaded into KieBase. Applied to all calculations.   | Yes        |
| `INACTIVE` | Deactivated. Removed from engine. Preserved in DB.  | No         |

### Zero-Downtime Reload Pattern

```java
// PromotionRuleEngine.calculate() — always snapshots the reference first
CalculationResult calculate(EnrichedTransaction tx) {
    KieBase base = kieBaseRef.get();     // snapshot — reload-safe
    StatelessKieSession session = base.newStatelessKieSession();
    List<PromotionResult> results = new ArrayList<>();
    session.setGlobal("results", results);
    session.execute(EngineFactBuilder.build(tx));
    return new CalculationResult(results, tx.getSkippedItems());
    // session discarded — base reference released — eligible for GC if swapped
}

// PromotionRuleEngine.reload() — atomic swap, zero blocked requests
void reload(List<RuleDefinition> rules) {
    KieBase newBase = compile(rules);     // build + validate DRL (~100–500ms)
    kieBaseRef.set(newBase);             // atomic swap — never blocks callers
    log.info("[{}] Rules reloaded: {} active rules", tenantId, rules.size());
}
```

---

## Database Schema (Section 10)

```sql
-- Individual promotion rules (one row = one promotion)
CREATE TABLE promotion_rule (
    id               VARCHAR2(100)   PRIMARY KEY,        -- promotionId
    definition_id    VARCHAR2(100)   NOT NULL,
    tenant_id        VARCHAR2(50)    NOT NULL DEFAULT 'default',
    rule_type        VARCHAR2(10)    NOT NULL,            -- STANDARD | COUPON
    mmed_group       NUMBER(3)       NOT NULL,            -- 1=standard, 2=coupon
    priority         NUMBER(5)       NOT NULL DEFAULT 10,
    rule_body        CLOB            NOT NULL,            -- DRL rule text
    description      VARCHAR2(4000),
    description_i18n CLOB,                               -- JSON: { "TH": "...", "FR": "..." }
    start_date       DATE,
    end_date         DATE,
    status           VARCHAR2(10)    DEFAULT 'DRAFT',     -- DRAFT | ACTIVE | INACTIVE
    version          NUMBER(5)       DEFAULT 1,
    created_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR2(100),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR2(100)
);

-- Preset definitions (item/customer groupings used as rule conditions)
CREATE TABLE promotion_preset (
    id          VARCHAR2(100)   PRIMARY KEY,
    tenant_id   VARCHAR2(50)    NOT NULL DEFAULT 'default',
    mmed_group  NUMBER(3),
    rule_body   CLOB            NOT NULL,                -- DRL rule text
    status      VARCHAR2(10)    DEFAULT 'ACTIVE',
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- Full audit trail — every version of every rule is preserved
CREATE TABLE promotion_rule_history (
    history_id    NUMBER          GENERATED ALWAYS AS IDENTITY,
    rule_id       VARCHAR2(100)   NOT NULL,
    version       NUMBER(5)       NOT NULL,
    rule_body     CLOB            NOT NULL,              -- DRL rule text
    status        VARCHAR2(10),
    changed_at    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    changed_by    VARCHAR2(100),
    change_reason VARCHAR2(500),
    PRIMARY KEY (history_id)
);
```

---

## Complete Endpoint List (Section 12)

### Calculation

| Method | Path                             | Auth     | Response          |
|--------|----------------------------------|----------|-------------------|
| POST   | `/api/v1/promotions/calculate`   | X-API-Key| 200/206 CalculateResponse |

### Promotion Rule Management (CRUD)

| Method | Path                                  | Auth     | Description                               | Response                     |
|--------|---------------------------------------|----------|-------------------------------------------|------------------------------|
| POST   | `/api/v1/rules`                       | X-API-Key| Create rule (DRAFT status)                | 201 PromotionRuleResponse    |
| GET    | `/api/v1/rules`                       | X-API-Key| List rules (filter: status/type/date/page)| 200 Page<PromotionRuleResponse>|
| GET    | `/api/v1/rules/{id}`                  | X-API-Key| Get single rule by ID                     | 200 PromotionRuleResponse    |
| PUT    | `/api/v1/rules/{id}`                  | X-API-Key| Update rule (new version → resets DRAFT)  | 200 PromotionRuleResponse    |
| POST   | `/api/v1/rules/{id}/activate`         | X-API-Key| Activate DRAFT → pushes to engine (atomic)| 200 PromotionRuleResponse    |
| POST   | `/api/v1/rules/{id}/deactivate`       | X-API-Key| Deactivate ACTIVE → removes from engine   | 200 PromotionRuleResponse    |
| POST   | `/api/v1/rules/{id}/rollback`         | X-API-Key| Rollback to version N `{ "version": N }`  | 200 PromotionRuleResponse    |
| POST   | `/api/v1/rules/{id}/test`             | X-API-Key| Dry-run: test DRAFT rule (no activation)  | 200 CalculateResponse        |
| GET    | `/api/v1/rules/{id}/history`          | X-API-Key| Full version history                      | 200 List<RuleHistoryResponse>|

### Preset Management

| Method | Path                   | Auth     | Description               | Response           |
|--------|------------------------|----------|---------------------------|--------------------|
| POST   | `/api/v1/presets`      | X-API-Key| Create preset             | 201 PresetResponse |
| GET    | `/api/v1/presets`      | X-API-Key| List all presets          | 200 List<PresetResponse> |
| PUT    | `/api/v1/presets/{id}` | X-API-Key| Update preset             | 200 PresetResponse |
| DELETE | `/api/v1/presets/{id}` | X-API-Key| Deactivate preset         | 200 PresetResponse |

### Bulk Import (Backwards Compatibility)

| Method | Path                      | Auth     | Description                                    | Response            |
|--------|---------------------------|----------|------------------------------------------------|---------------------|
| POST   | `/api/v1/rules/import`    | X-API-Key| Bulk import from multipart XML file upload     | 200 ImportResponse  |
| POST   | `/api/v1/rules/import/s3` | X-API-Key| Trigger bulk import from configured S3 path    | 200 ImportResponse  |

### System

| Method | Path               | Auth  | Description                      | Response       |
|--------|--------------------|-------|----------------------------------|----------------|
| GET    | `/api/v1/version`  | X-API-Key | Engine version info          | 200 VersionResponse |
| GET    | `/actuator/health` | None  | K8s liveness/readiness probe    | 200 UP / 503 DOWN |

### Rule Management Request / Response Shapes

```json
// POST /api/v1/rules — Create rule
{
  "promotionId": "PROMO_BUY2GET10",
  "definitionId": "DEF_BUY2GET10",
  "ruleType": "STANDARD",
  "priority": 10,
  "ruleBody": "rule \"Buy 2 Get 10%\" when ... then ... end",
  "description": "Buy 2 electronics — 10% off",
  "descriptionI18n": { "TH": "ซื้อ 2 ลด 10%" },
  "startDate": "2024-01-01",
  "endDate": "2024-12-31"
}

// Response
{
  "id": "PROMO_BUY2GET10",
  "ruleType": "STANDARD",
  "status": "DRAFT",
  "version": 1,
  "createdAt": "2024-01-10T09:00:00Z",
  "createdBy": "api-key-owner"
}

// GET /api/v1/rules?status=ACTIVE&type=STANDARD&page=0&size=20
{
  "content": [ ... ],
  "totalElements": 45,
  "totalPages": 3,
  "page": 0,
  "size": 20
}

// POST /api/v1/rules/{id}/test — dry-run (same shape as calculate request)
{
  "storeId": "0101",
  "customerId": 0,
  "transactionDate": "2024-01-15",
  "items": [ { "sku": "12345678", "quantity": 2 } ]
}
// Returns CalculateResponse showing what this DRAFT rule would produce
```

---

## Engine POJO Model Tables (Section 2)

### InvoiceItem / Transaction / Customer Fields

| POJO Class    | Field              | Description                 | Source                                      |
|---------------|--------------------|-----------------------------|---------------------------------------------|
| `InvoiceItem` | `sku`              | Article / SKU number        | `CalculateRequest.items[].sku`              |
| `InvoiceItem` | `lineAmount`       | Total line (price × qty)    | Computed from `promoprice` index / DB       |
| `InvoiceItem` | `quantity`         | Purchased quantity          | `CalculateRequest.items[].quantity`         |
| `InvoiceItem` | `unitPrice`        | Unit regular price          | Fetched from `promoprice` index / DB        |
| `InvoiceItem` | `categoryCode`     | Product category            | Fetched from `promoitem` index / DB         |
| `InvoiceItem` | `subcategoryCode`  | Product sub-category        | Fetched from `promoitem` index / DB         |
| `InvoiceItem` | `departmentCode`   | Product department          | Fetched from `promoitem` index / DB         |
| `InvoiceItem` | `foodItem`         | Food indicator (boolean)    | Fetched from `promoitem` index / DB         |
| `InvoiceItem` | `storeCode`        | Store identifier            | `CalculateRequest.storeId` injected per line|
| `Transaction` | `storeId`          | Store identifier (header)   | `CalculateRequest.storeId`                  |
| `Transaction` | `customerId`       | Customer identifier         | `CalculateRequest.customerId`               |
| `Transaction` | `transactionDate`  | Transaction date            | `CalculateRequest.transactionDate`          |
| `Customer`    | `customerId`       | Customer identifier         | `CalculateRequest.customerId`               |
| `Customer`    | `customerGroupCode`| Customer segment / group    | Fetched from `promocustomer` index / DB     |

### PromotionResult Fields (populated by DRL rules via `global List<PromotionResult> results`)

| Field               | Type           | Maps To (JSON)                               | Notes                            |
|---------------------|----------------|----------------------------------------------|----------------------------------|
| `promotionId`       | `String`       | `promotions[].promotionId`                   | Set by rule                      |
| `promotionType`     | `int`          | `promotions[].type`                          | <10 → `"STANDARD"`, ≥10 → `"COUPON"` |
| `description`       | `String`       | `promotions[].description`                   | Set by rule                      |
| `timesApplied`      | `double`       | `promotions[].timesApplied`                  | Set by rule                      |
| `discountAmount`    | `BigDecimal`   | `promotions[].discountAmount`                | Rounded 3dp                      |
| `lineDiscountAmount`| `BigDecimal`   | `items[].appliedPromotions[].discountAmount` | Per-line result                  |
| `prerequisites`     | `List<String>` | `promotions[].prerequisites[]`               | SKUs required to qualify         |

---

## Complete Naming System (Section 5)

### Product Identity

| Item                  | Value                     |
|-----------------------|---------------------------|
| Product name          | `PromoEngine`             |
| Maven groupId         | `io.promoengine`          |
| Maven artifactId      | `promoengine-service`     |
| Java package root     | `io.promoengine`          |
| Main class            | `PromoEngineApplication`  |
| Spring app name       | `promoengine-service`     |
| Config property prefix| `promoengine`             |
| Env var prefix        | `PE_`                     |

### OpenSearch Index Names

| Legacy Index Name  | PromoEngine Index Name | Data Stored                                    |
|--------------------|------------------------|------------------------------------------------|
| `rm3getprice`      | `promoprice`           | Item prices per store                          |
| `rm3product`       | `promoitem`            | Item/product properties (group, dept, food)    |
| `rm3customerlist`  | `promocustomer`        | Customer group, type, card type                |

Configurable via: `PE_PRICE_INDEX`, `PE_ITEM_INDEX`, `PE_CUSTOMER_INDEX`

### Class & Component Naming

| Concept           | Legacy Names                       | PromoEngine Names                                     |
|-------------------|------------------------------------|-------------------------------------------------------|
| Rule engine       | RM3S / EngineWrapper               | `PromotionRuleEngine` (Drools KieBase + StatelessKieSession) |
| Engine registry   | rm3sList + rm3sSemList (pool of 20)| `TenantEngineRegistry` (one per tenant, no pool)      |
| Result collection | callback in RM3S                   | `global List<PromotionResult> results` (per session)  |
| Fact builder      | `ServletService.convertToServletRequest()` | `EngineFactBuilder`                          |
| Response mapper   | `Transformer.java`                 | `PromotionResponseMapper`                             |
| Rule management   | MmedService (file-based)           | `PromotionRuleService` (DB-backed, DRL per rule)      |
| Rule importer     | S3DownloadFile                     | `S3RuleImporter` (one-time import only)               |
| Price result      | PriceRecord / SourceES             | `PriceData`                                           |
| Item result       | Product / ItemStoreValue           | `ItemPropertyData`                                    |
| Customer result   | (various)                          | `CustomerData`                                        |
| Enrichment output | EnginePayload                      | `EnrichedTransaction`                                 |
| Calc request      | InvoiceRequest (JAXB)              | `CalculateRequest` (POJO)                             |
| Calc response     | Invoice (JAXB)                     | `CalculateResponse` (POJO)                            |
| Per-line result   | InvoiceLineType (JAXB)             | `CalculatedItem` (POJO)                               |
| Per-promo result  | InvoiceResultType (JAXB)           | `PromotionResult` (POJO)                              |
| Promotion type    | SPI (standard), CPI (coupon)       | `"STANDARD"`, `"COUPON"` (JSON enum string)           |
| Exceptions (rules)| MmedException                      | `RuleSetException`                                    |
| Exceptions (timeout)| EngineTimeoutException           | `EngineTimeoutException`                              |

---

## What Is Dropped — Not Carried Forward (Section 15)

| Item                                             | Reason                                                                    |
|--------------------------------------------------|---------------------------------------------------------------------------|
| Proprietary engine JAR (`RM3E-3.5.0.jar`)        | Replaced by Drools 8.x (Apache 2.0, pure Java, container-native)         |
| JNI / native C++ code inside JAR                 | Platform-specific — cannot be containerised; eliminated                   |
| MML (Marketing Markup Language)                  | Proprietary DSL — replaced by DRL (Drools Rule Language), open standard   |
| Engine pool (ArrayBlockingQueue of N instances)  | Drools KieBase is thread-safe — no pool needed                            |
| `RM3Interface`, `RM3CallBack`, `RM3Item`, `RM3PromoResult`, `RM3PromoDef` | Replaced by Drools API + clean POJOs                 |
| High-level XML simulator (`RM3S`)                | Eliminated — Drools handles evaluation without XML                        |
| RPSD XML request/response format                 | Entirely eliminated — internal calc uses Java POJOs                       |
| All JAXB classes (InvoiceRequest, Invoice, etc.) | No XML in transaction path                                                |
| `JAXBUtil` (marshal/unmarshal/namespace)         | No marshalling needed                                                     |
| `Transformer.java`                               | Replaced by `PromotionResponseMapper` (Java POJOs)                        |
| SOAP envelope wrapping / namespace stripping     | No SOAP anywhere in the new service                                       |
| HTTP call from one service to another            | Engine runs in-JVM — no internal HTTP                                     |
| Spin-loop semaphore pool (Thread.sleep × 300)    | Replaced by Drools thread-safe KieBase                                    |
| Silent item drop (no-price articles)             | Now explicit via `skippedItems[]` in response                             |
| Tomcat 7 WAR packaging, web.xml                  | Spring Boot JAR, embedded Tomcat 10, application.yml                      |
| Any class/config/index containing `rm3`/`RM3`/`makro`/`capgemini` | PromoEngine is a new product — no legacy naming        |

---

## Confirmed Decisions (Section 16)

| # | Decision                        | Confirmed Answer                                                           |
|---|---------------------------------|----------------------------------------------------------------------------|
| D1| Product brand name              | **PromoEngine** — package `io.promoengine`, Maven `promoengine-service`, config prefix `promoengine`, env prefix `PE_` |
| D2| Context path                    | New path + configurable — Default: `/promotionengine/api`. Override via `PE_CONTEXT_PATH` (e.g. legacy path for zero-downtime cutover) |
| D3| Engine JAR replacement          | **Full JAR replacement** — Drools 8.x (Apache 2.0, pure Java). MML → DRL. No `lib/` directory. |
| D4| OpenSearch index names          | **Migrate immediately** — `promoprice`, `promoitem`, `promocustomer`. Configurable via `PE_PRICE_INDEX`, `PE_ITEM_INDEX`, `PE_CUSTOMER_INDEX` |
| D5| Partial results (skippedItems)  | **206 Partial Content** — when `skippedItems[]` non-empty. Full result = 200. |
| D6| Multi-tenancy                   | **Full multi-tenant from day one** — API key maps to tenant. Per-tenant rule sets, DB rows, and `PromotionRuleEngine` instances. `TenantEngineRegistry` is the central registry. |
