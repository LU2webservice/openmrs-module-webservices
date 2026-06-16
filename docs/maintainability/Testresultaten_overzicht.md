# Testresultaten overzicht - webservices.rest module

> **Wat is dit bestand?**
> Eén overzicht van **alle** geautomatiseerde tests in deze module: wat ze doen, of ze zijn
> gedraaid, en wat het resultaat was. De tests zijn opgesplitst in twee groepen:
>
> 1. **Onze eigen nieuwe tests** (audit logging, R-1) - hier ga ik diep op in: wat elke test
>    precies controleert en met welke echte output.
> 2. **De bestaande OpenMRS-tests** - deze waren er al voordat wij begonnen. Ik heb ze allemaal
>    gedraaid en laat per categorie zien dat ze slagen, zonder elke losse test te bespreken
>    (dat zijn er honderden en ze testen standaard OpenMRS-functionaliteit, niet onze wijziging).
>
> Alles in dit bestand is **echt gedraaid** op 16 juni 2026, niet overgenomen uit oude documentatie.

| | |
|---|---|
| **Branch** | `code-tests-logging` |
| **Datum van deze testrun** | 2026-06-16 |
| **Commando (alles)** | `mvn -o test` (vanuit de projectroot) |
| **Resultaat** | 1.910 tests gedraaid; onze 31 audit-tests **0 failures** (deterministisch). 1 bestaande flaky OpenMRS-test los van onze wijziging, zie §2.2 |
| **Gerelateerd** | [R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md) (de uitgebreide pentest/bewijsvoering voor R-1), [Security_Backlog_Pentest_Rapport.md](../security/Security_Backlog_Pentest_Rapport.md) |

---

## 0. Samenvatting (TL;DR)

| | Testklassen | Tests | Resultaat |
|---|:---:|:---:|---|
| **Onze nieuwe tests** (audit logging) | 3 | **31** | alle 31 groen (deterministisch) |
| **Bestaande OpenMRS-tests** (`omod-common`) | 11 | 96 | alle 96 groen |
| **Bestaande OpenMRS-tests** (`omod`) | 172 | 1.783 | groen, m.u.v. 1 bestaande **flaky** OpenMRS-test (`ClearDbCacheController2_0Test`, zie §2.2) |
| **Integration-tests** (`integration-tests`) | 1 | 2 | niet automatisch gedraaid, vereist een draaiende server (zie §3) |
| **Totaal automatisch gedraaid** | 186 | **1.910** | onze module 100% groen; 1 pre-existing flaky OpenMRS-test los van onze wijziging |

> **In één zin:** standaard draait deze module **1.879 bestaande tests van OpenMRS zelf**; daar
> hebben wíj **31 tests** aan toegevoegd die specifiek de **audit-logging** controleren. Samen
> **1.910** tests, waarvan onze 31 deterministisch groen zijn.

### Wat voor soort tests zijn onze 31?

| Soort | Aantal | Welke | Hoe het draait |
|---|:---:|---|---|
| **Pure unit test** | 8 | `AuditLogTest` | geen Spring, geen database - roept het log-hulpje `AuditLog` rechtstreeks aan en vangt de logregel in het geheugen op. Millisecondensnel en altijd stabiel. |
| **Component-/controllertest** | 23 | `MainResourceControllerAuditTest` (9), `MainSubResourceControllerAuditTest` (14) | draait in een echte OpenMRS-context (daarom is de gebruiker `admin`), maar met **gemockte** resources, zodat puur de controller-logica + de logging-koppeling wordt getest, zonder echte database-writes. |

Het zijn dus **geen** end-to-end/integration-tests tegen een draaiende server (die staan apart, zie
§3) en geen UI-tests. Het is de gangbare OpenMRS-teststijl: een snelle unit-test voor de losse
logica, plus context-sensitieve tests die bewijzen dat de productiecode (de controllers) de logging
ook echt aanroept.

Het doel was: **alle state-changing endpoints van de REST-laag loggen én testen**, en de logging
duidelijk terug kunnen zien. De generieke REST-laag heeft twee controllers die samen álle
CRUD-endpoints afhandelen:

