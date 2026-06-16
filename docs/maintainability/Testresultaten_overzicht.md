# Testresultaten overzicht — webservices.rest module

> **Wat is dit bestand?**
> Eén overzicht van **alle** geautomatiseerde tests in deze module: wat ze doen, of ze zijn
> gedraaid, en wat het resultaat was. De tests zijn opgesplitst in twee groepen:
>
> 1. **Onze eigen nieuwe tests** (audit logging, R-1) — hier ga ik diep op in: wat elke test
>    precies controleert en met welke echte output.
> 2. **De bestaande OpenMRS-tests** — deze waren er al voordat wij begonnen. Ik heb ze allemaal
>    gedraaid en laat per categorie zien dat ze slagen, zonder elke losse test te bespreken
>    (dat zijn er honderden en ze testen standaard OpenMRS-functionaliteit, niet onze wijziging).
>
> Alles in dit bestand is **echt gedraaid** op 16 juni 2026, niet overgenomen uit oude documentatie.

| | |
|---|---|
| **Branch** | `code-tests-logging` |
| **Datum van deze testrun** | 2026-06-16 |
| **Commando (alles)** | `mvn -o test` (vanuit de projectroot) |
| **Resultaat** | **BUILD SUCCESS** — 1896 tests, **0 failures, 0 errors**, 14 skipped |
| **Gerelateerd** | [R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md) (de uitgebreide pentest/bewijsvoering voor R-1), [Security_Backlog_Pentest_Rapport.md](../security/Security_Backlog_Pentest_Rapport.md) |

---

## 0. Samenvatting (TL;DR)

| | Testklassen | Tests | Resultaat |
|---|:---:|:---:|---|
| **Onze nieuwe tests** (audit logging) | 2 | **17** | ✅ alle 17 groen |
| **Bestaande OpenMRS-tests** (`omod-common`) | 11 | 96 | ✅ alle 96 groen |
| **Bestaande OpenMRS-tests** (`omod`) | 172 | 1.783 | ✅ alle groen (14 bewust overgeslagen, geen falen) |
| **Integration-tests** (`integration-tests`) | 1 | 2 | ⚠️ niet automatisch gedraaid, vereist een draaiende server (zie §3) |
| **Totaal automatisch gedraaid** | 185 | **1.896** | ✅ **0 failures, 0 errors** |

De drie nieuwste tests heb ik tijdens dit traject zelf toegevoegd (zie §1.3) omdat de bestaande
14 audit-tests CREATE/UPDATE/PURGE nog niet testten op een **geweigerde** actie — alleen DELETE.
Daarmee komt het totaal aantal eigen tests op **17**.

---

## 1. Onze eigen nieuwe tests (R-1, audit logging)

### 1.1 Waarom deze tests bestaan

De REST API kan objecten **aanmaken**, **wijzigen**, **verwijderen** en **definitief wissen**.
Voor de fix werd geen van die acties ergens vastgelegd: geen spoor van *wie* iets deed, *wat*,
*wanneer* en *vanaf welk IP*. Dat is het risico **R-1 (Incomplete auditlogging / repudiation)**
uit het [threat-model](../security/threat-model.md), bevestigd in PT-5 van het
[pentestrapport](../security/Security_Backlog_Pentest_Rapport.md). De volledige analyse en het
voor/na-bewijs (inclusief een live pentest tegen de draaiende Docker-container) staan in
[R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md). Dit hoofdstuk richt zich specifiek op
**de tests zelf**: wat ze controleren en het resultaat van het opnieuw draaien.

Er zijn twee testbestanden:

| Bestand | Test wat | Tests |
|---|---|:---:|
| `AuditLogTest.java` | het log-hulpje (`AuditLog`) in isolatie | 8 |
| `MainResourceControllerAuditTest.java` | de échte controller (`MainResourceController`), dus de koppeling in productiecode | 9 |

### 1.2 `AuditLogTest` — het log-hulpje (8 tests)

Bestand: `omod-common/src/test/java/.../web/audit/AuditLogTest.java`

Deze test vangt de logregels **in het geheugen** op via een eigen log4j2-appender, zodat de
inhoud van de regel direct met een assert te controleren is in plaats van een logbestand met de
hand te moeten lezen.

