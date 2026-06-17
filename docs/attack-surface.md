# Attack Surface Mapping — OpenMRS REST Webservices Module

**Versie:** 3.2.0 (OpenMRS Core 2.8.3+)
**Datum:** 2026-06-12
**Methodiek:** Broncode-analyse (`@GetMapping`/`@PostMapping`/`@RequestMapping`, filters, config.xml) + live tests
**NEN-7510:2024 koppeling:** 8.25 (beveiligen tijdens de ontwikkelcyclus — attack surface mapping in de designfase)
**Bijbehorend threat model:** [threat-model.md](threat-model.md)

---

## 1. Overzicht aanvalsoppervlak

Het aanvalsoppervlak is verdeeld in drie zones op basis van de filterketen die elk verzoek doorloopt.
Hoe eerder een verzoek de filterketen verlaat, hoe groter het risico.

```
INTERNET / INTRANET
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Tomcat (HTTP — geen TLS afgedwongen)                               │
│                                                                     │
│   URL-pad /ws/rest/*          URL-pad /module/*                     │
│        │                            │                               │
│        ▼                            │                               │
│  [GZIPFilter]                       │  ← GEEN filter op /module/*  │
│        │                            │                               │
│        ▼                            │                               │
│  [ContentTypeFilter]                │  ← blokkeert XML (blacklist) │
│        │                            │                               │
│        ▼                            │                               │
│  [AuthorizationFilter]              │  ← ALLEEN op /ws/rest/*      │
│        │                            │                               │
│        ▼                            ▼                               │
│   Zone B + C              !! ZONE A — GEEN FILTER !!               │
│   (authenticated)         (SettingsFormController, swagger.json)   │
└─────────────────────────────────────────────────────────────────────┘
```

> **Kernbevinding:** `AuthorizationFilter` en `ContentTypeFilter` zijn uitsluitend
> gekoppeld aan `url-pattern /ws/rest/*` (config.xml regels 92-103).
> Alle `/module/*` endpoints vallen **volledig buiten de filterketen**.

---

## 2. Attack surface overzicht — alle entry points (deliverable)

> Dit is de kerntabel volgens de opgevraagde kolommen:
> **Endpoint · Methode · Vereiste privilege · Inputvalidatie aanwezig? · Autorisatiecheck aanwezig?**
> De kolom **Risico** markeert hoge-risico ingangen (KRITIEK/HOOG). De zone-analyse in
> §3–§5 geeft de onderbouwing per ingang.