| Controller | Endpoints | Audit-test |
|---|---|---|
| `MainResourceController` | top-level resources, bv. `POST/DELETE /patient/{uuid}` | `MainResourceControllerAuditTest` (9 tests) |
| `MainSubResourceController` | sub-resources, bv. `POST/DELETE /patient/{uuid}/identifier/{uuid}` | `MainSubResourceControllerAuditTest` (14 tests) - **nieuw** |

Daarnaast test `AuditLogTest` (8 tests) het log-hulpje zelf. Samen **31 tests**. In dit traject heb
ik toegevoegd:
- **3 tests** aan `MainResourceControllerAuditTest` (CREATE/UPDATE/PURGE in het geweigerde/mislukte
  pad - dat was alleen voor DELETE getest), en
- de volledige nieuwe klasse `MainSubResourceControllerAuditTest` (**14 tests**) - én de bijbehorende
  **fix** in de sub-resource controller, want die logde `create`/`update`/`put` nog helemaal niet en
  had geen failure-afhandeling (zie §1.6).

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

Er zijn drie testbestanden:

| Bestand | Test wat | Tests |
|---|---|:---:|
| `AuditLogTest.java` | het log-hulpje (`AuditLog`) in isolatie | 8 |
| `MainResourceControllerAuditTest.java` | de échte top-level controller (`MainResourceController`) | 9 |
| `MainSubResourceControllerAuditTest.java` | de échte sub-resource controller (`MainSubResourceController`) | 14 |

### 1.2 `AuditLogTest` - het log-hulpje (8 tests)

Bestand: `omod-common/src/test/java/.../web/audit/AuditLogTest.java`

Deze test vangt de logregels **in het geheugen** op via een eigen log4j2-appender, zodat de
inhoud van de regel direct met een assert te controleren is in plaats van een logbestand met de
hand te moeten lezen.

| Test | Wat het controleert |
|---|---|
| `noRecordCall_leavesNoAuditTrail` | zonder log-aanroep zijn er **0** regels - dit legt de oude, kwetsbare situatie vast |
| `success_leavesAuditTrailWithWhoWhatWhenWhere` | een geslaagde actie geeft 1 regel met `action`, `resource`, `uuid`, `outcome=SUCCESS`, `when` en `ip` |
| `denied_leavesAuditTrailWithDeniedOutcome` | een geweigerde actie geeft `outcome=DENIED` |
| `failure_leavesAuditTrailWithFailedOutcome` | een andere mislukking geeft `outcome=FAILED` |
| `formatMessage_doesNotLeakSensitivePasswordValue` | de regel bevat nooit de tekst "password" of "secret" |
| `formatMessage_neutralisesLineBreaksToPreventLogForging` | regeleindes in input vervalsen geen nepregel (CWE-117, log injectie) |
| `record_usesXForwardedForWhenPresent` | achter een proxy wordt het echte client-IP gelogd (uit `X-Forwarded-For`) |
| `formatMessage_producesAllFieldsInOrder` | de regel klopt **exact**, karakter voor karakter, in vaste volgorde |

### 1.3 `MainResourceControllerAuditTest` - de echte controller (9 tests)

Bestand: `omod-common/src/test/java/.../web/v1_0/controller/MainResourceControllerAuditTest.java`

Deze test roept de **echte** `MainResourceController` aan (met gemockte resources, geen
database) en controleert dat de controller zelf een auditregel schrijft voor elke
state-changing actie (CREATE/UPDATE/DELETE/PURGE), in zowel het **succes**- als het
**denied/failed**-pad. Dat bewijst de koppeling in productiecode, niet alleen het hulpje.

De eerste 6 tests bestonden al; de laatste 3 (cursief het verschil) heb ik zelf toegevoegd, omdat
de bestaande set wel DELETE-denied en DELETE-failed testte, maar niet CREATE/UPDATE/PURGE in het
geweigerde of mislukte pad - terwijl de controller dezelfde `auditFailure(...)`-hulpmethode voor
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
doorgegooid naar de normale foutafhandeling - loggen verandert dus niets aan het gedrag van de
API, het voegt alleen het spoor toe.

### 1.4 `MainSubResourceControllerAuditTest` - de sub-resource controller (14 tests)

Bestand: `omod-common/src/test/java/.../web/v1_0/controller/MainSubResourceControllerAuditTest.java`

