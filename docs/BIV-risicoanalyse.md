# BIV/CIA-risicoanalyse: OpenMRS REST Webservices module

**Project:** fork van `openmrs-module-webservices.rest` (v3.2.0). Dit is een REST-API die patiënt- en
zorggegevens uit OpenMRS beschikbaar maakt onder `/ws/rest/v1/...`.

**Auteur:** Pluk Zwaal · **Datum:** 2026-06-08

## 1. BIV-analyse

De module is de toegangspoort naar alle medische data. We kijken naar de drie doelen (NL: BIV, EN: CIA).

| Dimensie | Wat we zien in dit project | Referentie |
|----------|----------------------------|------------|
| **Vertrouwelijkheid** | Toegang loopt via Basic Auth over de REST-laag. De filter blokkeert zelf niets en vertrouwt op de privilege-checks eronder. Credentials zijn alleen base64, dus TLS is verplicht. | [AuthorizationFilter.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java) |
| **Integriteit** | Volledige CRUD, inclusief purge (hard verwijderen). Een onbevoegde schrijfactie raakt direct het medisch dossier, bijvoorbeeld een medicatie-order. | resource-`api/` interfaces (`Updatable`, `Purgeable`) |
| **Beschikbaarheid** | Eén centrale ingang voor alle integraties. Er is geen zichtbare rate-limiting, dus zware `?v=full`-queries kunnen het systeem overbelasten (DoS). | [RestConstants.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/RestConstants.java) |

## 2. Kroonjuwelen (met referenties)

De gegevens met de hoogste waarde, met verwijzing naar het endpoint in de broncode.

| Kroonjuweel | AVG-classificatie | Referentie |
|-------------|-------------------|------------|
| **Observaties, consulten en medicatie** (Obs, Encounter, Order) | Gezondheidsgegevens (AVG art. 9) | [ObsResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/ObsResource1_8.java) · [EncounterResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/EncounterResource1_8.java) · [OrderResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/OrderResource1_8.java) |
| **Patiënt- en persoonsgegevens** (NAW, identifiers) | Persoonsgegevens (PII) | [PatientResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/PatientResource1_8.java) · [PatientIdentifierResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/PatientIdentifierResource1_8.java) |
| **Gebruikers, rollen en privileges** | Authenticatie / IAM | [UserResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/UserResource1_8.java) · [RoleResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/RoleResource1_8.java) |
| **Secrets** (DB-wachtwoorden en dergelijke) | Secrets | `example.env` / `.env` ([README](../README.md)) |

Wie het IAM-domein of de secrets in handen krijgt, heeft daarmee indirect toegang tot alle andere
kroonjuwelen.

## 3. Risicocriteria en scoreschaal

We gebruiken dezelfde schaal als onze [risicomatrix](riskassesment/risk-matrix.md): elk risico krijgt een **kans**
(cijfer 1 t/m 5) en een **impact** (letter A t/m E). De impact is de zwaarste van de drie BIV-dimensies.
De code is de combinatie van beide, bijvoorbeeld **E5** (hoogste) of **B2** (laag).

**Kans (cijfer):**

| Cijfer | Kans | Betekenis |
|:------:|------|-----------|
| 5 | Zeer waarschijnlijk | Heel makkelijk, vaak zonder login uit te voeren |
| 4 | Waarschijnlijk | Bekende techniek, lage drempel |
| 3 | Mogelijk | Realistisch onder bepaalde voorwaarden |
| 2 | Onwaarschijnlijk | Vergt specifieke kennis of toegang |
| 1 | Zeer onwaarschijnlijk | Vrijwel alleen theoretisch |

**Impact (letter):**

| Letter | Impact | Betekenis (B / I / V) |
|:------:|--------|------------------------|
| E | Catastrofaal | Volledige systeemovername of grootschalig lek van medische data; gevaar voor de patiënt |
| D | Zeer serieus | Gezondheidsgegevens van meerdere patiënten gelekt of gewijzigd; meldplichtig datalek |
| C | Serieus | Persoonsgegevens (PII) betrokken of merkbare uitval |
| B | Minder ernstig | Beperkte gevolgen, makkelijk te herstellen |
| A | Verwaarloosbaar | Nauwelijks gevolgen |