| Risico | Endpoint | Methode | Vereiste privilege | Inputvalidatie aanwezig? | Autorisatiecheck aanwezig? |
|:---:|---|---|---|---|---|
| KRITIEK | `/module/webservices/rest/settings.form` | GET | **Geen** (zou beheer moeten zijn) | Nee — n.v.t. (leesactie) | **Nee** — geen `@Authorized`, geen `isAuthenticated()` |
| KRITIEK | `/module/webservices/rest/settings.form/search` | GET | **Geen** | Nee — n.v.t. | **Nee** — lekt property-waarden incl. secrets |
| HOOG | `/module/webservices/rest/settings.form` | POST | **Geen** | Deels — Spring-binding | **Nee** |
| MIDDEL | `/module/webservices/rest/swagger.json` | GET | **Geen** | Nee — n.v.t. | **Nee** — buiten filterketen |
| MIDDEL | `/module/webservices/rest/apiDocs` | GET | **Geen** | Nee — n.v.t. | **Nee** |
| HOOG | `/ws/rest/v1/cleardbcache` | POST | **Geen** (ontbreekt) | Nee — body niet gevalideerd | **Nee** — `@Authorized` ontbreekt; iedereen kan cache wissen |
| HOOG | `/ws/rest/v1/session` | POST (login) | Geen (intentioneel) | Deels — Basic Auth decode | Deels — authenticatie zelf; geen TLS afgedwongen (S-1) |
| MIDDEL | `/ws/rest/v1/session/diag` | GET | Geen (intentioneel) | Nee — `token`-param genegeerd | Deels — `serverTime` zichtbaar zonder auth (I-5) |
| LAAG | `/ws/rest/v1/session` | DELETE (logout) | Geldige sessie | Nee — n.v.t. | Ja — sessie |
| MIDDEL | `/ws/rest/v1/passwordreset` | POST | Geen (intentioneel) | Deels — e-mail/username | Deels — geen rate-limiting/lockout (D-3) |
| MIDDEL | `/ws/rest/v1/passwordreset/{activationkey}` | POST | Geen (intentioneel) | Deels — activatiesleutel | Deels — geen brute-force-bescherming (D-3) |
| HOOG | `/ws/rest/v1/user`, `/ws/rest/v1/user/{uuid}` | GET/POST/DELETE | `Get/Add/Edit/Manage Users` (via Core) | Deels — Core-validators | Ja — afgedwongen door OpenMRS Core-service (E-1 niet exploiteerbaar) |
| MIDDEL | `/ws/rest/v1/loggedinusers` | GET | `Get Users` (`requirePrivilege`) | Nee — n.v.t. | Ja — `Context.requirePrivilege(GET_USERS)` |
| LAAG | `/ws/rest/v1/password` | POST | `isAuthenticated()` | Deels — wachtwoordsterkte (Core) | Ja |
| MIDDEL | `/ws/rest/v1/{resource}` (obs, encounter, order, patient, …) | GET/POST/DELETE | Domein-privilege via Core (`Get/Edit/Delete <X>`) | Deels — Core-validators (`ValidateUtil`) | Ja — per domeinobject door Core |
| MIDDEL | `/ws/rest/v1/{resource}?q=` | GET | Domein-privilege | Deels — string doorgegeven aan Hibernate (T-2) | Ja |
| MIDDEL | `/ws/rest/v1/hl7` | POST | Domein-privilege | Deels — beperkte HL7-parsing | Ja |

**Legenda inputvalidatie/autorisatie:** Ja = aanwezig · Deels = gedelegeerd aan Core / onvolledig · Nee = afwezig of n.v.t.

### 2.1 Externe inputs buiten de URL-paden

De opdracht vraagt ook niet-endpoint ingangen in kaart te brengen:

| Type ingang | Concreet in deze module | Validatie / risico |
|---|---|---|
| **Gebruikersinvoer (velden)** | `?q=` zoekparameters, JSON request-bodies, `?v=full`/`?limit=` | Gedelegeerd aan Core-validators; `?q=` als string naar Hibernate (T-2) |
| **Bestandsuploads** | `ObsComplex` binaire waarde (`/obs/{uuid}/value`), bytes-resources | Geen type-/inhoudvalidatie in REST-laag; afhankelijk van Core-handler |
| **HTTP-headers** | `Authorization: Basic`, `Cookie: JSESSIONID`, `Content-Type` | Basic Auth zonder TLS (S-1); Content-Type-blacklist te omzeilen (E-2) |
| **Omgevingsvariabelen / config** | `webservices.rest.allowedips`, `webservices.rest.maxResults...`, secrets als global property in `runtime.properties` | Lege `allowedips` = alles toegestaan (TB-2); secrets lekken via `settings.form` (I-2/I-4) |

### 2.2 NEN-7510:2024 — 8.25 gap-analyse per categorie

Maatregel **8.25** ("attack surface mapping in de designfase; secure defaults") vraagt per entry point:
*is er inputvalidatie* en *is er een autorisatiecheck*? De gaps:

| Gap | Entry points | Maatregel 8.25 gap | Backlog-item |
|---|---|---|---|
| Geen autorisatiecheck | `settings.form*`, `cleardbcache`, `swagger.json` | Geen secure default — `/module/*` buiten filter | SR-7, SR-17, SR-18 |
| Geen rate-limiting | `passwordreset*` | Brute-force mogelijk (geen secure default) | SR-15 |
| Recon-info zonder auth | `session/diag` | Onnodige blootstelling | SR-16 |
| Inputvalidatie gedelegeerd | `?q=`, JSON-bodies, ObsComplex-upload | Geen eigen validatielaag; vertrouwt volledig op Core | SR-3 (T-2) |
| Onveilige default | lege `allowedips` | Default = alle IP's toegestaan | config-hardening |