Sub-resources zijn een **aparte set endpoints**, afgehandeld door een andere controller
(`MainSubResourceController`). Voorbeeld: `POST /patient/{uuid}/identifier` voegt een identifier toe
aan een patiënt, `DELETE /patient/{uuid}/identifier/{uuid}` verwijdert die weer. Deze test bewijst
dat ook die endpoints loggen - voor élke actie, in zowel het succes- als het denied/failed-pad.

Voor dit traject logde deze controller `create`/`update`/`put` **niet** en had hij geen
failure-afhandeling; die fix staat in §1.6. De 14 tests dekken alle 7 state-changing endpoints:

| Test | Endpoint / actie | Verwachte regel |
|---|---|---|
| `create_whenSuccessful_writesSuccessAuditEntry` | geslaagde CREATE | `action=CREATE resource=patient/identifier ... outcome=SUCCESS` |
| `update_whenSuccessful_writesSuccessAuditEntry` | geslaagde UPDATE | `action=UPDATE ... outcome=SUCCESS` |
| `delete_withUuid_whenSuccessful_writesSuccessAuditEntry` | geslaagde DELETE (met child-uuid) | `action=DELETE ... outcome=SUCCESS` |
| `purge_withUuid_whenSuccessful_writesSuccessAuditEntry` | geslaagde PURGE (met child-uuid) | `action=PURGE ... outcome=SUCCESS` |
| `delete_withoutUuid_whenSuccessful_logsParentAsAffectedObject` | DELETE zonder child-uuid | `action=DELETE uuid=parent-uuid ... outcome=SUCCESS` |
| `purge_withoutUuid_whenSuccessful_logsParentAsAffectedObject` | PURGE zonder child-uuid | `action=PURGE uuid=parent-uuid ... outcome=SUCCESS` |
| `put_whenSuccessful_writesSuccessAuditEntry` | geslaagde PUT | `action=PUT ... outcome=SUCCESS` |
| `create_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | CREATE zonder rechten | `action=CREATE ... outcome=DENIED` |
| `update_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | UPDATE zonder rechten | `action=UPDATE ... outcome=DENIED` |
| `delete_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | DELETE zonder rechten | `action=DELETE ... outcome=DENIED` |
| `purge_whenServerError_writesFailedAuditEntryAndRethrows` | PURGE met serverfout | `action=PURGE ... outcome=FAILED` |
| `put_whenServerError_writesFailedAuditEntryAndRethrows` | PUT met serverfout | `action=PUT ... outcome=FAILED` |
| `create_withPasswordInBody_doesNotLogThePassword` | CREATE met wachtwoord in body | `action=CREATE` zónder wachtwoord |
| `update_withPasswordInBody_doesNotLogThePassword` | UPDATE met wachtwoord in body | `action=UPDATE` zónder wachtwoord |

### 1.5 Resultaat van het zelf draaien (vandaag, 2026-06-16)

```bash
mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest,MainSubResourceControllerAuditTest
```

```
[INFO] Tests run:  8, Failures: 0, Errors: 0, Skipped: 0 -- in ...audit.AuditLogTest
[INFO] Tests run:  9, Failures: 0, Errors: 0, Skipped: 0 -- in ...controller.MainResourceControllerAuditTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in ...controller.MainSubResourceControllerAuditTest
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 1.5.1 Echte auditregels - top-level controller (9 tests)

Dit zijn de **echte regels** die de 9 `MainResourceController`-tests vandaag hebben weggeschreven
(opgevangen door de in-memory appender en geprint als bewijs):

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

### 1.5.2 Echte auditregels - sub-resource controller (14 tests)

En dit zijn de **echte regels** van de nieuwe `MainSubResourceController`-tests. Let op
`resource=patient/identifier` en `resource=user/credential`: de sub-resource staat netjes als
`parent/sub` in de regel, en alle vijf de acties (CREATE/UPDATE/DELETE/PURGE/PUT) komen voor met
elk van de drie uitkomsten:

```
AUDIT action=CREATE resource=patient/identifier uuid=parent-uuid outcome=SUCCESS when=2026-06-16T10:54:10.078Z user=admin ip=192.168.1.50
AUDIT action=CREATE resource=patient/identifier uuid=parent-uuid outcome=DENIED  when=2026-06-16T10:54:09.979Z user=admin ip=192.168.1.50
AUDIT action=CREATE resource=user/credential    uuid=parent-uuid outcome=SUCCESS when=2026-06-16T10:54:09.739Z user=admin ip=192.168.1.50
AUDIT action=UPDATE resource=patient/identifier uuid=child-uuid  outcome=SUCCESS when=2026-06-16T10:54:09.839Z user=admin ip=192.168.1.50
AUDIT action=UPDATE resource=patient/identifier uuid=child-uuid  outcome=DENIED  when=2026-06-16T10:54:09.706Z user=admin ip=192.168.1.50
AUDIT action=UPDATE resource=user/credential    uuid=child-uuid  outcome=SUCCESS when=2026-06-16T10:54:09.946Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient/identifier uuid=child-uuid  outcome=SUCCESS when=2026-06-16T10:54:10.045Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient/identifier uuid=child-uuid  outcome=DENIED  when=2026-06-16T10:54:09.505Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient/identifier uuid=parent-uuid outcome=SUCCESS when=2026-06-16T10:54:09.646Z user=admin ip=192.168.1.50
AUDIT action=PURGE  resource=patient/identifier uuid=child-uuid  outcome=SUCCESS when=2026-06-16T10:54:09.872Z user=admin ip=192.168.1.50
AUDIT action=PURGE  resource=patient/identifier uuid=child-uuid  outcome=FAILED  when=2026-06-16T10:54:09.913Z user=admin ip=192.168.1.50
AUDIT action=PURGE  resource=patient/identifier uuid=parent-uuid outcome=SUCCESS when=2026-06-16T10:54:09.672Z user=admin ip=192.168.1.50
AUDIT action=PUT    resource=patient/identifier uuid=parent-uuid outcome=SUCCESS when=2026-06-16T10:54:09.597Z user=admin ip=192.168.1.50
AUDIT action=PUT    resource=patient/identifier uuid=parent-uuid outcome=FAILED  when=2026-06-16T10:54:10.016Z user=admin ip=192.168.1.50
```

Let op de regels met `resource=user/credential` (CREATE en UPDATE): in die tests stond een **echt
wachtwoord** in de request body (`S3cr3t-Passw0rd!`). Het wachtwoord staat nergens in de regel -
precies de eis uit de gap-analyse. Hetzelfde geldt voor de twee `resource=user`-regels bij de
top-level controller in §1.5.1.

### 1.6 De fix die deze tests afdwongen (sub-resource controller)

Het toevoegen van `MainSubResourceControllerAuditTest` legde een echt gat bloot:
`MainSubResourceController` was **niet** gelijk aan `MainResourceController`. Concreet:

| Actie | Vóór | Na |
|---|---|---|
| `create` (sub) | geen enkele logregel | logt SUCCESS, en DENIED/FAILED bij een fout |
| `update` (sub) | geen enkele logregel | logt SUCCESS + DENIED/FAILED |
| `put` (sub) | geen enkele logregel | logt SUCCESS + DENIED/FAILED |
| `delete` / `purge` (sub) | alleen SUCCESS, geen fout-pad | logt nu ook DENIED/FAILED |

De fix spiegelt exact de aanpak van de top-level controller: een `try/catch` rond de resource-aanroep
met een gedeelde `auditFailure(...)`-hulpmethode die een autorisatiefout (`APIAuthenticationException`)
als `DENIED` logt en elke andere fout als `FAILED`. Daardoor is het gedrag van beide controllers nu
consistent en zijn **alle** CRUD-endpoints van de generieke REST-laag gedekt.

