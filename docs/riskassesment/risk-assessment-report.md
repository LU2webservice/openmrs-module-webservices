# Risk Assessment Report openmrs-module-webservices.rest

Dit rapport vat de risicobeoordeling van onze module samen. Het bouwt voort op de
scanresultaten uit onze CI/CD-pipeline en op de security backlog (ons risicoregister
met STRIDE-bevindingen). Per kwetsbaarheid staat een mitigatie, gekoppeld aan een
maatregel uit NEN 7510-2:2024, en aan het eind een kostenraming. De kostenraming is een
inschatting van resources, tijd en budget.

De module is een REST-API die patiënt- en zorggegevens uit OpenMRS beschikbaar maakt
onder `/ws/rest/v1/...`. Omdat het om bijzondere persoonsgegevens gaat (AVG art. 9),
houden we een lage risicobereidheid aan.

We baseren ons alleen op wat we in dit project zelf hebben gevonden of in de broncode
kunnen aantonen. Niets is verzonnen: elke bevinding heeft een referentie naar de code
of naar een eerder opgeleverd document.

---

## 1. Bronnen van deze beoordeling

| Bron | Wat het oplevert | Referentie |
|------|------------------|------------|
| SAST (CodeQL) | Statische analyse van onze eigen Java-code. Resultaten staan in de GitHub Security-tab | `.github/workflows/codeql.yml`, [CICD.md](../CICD.md) |
| SCA (Grype) | Scan van externe libraries op bekende lekken. Toont alle lekken in de Security-tab, maar blokkeert niet | `.github/workflows/sca.yml` |
| SBOM (Syft) | Lijst van alle onderdelen, zodat een later bekend lek terug te zoeken is | `.github/workflows/sbom.yml` |
| Security backlog | Risicoregister met STRIDE-ID's, kans en impact en codereferenties | [risk-criteria.md](risk-criteria.md), [risk-matrix.md](risk-matrix.md) |
| Gap-analyses | Huidige tegenover gewenste situatie, met NEN 7510-koppeling | [01-gap-analyse.md](../auditrapport/01-gap-analyse.md) (branch `docs_auditreport_gapanalysis`) |

De zwaarste backlog-items hebben we daarnaast handmatig in de broncode nagekeken (zie
de referenties in hoofdstuk 3 en 4).

---

## 2. Welke gevoelige gegevens worden verwerkt (met referenties)

Dit zijn de kroonjuwelen: de gegevens met de hoogste waarde, met verwijzing naar het
endpoint of bestand in de broncode.

| Gevoelige gegevens | AVG-classificatie | Referentie (code) |
|--------------------|-------------------|-------------------|
| Observaties, consulten en medicatie (Obs, Encounter, Order) | Gezondheidsgegevens (AVG art. 9) | `ObsResource1_8`, `EncounterResource1_8`, `OrderResource1_8` |
| Patiënt- en persoonsgegevens (NAW, identifiers) | Persoonsgegevens (PII) | `PatientResource1_8`, `PatientIdentifierResource1_8` |
| Gebruikers, rollen en privileges | Authenticatie / IAM | `UserResource1_8`, `RoleResource1_8` |
| Secrets (DB-wachtwoorden, API-keys als global property) | Secrets | `SettingsFormController.java` (lekt via `/search`), `example.env` / `.env` |

Wie het IAM-domein of de secrets in handen krijgt, heeft daarmee indirect toegang tot
alle andere kroonjuwelen. De toegang loopt via één centrale REST-laag, wat de impact van
een lek vergroot.

---

## 3. Geverifieerde bevindingen in de broncode

De vier zwaarste backlog-items zijn één-op-één terug te vinden in de code:

- I-2 en I-4, unauthenticated settings met secrets-lek. `SettingsFormController.searchProperties()`
  heeft geen `Context.isAuthenticated()`-check en geeft van elke global property zowel de naam
  als de waarde terug. Daarmee kunnen secrets (wachtwoorden, API-keys) ongeauthenticeerd worden
  uitgelezen. Het endpoint valt bovendien onder `/module/webservices/rest/...`, dus de
  `AuthorizationFilter` en de IP-allowlist raken het niet eens.
  Referentie: [SettingsFormController.java:50-63](../../omod/src/main/java/org/openmrs/module/webservices/rest/web/controller/SettingsFormController.java)
- S-1, Basic Auth die stil faalt. De filter decodeert base64-credentials en stopt bij een fout
  nooit zelf ("It will not fail on invalid or missing credentials"). TLS wordt niet afgedwongen,
  dus credentials zijn te onderscheppen.
  Referentie: [AuthorizationFilter.java:64-122](../../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/AuthorizationFilter.java)
