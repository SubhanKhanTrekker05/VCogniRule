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
