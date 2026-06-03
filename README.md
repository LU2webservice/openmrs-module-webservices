# OpenMRS REST Webservices Module - Beveiligingsonderzoek

Fork van de OpenMRS REST module. Doel: beveiligingsopties ontdekken, documenteren en oplossen.

## Opstarten

Maak een `.env` aan op basis van `.env.example` en vul de wachtwoorden in:

```bash
cp .env.example .env
```

```bash
docker compose --env-file .env up -d
```

OpenMRS draait op `http://localhost:8080/openmrs`

## Stoppen

```bash
docker compose down
```

Data ook verwijderen:

```bash
docker compose down -v
```

## Module bouwen en installeren

Voer het script uit:

```bash
build_module.bat
```

Dit doet automatisch:
1. Maven build
2. `.omod` bestand kopiëren naar `docker/modules/`
3. OpenMRS herstarten

## Licentie

[MPL-2.0 w/ HD](http://openmrs.org/license/)
