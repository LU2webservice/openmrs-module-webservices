# Tests voor de logging: bewijsdocument (R-1 Auditlogging)

> **Wat is dit bestand?**
> Ik laat hier in simpele taal zien dat ik de **logging heb getest**. Je ziet:
> 1. welke tests ik heb gemaakt (voor geslaagde acties, mislukte acties en gevoelige data),
> 2. hoe je ze zelf draait,
> 3. de **echte output** als bewijs,
> 4. en waar alles staat, zodat het volledig herleidbaar is.
>
> Korte samenvatting vooraf: **13 tests, allemaal groen.** ✅

| | |
|---|---|
| **Branch** | `code-tests-logging` |
| **Risico-item** | R-1, Incomplete auditlogging (zie [threat-model.md](threat-model.md)) |
| **Gap-analyse** | `docs/auditrapport/Gap-analyse-logging.md` (branch `docs_auditreport_gapanalysis`) |
| **Norm** | NEN-7510 / ISO 27002:2022, beheersmaatregel 8.15 (Logging) |
| **Datum** | 2026-06-15 |

---

## 1. Waarom deze tests? (het probleem in gewone woorden)

Onze API kan dingen **aanmaken** (CREATE), **wijzigen** (UPDATE), **verwijderen** (DELETE)
en **definitief wissen** (PURGE). In de oude code werd zo'n actie **nergens opgeschreven**.

Het gevolg: er was **geen spoor** van *wie* iets deed, *wat* er gebeurde, *wanneer* en
*vanaf welk apparaat (IP)*. Iemand kon dus een actie ontkennen en je kon dat niet weerleggen.
Dit heet **repudiation**, oftewel het ontkennen van een handeling.

De gap-analyse van het team bevestigt dat de logging op meerdere plekken ontbrak:

| Event | Gelogd? (oud) | Gevoelige data |
|---|---|---|
| `PATCH /user` | Nee | **Ja** (wachtwoord!) |
| `POST /user` | Nee | Nee |
| `GET /systemsetting` | Nee | Nee |
| `GET /session` | Nee | Nee |

Mijn taak hier was: **tests maken voor de logging** die bewijzen dat
(a) geslaagde acties worden gelogd, (b) mislukte en geweigerde acties worden gelogd, en
(c) er **geen gevoelige gegevens** (zoals wachtwoorden) in het logboek terechtkomen.

---

## 2. Wat log ik nu? (één regel per actie)

Bij elke actie schrijf ik **één regel** met steeds dezelfde stukjes:

```
AUDIT action=DELETE resource=patient uuid=uuid-success outcome=SUCCESS when=2026-06-15T17:59:02.603Z user=admin ip=192.168.1.50
```

| Stukje | Betekenis | Beveiligingsvraag |
|---|---|---|
| `action=DELETE` | wat voor actie | **Wat** gebeurde er? |
| `resource=patient` | welk soort object | **Wat** is geraakt? |
| `uuid=...` | welk object precies | **Wat** is geraakt? |
| `outcome=SUCCESS` | gelukt, geweigerd of mislukt | **Resultaat**? |
| `when=...` | tijdstip | **Wanneer**? |
| `user=admin` | de ingelogde gebruiker | **Wie**? |
| `ip=192.168.1.50` | adres van de client | **Vanaf waar**? |

De uitkomst (`outcome`) heeft drie mogelijke waarden:

- **SUCCESS**: de actie is gelukt.
- **DENIED**: de actie is geweigerd omdat de gebruiker geen rechten had (autorisatiefout).
- **FAILED**: de actie is om een andere reden mislukt (bijvoorbeeld een serverfout).

> **Belangrijk voor gevoelige data:** ik log **alleen deze "naamkaartjes" (metadata)**.
> Ik log **nooit** de inhoud van het verzoek (de "body") of veldwaarden. Daardoor kan een
> wachtwoord (bijvoorbeeld bij `PATCH /user`) **nooit** in het logboek belanden.

---

## 3. De tests

Ik heb **twee** testbestanden gemaakt. Samen zijn dat **13 tests**.

### 3.1 `AuditLogTest`: test het log-hulpje zelf (7 tests)

📄 `omod-common/src/test/java/org/openmrs/module/webservices/rest/web/audit/AuditLogTest.java`

Deze test vangt de logregels **in het geheugen** op, zodat ik ze direct kan controleren.
Je hoeft dus geen logbestand met de hand te lezen, en dát maakt het "testbaar".