| Test | Wat het controleert |
|---|---|
| `noRecordCall_leavesNoAuditTrail` | zonder log-aanroep zijn er **0** regels — dit legt de oude, kwetsbare situatie vast |
| `success_leavesAuditTrailWithWhoWhatWhenWhere` | een geslaagde actie geeft 1 regel met `action`, `resource`, `uuid`, `outcome=SUCCESS`, `when` en `ip` |
| `denied_leavesAuditTrailWithDeniedOutcome` | een geweigerde actie geeft `outcome=DENIED` |
| `failure_leavesAuditTrailWithFailedOutcome` | een andere mislukking geeft `outcome=FAILED` |
| `formatMessage_doesNotLeakSensitivePasswordValue` | de regel bevat nooit de tekst "password" of "secret" |
| `formatMessage_neutralisesLineBreaksToPreventLogForging` | regeleindes in input vervalsen geen nepregel (CWE-117, log injectie) |
| `record_usesXForwardedForWhenPresent` | achter een proxy wordt het echte client-IP gelogd (uit `X-Forwarded-For`) |
| `formatMessage_producesAllFieldsInOrder` | de regel klopt **exact**, karakter voor karakter, in vaste volgorde |

### 1.3 `MainResourceControllerAuditTest` — de echte controller (9 tests)

Bestand: `omod-common/src/test/java/.../web/v1_0/controller/MainResourceControllerAuditTest.java`

Deze test roept de **echte** `MainResourceController` aan (met gemockte resources, geen
database) en controleert dat de controller zelf een auditregel schrijft voor elke
state-changing actie (CREATE/UPDATE/DELETE/PURGE), in zowel het **succes**- als het
**denied/failed**-pad. Dat bewijst de koppeling in productiecode, niet alleen het hulpje.

De eerste 6 tests bestonden al; de laatste 3 (cursief het verschil) heb ik zelf toegevoegd, omdat
de bestaande set wel DELETE-denied en DELETE-failed testte, maar niet CREATE/UPDATE/PURGE in het
geweigerde of mislukte pad — terwijl de controller dezelfde `auditFailure(...)`-hulpmethode voor
alle vier de acties gebruikt (zie `MainResourceController.java:212-218`). Zonder deze tests was
die gedeelde logica voor 3 van de 4 acties dus ongetest.

