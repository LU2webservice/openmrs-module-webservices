# Improvements - Prioritization and Justification

> **What is this document?**
> This is the **improvement plan** for the OpenMRS REST Webservices module. It does not
> introduce new findings. Instead it **consolidates** everything we already analysed and
> tested across the project - the threat model, the risk criteria and matrix, the gap
> analyses, the security backlog, the penetration test, the dependency scans and the test
> results - and turns it into a **single, prioritized list of improvements** with an
> explicit justification for the order.
>
> Every improvement is traceable to (a) a documented analysis and (b) a measurement
> (pentest result, dependency-scan CVSS, or test run). Nothing here is invented.

| | |
|---|---|
| **Module** | OpenMRS REST Webservices Module v3.2.0 (`/ws/rest/v1/...`) |
| **Date** | 2026-06-16 |
| **Author** | Pluk Zwaal |
| **Branch** | `code-tests-logging` |
| **Status** | 1 critical-path improvement (R-1 audit logging) already implemented and tested; the rest prioritized below |

---

## 1. Purpose and scope

The module is the single REST entry point to all clinical data in OpenMRS. Because it
processes **special category personal data** (GDPR / AVG art. 9) we apply a **low risk
appetite**, as defined in the risk criteria. This document answers one question:

> *Given everything we found and measured, which improvements do we make, in what order,
> and why?*

It is the bridge between **analysis** (what is wrong and how bad) and **action** (what we
fix first). It builds directly on the work in the other documents and does not repeat the
underlying analysis - it references it.

---

## 2. Sources used (traceability)

Each improvement below is linked back to these inputs. Some deliverables live on another
branch; that branch is named so the document can be found.

| Source | What it provides | Document |
|---|---|---|
| Threat model (STRIDE + C4) | Threats, trust boundaries (TB-1...TB-7), security goals (BG-1...BG-6) | `threat-model.md` |
| Risk criteria & risk register | BIV/CIA scale, likelihoodximpact scoring, risk classes, decision rules | `risk-criteria.md` (branch `docs_risk_criteria_assessment`), `risk-matrix.md`, `risk-evaluation.md` |
| Risk assessment report | Mitigations per finding, NEN 7510-2:2024 mapping, **cost estimate** | `risk-assessment-report.md` (branch `docs_risk_criteria_assessment`) |
| BIV/CIA risk analysis | Crown jewels, applied risk register | `BIV-risicoanalyse.md` (branch `analyze-project`) |
| Gap analyses | Current vs. desired state, NEN 7510 coupling | `01-gap-analyse.md`, `Gap-analyse-logging.md` (branch `docs_auditreport_gapanalysis`) |
| Security backlog + pentest report | Prioritized requirements (SR-1...SR-18) with **effort**, and pentest findings (PT-1...PT-15) with **measured exploitability** and the solve/accept decision | `Security_Backlog_Pentest_Rapport.md` |
| Test results | Audit-logging tests (R-1), regression run | `Testresultaten_overzicht.md`, `R-1_auditlogging_bewijs.md` |
| CI/CD & coverage | Pipeline controls (SAST/SCA/SBOM), coverage gate | `CICD.md`, `CODE_COVERAGE.md` (branch `code_coverage`) |

---

## 3. How we prioritize (explicit criteria)

We rank improvements on **four criteria**, applied in this order. This is the explicit
basis for every position in the backlog in §5.

1. **Impact (risk class).** Taken straight from the risk matrix: each finding has a code
   (likelihood digit 1-5 x impact letter A-E) and a class **Critical / High / Medium /
   Low**. The decision rules from the risk criteria are hard:
   - **Critical** -> not acceptable; must be fixed **before any production deploy**.
   - **High** -> mandatory fix within an agreed term.
   - **Medium** -> fix where reasonable, otherwise consciously accept (owner).
   - **Low** -> acceptable; monitor.
2. **Exploitability (measured, not assumed).** The penetration test re-tested the high
   risks against a running OpenMRS (Docker, MariaDB). A finding that is **CONFIRMED**
   exploitable keeps or raises its priority; a finding that is **NOT EXPLOITABLE** in
   practice is **downgraded to accept**, even if its theoretical matrix score was high.
   This is where measurement overrides the initial estimate (see §4.2).
3. **Effort.** From the backlog: **Small (< 1 day) / Medium (2-3 days) / Large (~1 week)**.
   Within the same impact band we do the **low-effort items first** (quick wins) so that
   the most risk is removed per hour spent.
4. **Compliance driver.** Each improvement maps to a concrete **NEN 7510-2:2024 /
   ISO 27002:2022** control and, where relevant, to GDPR art. 9 / 33 / 34. A finding that
   touches **health data or IAM/secrets** may never silently remain open.