- E-2, XML Content-Type bypass. Het filter gebruikt een blacklist: het blokkeert alleen als de
  content-type letterlijk de tekst `xml` bevat. Een afwijkende of samengestelde content-type
  glipt er langs.
  Referentie: [ContentTypeFilter.java:67-74](../../omod-common/src/main/java/org/openmrs/module/webservices/rest/web/filter/ContentTypeFilter.java)
- D-3, brute-force op wachtwoord-reset. Er is geen rate-limiting of lockout op het reset-endpoint.
  Referentie: `PasswordResetController2_2.java`, `SessionController1_9.java` (gap-analyse 3).

---

## 4. Kwetsbaarheden, mitigatie en NEN 7510-2:2024-maatregel

De ID's en risicocodes komen uit de [security backlog](risk-criteria.md). De NEN-kolom
verwijst naar de maatregelnummering van NEN 7510-2:2024 (afgestemd op ISO/IEC 27002:2022;
thema 5 is organisatorisch, thema 8 is technologisch).

| ID | Kwetsbaarheid | Code / klasse | Mitigatie | NEN 7510-2:2024 |
|----|---------------|:-------------:|-----------|-----------------|
| I-2 / I-4 | `settings.form/search` zonder auth, lekt secrets | E5 · Kritiek | Auth-check (`Context.isAuthenticated()` plus privilege) toevoegen of het endpoint verwijderen. Nooit de property-waarden teruggeven. Secrets uit de global properties halen | 8.3 Toegangsbeperking informatie; 5.15 Toegangsbeveiliging; 8.24 Gebruik van cryptografie |
| S-1 | Basic Auth onderschepping (base64, geen TLS afgedwongen) | D4 · Kritiek | TLS afdwingen (HTTPS-only, HSTS). Op termijn token- of OAuth2-authenticatie in plaats van Basic | 8.24 Gebruik van cryptografie; 8.5 Veilige authenticatie |
| T-1 | Mass assignment bij het schrijven van resources | D4 · Kritiek | Allowlist van schrijfbare velden. Gevoelige velden (`uuid`, `voided`) server-side negeren | 8.28 Veilig programmeren; 8.26 Beveiligingseisen voor applicaties |
| E-1 | Privilege-escalatie via `/user` | D4 · Kritiek | Autorisatiecontrole op rol- en privilege-wijzigingen. Functiescheiding tussen beheer en medische data | 8.2 Speciale toegangsrechten; 5.18 Toegangsrechten |
| S-2 | Sessie-hijacking | C3 · Middel | `Secure`-, `HttpOnly`- en `SameSite`-cookies. Korte sessieduur. TLS | 8.5 Veilige authenticatie; 8.24 Gebruik van cryptografie |
| T-2 | Inputvalidatie / injectie | C3 · Middel | Server-side validatie. Geparameteriseerde queries | 8.28 Veilig programmeren; 8.26 Beveiligingseisen voor applicaties |
| R-1 | Incomplete auditlogging | C3 · Middel | Toegang tot medische data en wijzigingen centraal loggen. Logs beschermen | 8.15 Registreren van gebeurtenissen; 8.16 Monitoringactiviteiten |
| D-1 | Onbeperkte resultaatsets (geen rate-limiting) | C3 · Middel | `maxResults` afdwingen. Rate-limiting op zware `?v=full`-queries | 8.6 Capaciteitsbeheer |
| D-3 | Brute-force op wachtwoord-reset (geen lockout) | C3 · Middel | Rate-limiting plus accountlockout na X foute pogingen. Eenmalige, kortlevende reset-token | 8.5 Veilige authenticatie; 5.17 Authenticatie-informatie |
| E-2 | XML Content-Type bypass (blacklist-filter) | C3 · Middel | De blacklist vervangen door een allowlist van toegestane content-types | 8.28 Veilig programmeren; 8.9 Configuratiebeheer |
| I-1 | Stack traces in responses | B3 · Middel | Globale exception-handler. Generieke foutmeldingen naar de client | 8.28 Veilig programmeren; 8.26 Beveiligingseisen voor applicaties |
| I-5 | `/session/diag` lekt recon-info (serverTime) | B3 · Middel | Diagnostische velden weglaten of achter auth plaatsen | 8.3 Toegangsbeperking informatie |
| I-3 | Gevoelige data via `?v=full` | B2 · Laag | Velden in de `full`-representatie beperken op basis van privilege | 8.3 Toegangsbeperking informatie |
| D-2 | Async herstart-misbruik | B2 · Laag | Herstart-actie achter een beheerprivilege plus rate-limiting | 8.6 Capaciteitsbeheer |

