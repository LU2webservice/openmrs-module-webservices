# Verbeteringen - prioritering en onderbouwing

> **Wat is dit bestand?**
> Dit is het **verbeterplan** voor de OpenMRS REST Webservices module. Het bevat geen
> nieuwe bevindingen. In plaats daarvan **brengt het alles samen** wat we eerder in het
> project hebben geanalyseerd en getest - het threat model, de risicocriteria en -matrix,
> de gap-analyses, de security backlog, de penetratietest, de dependency-scans en de
> testresultaten - en maakt daar **een enkele, geprioriteerde lijst met verbeteringen** van,
> met een expliciete onderbouwing van de volgorde.
>
> Elke verbetering is herleidbaar naar (a) een gedocumenteerde analyse en (b) een meting
> (pentestresultaat, CVSS uit de dependency-scan of een testrun). Niets hier is verzonnen.

| | |
|---|---|
| **Module** | OpenMRS REST Webservices Module v3.2.0 (`/ws/rest/v1/...`) |
| **Datum** | 2026-06-16 (prioritering) · **bijgewerkt 2026-06-17** (remediatie-stand) |
| **Branch** | `code-tests-logging` (auditlogging) · `secure-backlog-fix` (PR #37, security-backlog-fixes) |
| **Status** | **10 van de 18 backlog-items zijn inmiddels opgelost en geverifieerd** (R-1 auditlogging + de 9 fixes uit PR #37, incl. beide CVSS-9.8-deps). Een **live hertest op 17-06** bevestigde de fixes. De resterende items zijn ofwel bewust uitgesteld (met onderbouwing) ofwel infrastructuur/config. Zie §4.1 en de Status-kolom in §5. |

---

## 1. Doel en scope

De module is de centrale REST-ingang naar alle klinische data in OpenMRS. Omdat het om
**bijzondere persoonsgegevens** gaat (AVG art. 9) hanteren we een **lage risicobereidheid**,
zoals vastgelegd in de risicocriteria. Dit document beantwoordt één vraag:

> *Gegeven alles wat we hebben gevonden en gemeten: welke verbeteringen voeren we door, in
> welke volgorde, en waarom?*

Het vormt de brug tussen **analyse** (wat is er mis en hoe erg) en **actie** (wat lossen we
eerst op). Het bouwt direct voort op de andere documenten en herhaalt de onderliggende
analyse niet - het verwijst ernaar.

---

## 2. Gebruikte bronnen (herleidbaarheid)

Elke verbetering hieronder is terug te koppelen naar deze bronnen.

| Bron | Wat het oplevert | Document |
|---|---|---|
| Threat model (STRIDE + C4) | Dreigingen, trust boundaries (TB-1...TB-7), beveiligingsdoelen (BG-1...BG-6) | `threat-model.md` |
| Risicocriteria en risicoregister | BIV/CIA-schaal, kans x impact-score, risicoklassen, beslisregels | `risk-criteria.md`, `risk-matrix.md`, `risk-evaluation.md` |
| Risk assessment report | Mitigatie per bevinding, koppeling NEN-7510:2024, **kostenraming** | `risk-assessment-report.md` |
| BIV/CIA-risicoanalyse | Kroonjuwelen, toegepast risicoregister | `biv-risicoanalyse.md` |
| Gap-analyses | Huidige versus gewenste situatie, NEN 7510-koppeling | `gap-analyse.md`, `gap-analyse-logging.md` |
| Security backlog + pentestrapport | Geprioriteerde requirements (SR-1...SR-18) met **effort**, en pentestbevindingen (PT-1...PT-15) met **gemeten exploiteerbaarheid** en het oplos/accepteer-besluit | `security-backlog-pentest-rapport.md` |
| **Remediatie-status** | Wat is **opgelost** (hoe + bestand) en wat **bewust niet** (met reden); de stand per 17-06 | `remediatie-status.md` |
| **Live hertest 17-06** | Voor/na-bewijs (curl) dat de fixes het kwetsbare gedrag wegnemen (D-4, I-5, deps, R-1) | `pentest-hertest-17-06.md` |
| Testresultaten | Auditlogging-tests (R-1), regressierun, testtypen-overzicht | `testresultaten-overzicht.md`, `r-1-auditlogging-bewijs.md` |
| **Onderhoudbaarheid** | ISO 25010-metrieken (PMD/CPD/JaCoCo), complexiteits-hotspots, refactor-acties O1–O6 | `onderhoudbaarheid-analyse.md` |
| **Traceability-matrix** | Per NEN-7510:2024-control: risico → maatregel → code/config → test/CI/scan → document, met GitHub-permalinks | `traceability-matrix.md` |
| CI/CD en coverage | Pipeline-controles (SAST/SCA/SBOM), coverage-gate | `cicd.md`, `code-coverage.md` |

---

## 3. Hoe we prioriteren (expliciete criteria)

We rangschikken verbeteringen op **vier criteria**, in deze volgorde toegepast. Dit is de
expliciete basis voor elke positie in de backlog in §5.

1. **Impact (risicoklasse).** Rechtstreeks uit de risicomatrix: elke bevinding heeft een
   code (kans-cijfer 1-5 x impact-letter A-E) en een klasse **Kritiek / Hoog / Middel /
   Laag**. De beslisregels uit de risicocriteria zijn hard:
   - **Kritiek** -> niet acceptabel; oplossen **vóór elke productie-deploy**.
   - **Hoog** -> oplossen verplicht binnen een afgesproken termijn.
   - **Middel** -> oplossen waar redelijk, anders bewust accepteren (eigenaar).
   - **Laag** -> acceptabel; in de gaten houden.
2. **Exploiteerbaarheid (gemeten, niet aangenomen).** De penetratietest heeft de hoogste
   risico's opnieuw getest tegen een draaiende OpenMRS (Docker, MariaDB). Een bevinding die
   **BEVESTIGD** exploiteerbaar is, behoudt of verhoogt zijn prioriteit; een bevinding die
   in de praktijk **NIET EXPLOITEERBAAR** is, wordt **teruggebracht naar accepteren**, ook
   als de theoretische matrixscore hoog was. Hier overrulet de meting de eerste inschatting
   (zie §4.2).
3. **Effort.** Uit de backlog: **Klein (< 1 dag) / Middel (2-3 dagen) / Groot (~1 week)**.
   Binnen dezelfde impactklasse pakken we de **laagste-effort items eerst** (quick wins),
   zodat per bestede uur het meeste risico verdwijnt.
4. **Compliance-driver.** Elke verbetering koppelt aan een concrete maatregel uit
   **NEN-7510:2024 / ISO 27002:2022** en, waar relevant, aan AVG art. 9 / 33 / 34. Een
   bevinding die **gezondheidsgegevens of IAM/secrets** raakt, mag nooit stilletjes open
   blijven staan.

**Quick-win-regel (impact x effort).** Een verbetering die **Hoge of Kritieke impact** heeft
*en* **Kleine effort** is een quick win en gaat vooraan in zijn klasse. Drie items voldoen
hieraan: **SR-7, SR-12 en SR-17** - elk een wijziging van één annotatie of één dependency die
een Kritiek of Hoog risico wegneemt.

```
Effort ->       Klein (<1d)          Middel (2-3d)         Groot (~1w)
Impact v
Kritiek         SR-7, SR-12          SR-10
                (eerst doen!)
Hoog            SR-17                SR-1
Middel          SR-3, SR-16,         SR-15
                SR-18, SR-11
Laag            SR-9, SR-13
```

---

## 4. Stand van zaken van het verbeterlandschap

Vóór de geprioriteerde backlog vallen de bevindingen in drie groepen uiteen. Dit is van
belang voor de prioritering: we moeten geen effort steken in wat al af is of in wat de
pentest als geen echt probleem heeft aangetoond.

### 4.1 Al gebouwd en geverifieerd (af)

Sinds de eerste prioritering (16-06) is er in **twee leverrondes** gebouwd: eerst de auditlogging
(R-1, PR #36, branch `code-tests-logging`), daarna de **security-backlog-fixes** (PR #37, branch
`secure-backlog-fix`, 17-06). Samen zijn **10 backlog-items** opgelost en geverifieerd. Ze staan
hier zodat ze in §5 **niet** dubbel als "te doen" worden geteld; de volledige hoe-en-bestand-tabel
staat in [`remediatie-status.md`](remediatie-status.md), de voor/na-meting in
[`pentest-hertest-17-06.md`](pentest-hertest-17-06.md).

| ID | Verbetering | Klasse | Bewijs (meting) | Bron |
|---|---|:---:|---|---|
| **SR-5** | Auditlogging op alle state-changing endpoints (R-1) | Middel | **31 geautomatiseerde tests, allemaal groen**. Rood/groen-bewijs: de controllertest **faalt (6/6)** op de oude code en **slaagt** na de fix. Live hertest 17-06: echte `DENIED`/`FAILED`-regels met `user`/`uuid`/`when`/`ip`. NEN 8.15. | `testresultaten-overzicht.md`, `r-1-auditlogging-bewijs.md` |
| **SR-7** | `requirePrivilege` op `settings.form` (secrets-lek I-2/I-4) | **Kritiek** | `Context.requirePrivilege(MANAGE_GLOBAL_PROPERTIES)` op `showForm()`+`searchProperties()` → 403 zonder rechten; `mvn clean package` BUILD SUCCESS. NEN 8.3. | `remediatie-status.md` |
| **SR-12** | swagger-core 1.6.2 → 1.6.12 (SnakeYAML <2.0, CVSS 9.8) | **Kritiek** | `dependency:tree` bevestigt nu **SnakeYAML 2.2/2.4**; CVE-2022-1471 weg. NEN 8.8. | `remediatie-status.md`, `pentest-hertest-17-06.md` §5 |
| **SR-10** | Tomcat Jasper 6.0.53 → 9.0.106 (EOL, 3× CVSS 9.8) | **Kritiek** | `tomcat-jasper:9.0.106` (provided); `dependency:tree` bevestigt. NEN 8.8. | `remediatie-status.md`, `pentest-hertest-17-06.md` §5 |
| **SR-17** | `requirePrivilege(SQL_LEVEL_ACCESS)` op `/cleardbcache` (DoS, D-4) | Hoog | Hertest 17-06: van `HTTP 204` (cache gewist) → **geweigerd** ("SQL Level Access"). NEN 5.15. | `pentest-hertest-17-06.md` §2, `remediatie-status.md` |
| **SR-1** | TLS/HSTS afdwingbaar (`TransportSecurityFilter`) (S-1) | Hoog | Nieuwe filter: HSTS-header + optioneel HTTPS afdwingen via `webservices.rest.requireHttps`. NEN 8.24/8.5. | `remediatie-status.md` |
| **SR-16** | `/session/diag` achter authenticatie (I-5) | Middel | Hertest 17-06: van `HTTP 200` + `serverTime` → **`HTTP 401`**. NEN 8.3. | `pentest-hertest-17-06.md` §2, `remediatie-status.md` |
| **SR-18** | `swagger.json` achter authenticatie (I-6) | Middel | `isAuthenticated()`-check → 401 voor anoniem. NEN 8.3. | `remediatie-status.md` |
| **SR-11** | jackson-dataformat-yaml → 2.19.1 (CVE-2022-42003/4, test-scope) | Middel | Uitgelijnd op `${jacksonVersion}`; houdt de SCA-scan schoon. NEN 8.8. | `remediatie-status.md` |
| **SR-13** | commons-codec 1.14 → 1.17.1 | Laag | Bump; opschoning. NEN 8.8. | `remediatie-status.md` |

> **Eerlijke kanttekening (uit `remediatie-status.md` §1/§4).** De endpoint-fixes zijn op build-niveau
> geverifieerd (`mvn clean package` + live hertest), maar **dedicated regressietests die 401/403 voor
> een *laag-privilege* gebruiker hard aantonen** (de "rood-dan-groen"-stap zoals bij R-1) zijn nog niet
> toegevoegd — dat is open testwerk. En de twee **kritieke dependency-upgrades** (SR-10/SR-12) horen
> vóór productie nog een volledige **regressietest** (swagger-spec-generatie; JSP-rendering op Tomcat 9):
> de build is groen, maar dat vervangt geen draai-test op een echte server.

### 4.2 Geaccepteerd als restrisico (gemeten: niet exploiteerbaar)

Deze hadden een hoge of middelhoge matrixscore, maar de penetratietest toonde aan dat ze in
de huidige code **niet exploiteerbaar** zijn. Volgens criterium 2 worden ze **geaccepteerd
en bewaakt** in plaats van opgelost - dat is zelf een prioriteringsbeslissing, onderbouwd
met een meting.

| Bevinding | Matrixscore | Pentestresultaat | Besluit |
|---|:---:|---|---|
| **T-1** Mass assignment | D4 (Kritiek, theoretisch) | PT-2: velden `uuid`/`voided` **genegeerd** door de server -> NIET EXPLOITEERBAAR | Accepteren; bewaken bij nieuwe resources |
| **E-1** Privilege-escalatie via `/user` | C4 (Hoog) | PT-11: alle 5 pogingen gaven HTTP 403 / correcte afwijzing -> NIET EXPLOITEERBAAR | Accepteren; bewaken van `/user/self`-velden bij updates |
| **T-2** SQL-injectie | C3 (Middel) | PT-4: Hibernate gebruikt geparametriseerde queries, geen DB-fout -> NIET EXPLOITEERBAAR | Accepteren; bewaken van nieuwe search handlers |
| **D-1** Onbeperkte resultaatsets | C3 (Middel) | PT-6: `maxResultsAbsolute=100` afgedwongen -> NIET EXPLOITEERBAAR | Accepteren; rate-limiting optioneel voor productie |
| **E-2** XML content-type bypass | D2 (Middel) | PT-8: `ContentTypeFilter` blokkeert alle XML-varianten (HTTP 415) -> NIET EXPLOITEERBAAR | Feitelijk al gemitigeerd (SR-8) |

### 4.3 Open verbeteringen - geprioriteerd in §5

Al het overige: de bevestigd-exploiteerbare bevindingen en de bevestigd-kwetsbare
dependencies. Dit is het echte werk en wordt hierna gerangschikt.

---

## 5. Geprioriteerde verbeterbacklog

Gerangschikt volgens de criteria in §3: **eerst impactklasse, dan gemeten
exploiteerbaarheid, dan effort (quick wins eerst)**. De kolom "Waarom deze rang" is de
onderbouwing en koppelt elk item aan zijn analyse en zijn meting.

> **Lees dit als de oorspronkelijke onderbouwing van de volgorde** (de prioritering zelf is
> onveranderd). De toegevoegde **Status**-kolom toont de stand **per 17-06** na PR #37: ✅ opgelost
> (zie §4.1 + `remediatie-status.md`), 🟡 open. De prioritering blijkt te kloppen: alle Kritieke en
> Hoge items zijn als eerste afgerond.

### P0 - Kritiek (oplossen vóór productie-deploy)

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) | Status |
|:--:|---|---|:---:|:---:|---|---|:---:|
| 1 | **SR-7** | `@Authorized('Manage RESTWS')` op de instellingenpagina (`SettingsFormController.searchProperties()`) | **E4 - Kritiek** | **Klein** | 8.3, 5.15, 8.24 | **Quick win.** PT-7 (code review) bevestigde dat het endpoint **geen auth-check** heeft en global-property-**waarden incl. secrets/API-keys** teruggeeft, en onder `/module/*` valt *buiten* de `AuthorizationFilter` (TB-4). Catastrofale impact, fix van één annotatie -> hoogste rendement van de hele backlog. | ✅ opgelost (PR #37) |
| 2 | **SR-12** | Upgrade `swagger-core 1.6.2 -> 1.6.12+` (trekt SnakeYAML >= 2.0 mee) | **D4 - Kritiek** | **Klein** | 8.8 | **Quick win.** PT-10 (SCA) bevestigde transitief **SnakeYAML < 2.0**, **CVE-2022-1471 CVSS 9.8** (deserialisatie-RCE). Eén dependency-bump haalt een 9.8 weg -> direct doen. | ✅ opgelost — SnakeYAML 2.2/2.4 (hertest §5) |
| 3 | **SR-10** | Upgrade `Apache Tomcat Jasper 6.0.53 -> 9.0.x` (`provided` scope behouden) | **D4 - Kritiek** | Middel | 8.8 | PT-10 (SCA) bevestigde een **EOL**-component met **drie CVSS 9.8 CVE's** (CVE-2020-9484, CVE-2020-1938 Ghostcat, CVE-2019-0232). Meer effort dan SR-12, dus erna, maar nog steeds verplicht vóór productie. | ✅ opgelost — tomcat-jasper 9.0.106 (regressietest aanbevolen, §4.1) |

### P1 - Hoog

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) | Status |
|:--:|---|---|:---:|:---:|---|---|:---:|
| 4 | **SR-17** | `@Authorized` toevoegen aan `/cleardbcache` (`ClearDbCacheController2_0`) | **C4 - Hoog** | **Klein** | 5.15, 8.6 | **Quick win.** PT-14 bevestigde HTTP **204 voor een anonieme aanroeper** - iedereen kan de volledige Hibernate-cache wissen (DoS). Oorzaak: ontbrekende annotatie + stil falend filter (TB-5/TB-6). Eén regel, hoge impact. | ✅ opgelost — hertest: nu geweigerd ("SQL Level Access") |
| 5 | **SR-1** | HTTPS/TLS afdwingen voor alle REST-endpoints (+ HSTS) | **D4 - Hoog** | Middel | 8.24, 8.5 | PT-1 bevestigde dat Basic-Auth-credentials als Base64 over HTTP leesbaar zijn (`admin:Admin123` te decoderen op de lijn). Reëel risico op een ziekenhuisnetwerk. Effort is Middel (config + deploy), dus na de Hoge quick win. | ✅ opgelost — `TransportSecurityFilter` (HSTS + optioneel HTTPS) |

### P2 - Middel

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) | Status |
|:--:|---|---|:---:|:---:|---|---|:---:|
| 6 | **SR-3** | `Secure` + `SameSite=Strict` op de sessiecookie | **C3 - Middel** | **Klein** | 8.5, 8.24 | PT-3 bevestigde `HttpOnly` aanwezig, maar **`Secure` en `SameSite` ontbreken**. Samen met S-1 maakt dit sessie-hijacking mogelijk. Kleine effort -> eerste van de Middel-klasse. | 🟡 open — **buiten module-scope**: de `JSESSIONID`-cookie wordt door de servletcontainer/OpenMRS-core gezet (container-/`web.xml`-config), niet door deze module (`remediatie-status.md` §3) |
| 7 | **SR-16** | `/session/diag` achter authenticatie plaatsen | **B3 - Middel** | **Klein** | 8.3 | PT-13 bevestigde dat `serverTime` **zonder auth** wordt teruggegeven en de `token`-param wordt **genegeerd** (schijnbeveiliging). Klein, lage-impact recon-lek. | ✅ opgelost — hertest: nu `HTTP 401` |
| 8 | **SR-18** | `swagger.json` achter authenticatie plaatsen | **C3 - Middel** | **Klein** | 8.3 | PT-15 (code review) bevestigde geen auth op `/module/*` (TB-6); in productie geeft dit de volledige API-kaart prijs. Kleine fix. | ✅ opgelost (PR #37) |
| 9 | **SR-11** | Upgrade `jackson-dataformat-yaml 2.13.3 -> 2.15.0+` | **C3 - Middel** | **Klein** | 8.8 | PT-10 (SCA) bevestigde CVE-2022-42003/42004 (CVSS 7.5). **Alleen test-scope**, dus geen productierisico, maar het houdt de CI-dependency-scan schoon. | ✅ opgelost — uitgelijnd op 2.19.1 |
| 10 | **SR-15** | Rate-limiting + lockout op het wachtwoord-reset-endpoint | **C3 - Middel** | Middel | 8.5, 5.17 | PT-12 bevestigde **geen HTTP 429 en geen vertraging** na herhaalde pogingen (`PasswordResetController2_2`) - activatiesleutels zijn te enumereren. Sluit aan op gap-analyse 3. Middel effort, dus laatste van de Middel-klasse. | 🟡 open — vereist een nieuw throttle-mechanisme; gepland als eigen, geteste feature |

### P3 - Laag

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) | Status |
|:--:|---|---|:---:|:---:|---|---|:---:|
| 11 | **SR-9** | Stack traces uitschakelen in productie (`enableStackTraceDetails=false`) | **B3 - Laag** | **Klein** | 8.28 | PT-9 bevestigde dat interne klassenamen in de response-body lekken. Configuratiewijziging, lage impact. | 🟡 open — **runtime/deploy-config** (global property), geen code-wijziging in de module |
| 12 | **SR-13** | Upgrade `commons-codec 1.14 -> 1.17+` | **B2 - Laag** | **Klein** | 8.8 | PT-10: verouderd, geen actieve CVE's; opschoning. | ✅ opgelost — 1.17.1 |