**Risicomatrix (impact x kans):**

| Impact \ Kans | 1 | 2 | 3 | 4 | 5 |
|---|:--:|:--:|:--:|:--:|:--:|
| **E Catastrofaal** | Hoog | Hoog | Kritiek | Kritiek | Kritiek |
| **D Zeer serieus** | Middel | Hoog | Hoog | Kritiek | Kritiek |
| **C Serieus** | Middel | Middel | Middel | Hoog | Hoog |
| **B Minder ernstig** | Laag | Laag | Middel | Middel | Hoog |
| **A Verwaarloosbaar** | Laag | Laag | Laag | Laag | Middel |

## 4. Risicoregister (toegepast)

De risico's uit onze [risicomatrix](riskassesment/risk-matrix.md), met dezelfde STRIDE-ID's en codes. De BIV-kolom
geeft aan welke dimensie het zwaarst geraakt wordt.

| ID | Risico | BIV | Code | Klasse |
|----|--------|:---:|:----:|--------|
| I-2 | Unauthenticated systeeminstellingen opvraagbaar | V | E5 | Kritiek |
| S-1 | Basic Auth onderschepping (base64, geen TLS afgedwongen) | V | D4 | Kritiek |
| T-1 | Mass assignment bij het schrijven van resources | I | D4 | Kritiek |
| E-1 | Privilege-escalatie via `/user` | V | D4 | Kritiek |
| S-2 | Sessie-hijacking | V | C3 | Middel |
| T-2 | Inputvalidatie / injectie | I | C3 | Middel |
| R-1 | Incomplete auditlogging | I | C3 | Middel |
| D-1 | Onbeperkte resultaatsets (geen rate-limiting) | B | C3 | Middel |
| E-2 | XML Content-Type bypass | V | C3 | Middel |
| I-1 | Stack traces in responses | V | B3 | Middel |
| I-3 | Gevoelige data via `?v=full` | V | B2 | Laag |
| D-2 | Async herstart-misbruik | B | B2 | Laag |

De hoogste risico's (I-2, S-1, T-1, E-1) zijn verder uitgewerkt in de [gap-analyses](auditrapport/01-gap-analyse.md).

## 5. Risicobereidheid en grenswaarden

Omdat het om bijzondere persoonsgegevens gaat, houden we een **lage risicobereidheid** aan.

| Klasse | Codes (voorbeeld) | Beslisregel |
|--------|-------------------|-------------|
| Kritiek | E5, E4, E3, D5, D4 | Niet acceptabel. Direct oplossen, geen deploy tot het lager is. |
| Hoog | E2, E1, D3, D2, C5, C4, B5 | Boven de grens. Oplossen verplicht binnen een afgesproken termijn. |
| Middel | C3, C2, C1, B4, B3, A5 | Voorwaardelijk. Oplossen waar het redelijk kan; anders bewust accepteren door de eigenaar. |
| Laag | B2, B1, A4 t/m A1 | Acceptabel. In de gaten houden. |

**Harde grenswaarde:** alles in de klasse **Hoog of Kritiek** dragen we niet zonder een vastgelegde
acceptatie door de risico-eigenaar. Raakt een risico **gezondheidsgegevens of IAM/secrets**, dan is
voor de klasse Hoog al meteen een plan van aanpak nodig (geen alleen-accepteren).

## Referenties

- Authenticatie en constanten: [AuthorizationFilter.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java), [RestConstants.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/RestConstants.java)
- Kaders: AVG art. 9 (bijzondere persoonsgegevens) en art. 33 en 34 (meldplicht datalekken), NEN 7510 (informatiebeveiliging in de zorg)
- OpenMRS REST docs: <https://wiki.openmrs.org/display/docs/WebServices>
