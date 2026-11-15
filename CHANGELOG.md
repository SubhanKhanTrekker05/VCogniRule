# Changelog

All changes made during the OOAD (CT-263) review and refactor.
Each entry says what changed, which files, and why.
Baseline: commit `e7095d2` (the code as received). It built and ran correctly before any change.

## Phase 3: Refactor

### 1. M4: Fix crash when opening an inactive rule (bug fix)
- **Problem:** In the admin panel, opening a rule that was switched off (or a rule id that does
  not exist) showed an HTTP 500 error page. `RuleRepository.findById` searched only the cache of
  *active* rules, so the page received `null`.
- **Changed:** `repository/RuleRepository.java`: `findById` now searches all rules, active or not.
- **Changed:** `controller/AdminController.java`: `ruleDetail` sends an unknown id back to the
  dashboard with a "Rule N was not found" message instead of crashing.
- **Verified:** inactive rule 1 gives 200 (was 500); rule 99999 redirects to the dashboard (was 500).

### 2. M5: PHQ-9 total of 0 now gets the "Minimal depression" finding (domain fix)
- **Problem:** Rule `PHQ9-MINIMAL` required `phq9_total BETWEEN 1 AND 4`. The PHQ-9 minimal band
  is 0–4 (Kroenke 2001), so a person with no symptoms got no PHQ-9 finding. (GAD-7 already used 0–4.)
- **Changed:** `data.sql` and `rules_master.sql`: condition changed to `BETWEEN 0 AND 4`.
- **Changed:** `data.sql`: added a one-line `DELETE` of the old 1–4 condition, so databases created
  before this fix repair themselves at startup. Without it, `INSERT OR IGNORE` would have added the
  new 0–4 row *next to* the old 1–4 row, and the rule would still fail for 0.
- **What you need to do:** nothing. The existing `database/vcognirule.db` is fixed automatically
  the next time the app starts (tested on a copy of it). A factory reset also gives the fixed rule.
- **Verified:** all-healthy screening now also shows `PHQ9-MINIMAL`; no other result changed.

### 3. M3: "Not a medical diagnosis" disclaimer on the report page
- **Problem:** The disclaimer was on the welcome and about pages, but not on the report, which is
  the page people read, print and share.
- **Changed:** `templates/report.html`: new disclaimer box under the crisis alert (the crisis alert
  stays first). It is also included when the report is printed / saved as PDF.
- **Changed:** `static/css/style.css`: new `.report-disclaimer` style (same amber colours as the
  welcome page disclaimer).

### 4. S7: Hide stack traces from users
- **Problem:** `application.properties` had `server.error.include-stacktrace=always`, so any error
  during a demo showed Java internals on screen.
- **Changed:** `application.properties`: `include-stacktrace=never`, `include-message=never`.
  Errors are still logged in full in the console.
- **Verified:** `/admin/rule/abc` gives a plain HTTP 400 page with no stack trace.

### 5. M2: Strategy + Factory pattern for condition operators
- **Problem:** The 8 operators (EQ, NE, GT, GTE, LT, LTE, BETWEEN, IN) were handled by a `switch`
  in two places (`ConditionEvaluator` to evaluate, `FindingBuilder` to describe). Adding an
  operator meant editing both switches.
- **New package `engine/operator/`:**
  - `OperatorStrategy` (interface): `test(actual, condition)` and `describe(condition)`.
  - `SingleValueStrategy` (abstract class, Template Method): shared logic for the six operators
    that compare against one number; subclasses give only the comparison and symbol.
  - `EqualsStrategy`, `NotEqualsStrategy`, `GreaterThanStrategy`, `GreaterOrEqualStrategy`,
    `LessThanStrategy`, `LessOrEqualStrategy` (extend `SingleValueStrategy`).
  - `BetweenStrategy`, `InStrategy` (implement `OperatorStrategy` directly).
  - `OperatorStrategyFactory` (Factory): returns the shared strategy for each `Operator`.
- **Changed:** `engine/ConditionEvaluator.java`: the switch is replaced by
  `OperatorStrategyFactory.forOperator(op).test(...)`.
- **Changed:** `engine/FindingBuilder.java`: `formatRequiredValue` switch removed, uses `describe(...)`.
- **Changed:** `util/Operator.java`: comment updated.
- **Verified:** the 4 screening + 3 admin tests give identical results to before, including the
  full text of every report page (so the "Rule needs" texts are unchanged).
  Note: no rule uses NE or IN yet; these are covered by unit tests in Phase 4.