> **Twee backlog-items die geen *fix* maar een besluit/testwerk zijn** (daarom niet als rang
> hierboven, wel in `remediatie-status.md` §3): **SR-2** (mass-assignment whitelist, T-1) — grote
> refactor over 144+ resources met hoog regressierisico, terwijl PT-2 aantoonde dat `uuid`/`voided`
> al genegeerd worden (nu niet exploiteerbaar); en **SR-14** (autorisatietests `/user` uitbreiden,
> E-1) — testwerk, geen kwetsbaarheidsfix, want E-1 is al bewezen niet exploiteerbaar (PT-11). En
> **SR-6** (rate limiting, D-1) vereist een API-gateway/infralaag; PT-6 toonde dat
> `maxResultsAbsolute=100` al wordt afgedwongen.

---

## 6. Aanbevolen volgorde (roadmap)

Dit volgde direct uit §5 en uit de **kostenraming** in het risk assessment report
(indicatief: ~144 u / ~EUR 11.520 totaal bij EUR 80/u; de kritieke fixes zijn ~56 u). De
volgorde is **conform plan uitgevoerd**; hieronder per sprint de stand per 17-06.

- **Sprint 1 - "deploy-blokkers" — ✅ afgerond.**
  De twee Kritieke **quick wins** SR-7 en SR-12 (beide Klein) en de Hoge quick win SR-17
  (Klein) zijn gedaan — samen halen die een catastrofaal secrets-lek, een CVSS 9.8 en een
  anonieme DoS weg. Ook SR-10 (Tomcat 9.0.106) en SR-1 (TLS/HSTS-filter) zijn af. De live
  hertest 17-06 bevestigde de werking (`pentest-hertest-17-06.md`).
