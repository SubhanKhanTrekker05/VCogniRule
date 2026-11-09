# VCogniRule

**Rule-Based Early Mental Health Screening System**

A privacy-first mental health screening tool that evaluates user responses across 10 validated clinical instruments using a 4-level forward-chaining rule engine with 150 rules. Produces a screening report with traceable findings, comorbid pattern detection, and treatment recommendations aligned with NICE NG222 clinical guidelines.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Default Credentials](#default-credentials)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Clinical Instruments](#clinical-instruments)
- [Privacy](#privacy)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **10 validated clinical instruments** — PHQ-9, GAD-7, C-SSRS, WHO-5, PSS-10, PSQI, PHQ-15, WSAS, ASRS, PC-PTSD
- **150-rule knowledge base** organized into a 4-level forward-chaining DAG:
  - Level 1 (78 rules): Instrument-specific scoring
  - Level 2 (57 rules): Comorbid pattern detection + NICE NG222 clinical pathways
  - Level 3 (12 rules): Complex multi-domain interactions
  - Level 4 (6 rules): Overall severity synthesis
- **Privacy by design** — zero user data stored. Responses are processed in memory and discarded after the report is generated.
- **Explanation facility** — every finding includes a trace showing which conditions matched and why
- **Admin knowledge-acquisition module** — full CRUD on rules and conditions via a web interface
- **Factory reset** — one-click restoration of the entire rule knowledge base to its original state
- **Real-time search & sortable columns** in the admin dashboard

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Template engine | Thymeleaf |
| Database | SQLite 3.54 (via JDBC) |
| Data access | Spring JdbcTemplate |
| Auth | BCrypt password hashing |
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

### Multiple Java versions on Mac/Linux?

No problem — they coexist without conflict:

- **macOS:** use `/usr/libexec/java_home -v 21` to select JDK 21 for the current session
- **Linux:** use `sudo update-alternatives --config java` to switch the system default

Or install [SDKMAN](https://sdkman.io/) for per-project version management:

```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.2-tem
sdk use java 21.0.2-tem
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

Place the cloned `VCogniRule` folder next to your `jdk21` and `apache-maven` folders (Windows) or anywhere you like (Mac/Linux).

### 2. Run

**Windows — double-click:**

1. Open the `VCogniRule` folder in File Explorer
2. Double-click `start-dev.bat`
3. A terminal opens — type `mvn spring-boot:run`

**Windows — terminal:**

```cmd
cd path\to\VCogniRule
start-dev.bat
mvn spring-boot:run
```

**Mac/Linux:**

```bash
cd path/to/VCogniRule
chmod +x start-dev.sh
./start-dev.sh
mvn spring-boot:run
```

### 3. Open

Navigate to **http://localhost:8080**

The app starts in ~15 seconds. On first launch, Spring Boot creates the SQLite database, loads the schema, seeds 150 rules + 151 questions (84 user-facing + 67 derived), and creates a default admin account.

> **Do not open the `.html` files directly in a browser.** Thymeleaf templates must be served through the running Spring Boot app. Always access the app via `http://localhost:8080`.

---

## Default Credentials

| | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |

These are created automatically on first startup if no admin exists. The password is BCrypt-hashed in the database. **Change this immediately in any non-local environment.**

---

## Project Structure

```
VCogniRule/
├── src/main/java/com/ooad/mhs/
│   ├── MentalHealthApp.java              # Entry point
│   ├── config/
│   │   └── DataSeeder.java               # Creates default admin on startup
│   ├── controller/
│   │   ├── WelcomeController.java        # Home page
│   │   ├── AboutController.java          # About page
│   │   ├── ScreeningController.java      # Assessment form + report
│   │   └── AdminController.java          # Admin CRUD + reset
│   ├── domain/                           # Records: Rule, Condition, ConditionGroup, etc.
│   ├── engine/
│   │   └── RuleEngine.java               # 4-pass forward-chaining inference
│   ├── repository/
│   │   ├── RuleRepository.java           # Rule CRUD + in-memory cache + factory reset
│   │   ├── QuestionRepository.java
│   │   └── AdminRepository.java
│   ├── service/
│   │   ├── AdminService.java             # Admin operations + reset wrapper
│   │   ├── AssessmentService.java        # Form data → working facts
│   │   ├── DerivedFieldCalculator.java   # 34 derived fields
│   │   ├── ScreeningService.java         # Orchestrates rule evaluation
│   │   └── ReportService.java            # Builds report model
│   └── util/
│       ├── ActionType.java               # Enum: 6 action types
│       ├── Category.java                 # Enum: 8 clinical domains
│       └── Operator.java                 # Enum: 8 comparison operators
├── src/main/resources/
│   ├── application.properties            # Spring Boot config
│   ├── schema.sql                        # 5-table SQLite schema
│   ├── data.sql                          # Seed data (150 rules + 151 questions)
│   ├── rules_master.sql                  # Factory-reset data source
│   ├── static/
│   │   ├── css/style.css
│   │   └── js/admin-rules.js             # Search + sort
│   └── templates/
│       ├── welcome.html
│       ├── about.html
│       ├── assessment.html
│       ├── report.html
│       └── admin-*.html                  # 5 admin templates
├── database/                             # SQLite .db file created here at runtime
│   └── .gitkeep
├── pom.xml
├── start-dev.bat                         # Windows launcher
├── start-dev.sh                          # Mac/Linux launcher
├── .gitignore
├── LICENSE
└── README.md
```

---

## How It Works

### Screening flow

```
User submits form
       │
       ▼
AssessmentService
  converts form strings → integers
  parses PSQI times → minutes
       │
       ▼
DerivedFieldCalculator
  computes 34 derived fields
  (phq9_total, crisis_detected, etc.)
       │
       ▼
RuleEngine (4 passes)
  Level 1: instrument scoring (78 rules)
  Level 2: comorbid + NICE (57 rules)
  Level 3: complex interactions (12 rules)
  Level 4: severity synthesis (6 rules)
       │
       ▼
ReportService
  builds findings + trace + severity
  detects crisis → shows helpline
       │
       ▼
Thymeleaf renders report.html
```

### Rule structure

Each rule has:

- A unique `rule_id` (e.g., `PHQ9-MINIMAL`)
- A `level` (1–4) controlling evaluation order
- A `category` (MOOD, ANXIETY, RISK, etc.)
- A `priority` (0–100) for conflict resolution
- An `action_type` (DISPLAY_INFO → CRISIS_PROTOCOL)
- One or more condition groups (AND within group, OR between groups)
- A `conclusion_key` + `conclusion_value` that gets set in working memory when the rule fires (enabling forward chaining)

### Admin dashboard

The admin dashboard supports:

- **Create / edit / delete** rules and their conditions
- **Toggle** rule active/inactive
- **Search** by rule ID or name with real-time highlighting
- **Sort** by any column (ascending/descending) — click any column header
- **Factory reset** — wipes all CRUD changes and restores the original 150-rule knowledge base from `rules_master.sql`

---

## Configuration

Key properties in `application.properties`:

```properties
# === Database ===
spring.datasource.url=jdbc:sqlite:database/vcognirule.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.username=
spring.datasource.password=
spring.datasource.hikari.maximum-pool-size=1

# === Schema + data initialization ===
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
spring.sql.init.continue-on-error=false

# === Thymeleaf ===
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.encoding=UTF-8

# === Web server ===
server.port=${PORT:8080}
server.error.include-stacktrace=always
server.error.include-message=always

# === Logging ===
logging.level.com.ooad.mhs=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.springframework.jdbc.core=DEBUG
```

---

## Deployment

### Railway (no cold starts)

1. Push to GitHub
2. Go to [railway.app](https://railway.app) → New Project → Deploy from GitHub repo
3. Railway auto-detects Maven → builds → runs `java -jar target/*.jar`
4. Generate a domain in Settings → Networking
5. App is live at `https://yourapp.up.railway.app`

### Render (free tier alternative)

1. Push to GitHub
2. Go to [render.com](https://render.com) → New → Web Service
3. Build command: `mvn clean package`
4. Start command: `java -jar target/*.jar`
5. Select free tier
6. App is live at `https://yourapp.onrender.com`

> Render's free tier sleeps after 15 minutes of inactivity. The first request after sleep takes ~30–60 seconds to wake up.

### SQLite note

Both platforms use ephemeral filesystems on free tiers. Since `data.sql` uses `INSERT OR IGNORE` and runs on every startup, the app always starts in its factory state (150 rules, default admin). This is by design — consistent with the privacy-first philosophy.

---

## Clinical Instruments

All instruments are validated, peer-reviewed tools:

| Instrument | Domain | Items | Source |
|---|---|---|---|
| PHQ-9 | Depression | 9 | Public domain (Pfizer) |
| GAD-7 | Anxiety | 7 | Public domain (Pfizer) |
| C-SSRS | Suicide risk | 6 | Columbia University |
| WHO-5 | Well-being | 5 | WHO (CC BY-NC-SA 3.0) |
| PSS-10 | Stress | 10 | Public domain |
| PSQI | Sleep quality | 19 | University of Pittsburgh |
| PHQ-15 | Somatic symptoms | 15 | Public domain (Pfizer) |
| WSAS | Functional impairment | 5 | Freely available |
| ASRS | ADHD | 6 | WHO |
| PC-PTSD | PTSD | 4 | Public domain (VA) |

Clinical recommendations sourced from **NICE NG222** (Depression in adults: treatment and management, 2022).

---

## Privacy

This system is designed to be used without an account and without storing any user data:

- **No user accounts** — screening is anonymous
- **No response storage** — answers are processed in memory and discarded
- **No session persistence** — the report exists only in the current page
- **No analytics or tracking**
- **Only the knowledge base and admin credentials are persisted** in the SQLite database

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -am 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please ensure your code follows the existing layered architecture (Controller → Service → Repository) and that any new rules include a `source_ref` pointing to peer-reviewed literature.

---

## License

This project is released under the **MIT License**. See [LICENSE](LICENSE) for details.

The clinical instruments used have their own licensing terms (see the Clinical Instruments table above). This project uses them for non-commercial educational purposes.
