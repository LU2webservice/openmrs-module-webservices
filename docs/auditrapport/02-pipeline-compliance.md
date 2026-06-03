# Mini-complianceverslag — openmrs-module-webservices.rest

Dit zijn de maatregelen over hoe goed onze applicatie het doet op beveiliging

---

# 5.6 Complianceverslag matrix

| NEN-7510                                                  | Beheersmaatregel                                                                                                                                                                                                                              | Source                                                                                                                                                                  |
|:----------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **8.9 Configuratiebeheer**                                | **Overzicht van gebruikte software**<br>We hebben een lijst (SBOM) gemaakt van alle onderdelen waaruit onze module bestaat. Zo weten we welke bibliotheken we gebruiken en voorkomen we dat er onbekende code in onze software terrecht komt. | **Bestand:** `docs/sbom.cdx.json`<br><br>*Belangrijke onderdelen:*<br>- Module: `webservices.rest`<br>- Framework: `spring-webmvc`<br>- Data-parser: `jackson-databind` |
| **8.8 Beheer van technische kwetsbaarheden**              | **Automatische scannen van librarys via Dependabot**<br>GitHub scant de applicatie op bekende beveiligingsrisicos. Als er ergens een probleem wordt gevonden in een van onze onderdelen, krijgen we direct een melding.                       | **Status:** GitHub `Security` tabblad (Dependabot alerts).<br><br>*Actie:* Gevonden lekken worden direct in onze *Security Backlog* (5.7) gezet om ze te verhelpen.     |
| **8.29 Beveiligingstesten**                               | **Automatische code analyse via CodeQL**<br>Bij elke aanpassing in de software word onze broncode automatisch gecontrolleerd op programmeerfouten die beveiligingsrisico's kunnen opleveren.                                                  | **Status:** GitHub `Actions` tabblad & `Code scanning alerts`.<br><br>*Actie:* Fouten worden door het systeem gemeld en moeten worden opgelost voordat we verdergaan.   |
| **8.28 Veilig coderen**                                   | **Toegangscontrole & Code reviews**<br>Niemand kan zomaar code aanpassen. We moeten inloggen met extra beveiliging (MFA) en alle wijzigingen moeten door een ander worden gecontroleerd en goedgekeurd.                                       | **Status:** Repository `Settings` (Branch protection rules voor `main`) en MFA-instellingen van ons team.                                                               |