- **Sprint 2 - Middel-klasse — grotendeels afgerond.** SR-16, SR-18, SR-11 (Klein) ✅ gedaan.
  **SR-3 blijkt buiten module-scope** (container-/`web.xml`-config) en **SR-15** (Middel, rate-
  limiting reset) staat nog open als geplande feature.
- **Backlog - Laag-klasse.** SR-13 ✅ gedaan; **SR-9** open (runtime/deploy-config, geen code).
- **Open vervolg (aanbevolen volgorde).** (1) **Regressietests** voor de negen PR #37-fixes,
  vooral de 401/403-paden voor een laag-privilege gebruiker en de swagger-spec-generatie
  (de "rood-dan-groen"-stap, zie §4.1-kanttekening); (2) **SR-15** rate-limiting; (3) **SR-2**
  mass-assignment-whitelist als geplande hardening mét tests.
- **Bewaken (geen actief werk).** De geaccepteerde items in §4.2 (T-1, E-1, T-2, D-1, E-2):
  hertesten zodra er nieuwe resources of search handlers bij komen.

Dit respecteert de harde beslisregel uit de risicocriteria: niets in de klasse **Kritiek**
gaat ongemitigeerd naar productie, en niets in **Hoog/Kritiek** blijft open zonder
vastgelegde acceptatie — alle Kritieke en Hoge items zijn inmiddels opgelost.

