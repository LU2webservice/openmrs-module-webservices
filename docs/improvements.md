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
| **Datum** | 2026-06-16 |
| **Branch** | `code-tests-logging` |
| **Status** | 1 verbetering op het kritieke pad (R-1 auditlogging) is al gebouwd en getest; de rest is hieronder geprioriteerd |

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
| Testresultaten | Auditlogging-tests (R-1), regressierun | `testresultaten-overzicht.md`, `r-1-auditlogging-bewijs.md` |
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

| Verbetering | Bevinding | Bewijs (meting) |
|---|---|---|
| **Auditlogging op alle state-changing endpoints** (SR-5) | **R-1** Incomplete auditlogging (C3, Middel) | Gebouwd en bewezen met **31 geautomatiseerde tests, allemaal groen** (`testresultaten-overzicht.md`). Rood/groen-bewijs: de controllertest **faalt (6/6)** op de oude code en **slaagt** na de fix. Een **live pentest** (curl tegen de draaiende container) toont echte `DENIED`/`FAILED`-auditregels met `user`, `uuid`, `when`, `ip` (`r-1-auditlogging-bewijs.md`). Koppelt aan NEN-7510:2024 **8.15 Logging**. |

> Dit is het ene item uit de Middel-klasse dat we naar voren hebben gehaald en afgerond,
> omdat het direct testbaar was en een expliciete NEN 7510-eis is voor een EPD-systeem. Het
> staat hier zodat het in §5 **niet** dubbel wordt geteld.

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

### P0 - Kritiek (oplossen vóór productie-deploy)

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) |
|:--:|---|---|:---:|:---:|---|---|
| 1 | **SR-7** | `@Authorized('Manage RESTWS')` op de instellingenpagina (`SettingsFormController.searchProperties()`) | **E4 - Kritiek** | **Klein** | 8.3, 5.15, 8.24 | **Quick win.** PT-7 (code review) bevestigde dat het endpoint **geen auth-check** heeft en global-property-**waarden incl. secrets/API-keys** teruggeeft, en onder `/module/*` valt *buiten* de `AuthorizationFilter` (TB-4). Catastrofale impact, fix van één annotatie -> hoogste rendement van de hele backlog. |
| 2 | **SR-12** | Upgrade `swagger-core 1.6.2 -> 1.6.12+` (trekt SnakeYAML >= 2.0 mee) | **D4 - Kritiek** | **Klein** | 8.8 | **Quick win.** PT-10 (SCA) bevestigde transitief **SnakeYAML < 2.0**, **CVE-2022-1471 CVSS 9.8** (deserialisatie-RCE). Eén dependency-bump haalt een 9.8 weg -> direct doen. |
| 3 | **SR-10** | Upgrade `Apache Tomcat Jasper 6.0.53 -> 9.0.x` (`provided` scope behouden) | **D4 - Kritiek** | Middel | 8.8 | PT-10 (SCA) bevestigde een **EOL**-component met **drie CVSS 9.8 CVE's** (CVE-2020-9484, CVE-2020-1938 Ghostcat, CVE-2019-0232). Meer effort dan SR-12, dus erna, maar nog steeds verplicht vóór productie. |

### P1 - Hoog

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) |
|:--:|---|---|:---:|:---:|---|---|
| 4 | **SR-17** | `@Authorized` toevoegen aan `/cleardbcache` (`ClearDbCacheController2_0`) | **C4 - Hoog** | **Klein** | 5.15, 8.6 | **Quick win.** PT-14 bevestigde HTTP **204 voor een anonieme aanroeper** - iedereen kan de volledige Hibernate-cache wissen (DoS). Oorzaak: ontbrekende annotatie + stil falend filter (TB-5/TB-6). Eén regel, hoge impact. |
| 5 | **SR-1** | HTTPS/TLS afdwingen voor alle REST-endpoints (+ HSTS) | **D4 - Hoog** | Middel | 8.24, 8.5 | PT-1 bevestigde dat Basic-Auth-credentials als Base64 over HTTP leesbaar zijn (`admin:Admin123` te decoderen op de lijn). Reëel risico op een ziekenhuisnetwerk. Effort is Middel (config + deploy), dus na de Hoge quick win. |