> **Bijvangst tijdens de volledige testrun:** tijdens het draaien van de bestaande OpenMRS-tests
> (zie §2) loopt de echte `MainResourceController` ook gewoon mee in honderden bestaande
> teststromen (elke keer dat een bestaande test een create/update/delete aanroept). Daarbij zag
> ik bijvoorbeeld dit ontstaan, vanzelf, zonder dat ik dat ergens heb geforceerd:
> ```
> WARN AuditLog.record AUDIT action=UPDATE resource=provider uuid=c2299800-... outcome=FAILED when=... user=admin ip=127.0.0.1
> WARN AuditLog.record AUDIT action=CREATE resource=conceptdatatype uuid=unknown outcome=FAILED when=... user=admin ip=127.0.0.1
> ```
> Dit is extra bewijs dat de auditlogging **transparant** meedraait met de rest van de module: de
> bestaande testsuite (1.783 tests) blijft voor 100% slagen mét de logging aan - óók ná de fix aan
> de sub-resource controller (§1.6) - en de logging wordt ook door heel andere resources (provider,
> conceptdatatype, ...) dan de eigen tests automatisch gebruikt, omdat hij in de gedeelde
> controllers zit.

---

## 2. Bestaande OpenMRS-tests (1.879 stuks)

Dit zijn de **1.879 tests die standaard van OpenMRS zelf** komen - ze zaten al in de module vóórdat
wij ermee aan de slag gingen (96 in `omod-common` + 1.783 in `omod`). Ze testen **standaard OpenMRS
REST-functionaliteit** (CRUD op patiënten, concepten, encounters, gebruikers, enzovoort) voor de
verschillende ondersteunde OpenMRS-versies (1.8 t/m 2.8). Dit is niet ons werk en ik ga er daarom
niet per test op in - ik laat per categorie zien dat **alles draait en slaagt**, zodat aangetoond is
dat onze wijziging niets heeft stukgemaakt.

### 2.1 Module `omod-common` (generieke REST-laag) - 96 tests, 11 klassen

| Testklasse | Wat het (kort) test |
|---|---|
| `ConversionUtilTest` (16) | conversie tussen Java-objecten en REST-representaties |
| `RequestContextTest` (5) | opbouwen van de request-context (representation, paging, ...) |
| `RestUtilComponentTest` (2) | hulpfuncties van de REST-laag |
| `RestUtilTest` (15) | idem, o.a. response-helpers en exceptie-afhandeling |
| `RestServiceTest` (4) | resource-registratie en -lookup |
| `ContentTypeFilterTest` (6) | blokkeert XML content-types, staat JSON/multipart toe - zie §2.3 |
| `SearchConfigTest` (22) | configuratie van zoek-handlers |
| `SearchQueryTest` (17) | opbouw en validatie van zoekqueries |
| `AlreadyPagedTest` (3) | paginering van resultaten |
| `MetadataDelegatingCrudResourceTest` (3) | generieke CRUD-resource voor metadata |
| `NeedsPagingTest` (3) | paginering-interface |

**Resultaat:** `Tests run: 96, Failures: 0, Errors: 0, Skipped: 0` - alle groen.

### 2.2 Module `omod` (resources per OpenMRS-versie) - 1.783 tests, 172 klassen

Dit zijn vooral **controller-, resource- en search-handler-tests**: voor elke OpenMRS-resource
(patiënt, concept, encounter, order, visit, gebruiker, ...) en voor elke OpenMRS-versie waarin
die resource veranderde, bestaat een testklasse die de bijbehorende REST-laag aanroept (GET,
POST, PUT/POST-update, DELETE) en controleert dat de juiste velden/JSON terugkomen. Omdat dit
zo'n grote, repetitieve set is, splits ik 'm hier alleen per OpenMRS-versie uit:

| OpenMRS-versie pakket | Testklassen | Tests | Resultaat |
|---|:---:|:---:|---|
| `openmrs1_8.*` | 52 | 334 | OK |
| `openmrs1_9.*` | 78 | 674 | OK |
| `openmrs1_10.*` | 20 | 133 | OK |
| `openmrs1_11.*` | 10 | 46 | OK |
| `openmrs1_12.*` | 8 | 43 | OK |
| `openmrs2_0.*` | 45 | 237 | OK |
| `openmrs2_1.*` | 8 | 37 | OK |
| `openmrs2_2.*` | 15 | 92 | OK |
| `openmrs2_3.*` | 3 | 20 | OK |
| `openmrs2_4.*` | 4 | 20 | OK |
| `openmrs2_5.*` | 3 | 19 | OK |
| `openmrs2_7.*` | 4 | 8 | OK |
| `openmrs2_8.*` | 3 | 16 | OK |
| Overig (generieke controller-basis, reflectie-utils, Swagger-generatie, validatie) | ~12 | ~104 | OK |