---

## 7. Verbeteringen voor onderhoudbaarheid en testbaarheid

Naast de security backlog bracht het test- en analyse­werk een paar **engineering**-verbeteringen
aan het licht, elk gedocumenteerd als eerlijke kanttekening in onze eigen rapporten. Ze zijn
kleiner maar concreet, en niet één is een security-blocker.

**7a. Uit het testwerk (audit/CI):**

| # | Verbetering | Onderbouwing (bron) | Effort |
|:--:|---|---|:---:|
| M1 | Zet de aparte **audit-logger op `INFO` in productie** zodat ook geslaagde acties (niet alleen `DENIED`/`FAILED`) live worden vastgelegd | De container staat standaard op `WARN`, dus SUCCESS-regels worden nog niet in productie bewaard (`r-1-auditlogging-bewijs.md` §4.6, bevestigd in `pentest-hertest-17-06.md` §4) | Klein |
| M2 | Koppel de **integratietests** (`SessionIT`) achter een profiel/stage in de CI | Ze vereisen nu een draaiende server en worden **niet automatisch** gedraaid (`testresultaten-overzicht.md` §3) | Middel |
| M3 | Quarantaine / stabiliseer de flaky **`ClearDbCacheController2_0Test`** (Hibernate-cache-timing) | Bevestigd flaky **ook op de originele OpenMRS-code**, los van onze wijziging (`testresultaten-overzicht.md` §2.2) | Klein |
| M4 | Verhoog de **coverage-gate** mee als de dekking groeit (property staat al in `pom.xml`) | Huidige gecombineerde dekking **82,8%** (omod **86,6%**) ligt boven de **80%**-gate; verhoog naar 85% als het structureel hoger is (`code-coverage.md`) | Klein |
| M5 | Voeg **401/403-regressietests voor een laag-privilege gebruiker** toe op de PR #37-fixes (settings.form, cleardbcache, session/diag, swagger.json) | De fixes zijn nu op build-/hertest-niveau bewezen, maar de "rood-dan-groen"-unittest ontbreekt nog (`remediatie-status.md` §1) | Middel |