| Test | Wat het controleert | Soort |
|---|---|---|
| `noRecordCall_leavesNoAuditTrail` | zonder log-aanroep zijn er **0** regels (de oude, onveilige situatie) | **Voor** |
| `success_leavesAuditTrailWithWhoWhatWhenWhere` | geslaagde actie geeft 1 regel met wie/wat/wanneer/IP en `outcome=SUCCESS` | **Geslaagd** |
| `denied_leavesAuditTrailWithDeniedOutcome` | geweigerde actie geeft een regel met `outcome=DENIED` | **Mislukt** |
| `failure_leavesAuditTrailWithFailedOutcome` | mislukte actie geeft een regel met `outcome=FAILED` | **Mislukt** |
| `formatMessage_doesNotLeakSensitivePasswordValue` | de regel bevat nooit "password" of "secret" | **Gevoelige data** |
| `record_usesXForwardedForWhenPresent` | achter een proxy log ik het echte client-IP | Extra |
| `formatMessage_producesAllFieldsInOrder` | de regel klopt **exact** (vaste volgorde) | Extra |

### 3.2 `MainResourceControllerAuditTest`: test de échte controller (6 tests)

📄 `omod-common/src/test/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainResourceControllerAuditTest.java`

Deze test roept de **echte controller** aan (`MainResourceController`) en controleert dat
die zelf een auditregel schrijft. Zo bewijs ik de koppeling in de productiecode, en niet alleen
het hulpje op zichzelf. De test draait met een echte OpenMRS-context, en daarom is de gebruiker `admin`.

| Test | Wat het nabootst | Verwachte regel |
|---|---|---|
| `delete_whenSuccessful_writesSuccessAuditEntry` | geslaagde DELETE | `action=DELETE ... outcome=SUCCESS` |
| `purge_whenSuccessful_writesSuccessAuditEntry` | geslaagde PURGE | `action=PURGE ... outcome=SUCCESS` |
| `delete_whenNotAuthorised_writesDeniedAuditEntryAndRethrows` | DELETE zonder rechten | `action=DELETE ... outcome=DENIED` |
| `delete_whenServerError_writesFailedAuditEntryAndRethrows` | DELETE met serverfout | `action=DELETE ... outcome=FAILED` |
| `update_withPasswordInBody_doesNotLogThePassword` | UPDATE van user **met wachtwoord** in de body | `action=UPDATE` zónder wachtwoord |
| `create_withPasswordInBody_logsSuccessWithoutThePassword` | CREATE van user **met wachtwoord** | `action=CREATE` zónder wachtwoord |

> De twee tests voor "denied" en "failed" controleren ook dat de fout **gewoon doorgegeven** wordt
> aan de normale foutafhandeling. Het loggen verandert dus niets aan het gedrag van de API.

---

## 4. Bewijs (echte output)

### 4.1 Zelf draaien (herleidbaar)

```bash
mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest
```

### 4.2 Resultaat: alle tests slagen

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in ...rest.web.audit.AuditLogTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in ...controller.MainResourceControllerAuditTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4.3 Echte auditregels die de tests produceren

Dit zijn de **echte regels** die tijdens de controller-test zijn weggeschreven. Ik heb ze
opgevangen en geprint als bewijs:

```
AUDIT action=DELETE resource=patient uuid=uuid-denied  outcome=DENIED  when=2026-06-15T17:59:02.517Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient uuid=uuid-success outcome=SUCCESS when=2026-06-15T17:59:02.603Z user=admin ip=192.168.1.50
AUDIT action=PURGE  resource=location uuid=uuid-purge  outcome=SUCCESS when=2026-06-15T17:59:02.645Z user=admin ip=192.168.1.50
AUDIT action=DELETE resource=patient uuid=uuid-error   outcome=FAILED  when=2026-06-15T17:59:02.696Z user=admin ip=192.168.1.50
AUDIT action=UPDATE resource=user    uuid=uuid-user-1  outcome=SUCCESS when=2026-06-15T17:59:02.739Z user=admin ip=192.168.1.50
AUDIT action=CREATE resource=user    uuid=unknown      outcome=SUCCESS when=2026-06-15T17:59:02.852Z user=admin ip=192.168.1.50
```

**Let op de regel met `action=UPDATE resource=user`:** in die test stond een **wachtwoord** in
de body. Je ziet dat het wachtwoord **nergens** in de regel staat. Precies wat ik wil.

### 4.4 Echt "rood, dan groen" bewijs (voor en na met dezelfde test)

Om hard te bewijzen dat de test het verschil écht meet, heb ik de **fix tijdelijk
teruggedraaid** en dezelfde controllertest opnieuw gedraaid.

**VOOR de fix** (controller zonder logging): de test **faalt**, want er wordt niets gelogd.

