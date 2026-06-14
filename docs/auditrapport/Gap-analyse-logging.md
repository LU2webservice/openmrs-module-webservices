# Gap-analyse-logging — openmrs-module-webservices.rest

Omschrijving

### Logging Gap Analyse

| Event                | Gelogd? | Gevoelige data | Compliant NEN-7510 |
| :------------------- | :------ | :------------- | :----------------- |
| `PATCH /user`        | Nee     | Ja             | Nee                |
| `POST /user`         | Nee     | Nee            | Nee                |
| `GET /systemsetting` | Nee     | Nee            | Nee                |
| `GET /session`       | Nee     | Nee            | Nee                |

---

#### PATCH /user

- **Gelogd:** Nee.
- **Bevinding:** Bij het sturen van een PATCH request geeft de API een `200 OK` terug, zelfs als de wijziging niet doorgegeven wordt. Geen logs van pogingen tot ongeautoriseerde wijziging van gebruikersdata.

#### POST /user

- **Gelogd:** Nee.
- **Bevinding:** Het systeem accepteert verzoeken zonder autorisatiecontrole bij het mee geven van UUID's. Bij gefaalde of succesvolle pogingen wordt er niets geregistreerd.

#### GET /systemsetting

- **Gelogd:** Nee.
- **Bevinding:** Ongeautoriseerde toegangspogingen worden correct afgewezen met `401 Unauthorized`, maar deze worden niet gelogd.

#### GET /session

- **Gelogd:** Nee
- **Bevinding:** Het endpoint geeft onnodige informatie over sessiestatus (`authenticated: false`) bij 200 OK, in plaats van een 401.