**7b. Uit de onderhoudbaarheid-analyse (ISO 25010, PMD/CPD/JaCoCo — `onderhoudbaarheid-analyse.md` §8):**

De codebase is **goed onderhoudbaar** (gem. cyclomatische complexiteit 2,05; 94% van de methoden
laag-complex; 0 kritieke PMD-smells; duplicatie ~0,9%). Het risico is **niet diffuus maar
geconcentreerd** in een handvol methoden, wat gericht refactoren goedkoop maakt:

| # | Actie | Aanleiding (gemeten metriek) | Effort |
|:--:|---|---|:---:|
| O1 | Refactor `ConversionUtil.convert()` — opsplitsen per doeltype | Hoogste complexiteit (CC **48**) én meeste smells (17) in één klasse | Middel |
| O2 | Vereenvoudig de `search()`-handlers — gedeelde basislogica extraheren | 5 van de top-8 complexiteits-hotspots zijn `search()`-methoden (CC 21–31) | Middel |
| O3 | Vervang de 16 `EmptyCatchBlock`'s door logging of een bewuste comment | Stil opgeslokte fouten schaden de analyseerbaarheid | Klein |
| O4 | Voeg `DrugSearchByMappingHandler` en `DrugsSearchByMappingHandler` samen | 41 regels duplicatie, **niet** versie-gerelateerd (de overige duplicatie is wél verklaarbaar) | Klein |
| O5 | Ruim cosmetische smells op (overbodige haakjes/modifiers/imports) | 87 PMD-meldingen → richting 0; houdt toekomstige scans schoon | Klein |

