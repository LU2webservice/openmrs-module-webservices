# Bevinding & fix: /session/diag crasht op het ingelogde pad (I-5 nazorg)

> **Wat is dit bestand?**
> Een vastlegging van een bevinding die ná de hertest van 17-06 boven water kwam: het
> `/session/diag`-endpoint gaf voor een **ingelogde** gebruiker een HTTP 500 terug. Dit
> hoort bij risico **I-5 / SR-16** en is nazorg op [pentest-hertest-17-06.md](pentest-hertest-17-06.md).

| | |
|---|---|
| **Datum** | 2026-06-19 |
| **Risico-item** | I-5 / SR-16 — `/session/diag` (zie [threat-model.md](threat-model.md), [security-backlog-pentest-rapport.md](security-backlog-pentest-rapport.md)) |
| **Norm** | NEN-7510:2024 8.3 (toegangsbeperking), 8.28 (veilig coderen) |
| **Bestand** | `SessionController1_9.java` (`getDiagnostics`) |
| **Omgeving** | Lokaal, Docker, OpenMRS 2.8.x (Tomcat 9.0.118), MariaDB 10.11 |
| **Gevonden door** | testronde 19-06 (handmatig, Postman + curl) |

---

## 1. Samenvatting (in het kort)

De fix voor I-5 (commit `c9f1ad7`, PR #37, 17-06) loste het **gedocumenteerde** probleem op: een
anonieme aanroep van `/session/diag` geeft nu HTTP 401 in plaats van `serverTime` te lekken. Maar
dezelfde wijziging brak het **ingelogde** pad: een geauthenticeerde aanroep gaf een **HTTP 500**.

> **Kern:** de fix was goed voor het deel dat getest werd (anoniem → 401), maar de pentest én de
> hertest hebben `/session/diag` **uitsluitend ongeauthenticeerd** getest. Het ingelogde pad — waar
> de fout zat — is nooit aangeroepen, dus de crash bleef onopgemerkt.

---

## 2. Het probleem

Bij een ingelogde aanroep serialiseerde het endpoint de **rauwe** OpenMRS-`Role`/`Privilege`-
entiteiten naar JSON. Jackson roept daarbij `Role.getId()` aan, en die gooit bewust een
`UnsupportedOperationException` (de primary key van een `Role` is de naam, geen numeriek id).
Gevolg: HTTP 500 met een volledige stacktrace in de respons (zelf óók een informatielek).

**Oude code (`getDiagnostics`):**
```java
diag.add("currentUser", Context.getAuthenticatedUser().getUsername());
diag.add("userRoles", Context.getAuthenticatedUser().getRoles());        // <-- crasht op Jackson
diag.add("userPrivileges", Context.getAuthenticatedUser().getPrivileges());
```

**Fout in de respons:**
```
Caused by: java.lang.UnsupportedOperationException
    at org.openmrs.Role.getId(Role.java:246)
... (was java.lang.UnsupportedOperationException) (through reference chain:
    SimpleObject["userRoles"] -> PersistentSet[0] -> org.openmrs.Role["id"])
```

Bijkomend: `currentUser` was leeg (`""`) omdat de admin via `systemId` inlogt en geen `username` heeft.

---

## 3. De fix

Serialiseer alleen de **namen** (strings) in plaats van de hele entiteiten, en val voor
`currentUser` terug op `systemId` als de username leeg is. De auth-check (anoniem → 401) blijft
ongewijzigd.

```java
User authenticatedUser = Context.getAuthenticatedUser();
diag.add("authenticated", Context.isAuthenticated());
diag.add("serverTime", System.currentTimeMillis());
String username = authenticatedUser.getUsername();
diag.add("currentUser", (username != null && !username.isEmpty()) ? username : authenticatedUser.getSystemId());
Set<String> roleNames = new HashSet<String>();
for (Role role : authenticatedUser.getRoles()) {
    roleNames.add(role.getRole());
}
diag.add("userRoles", roleNames);
Set<String> privilegeNames = new HashSet<String>();
for (Privilege privilege : authenticatedUser.getPrivileges()) {
    privilegeNames.add(privilege.getPrivilege());
}
diag.add("userPrivileges", privilegeNames);
```

---

## 4. Voor en na (echte output)

### Ingelogd pad — `curl -i -u admin:<wachtwoord> .../ws/rest/v1/session/diag`

**Voor (build met PR #37, vóór deze fix):**
```
HTTP/1.1 500
{"error":{"message":"Could not write JSON: (was java.lang.UnsupportedOperationException) ...
  org.openmrs.Role[\"id\"] ..."}}
```

**Na (met deze fix):**
```
HTTP/1.1 200
{"authenticated":true,"serverTime":1781876009871,"currentUser":"admin",
 "userRoles":["System Developer","Provider"],"userPrivileges":[]}
```

### Anoniem pad — `curl -i .../ws/rest/v1/session/diag` (ongewijzigd, blijft veilig)
```
HTTP/1.1 401
"User is not logged in [Authentication is required to access diagnostics]"
```

---

## 5. Les: het testgat

| | |
|---|---|
| **Wat ging er mis in het testen** | `/session/diag` is in PT-13 en in de hertest 17-06 alleen **ongeauthenticeerd** getest (anoniem → 401). Het ingelogde pad is nooit aangeroepen. |
| **Waarom dat erg is** | De code-paden verschillen: alleen het ingelogde pad serialiseert rollen/privileges, en juist dáár zat de crash. Een fix "voor de helft die gemeten is" werd als volledig opgelost gemeld. |
| **Wat we nu anders doen** | Bij een endpoint met verschillend gedrag per authenticatiestatus testen we **beide** paden (anoniem én ingelogd) en leggen beide vast als bewijs. |

---

## 6. Herleidbaarheid

| Onderdeel | Locatie |
|---|---|
| Risico-omschrijving I-5 | [threat-model.md](threat-model.md) (Information Disclosure) |
| Backlog-eis SR-16 | [security-backlog-pentest-rapport.md](security-backlog-pentest-rapport.md) |
| Oorspronkelijke hertest (alleen anoniem) | [pentest-hertest-17-06.md](pentest-hertest-17-06.md) |
| Gewijzigd bestand | `SessionController1_9.java` (`getDiagnostics`) |
| Verificatie | handmatig: ingelogd → HTTP 200 + JSON; anoniem → HTTP 401 (sectie 4) |
