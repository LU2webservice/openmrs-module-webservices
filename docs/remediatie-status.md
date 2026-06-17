# Remediatie-status — security backlog

> **Wat is dit bestand?**
> Een overzicht van welke backlog-bedreigingen zijn **opgelost** (en hoe), en welke **bewust
> niet** zijn opgelost (en waarom). Het sluit aan op de [security backlog](security-backlog-pentest-rapport.md)
> (SR-1 t/m SR-18) en het [verbeterplan](improvements.md).

| | |
|---|---|
| **Datum** | 2026-06-17 |
| **Branch** | `secure-backlog-fix` |
| **Verificatie** | `mvn clean package` → **BUILD SUCCESS** (alle wijzigingen compileren en packagen) |

---

## 1. Opgelost in deze ronde (hoe)

| ID | Dreiging | Klasse | Hoe opgelost | Bestand |
|---|---|:---:|---|---|
| **SR-7** | I-2/I-4 — `settings.form` lekt secrets ongeauthenticeerd | **KRITIEK** | `Context.requirePrivilege(MANAGE_GLOBAL_PROPERTIES)` op `showForm()` én `searchProperties()` → HTTP 403 zonder rechten | `SettingsFormController.java` |
| **SR-16** | I-5 — `/session/diag` lekt `serverTime` zonder auth | MIDDEL | `if (!Context.isAuthenticated()) throw APIAuthenticationException` → HTTP 403 voor anoniem | `SessionController1_9.java` |
| **SR-17** | D-4 — `/cleardbcache` zonder autorisatie (DoS) | HOOG | `Context.requirePrivilege(SQL_LEVEL_ACCESS)` → HTTP 403 zonder rechten | `ClearDbCacheController2_0.java` |
| **SR-18** | I-6 — `swagger.json` publiek (API-kaart lekt) | MIDDEL | `Context.isAuthenticated()`-check → HTTP 401 voor anoniem | `SwaggerSpecificationController.java` |
| **SR-11** | DEP-3 — `jackson-dataformat-yaml` 2.13.3 (CVE-2022-42003/4) | MIDDEL | Versie uitgelijnd op `${jacksonVersion}` = **2.19.1** (test-scope) | `omod/pom.xml` |
| **SR-13** | Verouderde `commons-codec` 1.14 | LAAG | Bump naar **1.17.1** | `pom.xml` |
| **SR-12** | DEP-2 — swagger-core 1.6.2 → SnakeYAML <2.0 (CVE-2022-1471, CVSS 9.8) | **KRITIEK** | `swagger-core` → **1.6.12**; trekt nu **SnakeYAML 2.2** mee (geverifieerd via `dependency:tree`). Binnen 1.6.x-lijn, dus `io.swagger.models`-API ongewijzigd | `omod-common/pom.xml` |
| **SR-10** | DEP-1 — Tomcat Jasper 6.0.53 (EOL, 3× CVSS 9.8) | **KRITIEK** | Artifact `jasper` → **`tomcat-jasper:9.0.106`** (artifactId wijzigt vanaf Tomcat 8.5+); `provided` scope behouden. Geverifieerd via `dependency:tree` | `omod-common/pom.xml`, `pom.xml` |
| **SR-1** | S-1 — Basic Auth leesbaar over HTTP (geen TLS) | HOOG | Nieuwe `TransportSecurityFilter`: stuurt **HSTS-header** op beveiligde requests en kan **HTTPS afdwingen** via global property `webservices.rest.requireHttps` (standaard uit; respecteert `X-Forwarded-Proto`) | `TransportSecurityFilter.java`, `config.xml` |

**Patroon van de code-fixes:** alle vier de endpoint-fixes gebruiken de OpenMRS-idiomatische
autorisatiecontrole (`Context.requirePrivilege(...)` / `isAuthenticated()`), zoals
`LoggedInUsersController` dat al deed. Geen nieuwe frameworks, minimale wijzigingen.

### Verificatie
- **Compilatie/packaging:** volledige reactor `mvn clean package` → BUILD SUCCESS.
- **Regressie:** de bestaande `ClearDbCacheController2_0Test` (draait als super-user) blijft
  groen op de autorisatie; de enige failure is de al bekende, pre-existing flaky L2-cache-test
  (zie `testresultaten-overzicht.md` §2.2), niet de fix.