> O6 (coverage-gate naar 85%) valt samen met **M4** hierboven. De security-prioriteiten staan los
> van deze onderhoudbaarheidsacties: dit verbetert de **wijzigbaarheid/analyseerbaarheid**, niet de
> veiligheid.

---

## 8. Meetbaseline (zodat verbeteringen verifieerbaar blijven)

Om de verbeteringen **meetbaar** te houden, is dit de huidige gemeten baseline. Na elke
verbetering hoort dezelfde meting opnieuw te draaien om te bewijzen dat het risico daalde.

| Metriek | Baseline (16-06) | Bron | Stand na PR #37 (17-06) |
|---|---|---|---|
| Geslaagde geautomatiseerde tests | **1.910** gedraaid; onze **31** audit-tests deterministisch groen; 1 bestaande flaky OpenMRS-test | `testresultaten-overzicht.md` | Geen regressie; `mvn clean package` BUILD SUCCESS; flaky test nog in quarantaine (M3) |
| Gecombineerde code coverage | **82,8%** (omod 86,6%), gate op 80% | `code-coverage.md`, `onderhoudbaarheid-analyse.md` | Ongewijzigd; doel 85% (M4/O6) |
| Kritieke dependency-CVE's | **2 x CVSS 9.8** (Tomcat Jasper, SnakeYAML) + 1 x 7.5 (jackson-yaml, test) | PT-10 / Grype SCA | **0 kritiek** — SnakeYAML 2.2/2.4, tomcat-jasper 9.0.106, jackson-yaml 2.19.1 (`pentest-hertest-17-06.md` §5) |
| Bevestigd-exploiteerbare bevindingen | **7 open** (I-2/I-4, S-1, D-4, S-2, D-3, I-5, I-6) + dependency-CVE's | Pentest §4 | **D-4 en I-5 opgelost** (hertest), I-2/I-4/I-6/S-1 gefixt op codeniveau; **resteert:** D-3 (SR-15), S-2 (SR-3, container), I-1 (SR-9, config) |
| Onderhoudbaarheid (ISO 25010) | gem. CC **2,05**; 0 kritieke smells; duplicatie ~0,9% | `onderhoudbaarheid-analyse.md` | Ongewijzigd (geen regressie); hotspots O1–O5 open |
| Auditlogging-dekking van state-changing endpoints | **100%** van de REST CRUD-endpoints (top-level + sub-resource controller) | `testresultaten-overzicht.md` | Behouden; live `DENIED`/`FAILED` bevestigd; SUCCESS nog op `WARN` (M1) |