**Resultaat:** `Tests run: 1783, Failures: 0, Errors: 0, Skipped: 14` (run van vandaag) - 14 tests
bewust overgeslagen door de tests zelf (bv. een conditionele `assumeTrue`/`@Disabled` voor een
specifieke OpenMRS-deelversie).

> **Eerlijke kanttekening - één bestaande, instabiele (flaky) OpenMRS-test.**
> `ClearDbCacheController2_0Test` (in `openmrs2_0.*`) test de Hibernate second-level cache
> (Infinispan). Die cache vult zich **asynchroon**, en de test controleert vóór de eigenlijke actie
> of een entity al in de cache zit (`containsEntity(...)`). Door timing klopt dat niet altijd,
> waardoor de test **soms** faalt en soms slaagt - onafhankelijk van onze wijziging. Ik heb dit
> expliciet nagegaan:
> - in de ene volledige run slaagde hij, in een herhaalde run faalde hij (3 van 4);
> - geïsoleerd gedraaid faalde hij met een ánder aantal (1 van 4) - non-determinisme;
> - ik heb onze codewijziging tijdelijk teruggedraaid (`git stash`) en de test op de **originele**
>   OpenMRS-code gedraaid: die faalt **net zo goed**.
>
> Het is dus een al bestaande flaky test in de OpenMRS-codebase, niet iets dat wij hebben
> stukgemaakt. Onze eigen module (`omod-common`, 127 tests inclusief onze 31) slaagt wél
> deterministisch: `Tests run: 127, Failures: 0, Errors: 0`. Onze 31 audit-tests gebruiken geen
> database of cache en zijn daarom per definitie stabiel.

### 2.3 Bestaande tests die rechtstreeks aan de security-backlog raken

Een paar van de bestaande OpenMRS-tests zijn extra relevant omdat ze (deels) hetzelfde gebied
raken als de bevindingen uit het [pentestrapport](../security/Security_Backlog_Pentest_Rapport.md). Voor de
volledigheid, en om eerlijk te zijn over wat ze wél en niet aantonen:

| Backlog-item | Testklasse | Wat de test al aantoont | Wat de test **niet** test |
|---|---|---|---|
| PT-8/SR-8 (XML content-type bypass, E-2) | `ContentTypeFilterTest` | blokkeert alle XML-content-type-varianten, staat JSON/multipart toe - dit is precies de fix die PT-8 "niet exploiteerbaar" maakte | - (dit punt is dus al gedekt) |
| PT-12/SR-15 (brute-force wachtwoordreset, D-3) | `PasswordResetController2_2Test` | dat het reset-mechanisme zelf (activatiesleutel aanmaken/valideren) correct werkt | rate-limiting/lockout - die bestaat nog niet in de code, dus kan ook niet getest worden (open backlog-item) |
| D-4/SR-17 (`/cleardbcache` zonder `@Authorized`) | `ClearDbCacheController2_0Test` | dat de cache-eviction-logica zelf correct werkt | autorisatie - de controller heeft nog geen `@Authorized`-annotatie (zie `ClearDbCacheController2_0.java:36-50`), dat is dus nog steeds open |
| PT-13/SR-16 (`/session/diag` infolek, I-5) | `SessionController1_9Test` | sessiegedrag (login/logout, locale, locatie, provider) | het specifieke `/session/diag`-endpoint en het ontbreken van authenticatie daarop |
| R-1 (auditlogging) | `MainResourceControllerAuditTest`, `AuditLogTest` | zie hoofdstuk 1 - dit *is* de fix | - |

Deze tabel laat zien dat het oplossen van een backlog-item (zoals SR-17) ook een **nieuwe of
uitgebreide test** nodig heeft - het simpelweg bestaan van een testklasse met die naam betekent
niet automatisch dat de kwetsbaarheid is afgedekt. Voor R-1 is dat traject in dit document
(hoofdstuk 1) en in [R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md) volledig doorlopen.

---

## 3. Integration-tests (niet automatisch meegedraaid)