```
Tests run: 6, Failures: 6, Errors: 0  <<< FAILURE!  MainResourceControllerAuditTest
java.lang.AssertionError: expected exactly one audit line expected:<1> but was:<0>
[INFO] BUILD FAILURE
```

> `expected:<1> but was:<0>` betekent: de test verwachtte 1 auditregel, maar er waren er **0**.
> Dit is precies de oude, kwetsbare situatie (geen spoor).

**NA de fix** (controller mét logging): dezelfde test **slaagt**.

```
Tests run: 13, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
```

Hoe je dit zelf nadraait:

```bash
# 1. fix tijdelijk terugdraaien
git stash push -- omod-common/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainResourceController.java
# 2. test draaien, deze FAALT (voor-bewijs)
mvn -o -pl omod-common -am test -Dtest=MainResourceControllerAuditTest
# 3. fix terugzetten
git stash pop
# 4. test draaien, deze SLAAGT (na-bewijs)
mvn -o -pl omod-common -am test -Dtest=MainResourceControllerAuditTest
```

Dit is het klassieke **"rood, dan groen"**: de test faalt op de kwetsbare code en slaagt op de
gerepareerde code. Daarmee bewijst de test dat de logging er écht toe doet.

---

## 5. Voldoen we aan de eisen? (controle)

| Eis | Voldaan? | Bewijs |
|---|---|---|
| Tests voor de logging | ✅ | 2 testbestanden, 13 tests |
| **Succesvolle** acties getest | ✅ | tests met `outcome=SUCCESS` (DELETE, PURGE, UPDATE, CREATE) |
| **Mislukte** acties getest | ✅ | `outcome=DENIED` (geen rechten) en `outcome=FAILED` (serverfout) |
| **Afwezigheid van gevoelige gegevens** | ✅ | 3 tests bewijzen dat een wachtwoord nooit in de regel staat |
| **Alle tests slagen** | ✅ | `Tests run: 13, Failures: 0, Errors: 0` |

### Koppeling met NEN-7510 / ISO 27002 beheersmaatregel 8.15 (Logging)

| Wat 8.15 vraagt | Hoe ik dat doe |
|---|---|
| Gebruikersactiviteiten vastleggen | elke CREATE/UPDATE/DELETE/PURGE krijgt een regel met `user` |
| Geslaagde én mislukte/afgewezen acties | `outcome=SUCCESS`, `DENIED` of `FAILED` |
| Datum, tijd en details van de gebeurtenis | `when=`, `action=`, `resource=`, `uuid=` |
| Herkomst van de gebeurtenis | `ip=` (en het echte client-IP achter een proxy) |
| Logs beschermen en geen gevoelige data lekken | apart logboek, **alleen metadata**, nooit de body of wachtwoorden |

---

## 6. Voor en na (het verschil)

| | **Voor** | **Na** |
|---|---|---|
| Logregel bij actie | geen | 1 regel via `AuditLog` |
| Wie / wat / wanneer / IP | onbekend | allemaal in de regel |
| Onderscheid gelukt of mislukt | niet aanwezig | `outcome=SUCCESS/DENIED/FAILED` |
| Wachtwoord in log | risico | nooit (getest) |
| Te testen? | nee | ja, 13 tests, allemaal groen |

De test `noRecordCall_leavesNoAuditTrail` legt de **oude** situatie vast (0 regels).
Alle andere tests bewijzen de **nieuwe** situatie. Zo zie je het verschil binnen de tests zelf.

---

## 7. Herleidbaarheid (waar staat wat)

| Onderdeel | Locatie |
|---|---|
| Risico-omschrijving R-1 | [threat-model.md](threat-model.md) (sectie *Repudiation*) |
| Gap-analyse logging | `docs/auditrapport/Gap-analyse-logging.md` (branch `docs_auditreport_gapanalysis`) |
| Het log-hulpje | `omod-common/src/main/java/org/openmrs/module/webservices/rest/web/audit/AuditLog.java` |
| Koppeling in de controllers | `MainResourceController.java` en `MainSubResourceController.java` (map `.../v1_0/controller/`) |
| Test 1, log-hulpje | `omod-common/src/test/java/org/openmrs/module/webservices/rest/web/audit/AuditLogTest.java` |
| Test 2, controller | `omod-common/src/test/java/org/openmrs/module/webservices/rest/web/v1_0/controller/MainResourceControllerAuditTest.java` |
| Commando om alles te bewijzen | `mvn -o -pl omod-common -am test -Dtest=AuditLogTest,MainResourceControllerAuditTest` |