Naast de losse fixes dekken de pipeline-maatregelen meerdere risico's af. Die zijn er al
(zie [CICD.md](../CICD.md)): de SAST-, SCA- en SBOM-scans en de peer review koppelen we aan
8.8 Beheer van technische kwetsbaarheden, 8.25 Veilige ontwikkellevenscyclus en 8.28 Veilig
programmeren.

---

## 5. Kostenraming (resources, tijd, budget)

Hieronder schatten we wat het kost om de mitigaties uit hoofdstuk 4 door te voeren: welke
mensen het werk doen (resources), hoeveel tijd het kost (uren) en wat dat ongeveer kost
(budget). Het is een inschatting voor deze opdracht, geen offerte.

We rekenen met deze aannames:

- Een indicatief uurtarief van 80 euro per uur (gemengd tarief voor developer en
  security-engineer).
- Een werkdag is 8 uur. De uren zijn inclusief het bouwen, het hertesten en het vastleggen
  voor NEN (reden van een dismiss, documentatie).
- Het werk wordt opgepakt binnen het bestaande team. Er is geen extra inhuur nodig.

### 5.1 Raming per prioriteit

| Prioriteit | Risico's | Resource | Inschatting tijd | Inschatting budget |
|------------|----------|----------|:----------------:|:------------------:|
| Kritiek | I-2/I-4, S-1, T-1, E-1 | Developer plus security-engineer | ± 7 dagen (56 u) | ± 4.480 euro |
| Middel | S-2, T-2, R-1, D-1, D-3, E-2, I-1, I-5 | Developer | ± 8 dagen (64 u) | ± 5.120 euro |
| Laag | I-3, D-2 | Developer | ± 1 dag (8 u) | ± 640 euro |
| Overhead | Code review, hertest, NEN-documentatie | Team | ± 2 dagen (16 u) | ± 1.280 euro |
| Totaal | | | ± 18 dagen (144 u) | ± 11.520 euro |

### 5.2 Detail van de kritieke fixes

| ID | Werk | Tijd | Budget |
|----|------|:----:|:------:|
| I-2 / I-4 | Auth-check toevoegen of endpoint verwijderen. Geen waarden teruggeven | 8 u | 640 euro |
| S-1 | TLS/HTTPS afdwingen plus configuratie en deploy | 8 u | 640 euro |
| T-1 | Allowlist van schrijfbare velden over de resources | 24 u | 1.920 euro |
| E-1 | Autorisatie op `/user` aanscherpen plus tests | 16 u | 1.280 euro |

### 5.3 Aanpak binnen de resterende tijd

Met nog ongeveer anderhalve week tot de deadline is het hele programma niet haalbaar, en
dat hoeft ook niet. Dit rapport is de beoordeling en de planning. Voor de opdracht volstaat
het om de kritieke items als eerste sprint te plannen (de 56 uur hierboven) en de rest als
backlog met deze raming vast te leggen. Dat sluit aan op de beslisregels in
[risk-criteria.md](risk-criteria.md): een risico in de klasse Kritiek of Hoog mag niet
zonder vastgelegde acceptatie blijven staan.

---

## 6. Conclusie

- De grootste blootstelling zit in ongeauthenticeerde toegang met secrets-lek (I-2 en I-4)
  en in zwakke authenticatie en transport (S-1). Beide zijn in de code bevestigd en vallen
  in de klasse Kritiek.
- De mitigaties zijn telkens te koppelen aan een concrete maatregel uit NEN 7510-2:2024,
  vooral uit thema 8 (technologisch: 8.3, 8.5, 8.24, 8.28) en thema 5 (toegang: 5.15, 5.18).
- De totale raming is ongeveer 144 uur, ongeveer 11.520 euro. Dit is een inschatting. De
  kritieke fixes (ongeveer 56 uur) hebben prioriteit en passen als eerste sprint binnen de
  planning.

---

## Referenties

- Security backlog en risicocriteria: [risk-criteria.md](risk-criteria.md), [risk-matrix.md](risk-matrix.md), [risk-evaluation.md](risk-evaluation.md)
- Gap-analyses met NEN-koppeling: [01-gap-analyse.md](../auditrapport/01-gap-analyse.md) (branch `docs_auditreport_gapanalysis`)
- Pipeline en scans: [CICD.md](../CICD.md), `.github/workflows/`
- Code: `SettingsFormController.java`, `AuthorizationFilter.java`, `ContentTypeFilter.java`, `PasswordResetController2_2.java`, `SessionController1_9.java`
- Kaders: AVG art. 9 (bijzondere persoonsgegevens), art. 33 en 34 (meldplicht datalekken), NEN 7510-2:2024 (informatiebeveiliging in de zorg)