> **Conclusie 8.25:** de grootste gaps zitten in **ontbrekende autorisatiechecks op `/module/*`**
> (geen secure default) en in het **ontbreken van een eigen inputvalidatielaag** — de module
> vertrouwt impliciet op OpenMRS Core (zie trust boundary TB-3 in §7).

---

## 3. Zone A — Buiten filterketen (HOOGSTE RISICO)

Deze endpoints zijn bereikbaar **zonder enige filter** — geen IP-check, geen auth,
geen Content-Type-check.

| # | Endpoint | Methode | Auth vereist? | Risico | Dreiging |
|---|---|---|---|---|---|
| A-1 | `/module/webservices/rest/settings.form` | GET | Geen | **KRITIEK** | I-2 / I-4 |
| A-2 | `/module/webservices/rest/settings.form/search` | GET | Geen | **KRITIEK** | I-2 / I-4 |
| A-3 | `/module/webservices/rest/settings.form` | POST | Geen | HOOG | I-2 |
| A-4 | `/module/webservices/rest/swagger.json` | GET | Geen | MIDDEL | Informatielekkage |
| A-5 | `/module/webservices/rest/apiDocs` | GET | Geen | MIDDEL | Informatielekkage |
| A-6 | `/module/webservices/rest/apiDocs/debug?tag=` | GET | Geen | LAAG | XSS (gesaneerd) |

### A-1 / A-2 — SettingsFormController (KRITIEK)
```java
// SettingsFormController.java:50
@RequestMapping(value = "/search", method = RequestMethod.GET)
public @ResponseBody List<GlobalProperty> searchProperties(...) {
    // Missing auth: no Context.isAuthenticated() check
    // Retourneert property names + values inclusief secrets
}
```
**Impliciet vertrouwd:** alle ingelogde gebruikers krijgen volledige toegang.

### A-4 — swagger.json (NIEUW — niet eerder gedocumenteerd)
`SwaggerSpecificationController` ligt op `/module/webservices/rest/swagger.json`.
Valt buiten `AuthorizationFilter`. Geeft de volledige API-specificatie terug — alle
endpoints, parameters, response-schema's — **zonder authenticatie**. Dit versnelt
aanvalsverkenning aanzienlijk.

---

## 4. Zone B — Authenticatie-endpoints (speciale behandeling)

Deze endpoints vallen binnen `/ws/rest/*` (filter actief) maar zijn **doelbewust
zonder pre-authenticatie** beschikbaar. Dat is functioneel correct maar creëert risico's.

| # | Endpoint | Methode | Auth vereist? | Risico | Dreiging |
|---|---|---|---|---|---|
| B-1 | `/ws/rest/v1/session` | POST (login) | Intentioneel open | HOOG | S-1 (Basic Auth) |
| B-2 | `/ws/rest/v1/session` | GET | Intentioneel open | LAAG | I-1 (stacktrace) |
| B-3 | `/ws/rest/v1/session` | DELETE (logout) | Sessie vereist | LAAG | — |
| B-4 | `/ws/rest/v1/session/diag` | GET | Intentioneel open | MIDDEL | I-5 (serverTime) |
| B-5 | `/ws/rest/v1/passwordreset` | POST (aanvragen) | Intentioneel open | LAAG | — |
| B-6 | `/ws/rest/v1/passwordreset/{activationkey}` | POST (uitvoeren) | Intentioneel open | MIDDEL | D-3 (brute-force) |