**Quick-win rule (impact x effort).** An improvement that is **High or Critical impact**
*and* **Small effort** is a quick win and goes to the front of its band. Three items
qualify: **SR-7, SR-12 and SR-17** - each is a one-annotation / single-dependency change
that removes a Critical or High risk.

```
Effort ->        Small (<1d)          Medium (2-3d)         Large (~1w)
Impact v
Critical        SR-7, SR-12   --    SR-10
                (do first!)
High            SR-17         --    SR-1
Medium          SR-3, SR-16,        SR-15
                SR-18, SR-11
Low             SR-9, SR-13
```

---

## 4. Status of the improvement landscape

Before the ranked backlog, the findings split into three groups. This matters for
prioritization: we should not spend effort on what is already done or on what the pentest
proved is not a real problem.

### 4.1 Already implemented and verified (done)

| Improvement | Finding | Evidence (measurement) |
|---|---|---|
| **Audit logging on all state-changing endpoints** (SR-5) | **R-1** Incomplete audit logging (C3, Medium) | Implemented and proven by **31 automated tests, all green** (`Testresultaten_overzicht.md`). Red/green proof: the controller test **fails (6/6)** on the pre-fix code and **passes** after the fix. A **live pentest** (curl against the running container) shows real `DENIED`/`FAILED` audit lines with `user`, `uuid`, `when`, `ip` (`R-1_auditlogging_bewijs.md`). Maps to NEN 7510-2:2024 **8.15 Logging**. |

> This is the one improvement from the Medium band we pulled forward and completed, because
> it was directly testable and is an explicit NEN 7510 requirement for an EPD system. It is
> listed here so it is **not** double-counted in §5.

### 4.2 Accepted as residual risk (measured: not exploitable)

These had a high or medium matrix score, but the penetration test demonstrated they are
**not exploitable** in the current code. Per criterion 2, they are **accepted and
monitored** rather than fixed - this is itself a prioritization decision backed by
measurement.

| Finding | Matrix score | Pentest result | Decision |
|---|:---:|---|---|
| **T-1** Mass assignment | D4 (Critical, theoretical) | PT-2: `uuid`/`voided` fields **ignored** by server -> NOT EXPLOITABLE | Accept; monitor when adding new resources |
| **E-1** Privilege escalation via `/user` | C4 (High) | PT-11: all 5 attempts returned HTTP 403 / correct rejection -> NOT EXPLOITABLE | Accept; monitor `/user/self` fields on updates |
| **T-2** SQL injection | C3 (Medium) | PT-4: Hibernate uses parameterized queries, no DB error -> NOT EXPLOITABLE | Accept; monitor new search handlers |
| **D-1** Unbounded result sets | C3 (Medium) | PT-6: `maxResultsAbsolute=100` enforced -> NOT EXPLOITABLE | Accept; rate-limiting optional for prod |
| **E-2** XML content-type bypass | D2 (Medium) | PT-8: `ContentTypeFilter` blocks all XML variants (HTTP 415) -> NOT EXPLOITABLE | Effectively already mitigated (SR-8) |

### 4.3 Open improvements - prioritized in §5

Everything else: the confirmed-exploitable findings and the confirmed-vulnerable
dependencies. These are the actual work, ranked next.

---

## 5. Prioritized improvement backlog

Ranked by the criteria in §3: **impact class first, then measured exploitability, then
effort (quick wins first)**. The "Why this rank" column is the justification and ties each
item to its analysis and its measurement.

### P0 - Critical (must be fixed before production deploy)

| # | ID | Improvement | Score - Class | Effort | NEN 7510-2:2024 | Why this rank (analysis + measurement) |
|:--:|---|---|:---:|:---:|---|---|
| 1 | **SR-7** | Add `@Authorized('Manage RESTWS')` to the settings page (`SettingsFormController.searchProperties()`) | **E4 - Critical** | **Small** | 8.3, 5.15, 8.24 | **Quick win.** PT-7 code review confirmed the endpoint has **no auth check** and returns global-property **values incl. secrets/API-keys**, and sits under `/module/*` *outside* the `AuthorizationFilter` (TB-4). Catastrophic impact, one-annotation fix -> highest ROI of the whole backlog. |
| 2 | **SR-12** | Upgrade `swagger-core 1.6.2 -> 1.6.12+` (pulls in SnakeYAML >= 2.0) | **D4 - Critical** | **Small** | 8.8 | **Quick win.** PT-10 (SCA) confirmed transitive **SnakeYAML < 2.0**, **CVE-2022-1471 CVSS 9.8** (deserialization RCE). Single dependency bump removes a 9.8 -> do immediately. |
| 3 | **SR-10** | Upgrade `Apache Tomcat Jasper 6.0.53 -> 9.0.x` (keep `provided` scope) | **D4 - Critical** | Medium | 8.8 | PT-10 (SCA) confirmed an **EOL** component with **three CVSS 9.8 CVEs** (CVE-2020-9484, CVE-2020-1938 Ghostcat, CVE-2019-0232). Higher effort than SR-12 so it follows it, but still mandatory pre-production. |

