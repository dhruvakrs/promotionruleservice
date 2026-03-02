Feature: Promotion rule service

  Background:
    * def validDrl = new java.lang.String(read('classpath:fixtures/valid-rule.drl'))
    * def invalidDrl = 'not valid drl syntax ##!!'
    * def ctx = karate.call('classpath:helpers/spring-context.js')
    * def service = ctx.promotionRuleService

  Scenario: Create rule saves with DRAFT status
    * def req = { promotionId: 'TEST001', ruleBody: '#(validDrl)', priority: 10, description: 'Test rule' }
    * def result = service.create(req, 'default')
    * match result.status == 'DRAFT'
    * match result.promotionId == 'TEST001'

  Scenario: Activate rule with invalid DRL throws exception
    * def req = { promotionId: 'BADRL01', ruleBody: '#(invalidDrl)', priority: 10, description: 'Bad rule' }
    * def created = service.create(req, 'default')
    * def exceptionThrown = false
    * eval try { service.activate('BADRL01', 'default'); } catch(e) { exceptionThrown = true; }
    * assert exceptionThrown == true

  Scenario: Activate valid rule updates status to ACTIVE
    * def req = { promotionId: 'VALIDRL01', ruleBody: '#(validDrl)', priority: 10, description: 'Valid rule' }
    * def created = service.create(req, 'default')
    * def activated = service.activate('VALIDRL01', 'default')
    * match activated.status == 'ACTIVE'

  Scenario: Deactivate active rule sets status to INACTIVE
    * def req2 = { promotionId: 'VALIDRL02', ruleBody: '#(validDrl)', priority: 10, description: 'Valid rule 2' }
    * def created = service.create(req2, 'default')
    * def activated = service.activate('VALIDRL02', 'default')
    * def deactivated = service.deactivate('VALIDRL02', 'default')
    * match deactivated.status == 'INACTIVE'

  Scenario: Rule history records status changes
    * def req3 = { promotionId: 'HIST001', ruleBody: '#(validDrl)', priority: 10, description: 'History test' }
    * def created = service.create(req3, 'default')
    * def activated = service.activate('HIST001', 'default')
    * def history = service.getHistory('HIST001', 'default')
    * assert karate.sizeOf(history) >= 2
    * match history[0].status == 'DRAFT'
    * match history[1].status == 'ACTIVE'

  Scenario: MmlToDrlConverter produces valid DRL from MML
    * def MmlToDrlConverter = Java.type('io.promoengine.rules.MmlToDrlConverter')
    * def converter = new MmlToDrlConverter()
    * def mml = new java.lang.String(read('classpath:fixtures/sample-mml.txt'))
    * def drl = converter.convert('PROMO01', mml)
    * match drl contains 'rule "PROMO01"'
    * match drl contains 'then'
    * match drl contains 'end'
