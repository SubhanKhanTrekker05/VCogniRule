# VCogniRule: Code Explanation

This file explains the whole project in plain words: what it does, which file does what, how each feature
works inside the backend, and where we use OOP, SOLID and design patterns.

---

## Contents

1. [What the app is](#1-what-the-app-is)
2. [How to run it](#2-how-to-run-it)
3. [Architecture (layers)](#3-architecture-layers)
4. [Which file does what](#4-which-file-does-what)
5. [How each feature works](#5-how-each-feature-works)
6. [The rule engine in detail](#6-the-rule-engine-in-detail)
7. [Design patterns](#7-design-patterns)
8. [SOLID principles](#8-solid-principles)
9. [Other OOP concepts](#9-other-oop-concepts)
10. [Database](#10-database)
11. [Security and privacy](#11-security-and-privacy)
12. [Testing](#12-testing)
13. [Settings](#13-settings)
14. [Words you should know](#14-words-you-should-know)

---

## 1. What the app is

VCogniRule is a **rule-based expert system** for early mental-health screening.

- A person answers up to **10 standard questionnaires** (PHQ-9, GAD-7, C-SSRS, WHO-5, PSS-10, PSQI,
  PHQ-15, WSAS, ASRS, PC-PTSD).
- **150 IF-THEN rules** stored in a database decide what the answers mean.
- The report shows every result together with **why** it appeared (which answers and which rule).
- An **admin** can add, edit, switch off or reset the rules from the browser, with no Java change.

Nothing is guessed by AI. The same answers always give the same result.

**Two kinds of users**

| User | What they do |
|---|---|
| Screening user | Fills the questionnaires anonymously and reads the report. No login. |
| Admin | Logs in (default `admin` / `admin123`) and manages the knowledge base (rules and conditions). |

**Tech stack:** Java 21, Spring Boot (web MVC), Thymeleaf (HTML pages), SQLite (one-file database),
plain JDBC (`JdbcTemplate`), jBCrypt (password hashing), JUnit 5 (261 tests), Maven (build).

---

## 2. How to run it

| Way | Command / action |
|---|---|
| Maven | `mvn spring-boot:run`, then open http://localhost:8080 |
| Tests | `mvn test` (261 tests, about 30 seconds) |
| Desktop app | Double-click `dist\VCogniRule\VCogniRule.exe` (build it with `packaging\build-exe.ps1`) |
| Online | Railway / Render with the `prod` profile |

The database file `database/vcognirule.db` is created on first start. `schema.sql` makes the tables and
`data.sql` fills in the questions and the 150 rules.

---

## 3. Architecture (layers)

The app is **layered MVC**. Each layer only uses the layer under it.

```
Browser (View)      Thymeleaf pages + screening.js / admin-rules.js
      │
Controller          ScreeningController, AdminController      (+ AdminAuthInterceptor guards /admin/**)
      │
Service             AssessmentService, DerivedFieldCalculator, ScreeningService, ReportService,
      │             AdminService
      ├──────────── Rule engine: RuleEngine, ConditionEvaluator, OperatorStrategy ...
      │             (pure Java, no database, no Spring)
      │
Repository          RuleRepository, QuestionRepository, AdminRepository  (interfaces)
      │             JdbcRuleRepository, JdbcQuestionRepository, JdbcAdminRepository (SQL)
      │
SQLite database     5 tables
```

Important: the **rule engine never touches the database**. `ScreeningService` gets the active rules from
the repository and hands them to the engine. That is why the engine can be tested on its own.

---

## 4. Which file does what

All Java code is in `src/main/java/com/ooad/mhs/` (71 files). Each package is one layer.

### `config/`: start-up and web settings

| File | Job |
|---|---|
| `MentalHealthApp` (root package) | The `main` method. Starts Spring Boot. |
| `WebConfig` | Registers `AdminAuthInterceptor` for `/admin/**` (except login, logout). |
| `AdminAuthInterceptor` | Runs before every admin page; sends you to the login page if you are not logged in. |
| `DataSeeder` | On first start, creates the admin account `admin` / `admin123` (password stored as a hash). |
| `OperatorTracingSwitch` | Reads `vcognirule.trace-operators`; turns the Decorator demo on or off. |
| `BrowserLauncher` | Opens the browser when the app is started from `VCogniRule.exe`. |

### `controller/`: web pages (receive requests, return a page)

| File | URLs |
|---|---|
| `WelcomeController` | `GET /` home page |
| `AboutController` | `GET /about` |
| `ScreeningController` | `GET /screening` shows the form; `POST /screening` runs the screening and shows the report |
| `AdminController` | `/admin` login, `/admin/dashboard`, rule and condition create / edit / delete, `/admin/toggle/{id}`, `/admin/reset` |

### `domain/`: the data objects (Java records = read-only)

| File | What it holds |
|---|---|
| `Rule` | One rule: id, name, level 1–4, category, priority, action type, explanation, conclusion, groups |
| `ConditionGroup` | A group of conditions inside a rule (AND inside the group) |
| `Condition` | One check, e.g. `phq9_total BETWEEN 10 AND 14` |
| `Finding` | A rule that fired, with its trace |
| `ConditionTrace` | One "why" line: field, actual value, what was needed |
| `RuleDetails`, `ConditionDetails` | Admin form data with validation (`@NotBlank`, `@Min`, `@Max`) |
| `Question`, `Admin` | One question; one admin account |

### `engine/`: the inference engine (pure logic)

| File | Job |
|---|---|
| `RuleEngine` | Runs all rules in 4 passes (forward chaining) |
| `ConditionEvaluator` | Checks one condition against the facts |
| `FindingMapper` | Turns a fired rule into a `Finding` with its trace |
| `ConflictResolver` | Sorts findings by priority, crisis first |
| `operator/OperatorStrategy` | Interface: `test(actual, condition)` and `describe(condition)` |
| `operator/SingleValueStrategy` | Abstract parent of the 6 one-number operators |
| `operator/EqualsStrategy` … `InStrategy` | 8 operators: EQ, NE, GT, GTE, LT, LTE, BETWEEN, IN |
| `operator/OperatorStrategyRegistry` | The one shared set of operator objects (Singleton) |
| `operator/TracingOperatorStrategy` | Wraps an operator and logs every check (Decorator) |

### `questionnaire/`: scoring

| File | Job |
|---|---|
| `Questionnaire` | Abstract parent: `score(facts)` and `summary(...)` |
| `Phq9Questionnaire` … `WsasQuestionnaire` | 10 subclasses, each scores its own questionnaire |
| `QuestionnaireRegistry` | Holds exactly one questionnaire per section; stops the app at start-up if one is missing |

### `repository/`: database access

| File | Job |
|---|---|
| `RuleRepository` / `JdbcRuleRepository` | Read and write rules; keeps the active rules in memory (cache); factory reset |
| `QuestionRepository` / `JdbcQuestionRepository` | Read the questions |
| `AdminRepository` / `JdbcAdminRepository` | Read and save admin accounts |

### `security/`

| File | Job |
|---|---|
| `PasswordHasher` | Our interface: `hash()` and `matches()` |
| `BCryptPasswordHasher` | Does it with the jBCrypt library (Adapter) |

### `service/`: business logic

| File | Job |
|---|---|
| `QuestionnaireService` | Builds the questions and answer options for the form |
| `AssessmentService` | Validates answers, finds finished sections, converts answers to numbers |
| `DerivedFieldCalculator` | Asks each finished questionnaire to score itself |
| `ScreeningService` | Gets the active rules and runs the `RuleEngine` |
| `ReportService` | Builds the report shown to the user |
| `OverallResultResolver` | Picks the overall result (crisis always wins) |
| `FindingStyler` | Colour, icon and label of each finding |
| `AdminService` | All admin business logic (Facade + Observer subject) |
| `RuleChangeListener` | Observer interface: `onRulesChanged(change)` |
| `RuleCacheRefresher` | Observer: reloads the cached rules after a change |
| `AuditLogListener` | Observer: writes each change to the log |
| `InvalidInputException` | A business error whose message is shown on the form |

### `util/`: enums (fixed lists)

`Operator` (also creates the strategies: Factory Method), `Section` (the 10 tabs), `Category`
(MOOD, ANXIETY, RISK …), `ActionType` (DISPLAY_INFO up to CRISIS_PROTOCOL).

### Non-Java files

| File | Job |
|---|---|
| `src/main/resources/schema.sql` | Creates the 5 tables |
| `src/main/resources/data.sql` | Questions + 150 rules; runs on every start (`INSERT OR IGNORE`, no duplicates) |
| `src/main/resources/rules_master.sql` | The original rules, used by the factory reset |
| `application.properties` | Database path, port, logging, the 2 demo switches |
| `application-prod.properties` | Production settings (less logging, template cache on) |
| `templates/*.html` | Thymeleaf pages: welcome, about, assessment, report, 5 admin pages, error |
| `templates/fragments/finding.html` | One finding card, reused on the report |
| `static/js/screening.js` | Tabs, progress bar, one question at a time on the form |
| `static/js/admin-rules.js` | Live search and column sorting on the dashboard |
| `static/css/style.css` | All styles |
| `packaging/build-exe.ps1` | Builds `VCogniRule.exe` with jpackage |

---

## 5. How each feature works

### 5.1 Showing the questionnaire form

1. `ScreeningController.showAssessmentForm()` handles `GET /screening`.
2. `QuestionnaireService.getSections()` loads the questions (through `QuestionRepository`) and groups
   them into 10 tabs.
3. Thymeleaf fills `assessment.html`. `screening.js` shows the tabs and the "X of 10 sections done"
   progress bar. Each tab shows its estimated time, and the page says answers are not saved.
4. With `?resume=GAD-7` the last answers are read back from the HTTP session, so the user can go back
   from the report and finish a section without retyping.

### 5.2 Submitting: checking the answers

`ScreeningController.submitScreening()` handles `POST /screening` and calls `AssessmentService`:

- `filterAnswers()` keeps an answer **only if it is valid**: one of the allowed options, a real time like
  `23:05`, or a number inside its min/max. Things like `abc`, `99` on a 0–3 scale, or `25:00` are dropped
  and count as **not answered**.
- A section is scored **only if every question in it is answered**. A half-filled section is shown as
  "partly answered". Nothing is ever guessed.
- **Safety exceptions:** PHQ-9 question 9 (thoughts of self-harm) is always kept when answered, and any
  C-SSRS answer other than "0" counts as Yes. A risk signal is never lost.
- The Safety Check (C-SSRS) is **mandatory**. If it is not complete, the controller redirects back to
  that tab with an error.
- Answers become integers: `"2"` → 2, time `"23:00"` → 1380 minutes, `"7.5"` hours → 8.

### 5.3 Scoring each questionnaire

`DerivedFieldCalculator.calculate()` loops over all questionnaires and calls `score(facts)` on each
**finished** one. It does not know which questionnaire it is talking to: that is **polymorphism**.

Example, `Phq9Questionnaire.score()` adds:

| Field | Meaning |
|---|---|
| `phq9_total` | Sum of the 9 answers (0–27) |
| `phq9_core_symptom` | 1 if low interest or low mood on more than half the days |
| `phq9_symptom_count` | How many items are at least "more than half the days" |
| `depression_any` | 1 if total ≥ 5 |
| `depression_moderate_plus` | 1 if total ≥ 10 |

The result is one map called **working facts**: field name → number, e.g. `phq9_total = 14`.

Java does **not** decide "mild" or "severe". The rules in the database do, so an admin can change a
cut-off without recompiling.

### 5.4 Running the rules

`ScreeningService.evaluate()` takes the active rules from `RuleRepository` (from memory, not from the
database each time), creates a `RuleEngine` and calls `evaluate(rules, facts)`. See
[section 6](#6-the-rule-engine-in-detail).

### 5.5 Building the report

`ReportService.buildReport()` builds a `ScreeningReport` (a record). It is **display only**: it reads the
findings and scores and never changes a result.

| Part of the page | Where it comes from |
|---|---|
| Summary card (plain headline on a Low-to-Urgent scale) | `OverallResultResolver` picks the overall result. A crisis finding always wins. |
| Get help now (only in a crisis) | One box with message, helplines and steps, replacing the normal top part |
| What to do now | Plain steps for the overall result (`actionSteps`); a crisis gets the crisis steps |
| At a glance | One coloured tile per finished questionnaire (`GlanceTile`); unfinished ones on a "Not checked" line. A positive ASRS / PC-PTSD screen says "Needs attention" |
| Your scores | One row per finished section with a coloured score bar (`ScoreRow`, `ScoreBar`, `BarSegment`). The bands and colours are **read from the level-1 rules**, so if the admin changes a cut-off, the bar changes too |
| Your results | Finding cards grouped under topics (Safety, Mood, Anxiety, Stress, Sleep, Physical, Trauma, Attention, Daily life, Combined) by `TopicGroup`; `FindingStyler` gives each its colour and icon |
| Detailed report (one button; always printed) | "Why this appears" lines from each finding's trace, e.g. `PHQ-9 total = 14, needs 10 - 14`, and which sections were assessed, partly answered or not started |
| Date and time | When the screening was done |

`ReportExtras` is a **parameter object**: it groups the display parts so the `ScreeningReport`
constructor does not need a long list of arguments.

### 5.6 Admin login and page protection

1. `POST /admin/login` → `AdminService.authenticate()` finds the admin by username and calls
   `PasswordHasher.matches()` (BCrypt). Plain passwords are never stored or compared.
2. On success the username is stored in the HTTP session.
3. `AdminAuthInterceptor.preHandle()` runs **before** every `/admin/**` request and redirects to the
   login page if there is no admin in the session. One class instead of the same `if` in 13 controller
   methods (DRY).
4. `GET /admin/logout` calls `session.invalidate()`.

### 5.7 Creating and editing rules and conditions

1. The form is bound to `RuleDetails` / `ConditionDetails`. `@Valid` checks the simple rules (name
   required, level 1–4, priority 0–100, rule ID only letters, digits, `-` and `_`). Errors show on the form.
2. `AdminService` checks the rules that need the database: the rule ID must be unique, a condition's
   field must exist, and a condition must belong to the rule it is edited through. A broken rule throws
   `InvalidInputException`, and its message is shown on the form.
3. `JdbcRuleRepository` runs the SQL.
4. `AdminService` notifies its listeners (Observer): `RuleCacheRefresher` reloads the rules in memory, so
   the **next screening uses the change immediately**, with no restart, and `AuditLogListener` logs it.

### 5.8 Switching a rule on or off

`POST /admin/toggle/{id}` sets `is_active` to 0 or 1. Only active rules are loaded into the engine, so a
switched-off rule never fires. The observers refresh the cache.

### 5.9 Factory reset

`POST /admin/reset` → `AdminService.resetKnowledgeBase()` → `JdbcRuleRepository.resetKnowledgeBase()`:

1. Delete all conditions, then all groups, then all rules (children first).
2. Run `rules_master.sql` again (150 rules, 150 groups, 209 conditions).
3. Return the rule count (150). The observers reload the cache, and a message is shown on the dashboard.

### 5.10 Search and sort on the dashboard

Done in the browser by `admin-rules.js`: it filters the table rows while you type and sorts when you click
a column header. No request goes to the server.

### 5.11 Desktop app (`VCogniRule.exe`)

Built by `packaging/build-exe.ps1` with `jpackage`. The exe contains its own Java, so the PC does not
need Java installed. It starts Spring Boot with `vcognirule.open-browser=true`, so `BrowserLauncher`
opens http://localhost:8080 when the app is ready (`ApplicationReadyEvent`). Closing the console window
stops the app.

---

## 6. The rule engine in detail

### What a rule looks like

```
Rule PHQ9-MODERATE   (level 1, category MOOD, priority 50, action SUGGEST_PROFESSIONAL, source Kroenke 2001)
  Group 1:  phq9_total BETWEEN 10 AND 14
  THEN      finding "Moderate depression"
            conclusion: depression_moderate = 1   (added to the facts)
```

- **AND** between conditions inside one group.
- **OR** between groups (a rule fires if any group passes).
- The 150 rules are in **4 levels**: 79 / 53 / 12 / 6 rules.

| Level | Reads | Example |
|---|---|---|
| 1 | Answers and scores | PHQ9-MODERATE: `phq9_total` 10–14 |
| 2 | Combined flags and level-1 conclusions | COMP-MIXED-AD: `depression_moderate_plus = 1` **and** `anxiety_moderate_plus = 1` |
| 3 | Level-2 conclusions | Complex multi-domain patterns |
| 4 | Everything | Overall severity (MINIMAL, MILD, MODERATE …) |

### What `RuleEngine.evaluate()` does

```
for level = 1 to 4:
    take the rules of this level, highest priority first
    for each rule:
        for each group (OR):
            check every condition (AND) with ConditionEvaluator
        if the rule fired:
            add its conclusion to the working facts      ← forward chaining
            if it is the first level-4 "overall_severity" rule → that is the overall result
            FindingMapper creates a Finding with its trace
    after level 1: add any_treatment_recommendation (used by NICE rules)
sort all findings with ConflictResolver (highest priority first)
```

### Checking one condition

`ConditionEvaluator.evaluate(condition, facts)`:

1. If the field is **not in the facts** (section not finished), return `false`. So unfinished sections
   never fire rules.
2. Otherwise get the operator's strategy from `OperatorStrategyRegistry.getInstance()` and call
   `test(actual, condition)`.

### Conflict resolution

All fired rules are kept and shown. `ConflictResolver` sorts them by priority, so crisis findings
(priority ≥ 90) are always on top. For the overall result, the first level-4 severity rule that fires
(the highest priority) wins, so a later "mild" can never overwrite an earlier "critical".

### Example: Demo C

PHQ-9 questions 1–6 = 2, 7–9 = 0 → `phq9_total = 12`. GAD-7 all 2 → `gad7_total = 14`.

1. Scoring adds `depression_moderate_plus = 1` (12 ≥ 10) and `anxiety_moderate_plus = 1` (14 ≥ 10).
2. Level 1: PHQ9-MODERATE fires (12 is in 10–14) and adds `depression_moderate = 1`; GAD7-MODERATE fires.
3. Level 2: COMP-MIXED-AD needs both `_moderate_plus` flags, fires, and adds `mixed_anxiety_depression = 1`.
4. Level 4: the overall severity rule reads the facts so far and sets **MODERATE**.

---

## 7. Design patterns

We use **7 GoF patterns**: 2 creational, 3 structural, 2 behavioral.
(MVC, Repository and Dependency Injection are architecture that Spring gives us, not GoF patterns.)

| Type | Pattern | Files | In one line |
|---|---|---|---|
| Creational | Factory Method | `util/Operator` | Each operator constant decides which strategy object to create |
| Creational | Singleton | `OperatorStrategyRegistry` | One shared set of operator objects for the whole app |
| Structural | Adapter | `PasswordHasher`, `BCryptPasswordHasher` | Makes the jBCrypt library fit our own interface |
| Structural | Decorator | `TracingOperatorStrategy` | Wraps an operator to add logging without changing it |
| Structural | Facade | `AdminService` | One simple class in front of 3 repositories and the hasher |
| Behavioral | Strategy | `OperatorStrategy` + 8 classes | Each comparison is its own swappable class |
| Behavioral | Observer | `RuleChangeListener`, `RuleCacheRefresher`, `AuditLogListener`, `AdminService` | When rules change, all listeners are told automatically |

### Factory Method (creational): `util/Operator.java`

- **Problem:** the database stores the operator as text ("GTE"); we need the matching Java object.
- **How:** `Operator` declares `abstract OperatorStrategy createStrategy()`, and each constant overrides it:

  ```java
  GTE { public OperatorStrategy createStrategy() { return new GreaterOrEqualStrategy(); } },
  ```

- **Roles:** Creator = `Operator`; Concrete Creators = EQ, NE, GT …; Product = `OperatorStrategy`;
  Concrete Products = the 8 strategy classes.
- **Benefit:** no big `switch`. A new operator brings its own creation code.

### Singleton (creational): `engine/operator/OperatorStrategyRegistry.java`

- **Problem:** operator objects hold no data, so making new ones for 209 conditions on every screening is waste.
- **How:** `private static final INSTANCE`, a **private constructor**, and `getInstance()`. The constructor
  calls every `createStrategy()` once and keeps the results in an `EnumMap`.
- **Used by:** `ConditionEvaluator` and `FindingMapper`.

### Adapter (structural): `security/`

- **Problem:** jBCrypt has its own static methods (`hashpw`, `gensalt`, `checkpw`). We don't want the
  whole app tied to them.
- **How:** Target = our `PasswordHasher` (`hash`, `matches`); Adapter = `BCryptPasswordHasher`;
  Adaptee = the BCrypt library.
- **Benefit:** to switch to another library we change one class. `AdminService` and `DataSeeder` never see BCrypt.

### Decorator (structural): `engine/operator/TracingOperatorStrategy.java`

- **Problem:** for the demo we want to see each condition check in the console without editing the 8 operators.
- **How:** it **implements** `OperatorStrategy` and **holds** another `OperatorStrategy` (`wrapped`).
  `test()` calls the wrapped one, logs, and returns the same answer.
- **Switch:** `vcognirule.trace-operators=true` in `application.properties`. `OperatorTracingSwitch` then
  tells the registry to wrap every operator. Off by default.
- **Log line:** `[trace] phq9_total = 14 needs 10 - 14 -> true`

### Facade (structural): `service/AdminService.java`

- **Problem:** without it the admin controller would talk to 3 repositories and the hasher and repeat
  the business checks.
- **How:** `AdminController` only calls simple methods such as `createRule()`, `toggleRuleActive()`,
  `resetKnowledgeBase()`. `AdminService` does the checks and the repository calls behind them.

### Strategy (behavioral): `engine/operator/`

- **Problem:** a condition can compare in 8 ways; one method full of `if/else` would grow forever.
- **How:** interface `OperatorStrategy` with `test()` and `describe()`, and 8 classes. `SingleValueStrategy`
  is an abstract parent that shares helpers for the 6 one-number operators.
- **Context:** `ConditionEvaluator` uses the strategy without knowing which class it is.

### Observer (behavioral): `service/`

- **Problem:** after a rule changes, the cached rules must be reloaded and the change logged, but
  `AdminService` should not know every class that cares.
- **How:** Subject = `AdminService`. Spring gives it a `List<RuleChangeListener>`. After every change it
  calls `notifyListeners()`, which calls `onRulesChanged()` on each listener.
- **Observers:** `RuleCacheRefresher` (reloads the rules) and `AuditLogListener` (logs
  "Knowledge base changed: rule X created").
- **Benefit:** a third listener needs one new class and no change to `AdminService`.

### Three patterns on the same objects

Strategy = the 8 interchangeable comparison classes. Factory Method = how each one is **created**.
Singleton = where the created ones are **kept and shared**. Three different jobs.

---

## 8. SOLID principles

| Principle | Plain meaning | Where we follow it |
|---|---|---|
| **S**: Single Responsibility | One class, one reason to change | The screening is split into small classes: `AssessmentService` validates, `DerivedFieldCalculator` scores, `RuleEngine` infers, `ConflictResolver` sorts, `ReportService` builds the report, `FindingStyler` picks colours. `AdminAuthInterceptor` only checks login. |
| **O**: Open/Closed | Add new things without editing old code | New operator → 1 strategy class + 1 enum constant (`RuleEngine` untouched). New questionnaire → 1 subclass (`DerivedFieldCalculator` untouched). New observer → 1 listener class (`AdminService` untouched). New medical rule → added in the admin panel, **no Java change**. |
| **L**: Liskov Substitution | A child can replace its parent safely | Any of the 10 questionnaires works where `Questionnaire` is expected. Any operator (and the `TracingOperatorStrategy` wrapper) works where `OperatorStrategy` is expected. Fake repositories replace the JDBC ones in tests (`Fakes.java`). |
| **I**: Interface Segregation | Small interfaces | `OperatorStrategy` has 2 methods, `PasswordHasher` 2, `RuleChangeListener` 1. One repository per table instead of one big DAO. |
| **D**: Dependency Inversion | Depend on interfaces, not concrete classes | Services use `RuleRepository`, never `JdbcRuleRepository`. `AdminService` uses `PasswordHasher`, never BCrypt. Spring passes the real objects in through the constructor (**dependency injection**). |

**Where we bend it (say this honestly if asked):** `ConditionEvaluator` and `FindingMapper` call
`OperatorStrategyRegistry.getInstance()` directly, and `ScreeningService` creates the `RuleEngine` with
`new`. Both are stateless pure logic, so we accepted it; they are still tested on their own.

---

## 9. Other OOP concepts

| Concept | Where |
|---|---|
| Inheritance + polymorphism | `Questionnaire` (abstract) → 10 subclasses; `SingleValueStrategy` (abstract) → 6 subclasses |
| Interfaces | `OperatorStrategy` (8 implementations), the 3 repositories, `PasswordHasher`, `RuleChangeListener`; `AdminAuthInterceptor implements HandlerInterceptor` |
| Encapsulation | Records are read-only; fields `private final`; helpers `private` |
| Composition | `Rule` owns its `ConditionGroup`s, which own their `Condition`s (no life without the rule) |
| Aggregation | `QuestionnaireRegistry` holds questionnaires but Spring creates them; services hold repositories |
| Abstraction | Controllers see services, services see interfaces; only repositories see SQL |
| Enums with behaviour | `Operator` constants each implement `createStrategy()` |

---

## 10. Database

SQLite, one file (`database/vcognirule.db`), 5 tables made by `schema.sql`:

| Table | Holds |
|---|---|
| `admin` | username + BCrypt password hash |
| `question` | every question: field key, text, instrument, order |
| `rule` | rule ID, name, level, category, priority, action type, explanation, source, conclusion, active flag |
| `rule_condition_group` | the groups of a rule (OR between groups) |
| `rule_condition` | field, operator, value(s) (AND inside a group) |

```
rule 1 ──< rule_condition_group 1 ──< rule_condition
```

- `CHECK` constraints protect the data: level 1–4, priority 0–100, only the 8 operator names.
- A unique index on conditions stops duplicates when `data.sql` runs on every start.
- Access uses `JdbcTemplate` with `?` parameters, so there is no SQL injection.
- Active rules are **cached in memory** in `JdbcRuleRepository`; the Observer reloads the cache after a change.

---

## 11. Security and privacy

- **Anonymous screening:** no user accounts. Answers are **never saved** in the database. They are kept
  only in the HTTP session (expires after 30 minutes idle) so the user can go back and finish a section.
- **Passwords:** BCrypt hash with a random salt; the plain password is never stored.
- **Admin pages:** protected by `AdminAuthInterceptor`.
- **XSS:** Thymeleaf escapes text by default.
- **Errors:** users never see stack traces (`server.error.include-stacktrace=never`).
- **Safety:** a crisis answer always shows the helplines and outranks every other result.

---

## 12. Testing

`mvn test` runs **261 JUnit 5 tests** in about 30 seconds.

| Test class | What it checks |
|---|---|
| `OperatorStrategyTest` | The 8 operators, Factory Method, Singleton |
| `RuleEngineTest` | AND/OR, forward chaining, overall severity, sorting |
| `QuestionnaireScoringTest` | Scoring of all 10 questionnaires with hand-worked examples |
| `QuestionnaireRefactorEquivalenceTest` | New scoring = old scoring on 10,000 random answer sets |
| `AssessmentServiceTest` | Bad answers count as not answered; section completion |
| `AdminServiceTest` | Admin business rules + Observer notifications (fake repositories) |
| `ReportPartsTest` | Report parts, `OverallResultResolver`, `FindingStyler` |
| `FormValidationTest` | Validation rules of the admin forms |
| `BCryptPasswordHasherTest` | Adapter |
| `TracingOperatorStrategyTest` | Decorator gives the same results |
| `BrowserLauncherTest` | Exe launcher: address and open command per OS |
| `ScreeningFlowIntegrationTest` | The whole screening through the running app (MockMvc) |
| `AdminIntegrationTest` | Login, logout, the 13 protected URLs, CRUD, reset, bad input |
| `FormRecoveryAndErrorPageIntegrationTest` | Going back to the form, error pages |

Unit tests check one class alone. Integration tests start the real app with a fresh SQLite file in `target/`.
The test-case table (ID, input, expected, actual, result) is in `docs/TEST_CASES.md`.

---

## 13. Settings

`src/main/resources/application.properties`:

| Setting | Meaning |
|---|---|
| `spring.datasource.url=jdbc:sqlite:database/vcognirule.db` | Where the database file is |
| `spring.sql.init.mode=always` | Run `schema.sql` and `data.sql` on every start |
| `server.port=${PORT:8080}` | Port (Railway passes `PORT`) |
| `server.error.include-stacktrace=never` | No stack traces for users |
| `vcognirule.open-browser=false` | `true` only in the exe: open the browser on start |
| `vcognirule.trace-operators=false` | `true` turns on the Decorator demo (logs every condition check) |

---

## 14. Words you should know

| Word | Meaning here |
|---|---|
| Expert system | A program that copies an expert's reasoning with IF-THEN rules |
| Knowledge base | The 150 rules in the database |
| Working facts | The map of field → number the engine reads (answers, scores, conclusions) |
| Derived field | A value calculated from answers, e.g. `phq9_total` |
| Forward chaining | Fire rules from known facts, add their conclusions as new facts, continue |
| Finding | A rule that fired, shown on the report |
| Trace | The "why this appears" lines of a finding |
| Conflict resolution | Deciding the order when many rules fire (by priority) |
| Bean / dependency injection | Spring creates the objects and passes them into constructors |
| Record | A Java class for read-only data (fields final, `equals`/`toString` free) |
