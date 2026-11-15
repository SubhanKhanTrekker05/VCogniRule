# What we changed in VCogniRule — summary for the group

**Project:** Mental Health Early-Detection Screening System (CT-263 OOAD, NED University)

All changes are on the Git branch **`ooad-review`** (23 commits, one per change).
The full technical log is in `CHANGELOG.md`. This page is the short, simple version.

- **NEW file** = a file that did not exist before.
- **CHANGED file** = an existing file we edited.

---

## a) Bugs fixed

| # | Problem before | What we changed | File(s) | Why it matters |
|---|---|---|---|---|
| 1 | Opening a switched-off rule in the admin panel showed an error page. | The rule search now finds switched-off rules too; a missing rule goes back to the dashboard with a message. | CHANGED: `JdbcRuleRepository.java`, `AdminController.java` | The admin panel cannot crash during the live demo. |
| 2 | A person with a PHQ-9 score of **0** got no depression result. | The "Minimal depression" rule now covers scores 0–4 (as in the official PHQ-9). Old databases fix themselves at startup. | CHANGED: `data.sql`, `rules_master.sql` | Correct medical scoring; easy to explain in the viva. |
| 3 | One wrong admin entry (e.g. `a,b` in an IN condition) made **every** screening crash. | Admin input is now checked first, and the engine ignores old bad data. | CHANGED: `InStrategy.java` (+ validation, see e) | The demo cannot be broken by a typing mistake in the admin panel. |
| 4 | A new rule without a "conclusion" could not be saved (the form's default "1" was refused). Found while testing. | An empty conclusion key now simply means "no conclusion". | CHANGED: `RuleDetails.java` | Creating a rule in the demo works first time. |
| 5 | The Mac/Linux start script stopped with an error (a missing `"`). | Added the missing quote. | CHANGED: `start-dev.sh` | Group members on Mac/Linux can start the project. |
| 6 | The Git settings file was named `,gitattributes` (comma), so Git ignored it. | Renamed to `.gitattributes`. | CHANGED (renamed): `.gitattributes` | Line endings of scripts stay correct on every computer. |

---

## b) Inheritance and interfaces added

**Before:** the code had **no inheritance at all** — not one `extends`, `implements`, `abstract` or `interface`.
The teacher requires inheritance, so this was the most important change.

| What | Exact class names | Files | Why it matters |
|---|---|---|---|
| Abstract class + 10 subclasses | `Questionnaire` (abstract) → `Phq9Questionnaire`, `Gad7Questionnaire`, `CssrsQuestionnaire`, `Who5Questionnaire`, `Pss10Questionnaire`, `PsqiQuestionnaire`, `Phq15Questionnaire`, `PcPtsdQuestionnaire`, `AsrsQuestionnaire`, `WsasQuestionnaire` | NEW: 11 files in `questionnaire/` + `QuestionnaireRegistry.java`; CHANGED: `DerivedFieldCalculator.java`, `ReportService.java` | Each test (PHQ-9, GAD-7 …) scores itself. Shows **inheritance** and **polymorphism**. |
| Abstract class + 6 subclasses | `SingleValueStrategy` (abstract) → `EqualsStrategy`, `NotEqualsStrategy`, `GreaterThanStrategy`, `GreaterOrEqualStrategy`, `LessThanStrategy`, `LessOrEqualStrategy` | NEW: 7 files in `engine/operator/` | The 6 "compare with one number" operators share their common code. |
| Interface + 8 operator classes | `OperatorStrategy` ← implemented by `SingleValueStrategy` (and its 6 subclasses), `BetweenStrategy`, `InStrategy` | NEW: `OperatorStrategy.java`, `BetweenStrategy.java`, `InStrategy.java` | Every rule operator (=, ≥, between, in …) is its own class. |
| 3 repository interfaces | `RuleRepository` ← `JdbcRuleRepository`; `QuestionRepository` ← `JdbcQuestionRepository`; `AdminRepository` ← `JdbcAdminRepository` | NEW: 3 interfaces; CHANGED (renamed): the 3 `Jdbc…` classes | Services depend on interfaces, not on the database code (**Dependency Inversion**). |
| Using Spring / Java types | `AdminAuthInterceptor implements HandlerInterceptor`, `WebConfig implements WebMvcConfigurer`, `InvalidInputException extends RuntimeException` | NEW: 3 files | More real examples of `implements` and `extends`. |

---

## c) Design patterns added

| Pattern | Exact class names | Why it matters |
|---|---|---|
| **Strategy** | `OperatorStrategy` + the 8 operator classes | A new operator = a new class; no `switch` to edit. |
| **Factory** | `OperatorStrategyFactory`; `FindingFactory` (was wrongly called `FindingBuilder`) | One place creates the right object. |
| **Template Method** | `Questionnaire.contributeTo(...)`, `SingleValueStrategy.test(...)` | The base class fixes the steps; subclasses fill them in. |
| **Builder** | `ReportService.ScreeningReport.Builder` | The report (12 parts) is built step by step with named parts instead of one 12-argument call. |
| **Repository + Dependency Inversion** | `RuleRepository`, `QuestionRepository`, `AdminRepository` | Database code is hidden behind interfaces; tests use fake repositories. |
| Already there (now documented) | MVC (controllers + Thymeleaf), Singleton + Dependency Injection (Spring), Facade (`AdminService`) | Good to name in the report and viva. |

**Composition / aggregation** (for the class diagram): `Rule` ◆ `ConditionGroup` ◆ `Condition` (composition);
services → repositories and `QuestionnaireRegistry` → `Questionnaire` (aggregation).

---

## d) Code cleanup

| Problem before | What we changed | File(s) | Why it matters |
|---|---|---|---|
| The admin login check was copied into 13 methods. | One `AdminAuthInterceptor` protects all admin pages. | NEW: `AdminAuthInterceptor.java`, `WebConfig.java`; CHANGED: `AdminController.java` | **DRY** (Don't Repeat Yourself). |
| Rules were passed around as 10 separate values, conditions as 7. | One object each: `RuleDetails`, `ConditionDetails`. | NEW: 2 files in `domain/`; CHANGED: `AdminController`, `AdminService`, `RuleRepository`, `JdbcRuleRepository` | Removes the "long parameter list" code smell. |
| `ReportService` did too many jobs (516 lines). | Finding colours → `FindingStyler`; overall banner → `OverallResultResolver` (now 429 lines). | NEW: 2 files; CHANGED: `ReportService.java` | **Single Responsibility Principle**. |
| 4 pieces of code were never used. | Removed (`ConditionTrace.matched`, `calculate(Map)`, `findByFieldKey`, `Question.defaultValue`). | CHANGED: 6 files | Less code to explain; no dead code. |
| Some comments were wrong or copied from another project. | Corrected (e.g. 209 conditions, not 205). | CHANGED: 11 Java files + 2 SQL files | The examiner may read the comments. |
| Messages were printed with `System.out.println`. | Proper logger (SLF4J). | CHANGED: `DataSeeder.java`, `JdbcRuleRepository.java` | Professional logging. |
| Numbers like `1380`, `5`, `10` had no name. | Named constants (e.g. `DEFAULT_BEDTIME`, `MILD_CUTOFF`). The 4 copied engine passes became one loop. | CHANGED: `RuleEngine.java`, the questionnaire classes, `QuestionnaireService.java` | Removes "magic numbers". |
| 5 classes had no description; 7 comments broke the Javadoc tool. | Added / fixed. Javadoc now builds with 0 errors. | CHANGED: 9 files | Documentation is complete. |

---

## e) Safety and validation

| Problem before | What we changed | File(s) | Why it matters |
|---|---|---|---|
| The report page had no "not a diagnosis" warning. | Every report says: *"This is a screening result, not a medical diagnosis."* | CHANGED: `report.html`, `style.css` | Required for a mental-health tool. |
| Errors showed Java internals (stack traces) on screen. | Users see a plain error page; details go only to the console. | CHANGED: `application.properties` | Safer, and no ugly screens in the demo. |
| Wrong answers (e.g. "abc", 99 on a 0–3 scale) were secretly replaced with made-up values. | A wrong answer now counts as "not answered", so that section is not scored. | CHANGED: `AssessmentService.java` | The report is never based on invented data. |
| (kept on purpose) | Any Safety Check answer other than "No" still counts as **Yes**, even if it is unreadable. | — | A risk answer can never be hidden. |
| Wrong admin input gave error pages or was saved silently. | Admin forms are checked; a clear message is shown on the form. | NEW: `RuleDetails.java`, `ConditionDetails.java`, `InvalidInputException.java`; CHANGED: `AdminService`, `AdminController`, 3 admin pages | The admin panel cannot be broken. |
| Admin pages were protected by hand in each method. | Protected in one place (`AdminAuthInterceptor`); all 13 admin URLs tested. | see d) | Nobody can change rules without logging in. |

---

## f) Tests added

- **Before:** 0 tests. **Now: 237 automated tests, all passing** (179 unit tests + 58 integration tests).
- **Unit tests** check one class at a time: the 8 operators, the rule engine, the scoring of all
  10 questionnaires, the Builder, wrong answers, and the admin form rules.
- **Integration tests** start the whole app with its own test database and click through it like a user:
  the screening flows, admin login and logout, protected pages, creating / editing / deleting rules,
  and wrong input.
- A special test proves the new questionnaire code gives **exactly the same scores** as the original code
  on **10,000 random answer sets**.
- The app was also tested by hand in Chrome (8 checks). This found bug #4 above.

**How to run them:** in the `start-dev.bat` window, type:

```
mvn test
```

After about 30 seconds you should see `Tests run: 237, Failures: 0` and `BUILD SUCCESS`.
The tests never touch the real database.

Test files: 14 files in `src/test/java` (all NEW).

---

## g) New documents

| File | What it is |
|---|---|
| `DEMO_SCRIPT.md` (NEW) | Step-by-step guide for the live lab demo: exact answers to show MINIMAL, MILD, MODERATE and NEEDS ATTENTION, the admin demo, what to do if something goes wrong, and which screenshots to take. |
| `docs/TEST_CASES.md` (NEW) | The test case table for the report (ID, description, input, expected, actual, Pass/Fail). |
| `CHANGELOG.md` (NEW) | The full technical list of every change, with reasons. |
| `CHANGES_FOR_TEAM.md` (NEW) | This page. |
| `README.md` (CHANGED) | Correct numbers, project structure, design, testing and privacy sections. |

---

## What did NOT change

- The app **looks and works the same** for the user: same pages, same colours, same questions,
  same buttons, same one-question-at-a-time screening.
- The only visible differences: the "not a diagnosis" box on the report, clear error messages on the
  admin forms, and the PHQ-9 score of 0 now showing "Minimal depression".
- Same 10 questionnaires, same **150 rules**, same scores (proved by the 10,000-case test).
- Same database tables, same admin login (**admin / admin123**), same Java 21 + Spring Boot + SQLite.

---

## How to run the project

**What you need (Windows):** this folder layout — JDK 21 and Maven next to the project:

```
D:\
├── jdk21\
├── apache-maven\
└── VCogniRule\
```

**Start:**
1. Open the `VCogniRule` folder and double-click **`start-dev.bat`**.
2. In the black window, type: `mvn spring-boot:run`
3. Wait for `Started MentalHealthApp`, then open **http://localhost:8080** in your browser.
4. Admin page: **http://localhost:8080/admin** — username `admin`, password `admin123`.

**Stop:** click in the black window and press **Ctrl + C** (answer `Y` if it asks).

**Run the tests:** in the same window: `mvn test`

**Mac / Linux:** install JDK 21 and Maven, then in the project folder run `mvn spring-boot:run`.