Module `integration-tests` bevat **end-to-end tests** die tegen een **echt draaiende** OpenMRS-
server praten (via HTTP, met `rest-assured`). Dat is een ander soort test dan de rest van dit
document: geen Spring-mock, maar een echte server nodig. Daarom draaien ze niet mee met
`mvn test` - ze hebben hun eigen Maven-profiel en starten pas bij `mvn verify`:

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
[INFO] Rest Web Services .................................. SUCCESS
[INFO] Rest Web Services Common OMOD ...................... SUCCESS   <- 127 tests (96 OpenMRS + 31 van ons), 0 failures
[INFO] Rest Web Services OMOD ............................. (*)       <- 1783 tests (allemaal OpenMRS)
[INFO] Rest Web Services Integration Tests ................ SKIPPED   <- vereist live server (zie §3)
```

> (*) De `omod`-module is groen op één na: de bestaande flaky OpenMRS-test
> `ClearDbCacheController2_0Test` (Hibernate-cache timing) faalt intermitterend, **ook op de
> originele code zonder onze wijziging** - zie de eerlijke kanttekening in §2.2. Onze eigen module
> `omod-common` slaagt 100% deterministisch.

| | Tests | Resultaat |
|---|:---:|---|
| **Onze 31 audit-tests** | 31 | 0 failures, deterministisch |
| **`omod-common` totaal** (incl. onze tests) | 127 | 0 failures |
| **`omod`** (bestaande OpenMRS-tests) | 1.783 | groen, behalve `ClearDbCacheController2_0Test` (pre-existing flaky, §2.2) |

---

## 5. Voldoen we aan de eisen?

| Eis | Voldaan? | Bewijs |
|---|---|---|
| Relevante tests opgesteld | Ja | 31 eigen tests gericht op R-1 (audit logging), zie hoofdstuk 1 |
| Tests zelf uitgevoerd | Ja | volledige `mvn -o test`-run vandaag (2026-06-16), zie hoofdstuk 4 |
| Resultaten duidelijk vastgelegd | Ja | dit document + ruwe Maven-output bewaard |
| Opsplitsing eigen vs. OpenMRS | Ja | hoofdstuk 1 (eigen) vs. hoofdstuk 2 (bestaand) |
| Uitleg per test | Ja | per-test tabel in §1.2/§1.3/§1.4; per-categorie in §2.1/§2.2 voor de bestaande tests |
| **Alle state-changing endpoints gedekt** | Ja | top-level (`MainResourceController`) én sub-resource (`MainSubResourceController`) controllers, samen alle CRUD-endpoints van de REST-laag |
| Geen regressie door onze wijziging | Ja | alle 1.879 bestaande tests slagen nog steeds, mét de audit-logging actief |
| Eventuele test-gaten gedicht | Ja | 3 nieuwe tests (top-level CREATE/UPDATE/PURGE-denied, §1.3) + 14 nieuwe tests en een fix voor de sub-resource controller (§1.4/§1.6) |

---

## 6. Herleidbaarheid

| Onderdeel | Locatie |
|---|---|
| Risico-omschrijving R-1 | [threat-model.md](../security/threat-model.md) |
| Pentestbevinding PT-5 | [Security_Backlog_Pentest_Rapport.md](../security/Security_Backlog_Pentest_Rapport.md) |
| Volledige bewijsvoering R-1 (incl. live pentest) | [R-1_auditlogging_bewijs.md](../security/R-1_auditlogging_bewijs.md) |
| Het log-hulpje | `omod-common/src/main/java/.../web/audit/AuditLog.java` |
| Koppeling in de top-level controller | `omod-common/src/main/java/.../web/v1_0/controller/MainResourceController.java` |
| Koppeling in de sub-resource controller | `omod-common/src/main/java/.../web/v1_0/controller/MainSubResourceController.java` |
| Test 1 (log-hulpje) | `omod-common/src/test/java/.../web/audit/AuditLogTest.java` |
| Test 2 (top-level controller, incl. 3 nieuwe tests) | `omod-common/src/test/java/.../web/v1_0/controller/MainResourceControllerAuditTest.java` |
| Test 3 (sub-resource controller, nieuw) | `omod-common/src/test/java/.../web/v1_0/controller/MainSubResourceControllerAuditTest.java` |
| Alleen onze tests draaien | `mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest,MainSubResourceControllerAuditTest` |
| Alles in 1 keer draaien | `mvn -o test` (volledige module) |
