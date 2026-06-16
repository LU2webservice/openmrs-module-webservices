# OpenMRS REST Webservices Module - Beveiligingsonderzoek

Dit is een schoolopdracht waarin we specifiek kijken naar de **beveiliging** en
**onderhoudbaarheid** van deze OpenMRS-module (de REST-API die patiënt- en zorggegevens
beschikbaar maakt onder `/ws/rest/v1/...`). We tonen de problemen aan, lossen ze op en
proberen waar mogelijk verbeteringen door te voeren. Omdat het om bijzondere
persoonsgegevens gaat (AVG art. 9) toetsen we aan **NEN 7510**.

---

## Documentatie (`docs/`)

Alle analyse, tests en onderbouwing staan in de map `docs/`, opgedeeld in twee onderdelen:

- **`docs/security/`** - alles rond beveiliging: analyse, risico's, pentest en de oplossingen.
- **`docs/onderhoudbaarheid/`** - alles rond onderhoudbaarheid: tests, code coverage en verbeteringen.

---

## Project opstarten (Docker)

1. Repo clonen.
2. `example.env` kopiëren naar `.env` en de wachtwoorden invullen.
3. Starten:
   ```bash
   docker compose --env-file .env up -d
   ```
4. Wacht ~5 minuten en open http://localhost:8080/openmrs.

Controleren of de REST-API draait: http://localhost:8080/openmrs/ws/rest/v1/session

Stoppen:
```bash
docker compose down       # containers stoppen
docker compose down -v    # ook alle data verwijderen
```

---

## Module bouwen en deployen

1. Een JDK installeren (bijv. via [adoptium.net](https://adoptium.net)).
2. `build_module.bat` uitvoeren - dit bouwt de module en kopieert de `.omod` naar
   `docker/modules/`.
3. Docker herstarten zodat de nieuwe module wordt geladen:
   ```bash
   docker compose down
   docker compose --env-file .env up -d
   ```

---

## Tests draaien

Alle unit- en componenttests (inclusief de 31 auditlogging-tests):
```bash
mvn -o test
```

Build met tests én code-coverage-rapport (JaCoCo, gate op 80%):
```bash
mvn clean verify
# rapport: coverage-report/target/site/jacoco-aggregate/index.html
```

Integratietests (vereisen een draaiende OpenMRS-server):
```bash
mvn clean verify -Pintegration-tests -DtestUrl=http://admin:Admin123@localhost:8080/openmrs
```

Details en resultaten staan in `docs/maintainability/Testresultaten_overzicht.md`.

---

## Projectstructuur

| Module | Inhoud |
|---|---|
| `omod-common` | Basis van de REST-laag: framework, filters, hulpklassen en de auditlogging. |
| `omod` | De REST-resources per OpenMRS-versie (vrijwel alle endpoints en de meeste tests). |
| `integration-tests` | End-to-end tests tegen een draaiende server. |
| `coverage-report` | Module die de coverage van `omod-common` en `omod` samenvoegt tot één rapport. |

---

## Licentie

[MPL-2.0 w/ HD](http://openmrs.org/license/)
</content>
