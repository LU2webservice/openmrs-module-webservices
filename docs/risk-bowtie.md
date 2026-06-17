# Risk bowtie openmrs-module-webservices.rest

Dit zijn bowties van onze grootste risicos binnen ons systeem.

---

### 1. Risico: Unauthenticated systeeminstellingen

![Bow-Tie Diagram: Unauthenticated systeeminstellingen](assets/Bowtie_diagram_Unauthenticated_systeeminstellingen.png)

---

### 2. Risico: Basic Auth onderschepping

_Beschrijving: HTTP Basic Auth verstuurt credentials als Base64; zonder TLS zijn deze vatbaar voor onderschepping._

![Bow-Tie Diagram: Basic Auth onderschepping](assets/Bowtie_diagram_auth_onderschepping.png)

---

### 3. Risico: Mass assignment

_Beschrijving: Inkomende JSON wordt direct op domeinobjecten gemapped, waardoor gevoelige velden zoals 'uuid' of 'voided' ongeoorloofd kunnen worden gewijzigd._

![Bow-Tie Diagram: Mass assignment](assets/Bowtie_diagram_mass_assignment.png)

---

### 4. Risico: Privilege-escalatie via /user

_Beschrijving: Schrijftoegang op `/user` kan leiden tot ongeautoriseerde rol- of privilege-aanpassingen door gebrekkige autorisatiecontrole._

![Bow-Tie Diagram: Privilege-escalatie via /user](assets/Bowtie_diagram_privilege-escalatie.png)