### P1 - High

| # | ID | Improvement | Score - Class | Effort | NEN 7510-2:2024 | Why this rank (analysis + measurement) |
|:--:|---|---|:---:|:---:|---|---|
| 4 | **SR-17** | Add `@Authorized` to `/cleardbcache` (`ClearDbCacheController2_0`) | **C4 - High** | **Small** | 5.15, 8.6 | **Quick win.** PT-14 confirmed HTTP **204 for an anonymous caller** - anyone can flush the full Hibernate cache (DoS). Root cause is the missing annotation + silently-failing filter (TB-5/TB-6). One line, high impact. |
| 5 | **SR-1** | Enforce HTTPS/TLS for all REST endpoints (+ HSTS) | **D4 - High** | Medium | 8.24, 8.5 | PT-1 confirmed Basic-Auth credentials are readable as Base64 over HTTP (`admin:Admin123` decodable on the wire). Real risk on a hospital network. Effort is Medium (config + deploy), so it follows the High quick win. |

### P2 - Medium

| # | ID | Improvement | Score - Class | Effort | NEN 7510-2:2024 | Why this rank (analysis + measurement) |
|:--:|---|---|:---:|:---:|---|---|
| 6 | **SR-3** | Set `Secure` + `SameSite=Strict` on the session cookie | **C3 - Medium** | **Small** | 8.5, 8.24 | PT-3 confirmed `HttpOnly` present but **`Secure` and `SameSite` missing**. Combined with S-1 this enables session hijacking. Small effort -> first of the Medium band. |
| 7 | **SR-16** | Put `/session/diag` behind authentication | **B3 - Medium** | **Small** | 8.3 | PT-13 confirmed `serverTime` returned **without auth** and the `token` param is **ignored** (sham security). Small, low-impact recon leak. |
| 8 | **SR-18** | Put `swagger.json` behind authentication | **C3 - Medium** | **Small** | 8.3 | PT-15 code review confirmed no auth on `/module/*` (TB-6); in production it exposes the full API map. Small fix. |
| 9 | **SR-11** | Upgrade `jackson-dataformat-yaml 2.13.3 -> 2.15.0+` | **C3 - Medium** | **Small** | 8.8 | PT-10 (SCA) confirmed CVE-2022-42003/42004 (CVSS 7.5). **Test scope only**, so no production risk, but it keeps the CI dependency scan clean. |
| 10 | **SR-15** | Rate-limiting + lockout on the password-reset endpoint | **C3 - Medium** | Medium | 8.5, 5.17 | PT-12 confirmed **no HTTP 429 and no delay** after repeated attempts (`PasswordResetController2_2`) - activation keys can be enumerated. Matches gap-analysis 3. Medium effort, so last of the Medium band. |

### P3 - Low

| # | ID | Improvement | Score - Class | Effort | NEN 7510-2:2024 | Why this rank (analysis + measurement) |
|:--:|---|---|:---:|:---:|---|---|
| 11 | **SR-9** | Disable stack traces in production (`enableStackTraceDetails=false`) | **B3 - Low** | **Small** | 8.28 | PT-9 confirmed internal class names leak in the response body. Config change, low impact. |
| 12 | **SR-13** | Upgrade `commons-codec 1.14 -> 1.17+` | **B2 - Low** | **Small** | 8.8 | PT-10: outdated, no active CVEs; housekeeping. |

---

## 6. Recommended sequencing (roadmap)

This follows directly from §5 and from the **cost estimate** in the risk assessment report
(indicative: ~144 h / ~€11,520 total at €80/h; the critical fixes are ~56 h). With limited
time, the order is what matters, not finishing everything.

- **Sprint 1 - "no-deploy blockers" (~ the critical 56 h, but front-loaded with quick wins).**
  Start with the two Critical **quick wins** SR-7 and SR-12 (both Small) and the High quick
  win SR-17 (Small) - together they remove a catastrophic secrets leak, a CVSS 9.8, and an
  anonymous DoS in roughly a day. Then SR-10 (Tomcat) and SR-1 (HTTPS).
- **Sprint 2 - Medium band, quick wins first.** SR-3, SR-16, SR-18, SR-11 (all Small),
  then SR-15 (Medium).