### 6. M1: Abstract `Questionnaire` class with 10 subclasses (inheritance, Template Method)
- **Problem:** The project had no inheritance. Each test's scoring was spread over three places:
  a private `calculateX` method in `DerivedFieldCalculator`, a line in `calculateDerivedFlags`,
  and two `switch` statements on `Section` in `ReportService` (`scoreText`, `scoreNote`).
- **Safety net written FIRST:** `src/test/.../questionnaire/QuestionnaireRefactorEquivalenceTest.java`
  runs a frozen copy of the original code (`LegacyDerivedFieldCalculator`, `LegacyReportText`,
  copied from commit e7095d2) and the new code on the same inputs (10,000 random answer sets with a
  fixed seed, plus edge cases) and requires identical output. It was run on the old code first (pass),
  then shown to catch a deliberate one-character change (fail), then run on the new code (pass).
- **New package `questionnaire/`:**
  - `Questionnaire` (abstract class): holds its `Section`; `contributeTo(facts)` is a final
    template method (scores, then flags); abstract `computeScores`, hook `computeFlags`,
    abstract `summary`, overridable `scoreNote`; shared helpers `sum`, `countAtLeast`, `flag`, `countYes`.
  - 10 subclasses: `Phq9Questionnaire`, `Gad7Questionnaire`, `CssrsQuestionnaire`,
    `Who5Questionnaire`, `Pss10Questionnaire`, `PsqiQuestionnaire`, `Phq15Questionnaire`,
    `PcPtsdQuestionnaire`, `AsrsQuestionnaire`, `WsasQuestionnaire`.
  - `QuestionnaireRegistry`: holds one questionnaire per section and stops the app at startup
    if a section has none or two.
- **Changed:** `service/DerivedFieldCalculator.java`: the 10 `if` checks and 11 private methods
  are replaced by a loop that calls `contributeTo` on each completed questionnaire.
- **Changed:** `service/ReportService.java`: `scoreText`, `scoreNote` and `countYes` removed; the
  report asks the section's questionnaire for `summary(...)` and `scoreNote()`.
- **Not changed (on purpose):** severity (mild, moderate, ...) is still decided by the rules in the
  database, not by Java code, so admin edits to rules keep working. `Section` enum unchanged.
- **Verified:** 9/9 before/after tests pass; the 4 screening + 3 admin tests are identical to before,
  including the full report page text.

### 7. M6: Correct wrong / out-of-date comments; rename FindingBuilder → FindingFactory
- **Renamed:** `engine/FindingBuilder.java` → `engine/FindingFactory.java`, method `build` → `create`
  (via `git mv`, history kept). It was never a Builder (one static method that creates the object);
  its own comment said "Builder pattern conceptually". It is a static factory method.
  Caller updated: `engine/RuleEngine.java`.