### P2 - Middel

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) |
|:--:|---|---|:---:|:---:|---|---|
| 6 | **SR-3** | `Secure` + `SameSite=Strict` op de sessiecookie | **C3 - Middel** | **Klein** | 8.5, 8.24 | PT-3 bevestigde `HttpOnly` aanwezig, maar **`Secure` en `SameSite` ontbreken**. Samen met S-1 maakt dit sessie-hijacking mogelijk. Kleine effort -> eerste van de Middel-klasse. |
| 7 | **SR-16** | `/session/diag` achter authenticatie plaatsen | **B3 - Middel** | **Klein** | 8.3 | PT-13 bevestigde dat `serverTime` **zonder auth** wordt teruggegeven en de `token`-param wordt **genegeerd** (schijnbeveiliging). Klein, lage-impact recon-lek. |
| 8 | **SR-18** | `swagger.json` achter authenticatie plaatsen | **C3 - Middel** | **Klein** | 8.3 | PT-15 (code review) bevestigde geen auth op `/module/*` (TB-6); in productie geeft dit de volledige API-kaart prijs. Kleine fix. |
| 9 | **SR-11** | Upgrade `jackson-dataformat-yaml 2.13.3 -> 2.15.0+` | **C3 - Middel** | **Klein** | 8.8 | PT-10 (SCA) bevestigde CVE-2022-42003/42004 (CVSS 7.5). **Alleen test-scope**, dus geen productierisico, maar het houdt de CI-dependency-scan schoon. |
| 10 | **SR-15** | Rate-limiting + lockout op het wachtwoord-reset-endpoint | **C3 - Middel** | Middel | 8.5, 5.17 | PT-12 bevestigde **geen HTTP 429 en geen vertraging** na herhaalde pogingen (`PasswordResetController2_2`) - activatiesleutels zijn te enumereren. Sluit aan op gap-analyse 3. Middel effort, dus laatste van de Middel-klasse. |

### P3 - Laag

| # | ID | Verbetering | Score - klasse | Effort | NEN-7510:2024 | Waarom deze rang (analyse + meting) |
|:--:|---|---|:---:|:---:|---|---|
| 11 | **SR-9** | Stack traces uitschakelen in productie (`enableStackTraceDetails=false`) | **B3 - Laag** | **Klein** | 8.28 | PT-9 bevestigde dat interne klassenamen in de response-body lekken. Configuratiewijziging, lage impact. |
| 12 | **SR-13** | Upgrade `commons-codec 1.14 -> 1.17+` | **B2 - Laag** | **Klein** | 8.8 | PT-10: verouderd, geen actieve CVE's; opschoning. |

---

## 6. Aanbevolen volgorde (roadmap)

Dit volgt direct uit §5 en uit de **kostenraming** in het risk assessment report
(indicatief: ~144 u / ~EUR 11.520 totaal bij EUR 80/u; de kritieke fixes zijn ~56 u). Met
beperkte tijd telt de volgorde, niet het afronden van alles.

- **Sprint 1 - "deploy-blokkers" (~de kritieke 56 u, maar met quick wins vooraan).**
  Begin met de twee Kritieke **quick wins** SR-7 en SR-12 (beide Klein) en de Hoge quick win
  SR-17 (Klein) - samen halen die een catastrofaal secrets-lek, een CVSS 9.8 en een anonieme
  DoS weg in ongeveer een dag. Daarna SR-10 (Tomcat) en SR-1 (HTTPS).
- **Sprint 2 - Middel-klasse, quick wins eerst.** SR-3, SR-16, SR-18, SR-11 (allemaal
  Klein), daarna SR-15 (Middel).
- **Backlog - Laag-klasse.** SR-9, SR-13 wanneer er capaciteit is.
- **Bewaken (geen actief werk).** De geaccepteerde items in §4.2 (T-1, E-1, T-2, D-1, E-2):
  hertesten zodra er nieuwe resources of search handlers bij komen.

Dit respecteert de harde beslisregel uit de risicocriteria: niets in de klasse **Kritiek**
gaat ongemitigeerd naar productie, en niets in **Hoog/Kritiek** blijft open zonder
vastgelegde acceptatie.

---

## 7. Verbeteringen voor onderhoudbaarheid en testbaarheid

Naast de security backlog bracht het testwerk een paar **engineering**-verbeteringen aan het
licht, elk gedocumenteerd als eerlijke kanttekening in onze eigen rapporten. Ze zijn kleiner
maar concreet.