### B-4 — /session/diag (schijnbeveiliging bevestigd)
```java
// SessionController1_9.java:170
@RequestMapping(value = "/diag", method = RequestMethod.GET)
public @ResponseBody SimpleObject getDiag() {
    diag.add("authenticated", Context.isAuthenticated());
    diag.add("serverTime", ...);  // ← altijd zichtbaar, ook zonder auth
    if (Context.isAuthenticated()) {
        // rollen/privileges — alleen als ingelogd
    }
}
```
**Live test:** `token`-parameter volledig genegeerd. `serverTime` zichtbaar zonder auth.

---

## 5. Zone C — Geauthenticeerde REST-endpoints (privilege-checks vereist)

Alle endpoints onder `/ws/rest/v1/` vallen achter `AuthorizationFilter`.
Authenticatie is dus gewaarborgd — maar **autorisatie (privilege-check) varieert per endpoint**.

### 5.1 Hoog-risico geauthenticeerde endpoints

| # | Endpoint | Methode | @Authorized? | Risico | Dreiging |
|---|---|---|---|---|---|
| C-1 | `/ws/rest/v1/cleardbcache` | POST | **ONTBREEKT** | **HOOG — NIEUW** | DoS via cache-flush |
| C-2 | `/ws/rest/v1/user` | GET / POST | `GET_USERS` | HOOG | E-1 |
| C-3 | `/ws/rest/v1/user/{uuid}` | POST | aanwezig | HOOG | E-1 |
| C-4 | `/ws/rest/v1/password` | POST | `isAuthenticated()` | MIDDEL | — |
| C-5 | `/ws/rest/v1/loggedinusers` | GET | `GET_USERS` | MIDDEL | Privacy |
| C-6 | `/ws/rest/v1/searchindexupdate` | POST | privilege check | MIDDEL | DoS |
| C-7 | `/ws/rest/v1/hl7` | POST / GET | Geen extra check | MIDDEL | Injectie |

### C-1 — ClearDbCacheController (NIEUW — niet eerder gedocumenteerd)
```java
// ClearDbCacheController2_0.java:37
@RequestMapping(value = "/rest/v1/cleardbcache", method = RequestMethod.POST)
public void clearDbCache(@RequestBody String json) throws Exception {
    // GEEN @Authorized annotatie
    // GEEN Context.isAuthenticated() check
    SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
    sf.getCache().evictAllRegions();  // ← wist ALLE Hibernate cache
}
```
**Impact:** elke ingelogde gebruiker (ook `testuser_laag`) kan de volledige Hibernate-cache
wissen. Na een cache-flush worden alle queries opnieuw tegen de database uitgevoerd —
potentiële DoS bij herhaald aanroepen.

### 5.2 Normale geauthenticeerde resources (144+ endpoints)

| Categorie | Voorbeeld endpoints | AVG-impact |
|---|---|---|
| Klinische data | `/obs`, `/encounter`, `/order` | Gezondheidsgegevens (art. 9) |
| Patiëntdata | `/patient`, `/person`, `/patientidentifier` | PII |
| IAM | `/user`, `/role`, `/privilege` | Authenticatie / autorisatie |
| Beheer | `/concept`, `/location`, `/form` | Operationeel |
| Binaire data | `/obs/{uuid}/value` (ObsComplex) | Bestanden |
| HL7 | `/hl7` | Klinische berichten |

---

## 6. HTTP-aanvalsvectoren (input-oppervlak)

Naast URL-paden zijn ook HTTP-headers en parameters aanvalsvectoren.

| Vector | Waarde | Risico | Dreiging |
|---|---|---|---|
| `Authorization: Basic` header | Base64-encoded credentials | HOOG | S-1 |
| `Cookie: JSESSIONID` | Sessie-token | MIDDEL | S-2 |
| `Content-Type` header | `application/xml`, `text/xml` | MIDDEL | E-2 |
| `?q=` query parameter | Zoekstring | MIDDEL | T-2 |
| `?v=full` parameter | Representatieniveau | LAAG | I-3 |
| `?limit=` parameter | Resultaatlimiet | LAAG | D-1 |
| Request body (JSON POST) | Domeinobject met extra velden | HOOG | T-1 |
| Request body (`cleardbcache`) | JSON met `resource`, `uuid` | HOOG | C-1 nieuw |