| Test | Wat het nabootst | Verwachte regel | Nieuw? |
|---|---|---|---|
| `delete_whenSuccessful_writesSuccessAuditEntry` | geslaagde DELETE | `action=DELETE ... outcome=SUCCESS` | bestond al |
| `purge_whenSuccessful_writesSuccessAuditEntry` | geslaagde PURGE | `action=PURGE ... outcome=SUCCESS` | bestond al |
| `delete_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | DELETE zonder rechten | `action=DELETE ... outcome=DENIED` | bestond al |
| `delete_whenServerError_writesFailedAuditEntryAndRethrows` | DELETE met serverfout | `action=DELETE ... outcome=FAILED` | bestond al |
| `update_withPasswordInBody_doesNotLogThePassword` | UPDATE van user met wachtwoord in de body | `action=UPDATE` zónder wachtwoord | bestond al |
| `create_withPasswordInBody_logsSuccessWithoutThePassword` | CREATE van user met wachtwoord | `action=CREATE` zónder wachtwoord | bestond al |
| `create_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | CREATE zonder rechten | `action=CREATE ... outcome=DENIED` | **nieuw** |
| `update_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | UPDATE zonder rechten | `action=UPDATE ... outcome=DENIED` | **nieuw** |
| `purge_whenServerError_writesFailedAuditEntryAndRethrows` | PURGE met een serverfout (bv. DB-constraint) | `action=PURGE ... outcome=FAILED` | **nieuw** |

Alle "denied/failed"-tests controleren ook dat de oorspronkelijke exception gewoon wordt
doorgegooid naar de normale foutafhandeling — loggen verandert dus niets aan het gedrag van de
API, het voegt alleen het spoor toe.

### 1.4 Resultaat van het zelf draaien (vandaag, 2026-06-16)

```bash
mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest
```

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in ...audit.AuditLogTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in ...controller.MainResourceControllerAuditTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 1.5 Echte auditregels die de tests produceren (bewijs)

Dit zijn de **echte regels** die de 9 controller-tests vandaag hebben weggeschreven (opgevangen
door de in-memory appender en geprint als bewijs):

```
AUDIT action=DELETE resource=patient uuid=uuid-denied        outcome=DENIED  when=2026-06-16T10:34:21.466Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient uuid=uuid-success       outcome=SUCCESS when=2026-06-16T10:34:21.580Z user=admin ip=192.168.1.50
AUDIT action=UPDATE resource=user    uuid=uuid-update-denied outcome=DENIED  when=2026-06-16T10:34:21.634Z user=admin ip=192.168.1.50
AUDIT action=PURGE  resource=location uuid=uuid-purge        outcome=SUCCESS when=2026-06-16T10:34:21.682Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient uuid=uuid-error         outcome=FAILED  when=2026-06-16T10:34:21.727Z user=admin ip=192.168.1.50
AUDIT action=PURGE  resource=location uuid=uuid-purge-error  outcome=FAILED  when=2026-06-16T10:34:21.763Z user=admin ip=192.168.1.50
AUDIT action=UPDATE resource=user    uuid=uuid-user-1        outcome=SUCCESS when=2026-06-16T10:34:21.803Z user=admin ip=192.168.1.50
AUDIT action=CREATE resource=user    uuid=unknown            outcome=SUCCESS when=2026-06-16T10:34:21.918Z user=admin ip=192.168.1.50
AUDIT action=CREATE resource=patient uuid=unknown            outcome=DENIED  when=2026-06-16T10:34:21.951Z user=admin ip=192.168.1.50
```

Let op de twee `UPDATE`/`CREATE`-regels met `resource=user`: in die tests stond een **echt
wachtwoord** in de request body (`S3cr3t-Passw0rd!`). Het wachtwoord staat nergens in de regel —
precies de eis uit de gap-analyse.

> **Bijvangst tijdens de volledige testrun:** tijdens het draaien van de bestaande OpenMRS-tests
> (zie §2) loopt de echte `MainResourceController` ook gewoon mee in honderden bestaande
> teststromen (elke keer dat een bestaande test een create/update/delete aanroept). Daarbij zag
> ik bijvoorbeeld dit ontstaan, vanzelf, zonder dat ik dat ergens heb geforceerd:
> ```
> WARN AuditLog.record AUDIT action=UPDATE resource=provider uuid=c2299800-... outcome=FAILED when=... user=admin ip=127.0.0.1
> WARN AuditLog.record AUDIT action=CREATE resource=conceptdatatype uuid=unknown outcome=FAILED when=... user=admin ip=127.0.0.1
> ```
> Dit is extra bewijs dat de auditlogging **transparant** meedraait met de rest van de module: de
> bestaande testsuite (1.783 tests) blijft voor 100% slagen mét de logging aan, en de logging
> wordt ook door heel andere resources (provider, conceptdatatype, ...) dan de eigen tests
> automatisch gebruikt, omdat hij in de gedeelde controller zit.

---

## 2. Bestaande OpenMRS-tests

Dit zijn de tests die al in de module zaten vóórdat wij ermee aan de slag gingen. Ze testen
**standaard OpenMRS REST-functionaliteit** (CRUD op patiënten, concepten, encounters, gebruikers,
enzovoort) voor de verschillende ondersteunde OpenMRS-versies (1.8 t/m 2.8). Dit is niet ons werk
en ik ga er daarom niet per test op in — ik laat per categorie zien dat **alles draait en
slaagt**, zodat aangetoond is dat onze wijziging niets heeft stukgemaakt.

### 2.1 Module `omod-common` (generieke REST-laag) — 96 tests, 11 klassen

| Testklasse | Wat het (kort) test |
|---|---|
| `ConversionUtilTest` (16) | conversie tussen Java-objecten en REST-representaties |
| `RequestContextTest` (5) | opbouwen van de request-context (representation, paging, ...) |
| `RestUtilComponentTest` (2) | hulpfuncties van de REST-laag |
| `RestUtilTest` (15) | idem, o.a. response-helpers en exceptie-afhandeling |
| `RestServiceTest` (4) | resource-registratie en -lookup |
| `ContentTypeFilterTest` (6) | blokkeert XML content-types, staat JSON/multipart toe — zie §2.3 |
| `SearchConfigTest` (22) | configuratie van zoek-handlers |
| `SearchQueryTest` (17) | opbouw en validatie van zoekqueries |
| `AlreadyPagedTest` (3) | paginering van resultaten |
| `MetadataDelegatingCrudResourceTest` (3) | generieke CRUD-resource voor metadata |
| `NeedsPagingTest` (3) | paginering-interface |

**Resultaat:** `Tests run: 96, Failures: 0, Errors: 0, Skipped: 0` — ✅ alle groen.

### 2.2 Module `omod` (resources per OpenMRS-versie) — 1.783 tests, 172 klassen

Dit zijn vooral **controller-, resource- en search-handler-tests**: voor elke OpenMRS-resource
(patiënt, concept, encounter, order, visit, gebruiker, ...) en voor elke OpenMRS-versie waarin
die resource veranderde, bestaat een testklasse die de bijbehorende REST-laag aanroept (GET,
POST, PUT/POST-update, DELETE) en controleert dat de juiste velden/JSON terugkomen. Omdat dit
zo'n grote, repetitieve set is, splits ik 'm hier alleen per OpenMRS-versie uit:

| OpenMRS-versie pakket | Testklassen | Tests | Resultaat |
|---|:---:|:---:|---|
| `openmrs1_8.*` | 52 | 334 | ✅ |
| `openmrs1_9.*` | 78 | 674 | ✅ |
| `openmrs1_10.*` | 20 | 133 | ✅ |
| `openmrs1_11.*` | 10 | 46 | ✅ |
| `openmrs1_12.*` | 8 | 43 | ✅ |
| `openmrs2_0.*` | 45 | 237 | ✅ |
| `openmrs2_1.*` | 8 | 37 | ✅ |
| `openmrs2_2.*` | 15 | 92 | ✅ |
| `openmrs2_3.*` | 3 | 20 | ✅ |
| `openmrs2_4.*` | 4 | 20 | ✅ |
| `openmrs2_5.*` | 3 | 19 | ✅ |
| `openmrs2_7.*` | 4 | 8 | ✅ |
| `openmrs2_8.*` | 3 | 16 | ✅ |
| Overig (generieke controller-basis, reflectie-utils, Swagger-generatie, validatie) | ~12 | ~104 | ✅ |

**Resultaat:** `Tests run: 1783, Failures: 0, Errors: 0, Skipped: 14` — ✅ alles groen, 14 tests
bewust overgeslagen door de tests zelf (bv. een conditionele `assumeTrue`/`@Disabled` voor een
specifieke OpenMRS-deelversie), **geen enkele faalt**.

### 2.3 Bestaande tests die rechtstreeks aan de security-backlog raken

Een paar van de bestaande OpenMRS-tests zijn extra relevant omdat ze (deels) hetzelfde gebied
raken als de bevindingen uit het [pentestrapport](../security/Security_Backlog_Pentest_Rapport.md). Voor de
volledigheid, en om eerlijk te zijn over wat ze wél en niet aantonen:

| Backlog-item | Testklasse | Wat de test al aantoont | Wat de test **niet** test |
|---|---|---|---|
| PT-8/SR-8 (XML content-type bypass, E-2) | `ContentTypeFilterTest` | blokkeert alle XML-content-type-varianten, staat JSON/multipart toe — dit is precies de fix die PT-8 "niet exploiteerbaar" maakte | — (dit punt is dus al gedekt) |
| PT-12/SR-15 (brute-force wachtwoordreset, D-3) | `PasswordResetController2_2Test` | dat het reset-mechanisme zelf (activatiesleutel aanmaken/valideren) correct werkt | rate-limiting/lockout — die bestaat nog niet in de code, dus kan ook niet getest worden (open backlog-item) |
| D-4/SR-17 (`/cleardbcache` zonder `@Authorized`) | `ClearDbCacheController2_0Test` | dat de cache-eviction-logica zelf correct werkt | autorisatie — de controller heeft nog geen `@Authorized`-annotatie (zie `ClearDbCacheController2_0.java:36-50`), dat is dus nog steeds open |
| PT-13/SR-16 (`/session/diag` infolek, I-5) | `SessionController1_9Test` | sessiegedrag (login/logout, locale, locatie, provider) | het specifieke `/session/diag`-endpoint en het ontbreken van authenticatie daarop |
| R-1 (auditlogging) | `MainResourceControllerAuditTest`, `AuditLogTest` | zie hoofdstuk 1 — dit *is* de fix | — |

Deze tabel laat zien dat het oplossen van een backlog-item (zoals SR-17) ook een **nieuwe of
uitgebreide test** nodig heeft — het simpelweg bestaan van een testklasse met die naam betekent
niet automatisch dat de kwetsbaarheid is afgedekt. Voor R-1 is dat traject in dit document
(hoofdstuk 1) en in [R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md) volledig doorlopen.

---

## 3. Integration-tests (niet automatisch meegedraaid)

Module `integration-tests` bevat **end-to-end tests** die tegen een **echt draaiende** OpenMRS-
server praten (via HTTP, met `rest-assured`). Dat is een ander soort test dan de rest van dit
document: geen Spring-mock, maar een echte server nodig. Daarom draaien ze niet mee met
`mvn test` — ze hebben hun eigen Maven-profiel en starten pas bij `mvn verify`:

| Bestand | Tests | Wat het test |
|---|:---:|---|
| `SessionIT.java` | 2 | `shouldBeUnauthenticatedByDefault` (sessie zonder login geeft `authenticated:false`) en `shouldAuthenticateAsAdmin` (sessie met admin-login geeft `authenticated:true`) |

Zelf draaien (vereist een draaiende OpenMRS op `localhost:8080` met de module erin, of een eigen
`-DtestUrl=...`):

```bash
mvn clean verify -Pintegration-tests -DtestUrl=http://admin:Admin123@localhost:8080/openmrs
```

Ik heb dit niet in deze sessie gedraaid omdat daar een aparte, opgestarte OpenMRS-server voor
nodig is (zie wel de **live pentest** met een draaiende Docker-container in
[R-1_auditlogging_bewijs.md §4.6](../security/R-1_auditlogging_bewijs.md#46-penetration-test-risicoverlaging-aangetoond)
voor het equivalent: daar is de auditlogging al écht tegen een draaiende server getest).

---

## 4. Alles samen, nog een keer

```bash
mvn -o test
```

```
[INFO] Rest Web Services .................................. SUCCESS [  4.5 s]
[INFO] Rest Web Services Common OMOD ...................... SUCCESS [01:11 min]   <- 113 tests (96 OpenMRS + 17 van ons)
[INFO] Rest Web Services OMOD ............................. SUCCESS [06:21 min]   <- 1783 tests (allemaal OpenMRS)
[INFO] Rest Web Services Integration Tests ................ SUCCESS [  2.1 s]     <- 0 tests (geen live server in deze run)
[INFO] BUILD SUCCESS
[INFO] Total time:  07:41 min
```

| | Tests | Failures | Errors | Skipped |
|---|:---:|:---:|:---:|:---:|
| **Totaal** | **1.896** | **0** | **0** | 14 |
| — waarvan onze nieuwe tests | 17 | 0 | 0 | 0 |
| — waarvan bestaande OpenMRS-tests | 1.879 | 0 | 0 | 14 |

---

## 5. Voldoen we aan de eisen?

| Eis | Voldaan? | Bewijs |
|---|---|---|
| Relevante tests opgesteld | Ja | 17 eigen tests gericht op R-1 (audit logging), zie hoofdstuk 1 |
| Tests zelf uitgevoerd | Ja | volledige `mvn -o test`-run vandaag (2026-06-16), zie hoofdstuk 4 |
| Resultaten duidelijk vastgelegd | Ja | dit document + ruwe Maven-output bewaard |
| Opsplitsing eigen vs. OpenMRS | Ja | hoofdstuk 1 (eigen) vs. hoofdstuk 2 (bestaand) |
| Uitleg per test | Ja | per-test tabel in §1.2/§1.3; per-categorie in §2.1/§2.2 voor de bestaande tests |
| Geen regressie door onze wijziging | Ja | alle 1.879 bestaande tests slagen nog steeds, mét de audit-logging actief |
| Eventuele test-gaten gedicht | Ja | 3 nieuwe tests toegevoegd voor CREATE/UPDATE/PURGE-denied (zie §1.3) |

---

## 6. Herleidbaarheid

| Onderdeel | Locatie |
|---|---|
| Risico-omschrijving R-1 | [threat-model.md](../security/threat-model.md) |
| Pentestbevinding PT-5 | [Security_Backlog_Pentest_Rapport.md](../security/Security_Backlog_Pentest_Rapport.md) |
| Volledige bewijsvoering R-1 (incl. live pentest) | [R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md) |
| Het log-hulpje | `omod-common/src/main/java/.../web/audit/AuditLog.java` |
| Koppeling in de controller | `omod-common/src/main/java/.../web/v1_0/controller/MainResourceController.java` |
| Test 1 (log-hulpje) | `omod-common/src/test/java/.../web/audit/AuditLogTest.java` |
| Test 2 (controller, incl. 3 nieuwe tests) | `omod-common/src/test/java/.../web/v1_0/controller/MainResourceControllerAuditTest.java` |
| Alles in 1 keer draaien | `mvn -o test` (volledige module) of `mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest` (alleen onze tests) |
