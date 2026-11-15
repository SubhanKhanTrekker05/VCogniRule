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
