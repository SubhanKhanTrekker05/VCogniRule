# Live Demo Script — VCogniRule

A step-by-step guide for the CT-263 lab demo (about 10–12 minutes).
Every answer set below was checked on the final code: by an automated test
(`ScreeningFlowIntegrationTest.demoHealthy / demoMild / demoModerate / demoCrisis`) and by hand in Chrome.

---

## 1. Before the lab (the day before)

1. Open a terminal in the project folder and run the tests:
   ```
   start-dev.bat
   mvn clean package
   ```
   Wait for `Tests run: 253, Failures: 0` and `BUILD SUCCESS` (about 30 seconds).
   This also creates `target/vcognirule-1.0.0.jar`, your backup way to start the app (see section 6).
2. Start the app once (`mvn spring-boot:run`), open http://localhost:8080, then stop it (Ctrl + C).
   This makes sure Maven has downloaded everything, so you do not need internet in the lab.
3. Take the screenshots for the report now (see `docs/TEST_CASES.md` and the list at the end of this file),
   so you also have a backup if something fails live.
4. Charge the laptop and turn off sleep mode. Set the browser zoom to 100%.

## 2. Starting the app in the lab

1. Double-click `start-dev.bat` (it finds `jdk21` and `apache-maven` next to the project).
2. In the window that opens, type:
   ```
   mvn spring-boot:run
   ```
3. Wait for the line `Started MentalHealthApp in … seconds` (about 5–15 seconds).
4. Open **http://localhost:8080** in Chrome.

Optional, to start with the original knowledge base: log in to the admin page and press **Reset KB**
(section 4, step 7: **↺ Reset KB**, then **OK**), or stop the app, delete `database/vcognirule.db`, and start it again.

## 3. The screening demo (user side)

Answer labels used below (PHQ-9 and GAD-7): **0 = Not at all, 1 = Several days,
2 = More than half the days, 3 = Nearly every day**. The page shows one question at a time and moves
on automatically after each answer. Only the sections you fully complete are scored.

### Step 1 — Welcome page (30 s)
- Point out the disclaimer: *"This is a screening tool, not a diagnostic system."*
- Click **Start Screening**.

### Step 2 — Show the Safety Check is required (30 s)
- Without answering anything, click **Submit Screening** (bottom right).
- The page refuses: *"The Safety Check is required. Please answer every question in this section before submitting."*

### Step 3 — Demo A: healthy → MINIMAL (2 min)
| Section (tab) | Answers |
|---|---|
| Safety Check | all 6 questions: **No** |
| Depression & Mood (PHQ-9) | all 9 questions: **Not at all** |
| Anxiety (GAD-7) | all 7 questions: **Not at all** |

- Click **Submit Screening** → the window "Before you submit" lists the 3 sections → **Submit**.
- Expected report: overall **MINIMAL**, no helpline box, scores *Total score 0 / 27* and *0 / 21*.
- Show: the disclaimer box, a finding's **"Why this appears"** (click to open it), and
  "What this report covers" (3 assessed, 7 not assessed).
- Click **Start a new screening**.

### Step 4 — Demo C: depression + anxiety → MODERATE (2 min)
| Section | Answers |
|---|---|
| Safety Check | all **No** |
| Depression & Mood | questions 1–6: **More than half the days**; questions 7–9: **Not at all** (total 12) |
| Anxiety | all 7 questions: **More than half the days** (total 14) |

- Expected: overall **MODERATE**; findings *Moderate depression (PHQ9-MODERATE)*,
  *Moderate anxiety (GAD7-MODERATE)*, *Mixed anxiety-depressive (COMP-MIXED-AD)*.
- Explain **forward chaining**: Level 1 rules score each test → the Level 2 rule COMP-MIXED-AD reads
  both results → the Level 4 rule sets the overall result MODERATE.

### Step 5 (optional) — Demo B: MILD (1 min)
| Section | Answers |
|---|---|
| Safety Check | all **No** |
| Depression & Mood | questions 1–8: **Several days**; question 9: **Not at all** (total 8) |
| Anxiety | questions 1–3: **Several days**; questions 4–7: **Not at all** (total 3) |

- Expected: overall **MILD** (finding *Mild depression*).

### Step 6 — Demo D: safety concern → NEEDS ATTENTION (1.5 min)
| Section | Answers |
|---|---|
| Safety Check | question 4 (*"…had some intention of acting on them?"*): **Yes, once or twice**; all others: **No** |
| Depression & Mood | all **Not at all** |
| Anxiety | all **Not at all** |

