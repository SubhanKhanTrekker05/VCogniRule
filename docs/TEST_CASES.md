# Test Cases — VCogniRule

Results of `mvn test` on 22 November 2026: **261 tests, 261 passed, 0 failed.**

The Pass/Fail column of the automated cases below is generated from the real JUnit result files
(`target/surefire-reports`). A row passes only if every JUnit test listed for it ran and passed.
"Actual" is the value the passing test observed.

## Test suites

| Test class | Type | What it covers | Tests |
|---|---|---|---|
| `OperatorStrategyTest` | Unit | the 8 operator strategies, Factory Method and Singleton | 52 |
| `RuleEngineTest` | Unit | rule engine, forward chaining, FindingMapper, ConflictResolver | 12 |
| `QuestionnaireScoringTest` | Unit | scoring of the 10 questionnaires, registry | 25 |
| `QuestionnaireRefactorEquivalenceTest` | Unit | refactor safety net: old vs new scoring on 10,000 random answer sets | 9 |
| `ReportPartsTest` | Unit | ScreeningReport (parameter objects), OverallResultResolver, FindingStyler | 25 |
| `AssessmentServiceTest` | Unit | answer validation and section completion | 32 |
| `FormValidationTest` | Unit | Bean Validation of the admin forms | 16 |
| `AdminServiceTest` | Unit | admin business rules (with fake repositories), Observer | 10 |
| `BCryptPasswordHasherTest` | Unit | Adapter: PasswordHasher over the BCrypt library | 2 |
| `TracingOperatorStrategyTest` | Unit | Decorator: TracingOperatorStrategy and its demo switch | 2 |
| `ScreeningFlowIntegrationTest` | Integration | screening flow through the running app (MockMvc), incl. the redesigned report | 23 |
| `AdminIntegrationTest` | Integration | admin login, protected pages, create/edit/delete, bad input | 44 |
| `FormRecoveryAndErrorPageIntegrationTest` | Integration | typed values kept after an error, friendly error pages | 7 |
| `BrowserLauncherTest` | Unit | desktop launcher (VCogniRule.exe): home page address, open command per OS | 2 |
| **Total** | | | **261** |

## Automated test cases

