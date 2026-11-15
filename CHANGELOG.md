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