---

## 9. Samenvatting

- Verbeteringen worden geprioriteerd op **vier expliciete criteria** - impactklasse, gemeten
  exploiteerbaarheid, effort en NEN/AVG-compliance - in die volgorde toegepast, met een
  **quick-win-regel** (hoge impact x kleine effort eerst).
- De volgorde is **op metingen gebaseerd**: de penetratietest verhoogde code-bevestigde
  bevindingen (I-2/I-4, D-4) en **verlaagde** theoretisch-hoge maar niet-exploiteerbare
  bevindingen (T-1, E-1, T-2, D-1, E-2) naar *accepteren*. De dependency-scan zette de twee
  CVSS-9.8-items als harde P0-blokkers.
- **R-1 auditlogging is af en bewezen met 31 groene tests** plus een live pentest, dus het is
  uit de open backlog gehaald.
- **Per 17-06 zijn 10 van de 18 backlog-items opgelost en geverifieerd** (R-1 + de 9 fixes uit
  PR #37, inclusief beide CVSS-9.8-deps); een live hertest bevestigde D-4, I-5, de dependency-
  upgrades en de auditlogging (`pentest-hertest-17-06.md`). De geplande volgorde is gevolgd:
  **alle Kritieke en Hoge items eerst**.
- Wat resteert is ofwel **infrastructuur/config** (SR-3 container, SR-9 deploy-property, SR-6
  gateway), een **nieuw te bouwen feature** (SR-15 rate-limiting) of **testwerk** (SR-14 +
  M5-regressietests). De geaccepteerde restrisico's (§4.2) blijven onder bewaking.
- Voor de volledige norm-tot-bewijs-keten per maatregel, zie de [traceability-matrix](traceability-matrix.md).

---

## 10. Referentie-index

| Document | Onderwerp |
|---|---|
| [`threat-model.md`](threat-model.md) | STRIDE + C4, trust boundaries, beveiligingsdoelen |
| [`security-backlog-pentest-rapport.md`](security-backlog-pentest-rapport.md) | Backlog SR-1…18 + pentest PT-1…15 |
| [`remediatie-status.md`](remediatie-status.md) | **Wat is opgelost (hoe) en wat bewust niet (waarom)** |
| [`pentest-hertest-17-06.md`](pentest-hertest-17-06.md) | **Live voor/na-hertest van de fixes** |
| [`traceability-matrix.md`](traceability-matrix.md) | **NEN-control → risico → maatregel → code/test/scan → doc** |
| [`r-1-auditlogging-bewijs.md`](r-1-auditlogging-bewijs.md) | Volledig R-1-bewijs incl. live pentest |
| [`testresultaten-overzicht.md`](testresultaten-overzicht.md) | Alle tests + testtypen-overzicht |
| [`onderhoudbaarheid-analyse.md`](onderhoudbaarheid-analyse.md) | **ISO 25010-metrieken + refactor-acties O1–O6** |
| [`code-coverage.md`](code-coverage.md) | JaCoCo, coverage-gate 80% |
| [`risk-matrix.md`](risk-matrix.md), [`risk-evaluation.md`](risk-evaluation.md), [`risk-bowtie.md`](risk-bowtie.md) | Risicomatrix / -evaluatie / bow-tie |
| [`biv-risicoanalyse.md`](biv-risicoanalyse.md) | BIV/CIA, kroonjuwelen |
| [`gap-analyse.md`](gap-analyse.md), [`gap-analyse-logging.md`](gap-analyse-logging.md) | Gap-analyses (NEN-koppeling) |
| [`attack-surface.md`](attack-surface.md) | Attack surface mapping |
| [`cicd.md`](cicd.md), [`pipeline-compliance.md`](pipeline-compliance.md) | CI/CD, SAST/SCA/SBOM, compliance |
</content>