- Expected: overall **NEEDS ATTENTION**, the helpline box (*Pakistan: 1166 (Umang Helpline)*),
  and the crisis finding *Suicidal ideation with intent (CSSRS-INTENT)* shown first.
- Point out: any "Yes" (even "once or twice") counts as Yes, and the safety result outranks
  every other result.

## 4. The admin demo (knowledge base, about 3 min)

1. Go to **http://localhost:8080/admin** and log in with **admin / admin123**.
2. Dashboard: 150 rules. Type **PHQ9** in the search box → *"8 of 150 rules match"*.
   Click the **Pri** column header to sort.
3. Open rule **PHQ9-MINIMAL** (click its name): show its condition `phq9_total BETWEEN 0 and 4`.
4. **Validation:** on that page click **Add Condition**, choose operator **BETWEEN**, Min **9**, Max **5**, save.
   The form shows the message *"The values do not fit the operator …"* and nothing is saved.
5. **Rules drive the result, not Java code:** go back to the dashboard, find **COMP-MIXED-AD**
   (search "MIXED") and click its **Deactivate** button.
6. In a new tab, run **Demo C** again (step 4). The overall result is now **NOT CLASSIFIED**,
   because the rule that combined depression and anxiety is off.
7. Back in the admin tab press **↺ Reset KB** (factory reset) and click **OK** in the browser's
   confirmation box → *"Knowledge base restored to factory defaults. 150 rules reloaded."*
   Run Demo C once more → **MODERATE** again.
8. Click **Logout**.

## 5. Showing the tests (1 min, optional)

In the terminal (stop the app first with Ctrl + C, or open a second `start-dev.bat` window):
```
mvn test
```
Expected after about 30 seconds: `Tests run: 253, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.
Mention: unit tests for the 8 operators, the 10 questionnaires, each design pattern and validation, and
integration tests that start the whole app and send real requests (screening flows, admin login,
protected pages, bad input).

## 6. If something goes wrong

| Problem | What to do |
|---|---|
| `'java' is not recognized` or `'mvn' is not recognized` | Use the window opened by `start-dev.bat` (it sets the paths). Check `jdk21` and `apache-maven` are in the folder next to the project. |
| `Port 8080 was already in use` | Another copy is running: close the other terminal window. Or start on another port: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and open http://localhost:8081 |
| `mvn` fails (no internet, build error) | Start the ready-made jar instead: `java -jar target\vcognirule-1.0.0.jar` (built in section 1). |
| A result is not what this script says | Someone changed rules in the admin page. Admin → **↺ Reset KB** → **OK**, then run the demo again. |
| Report says "Not determined" | The Depression & Mood section was not finished. Click **Complete Depression section**; earlier answers are kept. |
| A section is shown as "Not enough data" | One question in it was skipped. Click **Complete this section**. |
| Admin login fails | Use exactly `admin` / `admin123`. If it still fails, stop the app, delete `database\vcognirule.db`, start again (a fresh admin is created). |
| The page looks stuck or old | Press **F5** to reload, or **Ctrl + F5** to reload without cache. |
| A browser error page appears | Click back, note the URL, carry on with the next step; the terminal window shows the error details (no stack trace is shown to users). |
| Laptop or app crashes completely | Restart with `mvn spring-boot:run` (or the jar), and show the screenshots prepared in section 1 meanwhile. |

## 7. Screenshots for the report (Results section)

Take these on a fresh start (after **↺ Reset KB**), at 100% zoom:

1. Welcome page with the disclaimer.
2. Assessment page: the Safety Check tab with question 1 and the tab bar.
3. The Safety Check "required" message after clicking Submit too early.
4. The "Before you submit" window listing the completed sections.
5. Report **MINIMAL** (Demo A): banner, disclaimer, one finding with "Why this appears" opened.
6. Report **MODERATE** (Demo C): banner and the findings PHQ9-MODERATE, GAD7-MODERATE, COMP-MIXED-AD.
7. Report **NEEDS ATTENTION** (Demo D): banner, helpline box and the crisis finding at the top.
8. Report section "What this report covers" + "Your answers" with the score lines.
9. Admin login page, and the login error after a wrong password.
10. Admin dashboard with the search "PHQ9" (8 of 150).
11. Rule detail page of PHQ9-MINIMAL (conditions table).
12. Admin form showing a validation message (BETWEEN with Min 9, Max 5).
13. The factory reset message "Knowledge base restored to factory defaults. 150 rules reloaded."
14. Terminal with `mvn test`: `Tests run: 253, Failures: 0` and `BUILD SUCCESS`.