---

## 7. Trust boundaries — wat wordt impliciet vertrouwd?

Een trust boundary is een grens waarbij data of een actor vertrouwd wordt
**zonder expliciete verificatie**. Dit zijn de impliciete vertrouwensrelaties in dit systeem.

| # | Wat wordt vertrouwd? | Hoe? | Risico | Opmerking |
|---|---|---|---|---|
| TB-1 | **IP-adres (allowedips)** | `request.getRemoteAddr()` (TCP-niveau) | LAAG | **Veilig:** gebruikt niet X-Forwarded-For — niet spoofbaar via header |
| TB-2 | **Lege IP-allowlist = alle IPs toegestaan** | `if (candidateIps.isEmpty()) return true` | MIDDEL | Default-instelling laat iedereen door als `allowedips` niet geconfigureerd is |
| TB-3 | **OpenMRS Core** | Alle domeinlogica gedelegeerd zonder validatie | MIDDEL | Als Core een fout retourneert, gaat REST mee — geen defensieve validatie |
| TB-4 | **MariaDB via Hibernate** | Queries via ORM, geen SQL-verificatie door REST-laag | LAAG | Hibernate biedt protectie; vertrouwen is gerechtvaardigd |
| TB-5 | **Geauthenticeerde sessie = betrouwbaar** | Na login: volledige session-trust | MIDDEL | Geen per-request re-authenticatie; sessie-hijacking (S-2) geeft volledige toegang |
| TB-6 | **Basic Auth faalt stil** | `AuthorizationFilter` stopt bij fout nooit de keten | HOOG | Ongeldige credentials → geen authenticatie maar de keten loopt door |
| TB-7 | **swagger.json is publiek beschikbaar** | Geen auth op `/module/*` | MIDDEL | Alle endpoint-namen, parameters en schema's zichtbaar voor aanvaller |
| TB-8 | **Elke ingelogde gebruiker mag cache wissen** | `ClearDbCacheController` mist `@Authorized` | HOOG | Authenticated = trusted voor cache-flush (onjuist) |

### TB-2 in detail — standaard alles open
```java
// RestUtil.java:150
public static boolean ipMatches(String ip, List<String> candidateIps) {
    if (candidateIps.isEmpty()) {
        return true;  // ← lege lijst = iedereen toegestaan
    }
    ...
}
```
Als `webservices.rest.allowedips` niet is ingesteld, geldt **geen IP-beperking**.
Dit is de standaard bij een verse installatie.

### TB-6 in detail — silent fail
```java
// AuthorizationFilter.java:33
// "Filter intended for all /ws/rest calls that allows the user to
//  authenticate via Basic Auth. It will not fail on invalid or
//  missing credentials."
```
Ongeldige credentials worden genegeerd — de request gaat door als anonieme gebruiker.
Endpoints die geen auth-check doen zijn daarmee effectief openbaar.

---

## 8. Hoog-risico ingangen samengevat

| Rang | Endpoint | Zone | Risico | Nieuw? |
|---|---|---|---|---|
| 1 | `/module/.../settings.form/search` | A | KRITIEK | — |
| 2 | `/ws/rest/v1/cleardbcache` (geen @Authorized) | C | **HOOG** | Ja |
| 3 | `/module/.../swagger.json` (geen auth) | A | MIDDEL | Ja |
| 4 | `/ws/rest/v1/session` POST (Basic Auth over HTTP) | B | HOOG | — |
| 5 | `/ws/rest/v1/passwordreset/{key}` (geen rate-limiting) | B | MIDDEL | — |
| 6 | `/ws/rest/v1/session/diag` (serverTime zonder auth) | B | MIDDEL | — |
| 7 | Lege `allowedips` (default = iedereen) | TB | MIDDEL | — |
| 8 | Basic Auth faalt stil (TB-6) | TB | HOOG | — |
