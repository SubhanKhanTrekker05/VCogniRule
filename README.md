# VCogniRule

**Rule-Based Early Mental Health Screening System**

A privacy-first mental health screening tool that evaluates user responses across 10 validated clinical instruments using a 4-level forward-chaining rule engine with 150 rules. Produces a screening report with traceable findings, comorbid pattern detection, and treatment recommendations aligned with NICE NG222 clinical guidelines.

> **This is a screening tool, not a diagnostic system.** It points out possible warning signs and recommends professional support. Every report says so.

Built for CT-263 Object-Oriented Analysis & Design (NED University). See [CHANGELOG.md](CHANGELOG.md) for the changes made during the OOAD review.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Default Credentials](#default-credentials)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Object-Oriented Design](#object-oriented-design)
- [Testing](#testing)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Clinical Instruments](#clinical-instruments)
- [Privacy](#privacy)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **10 validated clinical instruments**: PHQ-9, GAD-7, C-SSRS, WHO-5, PSS-10, PSQI, PHQ-15, WSAS, ASRS, PC-PTSD
- **150-rule knowledge base** (150 condition groups, 209 conditions) organized into a 4-level forward-chaining DAG:
  - Level 1 (79 rules): instrument scoring and single-item flags
  - Level 2 (53 rules): comorbid patterns, hedged findings, gating and NICE NG222 pathways
  - Level 3 (12 rules): complex multi-domain interactions
  - Level 4 (6 rules): overall severity synthesis (5 rules) and the NICE overall summary
- **Safety first**: the Safety Check (C-SSRS) is mandatory; any risk answer shows crisis helplines and outranks every other result
- **Only completed sections are scored**: answers are validated, and an invalid or missing answer leaves its section "partly answered" instead of being guessed
- **Explanation facility**: every finding shows "Why this appears" (the answers, scores or earlier findings that made the rule fire)
- **Admin knowledge-acquisition module**: create, edit, delete and switch rules and conditions, with validation and clear error messages
- **Factory reset**: one click restores the original 150-rule knowledge base
- **Real-time search & sortable columns** in the admin dashboard

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 (Spring MVC) |
| Template engine | Thymeleaf |
| Database | SQLite (sqlite-jdbc 3.54.1.0) |
| Data access | Spring JdbcTemplate |
| Validation | Jakarta Bean Validation |
| Auth | BCrypt password hashing (jBCrypt 0.4) |
| Tests | JUnit 5 (spring-boot-starter-test) |
| Build | Maven 3.9+ |

---

## Prerequisites

### Windows — Portable Setup (no admin rights needed)

Download the **ZIP** versions (not MSI installers):

1. **JDK 21 ZIP** — from [Adoptium](https://adoptium.net/) → extract and rename the folder to `jdk21`
2. **Apache Maven ZIP** — from [maven.apache.org](https://maven.apache.org/download.cgi) → extract and rename the folder to `apache-maven`
3. Place both folders next to the cloned project:

```
AnyFolder/
├── jdk21/
├── apache-maven/
└── VCogniRule/
    ├── start-dev.bat
    └── pom.xml
```

The `start-dev.bat` script auto-detects these folders — no PATH configuration needed.

> **Already have Java and Maven in your system PATH?** The script falls back to them automatically if the portable folders aren't found.

### macOS — Homebrew

```bash
brew install openjdk@21 maven
```

### Linux — System Package Manager

```bash
# Ubuntu / Debian
sudo apt install openjdk-21-jdk maven

# Fedora / RHEL
sudo dnf install java-21-openjdk maven

# Arch Linux
sudo pacman -S jdk-openjdk maven
```

### Verify your setup

```bash
java -version   # should show 21.x.x
mvn -version    # should show 3.9+
```

---

## Quick Start

### 1. Clone

```bash
git clone https://github.com/SubhanKhanTrekker05/VCogniRule.git
```

### 2. Run

**Windows — double-click:** open the `VCogniRule` folder, double-click `start-dev.bat`, then type `mvn spring-boot:run` in the terminal that opens.

**Windows — terminal:**

```cmd
cd path\to\VCogniRule
start-dev.bat
mvn spring-boot:run
```

**Mac/Linux:**

```bash
cd path/to/VCogniRule
mvn spring-boot:run
```

> Known issue: `start-dev.sh` currently has a missing closing quote on line 10, so it stops with a syntax error. Run `mvn spring-boot:run` directly as shown above.

### 3. Open

Navigate to **http://localhost:8080**

The app starts in about 5–15 seconds. On first launch, Spring Boot creates the SQLite database in `database/`, loads the schema, seeds 150 rules and 151 question rows (84 questions shown to the user + 67 calculated fields and rule conclusions), and creates a default admin account.

> **Do not open the `.html` files directly in a browser.** Thymeleaf templates must be served through the running Spring Boot app.

---

## Default Credentials

| | Value |
|---|---|
| Admin page | http://localhost:8080/admin |
| Username | `admin` |
| Password | `admin123` |

These are created automatically on first startup if no admin exists. The password is BCrypt-hashed in the database. **Change this in any non-local environment.**

---

## Project Structure

63 Java source files: 46 classes (2 of them abstract), 9 records, 4 enums and 4 interfaces.

```
VCogniRule/
├── src/main/java/com/ooad/mhs/
│   ├── MentalHealthApp.java                 # Entry point (@SpringBootApplication)
│   ├── config/
│   │   ├── DataSeeder.java                  # Creates the default admin on startup
│   │   ├── AdminAuthInterceptor.java        # Blocks /admin/** pages unless logged in
│   │   └── WebConfig.java                   # Registers the interceptor
│   ├── controller/                          # MVC controllers
│   │   ├── WelcomeController.java           # GET /
│   │   ├── AboutController.java             # GET /about
│   │   ├── ScreeningController.java         # GET/POST /screening (form → report)
│   │   └── AdminController.java             # /admin/** (rules, conditions, reset)
│   ├── domain/                              # Records: Rule, ConditionGroup, Condition, Finding,
│   │                                        # ConditionTrace, Question, Admin, RuleDetails, ConditionDetails
│   ├── engine/
│   │   ├── RuleEngine.java                  # 4-pass forward-chaining inference
│   │   ├── ConditionEvaluator.java          # Evaluates one condition
│   │   ├── FindingFactory.java              # Creates a Finding with its trace
│   │   ├── ConflictResolver.java            # Sorts findings by priority
│   │   └── operator/                        # Strategy pattern: one class per operator
│   │       ├── OperatorStrategy.java        #   interface
│   │       ├── SingleValueStrategy.java     #   abstract base of EQ, NE, GT, GTE, LT, LTE
│   │       ├── EqualsStrategy.java … LessOrEqualStrategy.java, BetweenStrategy.java, InStrategy.java
│   │       └── OperatorStrategyFactory.java #   Factory
│   ├── questionnaire/                       # Inheritance: one subclass per instrument
│   │   ├── Questionnaire.java               #   abstract base class (Template Method)
│   │   ├── Phq9Questionnaire.java … WsasQuestionnaire.java   # 10 subclasses
│   │   └── QuestionnaireRegistry.java       #   one questionnaire per section
│   ├── repository/                          # Repository pattern: interface + JDBC implementation
│   │   ├── RuleRepository.java / JdbcRuleRepository.java
│   │   ├── QuestionRepository.java / JdbcQuestionRepository.java
│   │   └── AdminRepository.java / JdbcAdminRepository.java
│   ├── service/
│   │   ├── QuestionnaireService.java        # Questions + answer options for the form
│   │   ├── AssessmentService.java           # Validates answers, finds completed sections
│   │   ├── DerivedFieldCalculator.java      # Asks each completed questionnaire to score itself
│   │   ├── ScreeningService.java            # Runs the rule engine on the active rules
│   │   ├── ReportService.java               # Builds the report (ScreeningReport + Builder)
│   │   ├── FindingStyler.java               # Tone / icon / label of each finding
│   │   ├── OverallResultResolver.java       # Overall banner
│   │   ├── AdminService.java                # Admin operations + business-rule checks
│   │   └── InvalidInputException.java       # Business-rule error with a message
│   └── util/                                # Enums: ActionType, Category, Operator, Section
├── src/main/resources/
│   ├── application.properties / application-prod.properties
│   ├── schema.sql                           # 5 tables
│   ├── data.sql                             # Seed data, runs on every startup (INSERT OR IGNORE)
│   ├── rules_master.sql                     # Factory-reset data
│   ├── static/                              # css/style.css, js/screening.js, js/admin-rules.js, logos
│   └── templates/                           # welcome, about, assessment, report, admin-* (5), fragments/finding
├── src/test/java/com/ooad/mhs/              # JUnit 5 tests
├── database/                                # SQLite .db file created here at runtime
├── pom.xml, Dockerfile, start-dev.bat, start-dev.sh
├── CHANGELOG.md, LICENSE, README.md
```

---

## How It Works

### Screening flow

```
User submits the form (POST /screening)
       │
       ▼
AssessmentService
  keeps only valid answers (invalid → not answered)
  works out which sections are complete (C-SSRS is mandatory)
  converts answers to integers (times → minutes)
       │
       ▼
DerivedFieldCalculator
  each completed Questionnaire subclass adds its own
  totals, subscales and flags (phq9_total, crisis_detected, …)
       │
       ▼
RuleEngine (4 passes, one per level)
  ConditionEvaluator → OperatorStrategy for each condition
  fired rules → FindingFactory → Finding + trace
  conclusions are added to the facts for the next level (forward chaining)
  ConflictResolver sorts by priority
       │
       ▼
ReportService
  OverallResultResolver → banner;  FindingStyler → colours/icons
  plain-language "Why this appears", answers per section
  ScreeningReport.builder()…build()
       │
       ▼
Thymeleaf renders report.html
```

### Rule structure

Each rule has:

- A unique `rule_id` (e.g., `PHQ9-MINIMAL`)
- A `level` (1–4) controlling evaluation order
- A `category` (MOOD, ANXIETY, RISK, etc.)
- A `priority` (0–100) for ordering
- An `action_type` (DISPLAY_INFO → CRISIS_PROTOCOL)
- One or more condition groups (AND within a group, OR between groups; every seeded rule has one group)
- A `conclusion_key` + `conclusion_value` that is added to the working facts when the rule fires (forward chaining)

### Admin dashboard

- **Create / edit / delete** rules and their conditions (validated; errors are shown on the form)
- **Toggle** a rule active/inactive
- **Search** by rule ID or name with real-time highlighting
- **Sort** by any column (click a column header)
- **Factory reset**: restores the original 150 rules, 150 groups and 209 conditions from `rules_master.sql`

---

## Object-Oriented Design

| Concept / pattern | Where |
|---|---|
| Inheritance + polymorphism | `Questionnaire` (abstract) → 10 subclasses; `SingleValueStrategy` (abstract) → 6 subclasses |
| Interfaces | `OperatorStrategy` (8 implementations), `RuleRepository`, `QuestionRepository`, `AdminRepository`; `AdminAuthInterceptor implements HandlerInterceptor` |
| Composition | `Rule` → `ConditionGroup` → `Condition`; `ScreeningReport` → `FindingView` |
| Aggregation | services → repositories; `QuestionnaireRegistry` → `Questionnaire` |
| Encapsulation | immutable records, `private final` fields, private helpers |
| Template Method | `Questionnaire.contributeTo`, `SingleValueStrategy.test/describe` |
| Strategy + Factory | `OperatorStrategy` + `OperatorStrategyFactory`; `FindingFactory` |
| Builder | `ReportService.ScreeningReport.Builder` |
| Repository + Dependency Inversion | `repository/` interfaces with JDBC implementations |
| MVC | controllers + Thymeleaf views + domain/service model |
| Singleton / Dependency Injection | Spring beans (one instance each, injected through constructors) |
| Facade | `AdminService` |

Severity (mild, moderate, …) is decided by the rules in the knowledge base, not by Java code, so an admin can change it without recompiling.

---

## Testing

```bash
mvn test
```

Runs the JUnit 5 tests in `src/test/java`, including a before/after test that compares the questionnaire scoring with a frozen copy of the original code on 10,000 random answer sets.

---

## Configuration

`src/main/resources/application.properties`:

```properties
# === Database ===
spring.datasource.url=jdbc:sqlite:database/vcognirule.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.hikari.maximum-pool-size=1

# === Schema + data initialization ===
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql

# === Web server ===
server.port=${PORT:8080}
server.error.include-stacktrace=never
server.error.include-message=never

# === Logging ===
logging.level.com.ooad.mhs=DEBUG
```

`application-prod.properties` (activate with `SPRING_PROFILES_ACTIVE=prod`) enables template caching and quieter logging.

---

## Deployment

### Railway

1. Push to GitHub
2. Go to [railway.app](https://railway.app) → New Project → Deploy from GitHub repo
3. Railway auto-detects Maven → builds → runs `java -jar target/*.jar`
4. Generate a domain in Settings → Networking

### Render (free tier) — uses the included `Dockerfile`

1. Push to GitHub
2. Go to [render.com](https://render.com) → New → Web Service
3. Select free tier

> Render's free tier sleeps after 15 minutes of inactivity. The first request after sleep takes ~30–60 seconds.

### SQLite note

Both platforms use ephemeral filesystems on free tiers. Since `data.sql` uses `INSERT OR IGNORE` and runs on every startup, the app always starts in its factory state (150 rules, default admin).

---

## Clinical Instruments

| Instrument | Domain | Items | Source |
|---|---|---|---|
| PHQ-9 | Depression | 9 | Public domain (Pfizer) |
| GAD-7 | Anxiety | 7 | Public domain (Pfizer) |
| C-SSRS | Suicide risk | 6 | Columbia University |
| WHO-5 | Well-being | 5 | WHO (CC BY-NC-SA 3.0) |
| PSS-10 | Stress | 10 | Public domain |
| PSQI | Sleep quality | 17 in this form (19 in the full PSQI) | University of Pittsburgh |
| PHQ-15 | Somatic symptoms | 15 | Public domain (Pfizer) |
| WSAS | Functional impairment | 5 | Freely available |
| ASRS | ADHD | 6 | WHO |
| PC-PTSD | PTSD | 4 | Public domain (VA) |

Clinical recommendations sourced from **NICE NG222** (Depression in adults: treatment and management, 2022).

---

## Privacy

- **No user accounts**: screening is anonymous.
- **Answers are never written to the database.** They are processed in memory to build the report.
- **Short-term session only:** the last submitted answers are kept in the server's HTTP session so the user can go back from the report and finish a section. They are cleared when a new screening is started and expire with the session (30 minutes of inactivity by default).
- **No analytics or tracking.**
- **Only the knowledge base and admin credentials are stored** in the SQLite database.

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes and push the branch
4. Open a Pull Request

Please keep the layered architecture (Controller → Service → Repository), run `mvn test` before committing, and give any new rule a `source_ref` pointing to peer-reviewed literature.

---

## License

This project is released under the **MIT License**. See [LICENSE](LICENSE) for details.

The clinical instruments used have their own licensing terms (see the Clinical Instruments table above). This project uses them for non-commercial educational purposes.