| ID | Description | Input | Expected output | Actual output | Result | JUnit test(s) |
|---|---|---|---|---|---|---|
| | **Unit tests: operator strategies (Strategy pattern)** | | | | | |
| TC-01 | EQ operator | answer 5, rule `== 5`; answer 4 | true; false | true; false | Pass (2) | `OperatorStrategyTest.singleValueOperators` |
| TC-02 | NE operator | answer 4, rule `!= 5`; answer 5 | true; false | true; false | Pass (2) | `OperatorStrategyTest.singleValueOperators` |
| TC-03 | GT operator | answer 6, rule `> 5`; answer 5 | true; false | true; false | Pass (2) | `OperatorStrategyTest.singleValueOperators` |
| TC-04 | GTE operator | answer 5, rule `>= 5`; answer 4 | true; false | true; false | Pass (2) | `OperatorStrategyTest.singleValueOperators` |
| TC-05 | LT operator | answer 4, rule `< 5`; answer 5 | true; false | true; false | Pass (2) | `OperatorStrategyTest.singleValueOperators` |
| TC-06 | LTE operator | answer 5, rule `<= 5`; answer 6 | true; false | true; false | Pass (2) | `OperatorStrategyTest.singleValueOperators` |
| TC-07 | BETWEEN includes both ends | answers 9, 10, 12, 14, 15 for rule 10-14 | false, true, true, true, false | false, true, true, true, false | Pass (6) | `OperatorStrategyTest.betweenIncludesBothEnds`, `OperatorStrategyTest.betweenWithMissingBoundIsFalse` |
| TC-08 | IN list (incl. old bad data) | answer 2 in `1,2,3`; answer 1 in `a,b` | true; false and no crash | true; false, no exception | Pass (7) | `OperatorStrategyTest.inMatchesListedNumbers`, `OperatorStrategyTest.inIgnoresSpaces`, `OperatorStrategyTest.inWithBadEntryDoesNotCrash` |
| TC-09 | Factory Method + Singleton give the right strategy for all 8 operators | each Operator value | EqualsStrategy … InStrategy; one shared registry | correct class, same object on repeat, private constructor | Pass (12) | `OperatorStrategyTest.factoryCoversEveryOperator`, `OperatorStrategyTest.factoryReturnsSharedInstances`, `OperatorStrategyTest.factoryMethodCreatesNewProducts`, `OperatorStrategyTest.registryIsTextbookSingleton`, `OperatorStrategyTest.singleValueStrategiesExtendBaseClass` |
| TC-10 | Rule text under 'Rule needs' | GTE 10; BETWEEN 10-14; IN 1,2 | `>= 10`; `10 - 14`; `in [1,2]` | `>= 10`; `10 - 14`; `in [1,2]` | Pass (8) | `OperatorStrategyTest.singleValueDescribe`, `OperatorStrategyTest.betweenDescribe`, `OperatorStrategyTest.inDescribe` |
| | **Unit tests: rule engine** | | | | | |
| TC-11 | AND inside a group, OR between groups | a=1,b=0 for rule (a AND b); rule (a) OR (b) | AND rule does not fire; OR rule fires on group b | as expected | Pass (2) | `RuleEngineTest.andWithinGroup`, `RuleEngineTest.orBetweenGroups` |
| TC-12 | Forward chaining across levels | phq9_total = 7; L1 sets depression_mild, L2 reads it | both rules fire | L1 and L2 fired | Pass (1) | `RuleEngineTest.forwardChaining` |
| TC-13 | Highest-priority overall severity wins | mild (30) and severe (80) both fire | overall = severe | severe | Pass (2) | `RuleEngineTest.highestPrioritySeverityWins`, `RuleEngineTest.noSeverityRuleGivesNull` |
| TC-14 | Missing answer → condition fails | rule on phq9_total, PHQ-9 not answered | false | false | Pass (1) | `RuleEngineTest.missingFieldFails` |
| TC-15 | FindingMapper builds the explanation trace | PHQ9-MODERATE fired with total 12 | trace: value 12, needs `10 - 14` | 12 / `10 - 14` | Pass (1) | `RuleEngineTest.findingMapperBuildsTrace` |
| TC-16 | Findings sorted by priority | priorities 10, 50, 100 | 100, 50, 10 | HIGH, MID, LOW | Pass (1) | `RuleEngineTest.findingsSortedByPriority` |
| | **Unit tests: questionnaire scoring (inheritance)** | | | | | |
| TC-17 | PHQ-9 score and cut-offs | items 2,1,0,0,0,0,0,0,1; totals 5 and 10 | total 4; ≥5 mild flag; ≥10 moderate flag | 4; flags set at 5 and 10 | Pass (3) | `QuestionnaireScoringTest.totalCoreSymptomAndCount`, `QuestionnaireScoringTest.cutOffs`, `QuestionnaireScoringTest.maximumScore` |
| TC-18 | GAD-7 score and cut-off | items 2,2,0,0,0,0,0; total 10 | total 4, count 1; moderate flag at 10 | as expected | Pass (2) | `QuestionnaireScoringTest.totalAndCount`, `QuestionnaireScoringTest.moderateCutOff` |
| TC-19 | C-SSRS safety logic | item 1 Yes; item 4/5/6 Yes | passive only; crisis_detected = 1 | as expected | Pass (4) | `QuestionnaireScoringTest.allNo`, `QuestionnaireScoringTest.passive`, `QuestionnaireScoringTest.crisis`, `QuestionnaireScoringTest.summaryCountsGradedYes` |
| TC-20 | WHO-5 (higher is better) | items 5,4,3,2,1 | 15 / 25 (60%) | Wellbeing score 15 / 25 (60%) | Pass (2) | `QuestionnaireScoringTest.rawScoreAndPercentage`, `QuestionnaireScoringTest.missingItemCountsAsFive` |
| TC-21 | PSS-10 reverse scoring | all 'Never'; all 'Very often' | total 16; total 24 | 16; 24 | Pass (3) | `QuestionnaireScoringTest.allNever`, `QuestionnaireScoringTest.allVeryOften`, `QuestionnaireScoringTest.lowestStress` |
| TC-22 | PSQI global score boundary | worked examples (23:00–07:00, 7 h …) and (23:00–04:00, 4 h, 90 min) | global 5 = not poor; global 6 = poor | 5 / sleep_poor 0; 6 / sleep_poor 1 | Pass (2) | `QuestionnaireScoringTest.globalFive`, `QuestionnaireScoringTest.globalSix` |
| TC-23 | PHQ-15, WSAS, ASRS, PC-PTSD | hand examples | totals 5, 15; ASRS 4 of 6; PC-PTSD 3 | as expected | Pass (5) | `QuestionnaireScoringTest.totalSevereCountAndPanicFlag`, `QuestionnaireScoringTest.noPanic`, `QuestionnaireScoringTest.wsasTotal`, `QuestionnaireScoringTest.asrsShadedCounts`, `QuestionnaireScoringTest.pcPtsdTotal` |
| TC-24 | Polymorphism + registry | 10 subclasses; list missing WSAS; two PHQ-9 | each scores its own section; startup error | as expected; IllegalStateException | Pass (3) | `QuestionnaireScoringTest.eachSubclassHasItsSection`, `QuestionnaireScoringTest.registryRejectsMissingSection`, `QuestionnaireScoringTest.registryRejectsDuplicate`, `QuestionnaireScoringTest.templateMethodRunsBothSteps` |
| TC-25 | Refactor did not change any score | 10,000 random answer sets + edge cases, old vs new code | identical results | identical | Pass (4) | `QuestionnaireRefactorEquivalenceTest.randomAnswerSets`, `QuestionnaireRefactorEquivalenceTest.allSectionsAllZero`, `QuestionnaireRefactorEquivalenceTest.allSectionsAllMaximum`, `QuestionnaireRefactorEquivalenceTest.reportScoreTextAndNotesUnchanged` |
| | **Unit tests: report parts** | | | | | |
| TC-26 | Report made from parameter objects | FindingGroups + Coverage; a missing part | parts readable; error if missing; lists read-only | as expected | Pass (3) | `ReportPartsTest.accessorsReadTheGroups`, `ReportPartsTest.requiresParameterObjects`, `ReportPartsTest.reportIsUnchangeable` |
| TC-27 | Overall banner rules | crisis + mild/minimal/moderate/none; no severity | NEEDS ATTENTION; Not determined / Not classified | as expected | Pass (7) | `ReportPartsTest.crisisOutranksLowSeverity`, `ReportPartsTest.noSeverity`, `ReportPartsTest.severeStays`, `ReportPartsTest.partialNote` |
| TC-28 | Finding colours | MONITOR finding linked to a risk answer | shown as crisis, 'Monitor · safety-related' | as expected | Pass (11) | `ReportPartsTest.safetyLinkedIsCrisis`, `ReportPartsTest.toneByAction`, `ReportPartsTest.infoTones` |
| | **Unit tests: answer validation (wrong input)** | | | | | |
| TC-29 | Radio answer must be an option | phq9_q1 = 4, -1, abc, 1.5 | not valid | not valid | Pass (4) | `AssessmentServiceTest.isValid` |
| TC-30 | Time must be HH:MM | 25:00, 23:60, 11pm | not valid | not valid | Pass (3) | `AssessmentServiceTest.isValid` |
| TC-31 | Numbers must be in range | minutes 181 / 45.5; hours 30 / -1 | not valid | not valid | Pass (4) | `AssessmentServiceTest.isValid` |
| TC-32 | Invalid answer → section not scored | PHQ-9 item 1 = abc | PHQ-9 partly answered, no total | partial, no phq9_total | Pass (3) | `AssessmentServiceTest.invalidAnswerMakesSectionPartial`, `AssessmentServiceTest.filterAnswers`, `AssessmentServiceTest.prepareValidatesItself` |
| TC-33 | Self-harm item kept when PHQ-9 unfinished | 8 of 9 answered, item 9 = 2 | no total, but phq9_q9 = 2 kept | as expected | Pass (1) | `AssessmentServiceTest.partialSectionNotScoredButSafetyItemKept` |
| TC-34 | Safety Check fail-safe | answers 3, abc, 0, 1 | 1, 1, 0, 1 (crisis) | 1, 1, 0, 1 | Pass (1) | `AssessmentServiceTest.safetyCheckFailSafe` |
| | **Unit tests: admin form validation and business rules** | | | | | |
| TC-35 | Rule form limits | priority 150 / -5; level 0 / 7; blank name; ID with spaces | refused with a message | matching messages | Pass (8) | `FormValidationTest.badRuleDetails`, `FormValidationTest.conclusionKeyNeedsValue`, `FormValidationTest.defaultValueWithoutKeyIsAllowed` |
| TC-36 | Condition values must fit the operator | GTE no value; BETWEEN 9-5; IN `a,b` | refused | refused | Pass (2) | `FormValidationTest.valuesMustFitOperator`, `FormValidationTest.validConditions` |
| TC-37 | Duplicate / empty rule ID, unknown field | PHQ9-MINIMAL again; ''; field xyz | InvalidInputException | exception with message | Pass (4) | `AdminServiceTest.duplicateRuleId`, `AdminServiceTest.emptyRuleId`, `AdminServiceTest.unknownField`, `AdminServiceTest.conditionMustBelongToRule` |
| TC-38 | Admin password check | admin123; wrong; unknown user | true; false; false | true; false; false | Pass (1) | `AdminServiceTest.authenticate` |
| | **Integration tests: screening (whole app, HTTP)** | | | | | |
| TC-39 | S1 all answers healthy | lowest answer everywhere, Safety Check No | MINIMAL, no helplines, disclaimer | MINIMAL, disclaimer shown | Pass (1) | `ScreeningFlowIntegrationTest.healthyScreening` |
| TC-40 | S2 all answers worst | highest answer everywhere | NEEDS ATTENTION + helplines | NEEDS ATTENTION, 1166 shown | Pass (1) | `ScreeningFlowIntegrationTest.highSymptomScreening` |
| TC-41 | S3 Safety Check crisis | items 1 and 4 = Yes | NEEDS ATTENTION, crisis finding first | as expected | Pass (1) | `ScreeningFlowIntegrationTest.safetyCheckCrisis` |
| TC-42 | S4 Safety Check skipped | no C-SSRS answers | back to Safety Check with error | redirect ?resume=C-SSRS&error=safety | Pass (1) | `ScreeningFlowIntegrationTest.safetyCheckIsMandatory` |
| TC-43 | Demo answer sets | see DEMO_SCRIPT.md A, B, C, D | MINIMAL, MILD, MODERATE, NEEDS ATTENTION | MINIMAL, MILD, MODERATE, NEEDS ATTENTION | Pass (4) | `ScreeningFlowIntegrationTest.demoHealthy`, `ScreeningFlowIntegrationTest.demoMild`, `ScreeningFlowIntegrationTest.demoModerate`, `ScreeningFlowIntegrationTest.demoCrisis` |
| TC-44 | Wrong answers in the form | phq9_q1=abc; gad7_q1=99; unknown field | section partly answered, not scored | as expected | Pass (3) | `ScreeningFlowIntegrationTest.invalidAnswer`, `ScreeningFlowIntegrationTest.outOfRangeAndUnknown`, `ScreeningFlowIntegrationTest.onlySafetyCheck` |
| TC-45 | Complete a section later | submit, then 'Complete this section' | previous answers restored | 6 answers restored | Pass (2) | `ScreeningFlowIntegrationTest.resumeKeepsAnswers`, `ScreeningFlowIntegrationTest.unknownResumeCodeIgnored` |
| TC-46 | Pages | /, /about, /screening, /no-such-page | 200, 200, 200, 404 | 200, 200, 200, 404 | Pass (2) | `ScreeningFlowIntegrationTest.pagesLoad`, `ScreeningFlowIntegrationTest.assessmentHasAllQuestions` |
| | **Integration tests: admin panel (whole app, HTTP)** | | | | | |
| TC-47 | Admin login | admin / wrong; admin / admin123 | error message; dashboard with 150 rules | as expected | Pass (4) | `AdminIntegrationTest.wrongPassword`, `AdminIntegrationTest.rightPassword`, `AdminIntegrationTest.loginPageWhenLoggedIn`, `AdminIntegrationTest.logout` |
| TC-48 | Admin pages protected | 13 admin URLs while logged out | all redirect to /admin, nothing changes | 13 of 13 redirect, data unchanged | Pass (14) | `AdminIntegrationTest.protectedUrls`, `AdminIntegrationTest.loggedOutChangesNothing` |
| TC-49 | Create, edit, switch off, delete | rule TEST-1; condition GTE 7 → BETWEEN 3-8 | saved / changed / removed | as expected | Pass (7) | `AdminIntegrationTest.createRule`, `AdminIntegrationTest.createRuleWithFormDefaults`, `AdminIntegrationTest.conditionLifecycle`, `AdminIntegrationTest.editRule`, `AdminIntegrationTest.toggleOffAndOpen`, `AdminIntegrationTest.deleteRule`, `AdminIntegrationTest.missingRule` |
| TC-50 | Factory reset | after delete + switch off | 150 rules, all active | 150 / 150 | Pass (1) | `AdminIntegrationTest.factoryReset` |
| TC-51 | Bad admin input | 8 bad rule forms + 6 bad conditions | message on the form, nothing saved | message shown, no change | Pass (15) | `AdminIntegrationTest.badRuleInput`, `AdminIntegrationTest.badConditionInput`, `AdminIntegrationTest.conditionOfOtherRule` |
| TC-52 | Screening after a refused bad condition | IN `a,b` refused, then a screening | report shown (was HTTP 500 before) | report shown | Pass (1) | `AdminIntegrationTest.screeningStillWorksAfterBadInput` |
| | **Unit tests: Adapter, Decorator, Observer** | | | | | |
| TC-53 | Adapter: PasswordHasher over BCrypt | hash admin123, check right and wrong password | hash is not the password; true; false | true; false; two hashes differ | Pass (2) | `BCryptPasswordHasherTest.hashThenMatch`, `BCryptPasswordHasherTest.saltedHashes` |
| TC-54 | Decorator: tracing changes no result | GTE 10 for answers 0-27, plain vs traced; switch on/off | same answers; all 8 wrapped only when on | identical; wrapped / plain | Pass (2) | `TracingOperatorStrategyTest.sameResultsAsWrapped`, `TracingOperatorStrategyTest.switchWrapsStrategies` |
| TC-55 | Observer: admin changes notify the listeners | create, deactivate, delete, reset; one refused create | 4 messages in order, none for the refused one | 4 messages; cache reloaded once | Pass (2) | `AdminServiceTest.changesNotifyListeners`, `AdminServiceTest.cacheRefresherReloads` |
| | **Integration tests: redesigned report** | | | | | |
| TC-56 | Crisis findings grouped into one card | Safety Check items 1 and 4 = Yes | one 'Safety concern' card; 'Get help now'; no finding lost | as expected | Pass (1) | `ScreeningFlowIntegrationTest.safetyFindingsGrouped` |
| TC-57 | Your scores and score bars | PHQ-9 total 12 | band from the fired rule; bar parts 0-4, 5-9, 10-14, 15-19, 20-27, green to red | as expected | Pass (2) | `ScreeningFlowIntegrationTest.scoresTable`, `ScreeningFlowIntegrationTest.scoreBarFollowsBandRules` |
| TC-58 | Results grouped by topic | all answers at the worst value | every result appears once; Safety first | as expected | Pass (1) | `ScreeningFlowIntegrationTest.resultsGroupedByTopic` |
| TC-59 | At a glance tiles | PHQ-9 12; crisis from PHQ-9 item 9; positive PC-PTSD and ASRS | Mood = Moderate; Safety = Needs attention; both screens = Needs attention | as expected | Pass (1) | `ScreeningFlowIntegrationTest.atAGlanceTiles` |
| TC-60 | Complete-section box hidden in a crisis | crisis without PHQ-9 | box not shown | not shown | Pass (1) | `ScreeningFlowIntegrationTest.depressionBoxHiddenOnCrisis` |

