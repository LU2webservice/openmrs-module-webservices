# BIV/CIA-risicoanalyse — OpenMRS REST Webservices module

**Project:** fork van `openmrs-module-webservices.rest` (v3.2.0) — een REST-API die patiënt- en zorggegevens
uit OpenMRS ontsluit onder `/ws/rest/v1/…`. **Auteur:** _(vul in)_ · **Datum:** 2026-06-08

---

## 1. BIV-analyse

De module is de **voordeur** naar alle medische data. Beoordeling op de drie doelen (NL: BIV / EN: CIA):

| Dimensie | Bevinding in dit project | Referentie |
|----------|--------------------------|------------|
| **Vertrouwelijkheid** | Toegang via **Basic Auth** over de REST-laag; de filter dwingt zélf niets af en vertrouwt op de onderliggende privilege-checks. Credentials zijn alleen base64 → **TLS verplicht**. | [AuthorizationFilter.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java) |
| **Integriteit** | Volledige CRUD **inclusief purge (hard delete)**. Onbevoegde schrijfacties raken direct medische dossiers (bv. een medicatie-order). | resource-`api/` interfaces (`Updatable`, `Purgeable`) |
| **Beschikbaarheid** | Single point of access voor integraties; **geen zichtbare rate-limiting** → risico op DoS/resource-uitputting via zware `?v=full`-queries. | [RestConstants.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/RestConstants.java) |

---

## 2. Kroonjuwelen (met referenties)

De assets met de hoogste waarde, met verwijzing naar het endpoint in de broncode:

| Kroonjuweel | AVG-classificatie | Referentie |
|-------------|-------------------|------------|
| **Klinische observaties, consulten & medicatie** (Obs, Encounter, Order) | Gezondheidsgegevens (AVG art. 9) | [ObsResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/ObsResource1_8.java) · [EncounterResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/EncounterResource1_8.java) · [OrderResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/OrderResource1_8.java) |
| **Patiënt- & persoonsgegevens** (NAW, identifiers) | Persoonsgegevens (PII) | [PatientResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/PatientResource1_8.java) · [PatientIdentifierResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/PatientIdentifierResource1_8.java) |
| **Gebruikers, rollen & privileges** | Authenticatie / IAM | [UserResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/UserResource1_8.java) · [RoleResource](../omod/src/main/java/org/openmrs/module/webservices/rest/web/v1_0/resource/openmrs1_8/RoleResource1_8.java) |
| **Secrets** (DB-wachtwoorden e.d.) | Secrets | `example.env` / `.env` ([README](../README.md)) |

> Compromittering van het IAM-domein of de secrets geeft indirect toegang tot **alle** andere kroonjuwelen.

---

## 3. Risicocriteria & scoreschaal

**Risico = Kans × Impact.** Impact = het hoogste van de drie BIV-dimensies.

| Score | Kans | Impact |
|:-----:|------|--------|
| 1 | Theoretisch | Geen gevoelige data / korte hinder |
| 3 | Realistisch, bekende techniek | PII betrokken / merkbare uitval |
| 5 | Triviaal, unauthenticated remote | Gezondheidsdata gelekt of systeemovername; patiëntveiligheid in gevaar |

(2 en 4 zijn de tussenliggende niveaus.)

**Risicomatrix (Kans × Impact) en klassen:**

| | I=1 | I=2 | I=3 | I=4 | I=5 |
|---|:--:|:--:|:--:|:--:|:--:|
| **K=5** | 5 | 10 | 15 | 20 | 25 |
| **K=4** | 4 | 8 | 12 | 16 | 20 |
| **K=3** | 3 | 6 | 9 | 12 | 15 |
| **K=2** | 2 | 4 | 6 | 8 | 10 |
| **K=1** | 1 | 2 | 3 | 4 | 5 |

**1–4 Laag** · **5–9 Middel** · **10–14 Hoog** · **15–25 Kritiek**

---

## 4. Risicoregister (toegepast)

De scoreschaal uit §3 toegepast op de belangrijkste risico's van dit project:

| # | Risico | BIV | Kans | Impact | Score | Klasse |
|---|--------|:---:|:----:|:------:|:-----:|--------|
| R1 | Basic Auth zonder afgedwongen TLS → onderschepping van credentials/data | V | 4 | 5 | 20 | Kritiek |
| R2 | Onbevoegde schrijf/purge op medische dossiers (zwakke privilege-check) | I | 3 | 5 | 15 | Kritiek |
| R3 | Datalek gezondheidsgegevens via te ruime `?v=full`-representatie of scraping | V | 3 | 4 | 12 | Hoog |
| R4 | DoS / resource-uitputting door ontbrekende rate-limiting | B | 3 | 3 | 9 | Middel |
| R5 | Lek van secrets uit `.env` (credentials, DB-wachtwoord) | V | 2 | 5 | 10 | Hoog |

---

## 5. Risicobereidheid & grenswaarden

Door de verwerking van **bijzondere persoonsgegevens** geldt een **lage risicobereidheid**.

| Risicoscore | Beslisregel |
|:-----------:|-------------|
| 15–25 (Kritiek) | Niet acceptabel — direct mitigeren, geen deploy tot teruggebracht. |
| 10–14 (Hoog) | Boven de grens — mitigatie verplicht binnen afgesproken termijn. |
| 5–9 (Middel) | Voorwaardelijk — mitigeren waar redelijk; expliciet accepteren door eigenaar. |
| 1–4 (Laag) | Acceptabel — monitoren. |

**Harde grenswaarde:** score **≥ 10** wordt niet zonder gedocumenteerde acceptatie gedragen.
Voor gezondheidsgegevens en IAM/secrets geldt een verlaagde drempel: **≥ 8** vereist al een mitigatieplan.

---

## Referenties
- Authenticatie & constanten: [AuthorizationFilter.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java), [RestConstants.java](../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/RestConstants.java)
- Kaders: AVG art. 9 (bijzondere persoonsgegevens) & art. 33–34 (meldplicht datalekken), NEN 7510 (informatiebeveiliging zorg)
- OpenMRS REST docs: <https://wiki.openmrs.org/display/docs/WebServices>