- **`domain/ConditionTrace.java`:** removed text copied from another project ("Adopted from the IAI
  project's trace patterns (VSecretServerAdvisor)") and its description of "may have" and not-fired
  traces, which this system does not have. Now describes what the code really does.
- **`repository/QuestionRepository.java`:** said FindingBuilder uses `findByFieldKey`; it never did.
- **`util/ActionType.java`:** said ConflictResolver sorts by action type; it sorts by priority.
- **Row counts:** conditions are 209, not 205 (`Rule`, `Condition`, `ConditionGroup`, `RuleRepository`,
  `AdminService`, `AdminController`, both SQL files); rules per level are 79/53/12/6, not 78/57/12/6
  (`Rule.java`); `data.sql` header said 114 questions, the real number is 151.
- **Verified:** build, 9/9 tests, 7/7 regression tests identical.

### 8. S1: Repository interfaces (Dependency Inversion Principle)
- **Problem:** Services depended directly on the concrete SQLite classes.
- **Renamed (git mv):** `RuleRepository` → `JdbcRuleRepository`, `QuestionRepository` →
  `JdbcQuestionRepository`, `AdminRepository` → `JdbcAdminRepository`. Each now `implements`
  its interface and marks the implemented methods `@Override`. Class comments added/updated.
- **New interfaces:** `RuleRepository`, `QuestionRepository`, `AdminRepository` (same names as the
  old classes, so no service, controller or config class had to change). Spring injects the JDBC
  implementation automatically.
- **Encapsulation:** `refreshCache()` and `getOrCreateGroup()` were public but only used inside the
  class; they are now `private` and not part of the interface.
- **Verified:** build, 9/9 tests, 7/7 regression tests identical, inactive-rule check, and a new
  admin write-path check (create rule, add/edit/delete condition, edit, toggle, delete, factory reset: 8/8 pass).

### 9. S2: Builder pattern for ScreeningReport
- **Problem:** The report (12 fields) was created with one 12-argument constructor call, where two
  lists or two booleans could be swapped without any error.
- **Changed:** `service/ReportService.java`: `ScreeningReport` has a nested `Builder` with named,
  grouped steps (`overall`, `crisis`, `findings`, `coverage`, `depressionMissing`, `answers`).
  Unset lists default to empty; `build()` refuses a report without an overall result and makes all
  lists unchangeable. `buildReport` now uses `ScreeningReport.builder()...build()`.
- **Verified:** build, 9/9 tests, 7/7 regression tests identical.

### 10. S3: Admin login check moved into an interceptor (DRY)
- **Problem:** The line `if (!isLoggedIn(session)) return "redirect:/admin";` was repeated in
  13 `AdminController` methods. Forgetting it in a new method would leave that page unprotected.
- **New:** `config/AdminAuthInterceptor.java` (implements Spring's `HandlerInterceptor`): runs before
  every `/admin/**` request and redirects to the login page when nobody is logged in. Holds the
  session attribute name as a constant.
- **New:** `config/WebConfig.java` (implements `WebMvcConfigurer`): registers the interceptor for
  `/admin/**`, except `/admin` (login page), `/admin/login` and `/admin/logout`.
- **Changed:** `controller/AdminController.java`: the 13 checks, the private `isLoggedIn` method and
  13 now-unused `HttpSession` parameters removed. Login uses the interceptor's constant.
- **Verified:** all 13 protected URLs (GET and POST), when logged out, redirect to `/admin` and change
  no data; 9/9 tests, 7/7 regression tests identical, admin write-path check 8/8.

### 11. S6: Invalid answers count as "not answered" instead of being replaced with made-up values
- **Problem:** `AssessmentService` silently replaced answers it could not read with invented values
  (hours slept "abc" → 7, any other unreadable answer → 0, a bad time → 23:00), and accepted
  out-of-range numbers (e.g. 99 on a 0-3 scale), so a section could be scored on data the person
  never gave.
- **Changed:** `service/AssessmentService.java`:
  - `filterAnswers` now keeps an answer only if it is valid for its question: one of its options
    (radio buttons), a real `HH:MM` time, or a number inside the field's min/max (whole numbers only
    unless the field allows decimals). The limits come from `QuestionnaireService` (one source).
  - Invalid answers are dropped, so the section shows as "partly answered" and is not scored.
  - `prepare` validates again, so it is safe even if a caller skips `filterAnswers`.
  - The invented defaults and `parseTimeToMinutes` are removed; conversion now uses each question's
    type (time / decimal / whole number) instead of hard-coded field names.
  - Depends on `QuestionnaireService` instead of `QuestionRepository`.
- **Kept on purpose:** the Safety Check (C-SSRS) rule from the original code: any non-blank answer
  other than "0" counts as Yes, so an error can never hide a risk answer.
- **Verified:** 12 wrong-input cases (text, negative, too large, bad time, decimal in a whole-number
  field, unknown field, C-SSRS safety rule) all behave as expected; 9/9 tests; 7/7 regression identical.

### 12. N2: Logger instead of System.out
- **Changed:** `config/DataSeeder.java` and `repository/JdbcRuleRepository.java`: `System.out.println`
  replaced with an SLF4J `Logger` (`log.info(...)`), so the messages appear in the normal Spring log
  with time, level and class name.

### 13. N3: Named constants instead of magic numbers
- **`engine/RuleEngine.java`:** the four copied "Pass 1..4" blocks became one loop from `FIRST_LEVEL`
  to `LAST_LEVEL`; new constants `OVERALL_SEVERITY_KEY` and `ANY_TREATMENT_KEY`
  (also used by `service/ReportService.java` instead of repeating the strings).
- **`questionnaire/Questionnaire.java`:** shared cut-offs `MORE_THAN_HALF_THE_DAYS` (2), `MILD_CUTOFF` (5),
  `MODERATE_CUTOFF` (10), used by `Phq9Questionnaire` and `Gad7Questionnaire`.
- **Subclasses:** `Phq15Questionnaire.BOTHERED_A_LOT`, `Who5Questionnaire.MAX_ITEM_SCORE` / `PERCENT_PER_POINT`,
  `Pss10Questionnaire.MAX_ITEM_SCORE`, `PsqiQuestionnaire` defaults (`DEFAULT_BEDTIME` = 23:00 ...),
  `MINUTES_PER_DAY`, `POOR_SLEEP_CUTOFF`, `AsrsQuestionnaire.INATTENTIVE_SHADED_FROM` / `HYPERACTIVE_SHADED_FROM`.
- **`service/QuestionnaireService.java`:** `MAX_MINUTES_TO_FALL_ASLEEP`, `MAX_HOURS_SLEPT`.
- **Kept inline on purpose:** the band limits of the official PSQI scoring tables (e.g. 15/30/60 minutes),
  with a comment, so they can be checked line by line against the published table.
- **Verified:** 9/9 before/after tests (scoring identical), 7/7 regression identical, 12/12 wrong-input cases.

### 14. N6: Javadoc
- **Added class Javadoc** to the five classes that had none: `MentalHealthApp`, `WelcomeController`,
  `AboutController`, `AdminController`, `AdminService`.
- **Fixed 7 Javadoc errors** (unescaped `<`, `>` and `&` in comments) in `Operator`, `AssessmentService`,
  `Finding`, `ReportService`. `mvn javadoc:javadoc` now reports 0 errors.

### 15. N1: Remove unused code (approved item by item)
Each item was first confirmed unused in all Java code, HTML templates and JavaScript.
- **`domain/ConditionTrace.java`:** removed `matched`. It was always `true` (only rules that fired get
  a trace) and was never read. `engine/FindingFactory.java` no longer passes it.
- **`service/DerivedFieldCalculator.java`:** removed the one-argument `calculate(Map)`; never called.
- **`repository/QuestionRepository.java` + `JdbcQuestionRepository.java`:** removed `findByFieldKey`; never called.
- **`domain/Question.java` + `JdbcQuestionRepository.java`:** removed `defaultValue`; never read.
  The `default_value` database column is left as it is (no schema change).
- **Kept:** `ConditionGroup.logic`, with a new comment: the engine supports AND only inside a group
  and does not read this value; OR is expressed with several groups.
- **Not touched:** the frozen test copy `LegacyDerivedFieldCalculator` (it must stay identical to the original).
- **Verified:** build, 9/9 tests, 7/7 regression identical, admin write-path 8/8, wrong-input 12/12.

### 16. S4: Form objects and server-side validation for the admin panel
- **Problems found (shown before fixing):**
  - A duplicate rule ID, priority 150, level 7, an unknown category or priority -5 gave an HTTP 500
    error page (database constraint errors).
  - An empty name, BETWEEN without Min/Max, BETWEEN with Min > Max, GTE without a value and an
    unknown field were saved silently and could never fire.
  - **Serious:** an IN condition with text such as `a,b` was saved, and from then on **every
    screening crashed** (HTTP 500, `NumberFormatException`) until a factory reset.
  - Rules were passed through controller, service and repository as 10 separate parameters,
    conditions as 7.
- **New:** `domain/RuleDetails.java` and `domain/ConditionDetails.java` (records) carry a rule's /
  condition's form values in one object, with Jakarta Bean Validation rules matching the database
  constraints, plus cross-field checks (`isConclusionComplete`, `isValueSuitableForOperator`) and
  `normalized()` (blank → null; a condition keeps only the values its operator uses).
- **New:** `service/InvalidInputException.java` (extends `RuntimeException`) for business-rule errors.
- **Changed:** `service/AdminService.java`: takes the new objects; checks that a new rule ID is
  given and unique, the rule exists, the condition field exists, and a condition being edited or
  deleted belongs to that rule. Now also uses `QuestionRepository`.
- **Changed:** `repository/RuleRepository.java` + `JdbcRuleRepository.java`: `save`, `updateRule`,
  `addCondition`, `updateCondition` take the new objects (10 and 7 parameters → 1 or 2).
- **Changed:** `controller/AdminController.java`: `@Valid @ModelAttribute` + `BindingResult`;
  on an error the admin goes back to the same form with a message; the new-rule form keeps what
  was typed. A successful create shows "Rule X created."
- **Changed:** `engine/operator/InStrategy.java`: a non-number in an IN list (old bad data) no
  longer crashes the screening; it simply never matches.
- **Changed templates:** `admin-rule-new.html` (error message, keeps typed values, category and
  action-type lists built from the enums), `admin-rule-detail.html`, `admin-condition-form.html`
  (error message). `style.css`: `.form-error`.
- **Verified:** 12 bad admin inputs are all refused with a message (no 500) and screenings keep
  working; admin write-path 8/8; security 13/13; 7/7 regression identical; wrong-input 12/12; 9/9 tests.

### 17. S5: ReportService split (Single Responsibility)
- **Problem:** `ReportService` (516 lines after the earlier steps) did everything for the report:
  the overall banner, the colour/icon of each finding, grouping, plain-language text and the answers list.
- **New:** `service/FindingStyler.java`: how a finding looks (tone, icon, action label), which facts
  count as safety signals, which info findings are "reassuring", and the most-serious-first order.
  Returns a small `Style` record.
- **New:** `service/OverallResultResolver.java`: the overall banner (Needs attention / severity /
  Not determined / Not classified, and its note). Returns a small `OverallResult` record.
- **Changed:** `service/ReportService.java`: uses the two classes (constructor injection) instead of
  its own private methods and constants (516 → 429 lines). The logic itself is unchanged.
  Fully written `java.util.regex` names replaced with imports.
- **Verified:** 7/7 regression tests identical (including the full report text, so every tone, icon
  and label is unchanged), wrong-input 12/12, admin write-path 8/8, security 13/13, 9/9 tests.

### 18. S8: README matches the final code
- **Corrected numbers:** rules per level 79 / 53 / 12 / 6 (was 78 / 57 / 12 / 6, which adds up to 153);
  209 conditions; 151 question rows = 84 shown + 67 calculated; PSQI has 17 items in this form.
- **Corrected privacy text:** it said "no session persistence", but the last answers are kept in the
  HTTP session (never in the database) so a section can be finished later; now stated accurately.
- **Updated:** project structure (all 63 Java files, new packages), screening flow, admin features,
  configuration (stack traces hidden), new "Object-Oriented Design" and "Testing" sections, a
  "not a diagnosis" note at the top. Removed "Roadmap" from the table of contents (there was no such section).
- **Noted as a known issue (not fixed, not in the approved list):** `start-dev.sh` has a missing
  closing quote on line 10 (`bash -n` reports a syntax error); README tells Mac/Linux users to run
  `mvn spring-boot:run` directly.

## Small fixes after Phase 3 (approved separately)

### 19. Fix missing closing quote in start-dev.sh
- Line 10 had an unterminated string, so the Mac/Linux launcher stopped with a syntax error.
  `bash -n start-dev.sh` now passes.

### 20. Rename `,gitattributes` to `.gitattributes`
- The name started with a comma, so Git ignored the file. Its line-ending rules (LF for `.sh`,
  CRLF for `.bat`, binary for images) are now active. No stored file needed renormalizing.

## Phase 4: Testing and trial runs

### 21. Bug fix found by the browser trial run: new rule with the form's default values
- **Problem:** the new-rule form fills in Conclusion Value = "1" by default and leaves the Conclusion Key
  empty. The validation added in S4 required key and value together, so **every rule without a
  conclusion was refused** unless the admin first cleared the "1". The S4 tests missed it because their
  test rule always had a conclusion key.
- **Changed:** `domain/RuleDetails.java`: an empty key now means "no conclusion" and the value is ignored
  (stored as null). A key without a value is still refused ("A conclusion key needs a conclusion value.").
- **Tests added:** `FormValidationTest.defaultValueWithoutKeyIsAllowed`,
  `AdminIntegrationTest.createRuleWithFormDefaults`.

### 22. JUnit 5 test suite (237 tests, all passing)
- **Unit tests:** `OperatorStrategyTest` (8 strategies incl. NE and IN, factory), `RuleEngineTest`,
  `QuestionnaireScoringTest` (all 10 questionnaires), `ReportPartsTest` (Builder, OverallResultResolver,
  FindingStyler), `AssessmentServiceTest` (bad answers), `FormValidationTest` (admin forms),
  `AdminServiceTest` (business rules with in-memory fake repositories in `Fakes.java`).
- **Integration tests** (Spring Boot Test + MockMvc, each run on a fresh SQLite file in `target/`):
  `ScreeningFlowIntegrationTest` (4 original screening flows, demo answer sets, wrong input, resume)
  and `AdminIntegrationTest` (login/logout, 13 protected URLs, create/edit/delete/reset, 14 bad inputs).
  Shared setup in the abstract class `IntegrationTestBase`.
- No `pom.xml` change: MockMvc is built from the application context (`MockMvcBuilders.webAppContextSetup`).
- Checked that the tests really catch mistakes: registering the admin interceptor on the wrong URLs made
  15 tests fail.

### 23. Documents
- `docs/TEST_CASES.md`: test case table (52 automated rows generated from the real JUnit results,
  8 manual browser rows).
- `DEMO_SCRIPT.md`: live demo guide with verified answer sets for MINIMAL / MILD / MODERATE /
  NEEDS ATTENTION, the admin demo, troubleshooting, and the screenshot list.
- `README.md`: Testing section, project structure, start-dev.sh note updated.