The number in brackets is how many JUnit test runs the row covers (parameterized tests run once per input).

## Manual browser trial run (Chrome)

The page script (tabs, one question at a time, confirmation window, admin search/sort) runs in the
browser, so it was also checked by hand in Chrome on a fresh database.

| ID | Description | Input | Expected output | Actual output | Result |
|---|---|---|---|---|---|
| BT-01 | Submit with empty Safety Check | click Submit Screening | not sent; message shown | "The Safety Check is required. Please answer every question …" | Pass |
| BT-02 | One question at a time | click 'No' on question 1 | moves to question 2, progress 1 of 6 | Question 2 of 6, 1 of 6 answered | Pass |
| BT-03 | Full screening in the browser (Demo C) | Safety Check No, PHQ-9 12, GAD-7 14 → Submit → Submit | confirmation lists 3 sections; report MODERATE | 3 sections listed; MODERATE, 12 / 27, 14 / 21, disclaimer | Pass |
| BT-04 | Admin search | type PHQ9 | only PHQ-9 rules, highlighted | 8 of 150 rules match, 8 highlighted | Pass |
| BT-05 | Admin sort | click 'Pri' twice | ascending, then descending | 5 … 100, then 100 … 5 | Pass |
| BT-06 | New rule with form defaults | ID + name only (conclusion value '1', key empty) | rule created | Rule DEMO-1 created. (FAILED first: bug found and fixed, see below) | Pass |
| BT-07 | Duplicate rule ID in the browser | ID DEMO-1 again | message, typed values kept | Rule ID DEMO-1 already exists.; values kept | Pass |
| BT-08 | Every page and file loads | 15 URLs while logged in | no error page | 15 of 15 OK | Pass |

### Bug found by the trial run

BT-06 first **failed**: the new-rule form fills in Conclusion Value = "1" by default, and the
validation added in S4 required key and value together, so every rule without a conclusion was refused.
Fixed in `RuleDetails` (an empty key now means "no conclusion" and the value is ignored); covered by
`FormValidationTest.defaultValueWithoutKeyIsAllowed` and `AdminIntegrationTest.createRuleWithFormDefaults`.
