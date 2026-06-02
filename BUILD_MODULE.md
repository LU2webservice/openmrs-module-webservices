# Deploying

## Eerste keer opzetten

1. Repo clonen
2. `.env.example` kopiëren naar `.env`
3. `docker compose --env-file .env up -d`
4. Wacht ~5 minuten, open http://localhost:8080/openmrs

## Module deployen (voor testen)

1. JDK 8 installeren via [adoptium.net](https://adoptium.net)
2. `deploy.bat` uitvoeren
3. `docker compose down` → `docker compose --env-file .env up -d`
4. Wacht even, check http://localhost:8080/openmrs/ws/rest/v1/

> Stap 3 is alleen de **eerste keer** nodig. Daarna volstaat `deploy.bat`.

## Dagelijks gebruik

| Actie | Commando |
|---|---|
| Starten | `docker compose --env-file .env up -d` |
| Stoppen | `docker compose down` |
| Module opnieuw deployen | `deploy.bat` |
