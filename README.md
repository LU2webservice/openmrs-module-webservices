# OpenMRS REST Webservices Module - Beveiligingsonderzoek

Fork van de OpenMRS REST module. Doel: beveiligingsopties ontdekken, documenteren en oplossen.

## Eerste keer opzetten

1. Repo clonen
2. `example.env` kopiëren naar `.env` en wachtwoorden invullen
3. `docker compose --env-file .env up -d`
4. Wacht ~5 minuten, open http://localhost:8080/openmrs

## Module deployen (voor testen)

1. JDK 8 installeren via [adoptium.net](https://adoptium.net)
2. `build_module.bat` uitvoeren (bouwt de module en kopieert de `.omod` naar `docker/modules/`)
3. Docker herstarten: `docker compose down` gevolgd door `docker compose --env-file .env up -d`
4. Wacht even, check http://localhost:8080/openmrs/ws/rest/v1/session

## Stoppen

```bash
docker compose down
```

Data ook verwijderen:

```bash
docker compose down -v
```

## Licentie

[MPL-2.0 w/ HD](http://openmrs.org/license/)
