# Traceability Matrix - OpenMRS REST Webservices Module

> **Wat is dit bestand?**
> Een centrale traceerbaarheidsmatrix die per **NEN-7510-2:2024**-beheersmaatregel laat zien:
> welk risico hij afdekt, welke maatregel we hebben genomen, in welk concreet artefact
> (code, configuratie, test, CI-workflow of scan) dat is geimplementeerd, hoe het is geverifieerd
> en in welk document het is onderbouwd. De matrix verzint niets: elke bewijscel verwijst naar
> een bestaand, traceerbaar artefact in deze repository.
>
> Het bindt de losse documenten van dit project samen tot een keten:
> NEN-control, risico/eis, maatregel, implementatie-artefact, verificatie-artefact, document.
>
> **Let op:** artefacten worden alleen met hun bestandsnaam genoemd (geen mappaden), zodat deze
> matrix blijft kloppen als de mapstructuur van de repository wijzigt.

| | |
|---|---|
| **Module** | OpenMRS REST Webservices Module v3.2.0 (`/ws/rest/v1/...`) |
| **Datum** | 2026-06-17 |
| **Branch** | `Traceability-matrix` |
| **Repository** | LU2webservice/openmrs-module-webservices ([github.com/LU2webservice/openmrs-module-webservices](https://github.com/LU2webservice/openmrs-module-webservices)) |
| **Bewijs vastgelegd op** | commit `fc94a94` (merge van PR #37 `secure-backlog-fix`). De GitHub-permalinks in sectie 6 zijn vastgepind op de commit waarin elke maatregel landde, inclusief regelnummer; ze blijven dus geldig, ook als de mapstructuur later wijzigt. |
| **Norm** | NEN-7510-2:2024 (afgeleid van ISO/IEC 27002:2022); ondersteunend: ISO/IEC 25010, AVG art. 9/33/34 |
| **Scope** | REST API-laag, authenticatie, autorisatie, logging, dependencies en de CI/CD-pijplijn |
| **Voldoet aan eis** | Ja, minimaal 3 NEN-7510:2024-controls (hier 11); en elk bewijs is een traceerbaar artefact (zie sectie 6 artefactregister) |

---

## 1. Doel en leeswijzer

Deze matrix beantwoordt de auditvraag: *"Toon aan dat elke beveiligingseis herleidbaar is van norm
tot bewijs."* Dat doen we met een vaste traceerketen:

```
NEN-7510-2:2024 control
  |
  v
Risico / eis  (STRIDE-ID uit het threat model, SR-ID uit de security backlog)
  |
  v
Maatregel  (wat we doen om het risico te beheersen)
  |
  v
Implementatie-artefact  (CODE / CONFIG, concreet bestand)
  |
  v
Verificatie-artefact  (TEST / CI / SCAN, bewijs dat de maatregel werkt)
  |
  v
Document-artefact  (de analyse of onderbouwing)
```

Lees de hoofdmatrix (sectie 3) als overzicht. De uitgewerkte ketens (sectie 4) laten de volledige
herleidbaarheid end-to-end zien voor de belangrijkste controls. De omgekeerde tracering (sectie 5)
gaat van risico naar bewijs. Het artefactregister (sectie 6) somt elk genoemd artefact op.

---

## 2. Artefacttypen en legenda

Elk bewijs is van een van deze traceerbare artefacttypen:

| Type | Betekenis | Voorbeeld |
|---|---|---|
| **CODE** | Java-broncode in de module | `AuditLog.java` |
| **CONFIG** | Configuratie of build | `config.xml`, `pom.xml` |
| **TEST** | Geautomatiseerde test (bewijs door uitvoering) | `AuditLogTest.java` |
| **CI** | Pipeline-workflow (continue verificatie) | `codeql.yml` |
| **SCAN/SBOM** | Scan- of inventarisatie-output | `sbom.cdx.json` |
| **DOC** | Analyse of onderbouwing | `threat-model.md` |

**Statuslegenda:**

| Status | Betekenis |
|---|---|
| **Geimplementeerd** | Maatregel zit in code/config en is geverifieerd |
| **Aangetoond** | Bewezen via test of (pen)test, los van een code-fix |
| **Continu** | Permanent bewaakt door de CI/CD-pijplijn |
| **Open** | Risico bekend, bewust uitgesteld met onderbouwing |
| **Geaccepteerd** | Restrisico; gemeten niet exploiteerbaar, wordt bewaakt |

---

## 3. Hoofdmatrix - NEN-7510-2:2024 naar traceerbaar bewijs

> Elke rij is een traceerketen. Artefacten staan met bestandsnaam (geen mappaden).

| # | NEN-7510-2:2024 | Eis (kort) | Risico / eis-ID | Maatregel | Implementatie-artefact (CODE/CONFIG) | Verificatie-artefact (TEST/CI/SCAN) | Document-artefact (DOC) | Status |
|:--:|---|---|:--:|---|---|---|---|---|
| **1** | **8.15 Logging** | Gebruikersactiviteiten, fouten en beveiligingsgebeurtenissen vastleggen | R-1 / SR-5 | Audit-regel (wie/wat/wanneer/IP plus outcome) op alle state-changing endpoints; geen gevoelige data in log; log-injectie (CWE-117) afgevangen | `AuditLog.java`, `MainResourceController.java`, `MainSubResourceController.java` | `AuditLogTest.java`, `MainResourceControllerAuditTest.java`, `MainSubResourceControllerAuditTest.java` (31 tests, groen) plus live pentest | `r-1-auditlogging-bewijs.md`, `gap-analyse-logging.md` | Geimplementeerd, aangetoond |
| **2** | **8.25 Beveiligen tijdens de ontwikkelcyclus** | Attack surface mapping in de designfase; secure defaults | (alle entry points) | Volledige inventarisatie van entry points met privilege-, inputvalidatie- en autorisatie-status per ingang | `AuthorizationFilter.java`, `ContentTypeFilter.java`, `config.xml` | Code-analyse plus live curl-tests (PT-1 t/m PT-15) | `attack-surface.md`, `threat-model.md` | Aangetoond |
| **3** | **8.3 Toegangsbeperking informatie** | Toegang tot informatie en functies beperken | I-2/I-4, I-5, I-6 / SR-7, SR-16, SR-18 | `Context.requirePrivilege(...)` / `isAuthenticated()` op `settings.form`, `/session/diag` en `swagger.json`, geeft 401/403 voor onbevoegden | `SettingsFormController.java`, `SessionController1_9.java`, `SwaggerSpecificationController.java` | Pentest PT-7/PT-13/PT-15 (DAST plus SAST); `mvn clean package` BUILD SUCCESS | `remediatie-status.md`, `security-backlog-pentest-rapport.md` | Geimplementeerd |
| **4** | **8.5 Beveiligde authenticatie** | Veilige authenticatieprocedures; bescherming tegen brute-force | S-1, D-3 / SR-1, SR-15 | HSTS-header plus optioneel HTTPS afdwingen (`TransportSecurityFilter`); rate-limiting/lockout op wachtwoord-reset (gepland) | `TransportSecurityFilter.java`, `config.xml`; `PasswordResetController2_2.java` | Pentest PT-1 (S-1 bevestigd), PT-12 (D-3 bevestigd) | `remediatie-status.md`, `gap-analyse.md` (gap 2/3) | Geimplementeerd (S-1) / open (D-3) |
| **5** | **8.24 Gebruik van cryptografie** | Vertrouwelijkheid in transport; TLS | S-1 / SR-1 | TLS afdwingbaar plus HSTS; Basic Auth alleen over versleuteld kanaal | `TransportSecurityFilter.java`, `config.xml` (`webservices.rest.requireHttps`) | Pentest PT-1 (Base64 over HTTP aangetoond) | `threat-model.md` (S-1), `improvements.md` sectie 5 | Geimplementeerd |
| **6** | **5.15 Toegangsbeveiliging (autorisatie)** | RBAC; least privilege op admin-functies | D-4, E-1 / SR-17, SR-14 | `@Authorized`/`requirePrivilege` op `/cleardbcache`; autorisatie op `/user` afgedwongen door Core | `ClearDbCacheController2_0.java` | Pentest PT-14 (D-4 opgelost: 403), PT-11 (E-1 niet exploiteerbaar) | `remediatie-status.md`, `attack-surface.md` sectie 5 | Geimplementeerd / geaccepteerd (E-1) |
| **7** | **8.8 Beheer van technische kwetsbaarheden** | Bekende kwetsbaarheden tijdig vinden en verhelpen | DEP-1/2/3 / SR-10, SR-11, SR-12, SR-13 | Kwetsbare deps geupgraded (Tomcat Jasper 9.0.106, swagger-core 1.6.12 met SnakeYAML 2.2, jackson-yaml 2.19.1, commons-codec 1.17.1); wekelijkse SCA | `pom.xml` (root / omod / omod-common) | `sca.yml` (Grype, wekelijks), Dependabot; `dependency:tree`-verificatie | `security-backlog-pentest-rapport.md` (PT-10), `remediatie-status.md`, `cicd.md` | Geimplementeerd, continu |
| **8** | **8.9 Configuratiebeheer** | Overzicht van softwarecomponenten (SBOM) | (supply chain) | Automatische SBOM bij elke wijziging; bewaard als artefact | `sbom.yml` | `sbom.cdx.json` (CycloneDX), CI-artefact (90 dagen) | `pipeline-compliance.md`, `cicd.md` | Geimplementeerd, continu |
| **9** | **8.28 Veilig coderen** | Veilige codeerstandaarden; review; code-kwaliteit | (kwaliteit) | Branch protection plus verplichte review plus MFA; meetbare onderhoudbaarheid (PMD/CPD/JaCoCo); stack traces uit in productie | `ci.yml`, `pom.xml` (coverage-gate 80%) | JaCoCo 82,8% (gate), PMD 0 kritieke smells; `deploy.yml` (review/MFA) | `onderhoudbaarheid-analyse.md`, `code-coverage.md`, `pipeline-compliance.md` | Geimplementeerd, continu |
| **10** | **8.29 Beveiligingstesten in ontwikkeling/acceptatie** | SAST en (pen)tests voor release | (alle) | CodeQL SAST (`security-and-quality`-set) bij elke PR plus wekelijks; STRIDE-pentest met 15 testcases | `codeql.yml` | CodeQL Code scanning alerts; pentest PT-1 t/m PT-15 | `security-backlog-pentest-rapport.md`, `cicd.md` | Geimplementeerd, continu, aangetoond |
| **11** | **8.31 Scheiding ontwikkel-, test- en productieomgevingen** | OTAP-scheiding; scheiding ICT-beheer en medische data | P-1 / gap 1 | Gefaseerde OTAP-deploy (dev, test, prod) met wachttijd plus handmatige goedkeuring; rolscheiding aanbevolen | `deploy.yml` | Branch protection (geen self-approve), required reviewers op `prod` | `cicd.md`, `risk-evaluation.md` (P-1), `gap-analyse.md` (gap 1) | Geimplementeerd / open (rolscheiding) |

---

## 4. Uitgewerkte traceerketens (verplichte controls in detail)

Hieronder vijf controls volledig uitgeschreven van norm tot bewijs. De eerste drie zijn de
kern-controls; ze tonen aan dat de keten sluit en dat elk schakelpunt een bestaand artefact is.

### 4.1 Keten - NEN-7510-2:2024 8.15 Logging (risico R-1)

| Schakel | Artefact / inhoud | Type |
|---|---|---|
| **Norm** | NEN-7510-2:2024 8.15, gebruikersactiviteiten, uitzonderingen en beveiligingsgebeurtenissen loggen | NORM |
| **Risico** | R-1 Incomplete auditlogging (repudiation), klasse C3 Middel | DOC: `threat-model.md` sectie Repudiation |
| **Eis** | SR-5: "Na DELETE staat entry in auditlog met user, UUID, timestamp en IP" | DOC: `security-backlog-pentest-rapport.md` sectie 1.2 |
| **Gap** | `PATCH/POST /user`, `GET /systemsetting`, `GET /session` werden niet gelogd | DOC: `gap-analyse-logging.md` |
| **Maatregel** | Een auditregel per CREATE/UPDATE/DELETE/PURGE/PUT met `action/resource/uuid/outcome/when/user/ip`; alleen metadata (nooit wachtwoord); `sanitize()` tegen log-injectie | - |
| **Implementatie** | `AuditLog.java` | CODE |
| **Implementatie** | `MainResourceController.java` plus `MainSubResourceController.java` | CODE |
| **Verificatie** | `AuditLogTest.java` (8), `MainResourceControllerAuditTest.java` (9), `MainSubResourceControllerAuditTest.java` (14), samen 31 tests groen | TEST |
| **Verificatie** | Rood/groen-bewijs (fix teruggedraaid, 6/6 faalt) plus live curl-pentest met echte DENIED/FAILED-regels | TEST: `r-1-auditlogging-bewijs.md` sectie 4.4/4.6 |
| **Verificatie** | CodeQL CWE-117-melding opgelost (log forging) | CI: `codeql.yml` |
| **Bewijsdocument** | Volledige onderbouwing plus voldaan-tabel | DOC: `r-1-auditlogging-bewijs.md`, `testresultaten-overzicht.md` |
| **Status** | Geimplementeerd, aangetoond | - |

### 4.2 Keten - NEN-7510-2:2024 8.3 Toegangsbeperking (risico I-2/I-4, secrets-lek)

| Schakel | Artefact / inhoud | Type |
|---|---|---|
| **Norm** | NEN-7510-2:2024 8.3, toegang tot informatie beperken tot bevoegden | NORM |
| **Risico** | I-4 Secrets-lek via `settings.form/search`, geeft global-property-waarden (DB-wachtwoorden, API-keys) ongeauthenticeerd terug; valt buiten `AuthorizationFilter`. Klasse E4/E5 KRITIEK | DOC: `threat-model.md` sectie I-4, `attack-surface.md` sectie 3 |
| **Eis** | SR-7: `showForm()`/`searchProperties()` geeft HTTP 403 voor laag-privilege gebruiker | DOC: `security-backlog-pentest-rapport.md` |
| **Maatregel** | `Context.requirePrivilege(MANAGE_GLOBAL_PROPERTIES)` op beide methoden, 403 zonder rechten | - |
| **Implementatie** | `SettingsFormController.java` | CODE |
| **Verificatie** | PT-7 (code review plus DAST); `mvn clean package` BUILD SUCCESS | TEST/DOC: `remediatie-status.md` sectie 1 (SR-7) |
| **Open testwerk** | Dedicated 401/403-regressietest voor laag-privilege gebruiker nog toe te voegen | DOC: `remediatie-status.md` sectie 1 (eerlijke kanttekening) |
| **Status** | Geimplementeerd | - |

### 4.3 Keten - NEN-7510-2:2024 8.8 Beheer van technische kwetsbaarheden (DEP-1/DEP-2)

| Schakel | Artefact / inhoud | Type |
|---|---|---|
| **Norm** | NEN-7510-2:2024 8.8, informatie over technische kwetsbaarheden tijdig verkrijgen en verhelpen | NORM |
| **Risico** | DEP-1 Tomcat Jasper 6.0.53 (EOL, 3x CVSS 9.8) en DEP-2 swagger-core 1.6.2 met SnakeYAML kleiner dan 2.0 (CVE-2022-1471, CVSS 9.8). Klasse D4 KRITIEK | DOC: `security-backlog-pentest-rapport.md` PT-10 |
| **Eis** | SR-10 / SR-12: geen CVSS 9.8-bevindingen meer in de scan; SnakeYAML groter of gelijk aan 2.0 in dependency-tree | DOC: backlog sectie 1.2 |
| **Maatregel** | `tomcat-jasper:9.0.106` (provided), `swagger-core:1.6.12` (trekt SnakeYAML 2.2), `jackson-yaml 2.19.1`, `commons-codec 1.17.1` | - |
| **Implementatie** | `pom.xml` (root / omod / omod-common) | CONFIG |
| **Verificatie** | `dependency:tree` bevestigt SnakeYAML 2.2 plus tomcat-jasper 9.0.106; `mvn clean package` BUILD SUCCESS | DOC: `remediatie-status.md` sectie 1 |
| **Verificatie (continu)** | Grype SCA wekelijks plus bij elke PR; Dependency Review blokkeert nieuwe high-CVE deps | CI: `sca.yml`, `ci.yml` |
| **Inventarisatie** | CycloneDX-SBOM voor "welk onderdeel zit er in onze build?" | SCAN/SBOM: `sbom.cdx.json`, `sbom.yml` |
| **Kanttekening** | Major upgrades horen voor productie nog een volledige regressietest (swagger-generatie, JSP op Tomcat 9) | DOC: `remediatie-status.md` sectie 4 |
| **Status** | Geimplementeerd, continu | - |

### 4.4 Keten - NEN-7510-2:2024 8.29 Beveiligingstesten (SAST plus pentest)

| Schakel | Artefact / inhoud | Type |
|---|---|---|
| **Norm** | NEN-7510-2:2024 8.29, beveiligingstesten tijdens ontwikkeling en acceptatie | NORM |
| **Maatregel** | CodeQL SAST (`security-and-quality`-set) bij elke push/PR en wekelijks; blokkeert merge. Aangevuld met een handmatige STRIDE-pentest | - |
| **Implementatie** | `codeql.yml` (verplichte check `Analyze (java)`) | CI |
| **Verificatie** | Code scanning alerts in de Security-tab; concrete vangst: CWE-117 log-injectie gevonden en opgelost | CI/TEST: `r-1-auditlogging-bewijs.md` sectie 4.5 |
| **Verificatie** | 15 pentest-cases (PT-1 t/m PT-15) met expliciet oplos/accepteer-besluit | DOC: `security-backlog-pentest-rapport.md` sectie 2 t/m 4 |
| **Status** | Geimplementeerd, continu, aangetoond | - |

### 4.5 Keten - NEN-7510-2:2024 8.25 Attack surface mapping (designfase)

| Schakel | Artefact / inhoud | Type |
|---|---|---|
| **Norm** | NEN-7510-2:2024 8.25, beveiligen tijdens de ontwikkelcyclus; secure defaults | NORM |
| **Maatregel** | Alle entry points in kaart met vereiste privilege / inputvalidatie aanwezig? / autorisatiecheck aanwezig?; zone-indeling op filterketen | - |
| **Implementatie/onderbouwing** | Filterketen: `AuthorizationFilter.java`, `ContentTypeFilter.java`, `config.xml` (url-patterns) | CODE/CONFIG |
| **Verificatie** | Per entry point getoetst via curl (Zone A/B/C) en code review | DOC: `attack-surface.md` sectie 2 t/m 5 |
| **Gap-conclusie** | Grootste gaps: ontbrekende auth-checks op `/module/*` (geen secure default) plus geen eigen inputvalidatielaag (TB-3), backlog SR-7/17/18 | DOC: `attack-surface.md` sectie 2.2 |
| **Status** | Aangetoond | - |

---

## 5. Omgekeerde tracering - risico naar maatregel naar bewijs naar status

Van elk geidentificeerd risico naar het bewijsartefact. STRIDE-ID's komen uit het threat model
(`threat-model.md`); SR-ID's uit de security backlog (`security-backlog-pentest-rapport.md`).

| Risico-ID | Omschrijving | Klasse | NEN | Maatregel / besluit | Bewijsartefact | Status |
|:--:|---|:--:|:--:|---|---|---|
| **I-2/I-4** | Secrets-lek `settings.form` | KRITIEK | 8.3 | `requirePrivilege` toegevoegd (SR-7) | `SettingsFormController.java`; PT-7 | Geimplementeerd |
| **DEP-1** | Tomcat Jasper EOL, 3x 9.8 | KRITIEK | 8.8 | Upgrade 9.0.106 (SR-10) | `pom.xml`; `dependency:tree` | Geimplementeerd |
| **DEP-2** | swagger-core met SnakeYAML kleiner dan 2.0, 9.8 | KRITIEK | 8.8 | Upgrade 1.6.12 met SnakeYAML 2.2 (SR-12) | `pom.xml`; `sca.yml` | Geimplementeerd |
| **S-1** | Basic Auth over HTTP | HOOG | 8.24/8.5 | `TransportSecurityFilter` (HSTS plus HTTPS) (SR-1) | `TransportSecurityFilter.java`; PT-1 | Geimplementeerd |
| **D-4** | `/cleardbcache` zonder auth (DoS) | HOOG | 5.15 | `requirePrivilege(SQL_LEVEL_ACCESS)` (SR-17) | `ClearDbCacheController2_0.java`; PT-14 | Geimplementeerd |
| **R-1** | Incomplete auditlogging | MIDDEL | 8.15 | `AuditLog` plus 31 tests (SR-5) | `AuditLog.java`; `AuditLogTest.java` e.a. | Geimplementeerd, aangetoond |
| **I-5** | `/session/diag` lekt serverTime | MIDDEL | 8.3 | `isAuthenticated()`-check (SR-16) | `SessionController1_9.java`; PT-13 | Geimplementeerd |
| **I-6** | `swagger.json` publiek | MIDDEL | 8.3 | `isAuthenticated()`-check (SR-18) | `SwaggerSpecificationController.java`; PT-15 | Geimplementeerd |
| **DEP-3** | jackson-yaml test-scope CVE | MIDDEL | 8.8 | Upgrade 2.19.1 (SR-11) | `pom.xml`; `sca.yml` | Geimplementeerd |
| **D-3** | Brute-force wachtwoord-reset | MIDDEL | 8.5/5.17 | Rate-limiting/lockout, gepland (SR-15) | `PasswordResetController2_2.java`; PT-12 | Open |
| **S-2** | Sessiecookie mist Secure/SameSite | MIDDEL | 8.5/8.24 | Container-config (SR-3) | PT-3 | Open |
| **I-1** | Stack traces in response | LAAG | 8.28 | `enableStackTraceDetails=false`, config (SR-9) | PT-9 | Open |
| **T-1** | Mass assignment | (D4 naar) LAAG | 8.28 | Niet exploiteerbaar (Core-whitelist) | PT-2 | Geaccepteerd |
| **E-1** | Privilege-escalatie `/user` | (C4 naar) LAAG | 5.15 | Niet exploiteerbaar (Core `@Authorized`) | PT-11 | Geaccepteerd |
| **T-2** | SQL/HQL-injectie | (C3 naar) LAAG | 8.28 | Niet exploiteerbaar (named params) | PT-4 | Geaccepteerd |
| **D-1** | Onbeperkte resultaatsets | (C3 naar) LAAG | 8.6 | `maxResultsAbsolute=100` afgedwongen | PT-6 | Geaccepteerd |
| **E-2** | XML content-type bypass | (D2 naar) LAAG | 8.26 | `ContentTypeFilter` blokkeert XML | PT-8; `ContentTypeFilterTest.java` | Geaccepteerd |
| **P-1** | Unauthorized deploy naar prod | HOOG | 8.31 | OTAP plus review plus handmatige prod-approval | `deploy.yml`; branch protection | Geimplementeerd |
| **P-2/P-3** | Foute code / onveilige deps in pipeline | HOOG | 8.29/8.8 | CodeQL plus Grype plus Dependency Review | `codeql.yml`, `sca.yml`, `ci.yml` | Geimplementeerd, continu |

---

## 6. Bewijsregister - artefacten met commit, datum en GitHub-permalink

Dit is het harde bewijs. Per artefact: de **exacte regel** waar de maatregel staat (klikbare
GitHub-permalink), de **commit-SHA** waarin het landde, de **datum** en de **pull request**. De
permalinks zijn vastgepind op een commit, dus ze blijven werken ook na een herinrichting van de
mapstructuur. De linktekst gebruikt alleen de bestandsnaam plus regelnummer; het volledige pad zit
in de onderliggende URL.

Repository: https://github.com/LU2webservice/openmrs-module-webservices

### 6.1 CODE - broncode (klik = exacte regel op GitHub)

| Artefact en regel (GitHub-bewijs) | Wat er op die regel staat / bewijst | Commit, datum, PR |
|---|---|---|
| [AuditLog.java L108-L190](https://github.com/LU2webservice/openmrs-module-webservices/blob/cd24b51/omod-common/src/main/java/org/openmrs/module/webservices/rest/web/audit/AuditLog.java#L108) | `record(...)` schrijft de auditregel (L108); `formatMessage()` bouwt `AUDIT action=... outcome=... user=... ip=...` (L135); `sanitize()` verwijdert CR/LF tegen log-injectie/CWE-117 (L190) | `cd24b51`, 2026-06-15, [PR #36](https://github.com/LU2webservice/openmrs-module-webservices/pull/36) |
| [MainResourceController.java L212](https://github.com/LU2webservice/openmrs-module-webservices/blob/63297ca/omod-common/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainResourceController.java#L212) | `auditFailure(...)` (L212) plus `AuditLog.record(...)` op CREATE/UPDATE/DELETE/PURGE (L100/L152/L175/L200) | `63297ca`, 2026-06-15, [PR #36](https://github.com/LU2webservice/openmrs-module-webservices/pull/36) |
| [MainSubResourceController.java L312](https://github.com/LU2webservice/openmrs-module-webservices/blob/08e3dc4/omod-common/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainSubResourceController.java#L312) | `auditFailure(...)` (L312) op alle 7 sub-resource-acties (L133..L300) | `08e3dc4`, 2026-06-16, [PR #36](https://github.com/LU2webservice/openmrs-module-webservices/pull/36) |
| [SettingsFormController.java L42, L60](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SettingsFormController.java#L42) | `Context.requirePrivilege(MANAGE_GLOBAL_PROPERTIES)` op `showForm()` (L42) en `searchProperties()` (L60), geeft HTTP 403 | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [SessionController1_9.java L177](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/openmrs1_9/SessionController1_9.java#L177) | `if (!Context.isAuthenticated()) throw new APIAuthenticationException(...)` op `/session/diag` (L177-L178), 403 voor anoniem | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [SwaggerSpecificationController.java L36](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SwaggerSpecificationController.java#L36) | `if (!Context.isAuthenticated())` op `swagger.json` (L36), 401 voor anoniem | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [ClearDbCacheController2_0.java L57](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/openmrs2_0/ClearDbCacheController2_0.java#L57) | `Context.requirePrivilege(SQL_LEVEL_ACCESS)` (L57), 403 zonder rechten | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [TransportSecurityFilter.java L49-L56](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/TransportSecurityFilter.java#L49) | `webservices.rest.requireHttps`-property (L49), `Strict-Transport-Security` HSTS-header (L51), respecteert `X-Forwarded-Proto` (L56) | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [AuthorizationFilter.java L34](https://github.com/LU2webservice/openmrs-module-webservices/blob/de346b5/omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java#L34) | Documenteert trust boundary TB-6: "will not fail on invalid or missing credentials" (silent fail) | `de346b5`, 2026-06-02 |
| [ContentTypeFilter.java](https://github.com/LU2webservice/openmrs-module-webservices/blob/de346b5/omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/ContentTypeFilter.java) | Blokkeert XML-content-types (E-2 / 8.26) | `de346b5`, 2026-06-02 |
| [PasswordResetController2_2.java](https://github.com/LU2webservice/openmrs-module-webservices/blob/de346b5/omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/openmrs2_2/PasswordResetController2_2.java) | Wachtwoord-reset-endpoint; D-3 rate-limiting nog open (SR-15) | `de346b5`, 2026-06-02 |

### 6.2 CONFIG - configuratie en build (klik = exacte regel op GitHub)

| Artefact en regel (GitHub-bewijs) | Wat er op die regel staat / bewijst | Commit, datum, PR |
|---|---|---|
| [pom.xml L51, L53, L152](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/pom.xml#L51) | `apacheTomcatVersion=9.0.106` (L51, SR-10), `jacksonVersion=2.19.1` (L53, SR-11), `commons-codec 1.17.1` (L152, SR-13); coverage-gate `0.80` (L59, 8.28) | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [omod-common/pom.xml L20-L42](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod-common/pom.xml#L20) | `tomcat-jasper` op `${apacheTomcatVersion}` (L20-L21, SR-10), `swagger-core 1.6.12` (L42, SR-12, trekt SnakeYAML 2.2) | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [omod/pom.xml L33-L37](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod/pom.xml#L33) | `jackson-dataformat-yaml` uitgelijnd op 2.19.1, test-scope (SR-11 / DEP-3) | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |
| [config.xml L77, L97](https://github.com/LU2webservice/openmrs-module-webservices/blob/c9f1ad7/omod/src/main/resources/config.xml#L77) | Registratie (L77-L90) en mapping op `/ws/rest/*` (L97-L112) van `TransportSecurityFilter`, `ContentTypeFilter`, `AuthorizationFilter` | `c9f1ad7`, 2026-06-17, [PR #37](https://github.com/LU2webservice/openmrs-module-webservices/pull/37) |

> De drie `pom.xml`-bestanden hebben dezelfde naam; in de linktekst zijn ze onderscheiden met een
> korte module-aanduiding. Het volledige pad zit in de onderliggende, op een commit vastgepinde URL.

### 6.3 TEST - verificatie door uitvoering (klik = testbestand op GitHub)

| Artefact (GitHub-bewijs) | Bewijst | Commit, datum, PR |
|---|---|---|
| [AuditLogTest.java](https://github.com/LU2webservice/openmrs-module-webservices/blob/cd24b51/omod-common/src/test/java/org/openmrs/module/webservices/rest/web/audit/AuditLogTest.java) | 8 tests op het log-hulpje (o.a. geen wachtwoord in log, CWE-117) | `cd24b51`, 2026-06-15, [PR #36](https://github.com/LU2webservice/openmrs-module-webservices/pull/36) |
| [MainResourceControllerAuditTest.java](https://github.com/LU2webservice/openmrs-module-webservices/blob/c2bd169/omod-common/src/test/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainResourceControllerAuditTest.java) | 9 tests top-level controller (SUCCESS/DENIED/FAILED) | `c2bd169`, 2026-06-16, [PR #36](https://github.com/LU2webservice/openmrs-module-webservices/pull/36) |
| [MainSubResourceControllerAuditTest.java](https://github.com/LU2webservice/openmrs-module-webservices/blob/08e3dc4/omod-common/src/test/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainSubResourceControllerAuditTest.java) | 14 tests sub-resource controller | `08e3dc4`, 2026-06-16, [PR #36](https://github.com/LU2webservice/openmrs-module-webservices/pull/36) |
| [ContentTypeFilterTest.java](https://github.com/LU2webservice/openmrs-module-webservices/blob/de346b5/omod-common/src/test/java/org/openmrs/module/webservices/rest/web/filter/ContentTypeFilterTest.java) | XML-blokkade (E-2 / 8.26) | `de346b5`, 2026-06-02 |

### 6.4 CI - pipeline-workflows (klik = workflow op GitHub; uitvoeringen onder Actions)

| Artefact (GitHub-bewijs) | Bewijst | Commit, datum |
|---|---|---|
| [ci.yml](https://github.com/LU2webservice/openmrs-module-webservices/blob/842d4e3/.github/workflows/ci.yml) | build plus tests plus Dependency Review (8.28/8.8) | `842d4e3`, 2026-06-12 |
| [codeql.yml](https://github.com/LU2webservice/openmrs-module-webservices/blob/73c1e35/.github/workflows/codeql.yml) | CodeQL SAST (8.29) | `73c1e35`, 2026-06-06 |
| [sbom.yml](https://github.com/LU2webservice/openmrs-module-webservices/blob/73c1e35/.github/workflows/sbom.yml) | SBOM-generatie met Syft (8.9) | `73c1e35`, 2026-06-06 |
| [sca.yml](https://github.com/LU2webservice/openmrs-module-webservices/blob/1e4a371/.github/workflows/sca.yml) | dependency-scan Grype (8.8) | `1e4a371`, 2026-06-08 |
| [deploy.yml](https://github.com/LU2webservice/openmrs-module-webservices/blob/513374f/.github/workflows/deploy.yml) | OTAP-scheiding plus prod-approval (8.31) | `513374f`, 2026-06-08 |

> Bewijs van uitvoering (groene runs, CodeQL- en Grype-alerts): de tabbladen **Actions** en
> **Security** van de repository: https://github.com/LU2webservice/openmrs-module-webservices/actions

### 6.5 SCAN/SBOM - scan-output

| Artefact (GitHub-bewijs) | Bewijst | Commit, datum, PR |
|---|---|---|
| [sbom.cdx.json](https://github.com/LU2webservice/openmrs-module-webservices/blob/899d52e/sbom.cdx.json) | CycloneDX-componentinventaris (8.9) | `899d52e`, 2026-06-04, [PR #41](https://github.com/LU2webservice/openmrs-module-webservices/pull/41) |

### 6.6 DOC - onderbouwende documenten

| Artefact | Onderwerp |
|---|---|
| `threat-model.md` | Threat model (STRIDE plus C4) |
| `attack-surface.md` | Attack surface mapping |
| `security-backlog-pentest-rapport.md` | Security backlog plus pentest |
| `remediatie-status.md` | Remediatie-status |
| `r-1-auditlogging-bewijs.md` | R-1 auditlogging bewijs |
| `biv-risicoanalyse.md` | BIV/CIA-risicoanalyse |
| `risk-matrix.md`, `risk-evaluation.md`, `risk-bowtie.md` | Risicomatrix / -evaluatie / bow-tie |
| `gap-analyse.md`, `gap-analyse-logging.md` | Gap-analyses |
| `pipeline-compliance.md` | Compliance-matrix (pipeline) |
| `improvements.md` | Verbeterplan (prioritering) |
| `onderhoudbaarheid-analyse.md` | Onderhoudbaarheid-analyse |
| `testresultaten-overzicht.md` | Testresultaten |
| `cicd.md` | CI/CD-uitleg |
| `code-coverage.md` | Code coverage |

> Documentatie is via pull requests in `main` gekomen, o.a.: threat model + backlog + pentest
> [PR #33](https://github.com/LU2webservice/openmrs-module-webservices/pull/33), gap-analyses
> [PR #40](https://github.com/LU2webservice/openmrs-module-webservices/pull/40), compliance
> [PR #41](https://github.com/LU2webservice/openmrs-module-webservices/pull/41), onderhoudbaarheid
> [PR #38](https://github.com/LU2webservice/openmrs-module-webservices/pull/38), risicoanalyse
> [PR #39](https://github.com/LU2webservice/openmrs-module-webservices/pull/39), code coverage
> [PR #32](https://github.com/LU2webservice/openmrs-module-webservices/pull/32).

### 6.7 Bewijs zelf verifieren (reproduceerbaar)

Een beoordelaar kan elk bewijs zelf nalopen:

```bash
# 1. Bekijk een maatregel exact zoals hij op de genoemde commit stond
git show c9f1ad7:omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SettingsFormController.java

# 2. Draai de 31 audit-tests (bewijs voor 8.15 / R-1)
mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest,MainSubResourceControllerAuditTest

# 3. Build plus de coverage-gate van 80% (bewijs voor 8.28)
mvn clean verify

# 4. Bevestig dat de dependency-fixes resolven (bewijs voor 8.8)
mvn -o dependency:tree | grep -E "snakeyaml|tomcat|swagger-core|jackson-dataformat-yaml|commons-codec"
```

De GitHub-permalinks openen rechtstreeks de geciteerde regel op de juiste commit; de tabbladen
Actions en Security tonen de uitvoering van de pipelines en de scan-resultaten.

---

## 7. Dekkingsverantwoording (toetsing aan de eis)

| Eis | Voldaan? | Bewijs in dit document |
|---|---|---|
| **Ten minste 3 NEN-7510:2024 controls** | Ja, 11 controls | Sectie 3 rijen 1 t/m 11: 8.3, 8.5, 8.8, 8.9, 8.15, 8.24, 8.25, 8.28, 8.29, 8.31 en 5.15 |
| **Elk bewijs een traceerbaar artefact** | Ja | Elke bewijscel verwijst naar een concreet bestand of workflow; alle artefacten staan op naam in het bewijsregister (sectie 6) |
| **Verifieerbaar bewijs (commit, datum, GitHub)** | Ja | Sectie 6 geeft per artefact de commit-SHA, de datum, de pull request en een GitHub-permalink naar de exacte regel; reproduceerbaar via sectie 6.7 |
| **Tweezijdige traceerbaarheid** | Ja | Voorwaarts: norm naar bewijs (sectie 3, 4). Achterwaarts: risico naar bewijs (sectie 5) |
| **Herleidbaar naar bron-analyse** | Ja | Elke keten verwijst naar het onderbouwende DOC-artefact (threat model, pentest, gap-analyse) |
| **Bestandsnaam in plaats van paden** | Ja | Artefacten worden alleen met bestandsnaam genoemd, zodat de matrix blijft kloppen na herinrichting van de mapstructuur |

> **Samenvatting:** deze matrix sluit de keten van NEN-7510-2:2024-control, risico, maatregel,
> code/config, test/CI/scan, document voor elf beheersmaatregelen. Geen enkele cel verwijst naar
> iets dat niet bestaat: alle artefacten zijn op bestandsnaam opgenomen in sectie 6.