- **Nog open testwerk:** dedicated regressietests die 401/403 voor een *laag-privilege* gebruiker
  hard aantonen (de "rood-dan-groen"-stap) zijn nog niet toegevoegd.

---

## 2. Al eerder afgedekt (niet opnieuw gedaan)

| ID | Dreiging | Status | Bewijs |
|---|---|---|---|
| **SR-5** | R-1 — Incomplete auditlogging | ✅ Al gebouwd (team) | `AuditLog` + 31 tests, zie `r-1-auditlogging-bewijs.md` |
| **SR-8** | E-2 — XML content-type bypass | ✅ Al gemitigeerd | `ContentTypeFilter` blokkeert XML; PT-8 niet exploiteerbaar |
| **SR-4** | T-2 — SQL-injectie via search | ✅ Niet exploiteerbaar | Hibernate gebruikt named parameters; PT-4 |

---

## 3. Bewust niet opgelost (met reden)

| ID | Dreiging | Klasse | Waarom niet (nu) opgelost |
|---|---|:---:|---|
| **SR-2** | T-1 — Mass-assignment whitelist | HOOG | **Grote refactor over 144+ resources, hoog regressierisico.** Pentest PT-2 toonde dat `uuid`/`voided` al genegeerd worden (nu niet exploiteerbaar). Beter als geplande hardening mét tests. |
| **SR-3** | S-2 — Secure/SameSite sessiecookie | MIDDEL | **De `JSESSIONID`-cookie wordt door de servletcontainer/OpenMRS-core gezet,** niet door deze module. Configuratie in container/`web.xml`, niet in module-code. |
| **SR-6** | D-1 — Rate limiting | MIDDEL | **Vereist een API-gateway/infra-laag.** Pentest PT-6 toonde dat `maxResultsAbsolute=100` al wordt afgedwongen, dus DoS via resultaatsets is beperkt. |
| **SR-9** | I-1 — Stack traces uit in productie | LAAG | **Runtime-configuratie** (`enableStackTraceDetails=false` global property), een deploy-instelling — geen code-wijziging in de module. |
| **SR-15** | D-3 — Rate-limiting/lockout wachtwoord-reset | MIDDEL | **Vereist een nieuw throttle-mechanisme** (middel effort); beter als eigen, geteste feature. |
| **SR-14** | E-1 — Autorisatietests `/user` uitbreiden | HOOG | **Dit is testwerk, geen fix.** E-1 is al bewezen niet exploiteerbaar (PT-11, alle pogingen HTTP 403). Voegt testdekking toe, geen kwetsbaarheidsfix. |

---

## 4. Samenvatting

| Categorie | Aantal | Items |
|---|:---:|---|
| **Opgelost** | 9 | SR-1, SR-7, SR-10, SR-11, SR-12, SR-13, SR-16, SR-17, SR-18 |
| Al afgedekt | 3 | SR-5, SR-8, SR-4 |
| Bewust niet opgelost | 6 | SR-2, SR-3, SR-6, SR-9, SR-14, SR-15 |

**Rode draad in de "niet opgelost"-keuzes:** het zijn ofwel **infrastructuur/config** (SR-3, SR-6,
SR-9), **groot refactorwerk dat nu niet exploiteerbaar is** (SR-2), een **nieuw te bouwen feature**
(SR-15) of **testwerk i.p.v. een fix** (SR-14).

**Belangrijke kanttekening bij de twee KRITIEKE dependency-fixes (SR-10/SR-12):** ze compileren,
packagen en resolven correct (`dependency:tree` bevestigt SnakeYAML 2.2 en tomcat-jasper 9.0.106),
maar een **major dependency-upgrade hoort vóór productie een volledige regressietest te krijgen**
(swagger-spec-generatie voor SR-12; JSP-rendering op een Tomcat 9-deploymentserver voor SR-10).
De build is groen, maar dat vervangt geen draai-test op een echte server.

**Aanbevolen vervolg:** regressietests voor de negen opgeloste items (vooral de 401/403-paden en
de swagger-generatie) → daarna SR-2/SR-15 als geplande hardening.