- **Backlog - Low band.** SR-9, SR-13 when capacity allows.
- **Monitor (no active work).** The accepted items in §4.2 (T-1, E-1, T-2, D-1, E-2):
  re-test when new resources or search handlers are added.

This respects the hard decision rule from the risk criteria: nothing in the **Critical**
class is deployed to production unmitigated, and nothing in **High/Critical** stays open
without a recorded acceptance.

---

## 7. Maintainability and testability improvements

Beyond the security backlog, the test work surfaced a few **engineering** improvements,
each documented as an honest caveat in our own reports. They are smaller but concrete.

| # | Improvement | Justification (source) | Effort |
|:--:|---|---|:---:|
| M1 | Set the dedicated **audit logger to `INFO` in production** so successful (not only `DENIED`/`FAILED`) actions are captured live | The container defaults to `WARN`, so SUCCESS lines are not yet persisted in production (`R-1_auditlogging_bewijs.md` §4.6) | Small |
| M2 | Wire the **integration tests** (`SessionIT`) into CI behind a profile/stage | They currently require a running server and are **not run automatically** (`Testresultaten_overzicht.md` §3) | Medium |
| M3 | Quarantine / stabilize the flaky **`ClearDbCacheController2_0Test`** (Hibernate cache timing) | Confirmed flaky **on the original OpenMRS code too**, independent of our change (`Testresultaten_overzicht.md` §2.2) | Small |
| M4 | Keep raising the **coverage gate** as coverage grows (property already in `pom.xml`) | Current combined coverage **82.8%** (omod **86.6%**) sits above the **80%** gate; raise to 85% when structurally higher (`CODE_COVERAGE.md`, branch `code_coverage`) | Small |

---

## 8. Measurement baseline (so improvements stay verifiable)

To keep the improvements **measurable**, this is the current measured baseline. After each
improvement, the same measurement should be re-run to prove the risk dropped.

| Metric | Current measured value | Source | Target after improvement |
|---|---|---|---|
| Automated tests passing | **1,910** run; our **31** audit tests deterministically green; 1 pre-existing flaky OpenMRS test | `Testresultaten_overzicht.md` | No regression; flaky test quarantined (M3) |
| Combined code coverage | **82.8%** (omod 86.6%), gate at 80% | `CODE_COVERAGE.md` (branch `code_coverage`) | >= 80%, raise to 85% (M4) |
| Critical dependency CVEs | **2 x CVSS 9.8** (Tomcat Jasper, SnakeYAML) + 1 x 7.5 (jackson-yaml, test) | PT-10 / Grype SCA | 0 critical after SR-10/SR-12; clean scan after SR-11 |
| Confirmed-exploitable findings | **7 open** (I-2/I-4, S-1, D-4, S-2, D-3, I-5, I-6) + dependency CVEs | Pentest §4 | Driven to 0 via P0-P2 |
| Audit-logging coverage of state-changing endpoints | **100%** of REST CRUD endpoints (top-level + sub-resource controller) | `Testresultaten_overzicht.md` | Maintained; SUCCESS captured live (M1) |

---

## 9. Summary

- Improvements are prioritized on **four explicit criteria** - impact class, measured
  exploitability, effort, and NEN/GDPR compliance - applied in that order, with a
  **quick-win rule** (high impact x small effort first).
- The order is **grounded in measurement**: the penetration test promoted code-confirmed
  findings (I-2/I-4, D-4) and **demoted** theoretically-high but non-exploitable ones
  (T-1, E-1, T-2, D-1, E-2) to *accept*. The dependency scan set the two CVSS-9.8 items as
  hard P0 blockers.
- **R-1 audit logging is already done and proven by 31 green tests** plus a live pentest,
  so it is removed from the open backlog.
- The result is a short, defensible roadmap: **SR-7, SR-12, SR-17 first** (a day of work
  removing the worst risk), then the remaining Critical/High, then Medium and Low, with the
  accepted residual risks under monitoring.

---

## 10. Reference index

| Document | Branch |
|---|---|
| `threat-model.md` | this branch |
| `Security_Backlog_Pentest_Rapport.md` | this branch |
| `R-1_auditlogging_bewijs.md` | this branch |
| `Testresultaten_overzicht.md` | this branch |
| `risk-matrix.md`, `risk-evaluation.md` | this branch |
| `CICD.md` | this branch |
| `risk-criteria.md`, `risk-assessment-report.md` | `docs_risk_criteria_assessment` |
| `01-gap-analyse.md`, `Gap-analyse-logging.md` | `docs_auditreport_gapanalysis` |
| `BIV-risicoanalyse.md` | `analyze-project` |
| `CODE_COVERAGE.md` | `code_coverage` |
</content>
