# OpenMRS REST Webservices Module - Beveiligingsonderzoek

Dit project is een fork van de officiële [OpenMRS REST Web Services module](https://github.com/openmrs/openmrs-module-webservices.rest). Het doel van dit project is om de beveiligingsopties van de module te **ontdekken, documenteren en waar nodig op te lossen**.

De module stelt de OpenMRS API bloot als REST webservices. Externe applicaties zoals frontends, mobiele apps en andere systemen kunnen via deze module data ophalen en opslaan in een OpenMRS database.

## Inhoud

- [Vereisten](#vereisten)
- [Opstarten met Docker](#opstarten-met-docker)
- [Module bouwen en installeren](#module-bouwen-en-installeren)
- [API testen](#api-testen)
- [Beveiligingsonderzoek](#beveiligingsonderzoek)
- [Ontwikkelaarsinfo](#ontwikkelaarsinfo)

## Vereisten

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Java 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/)

## Opstarten met Docker

### 1. Omgevingsvariabelen instellen

Kopieer het voorbeeld `.env` bestand en pas de wachtwoorden aan:

```bash
cp .env.example .env
```

Het `.env` bestand ziet er zo uit:

```env
MYSQL_ROOT_PASSWORD=
MYSQL_DATABASE=openmrs
MYSQL_USER=openmrs
MYSQL_PASSWORD=
OMRS_ADMIN_PASSWORD=
```

Commit het `.env` bestand nooit naar git. Het staat al in `.gitignore`.

### 2. Opstarten

```bash
docker compose --env-file .env up -d
```

OpenMRS is beschikbaar op: `http://localhost:8080/openmrs`

De eerste keer opstarten duurt enkele minuten omdat OpenMRS de database initialiseert.

### 3. Stoppen

Containers stoppen maar data bewaren:
```bash
docker compose down
```

Containers stoppen en alle data verwijderen:
```bash
docker compose down -v
```

## Module bouwen en installeren

### Stap 1 - Bouwen

Navigeer naar de projectmap en bouw de module met Maven:

```bash
mvn clean install -DskipTests
```

Dit genereert een `.omod` bestand in:
```
omod/target/webservices.rest-*.omod
```

### Stap 2 - Module in Docker zetten

Kopieer het `.omod` bestand naar de `docker/modules` map:

```bash
cp omod/target/webservices.rest-*.omod docker/modules/
```

### Stap 3 - Herstarten

```bash
docker compose down
docker compose --env-file .env up -d
```

OpenMRS laadt de module automatisch bij het opstarten.

### Controleren of de module actief is

```bash
curl -u gebruikersnaam:wachtwoord http://localhost:8080/openmrs/ws/rest/v1/session
```

Als je `"authenticated": true` ziet is alles in orde.

## API testen

### Sessie controleren
```bash
curl -u gebruikersnaam:wachtwoord http://localhost:8080/openmrs/ws/rest/v1/session
```

### Patienten zoeken
```bash
curl -u gebruikersnaam:wachtwoord "http://localhost:8080/openmrs/ws/rest/v1/patient?q=Jan"
```

### Testdata aanmaken

Er staat een seed-script klaar om neppe patienten aan te maken:

```powershell
.\seed_patients.ps1
```

### API documentatie

De volledige Swagger-documentatie is beschikbaar op:
```
http://localhost:8080/openmrs/module/webservices/rest/apiDocs.htm
```

## Beveiligingsonderzoek

Het doel van dit project is om de beveiliging van de OpenMRS REST module te analyseren. Dit omvat:

- **Authenticatie en Autorisatie** - hoe worden gebruikers geverifieerd, welke rollen hebben toegang tot welke endpoints
- **Filter-keten** - hoe werken `AuthorizationFilter` en `ContentTypeFilter`, en waar zitten zwakke punten
- **Input validatie** - worden binnenkomende payloads correct gevalideerd
- **Foutafhandeling** - worden foutmeldingen veilig afgehandeld zonder gevoelige informatie te lekken
- **CSRF-bescherming** - hoe werkt de OWASP CSRFGuard integratie
- **Bevindingen documenteren** - elk gevonden probleem wordt gedocumenteerd met uitleg en oplossing

## Ontwikkelaarsinfo

### Integratietests uitvoeren

Zorg dat OpenMRS draait en voer dan uit:

```bash
mvn clean verify -Pintegration-tests -DtestUrl=http://gebruikersnaam:wachtwoord@localhost:8080/openmrs
```

### Wiki

| Pagina | Beschrijving |
|--------|--------------|
| [REST Module](https://wiki.openmrs.org/display/docs/REST+Module) | Configuratieopties van de module |
| [Technische documentatie](https://wiki.openmrs.org/display/docs/REST+Web+Services+Technical+Documentation) | Technische details van de implementatie |
| [Core Developer Guide](https://wiki.openmrs.org/display/docs/Adding+a+Web+Service+Step+by+Step+Guide+for+Core+Developers) | Hoe voeg je REST resources toe aan OpenMRS core |

## Licentie

[MPL-2.0 w/ HD](http://openmrs.org/license/)