| # | Verbetering | Onderbouwing (bron) | Effort |
|:--:|---|---|:---:|
| M1 | Zet de aparte **audit-logger op `INFO` in productie** zodat ook geslaagde acties (niet alleen `DENIED`/`FAILED`) live worden vastgelegd | De container staat standaard op `WARN`, dus SUCCESS-regels worden nog niet in productie bewaard (`r-1-auditlogging-bewijs.md` §4.6) | Klein |
| M2 | Koppel de **integratietests** (`SessionIT`) achter een profiel/stage in de CI | Ze vereisen nu een draaiende server en worden **niet automatisch** gedraaid (`testresultaten-overzicht.md` §3) | Middel |
| M3 | Quarantaine / stabiliseer de flaky **`ClearDbCacheController2_0Test`** (Hibernate-cache-timing) | Bevestigd flaky **ook op de originele OpenMRS-code**, los van onze wijziging (`testresultaten-overzicht.md` §2.2) | Klein |
| M4 | Verhoog de **coverage-gate** mee als de dekking groeit (property staat al in `pom.xml`) | Huidige gecombineerde dekking **82,8%** (omod **86,6%**) ligt boven de **80%**-gate; verhoog naar 85% als het structureel hoger is (`code-coverage.md`) | Klein |

---

## 8. Meetbaseline (zodat verbeteringen verifieerbaar blijven)

Om de verbeteringen **meetbaar** te houden, is dit de huidige gemeten baseline. Na elke
verbetering hoort dezelfde meting opnieuw te draaien om te bewijzen dat het risico daalde.

| Metriek | Huidige gemeten waarde | Bron | Doel na verbetering |
|---|---|---|---|
| Geslaagde geautomatiseerde tests | **1.910** gedraaid; onze **31** audit-tests deterministisch groen; 1 bestaande flaky OpenMRS-test | `testresultaten-overzicht.md` | Geen regressie; flaky test in quarantaine (M3) |
| Gecombineerde code coverage | **82,8%** (omod 86,6%), gate op 80% | `code-coverage.md` | >= 80%, verhogen naar 85% (M4) |
| Kritieke dependency-CVE's | **2 x CVSS 9.8** (Tomcat Jasper, SnakeYAML) + 1 x 7.5 (jackson-yaml, test) | PT-10 / Grype SCA | 0 kritiek na SR-10/SR-12; schone scan na SR-11 |
| Bevestigd-exploiteerbare bevindingen | **7 open** (I-2/I-4, S-1, D-4, S-2, D-3, I-5, I-6) + dependency-CVE's | Pentest §4 | Naar 0 via P0-P2 |
| Auditlogging-dekking van state-changing endpoints | **100%** van de REST CRUD-endpoints (top-level + sub-resource controller) | `testresultaten-overzicht.md` | Behouden; SUCCESS live vastgelegd (M1) |

---

## 9. Samenvatting

- Verbeteringen worden geprioriteerd op **vier expliciete criteria** - impactklasse, gemeten
  exploiteerbaarheid, effort en NEN/AVG-compliance - in die volgorde toegepast, met een
  **quick-win-regel** (hoge impact x kleine effort eerst).
- De volgorde is **op metingen gebaseerd**: de penetratietest verhoogde code-bevestigde
  bevindingen (I-2/I-4, D-4) en **verlaagde** theoretisch-hoge maar niet-exploiteerbare
  bevindingen (T-1, E-1, T-2, D-1, E-2) naar *accepteren*. De dependency-scan zette de twee
  CVSS-9.8-items als harde P0-blokkers.
- **R-1 auditlogging is al af en bewezen met 31 groene tests** plus een live pentest, dus het
  is uit de open backlog gehaald.
- Het resultaat is een korte, verdedigbare roadmap: **SR-7, SR-12, SR-17 eerst** (een dag
  werk die het ergste risico wegneemt), daarna de overige Kritiek/Hoog, dan Middel en Laag,
  met de geaccepteerde restrisico's onder bewaking.

---

## 10. Referentie-index

| Document |
|---|
| `threat-model.md` |
| `security-backlog-pentest-rapport.md` |
| `r-1-auditlogging-bewijs.md` |
| `testresultaten-overzicht.md` |
| `risk-matrix.md`, `risk-evaluation.md` |
| `cicd.md` |
| `risk-criteria.md`, `risk-assessment-report.md` |
| `gap-analyse.md`, `gap-analyse-logging.md` |
| `biv-risicoanalyse.md` |
| `code-coverage.md` |
</content>
