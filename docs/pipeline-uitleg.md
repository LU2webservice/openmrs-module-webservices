# Uitleg pipeline

We hebben vijf workflows in GitHub Actions. Vier daarvan controleren de code bij
een pull request. De vijfde rolt de code uit nadat die naar main is gemerged.

## ci.yml — bouwen en testen

Draait zodra iemand een pull request naar main opent.

Wat er gebeurt:
1. Het haalt de code op.
2. Het checkt of er een nieuwe library wordt toegevoegd met een bekend lek. Is dat
   zo en is het ernstig (high), dan stopt het hier.
3. Het zet Java 17 klaar en bouwt de module met Maven, inclusief de unit tests.
4. Het start via Docker een complete OpenMRS-omgeving, zet de gebouwde module
   erin en draait de integratietests tegen dat draaiende systeem.
5. De testrapporten en de gebouwde module worden bewaard zodat je ze kunt
   downloaden.
6. De Docker-omgeving wordt weer afgebroken.

Kort gezegd: dit bewijst dat de code bouwt en blijft werken.

## codeql.yml — eigen code scannen (SAST)

Draait bij elke push en pull request naar main, en daarnaast elke maandag.

Wat er gebeurt:
1. Het haalt de code op en start CodeQL.
2. Het bouwt de module.
3. CodeQL leest onze eigen Java-code en zoekt naar onveilige patronen, zoals
   SQL-injectie of onveilig omgaan met bestanden.

De resultaten staan in GitHub onder Security, bij Code scanning. Testcode wordt
overgeslagen zodat we geen onnodige meldingen krijgen.

Kort gezegd: dit controleert onze eigen code op kwetsbaarheden.

## sbom.yml — lijst van onderdelen (SBOM)

Draait bij elke push en pull request naar main.

Wat er gebeurt:
1. Het bouwt de module.
2. Syft maakt een lijst van alle onderdelen en libraries die erin zitten, ook de
   libraries die door andere libraries worden meegetrokken.
3. Die lijst wordt 90 dagen bewaard.

Waarom dit handig is: als er morgen een nieuw lek bekend wordt, kun je in deze
lijst direct opzoeken of dat onderdeel in onze build zit.

## sca.yml — externe libraries scannen (SCA)

Draait bij elke push en pull request naar main, en daarnaast elke maandag.

Wat er gebeurt:
1. Het bouwt de module.
2. Grype controleert alle libraries tegen een database van bekende lekken.
3. Wordt er iets ernstigs gevonden (high of critical), dan faalt de pipeline.
4. De resultaten gaan naar de Security-tab.

Het verschil met stap 2 in ci.yml: die kijkt alleen naar libraries die in de pull
request veranderen. Grype scant alles, en doet dat ook wekelijks. Zo zien we ook
lekken die later bekend worden in libraries die al langer in het project zitten.

## deploy.yml — uitrollen (OTAP)

Draait pas nadat de code naar main is gemerged, dus als alle controles groen waren.

Wat er gebeurt:
1. Dev: de module wordt gebouwd en klaargezet.
2. Test: de module wordt op een testomgeving gezet en de integratietests draaien
   daar nog een keer tegen een echte OpenMRS-stack.
3. Prod: pas als test goed gaat, gaat het naar productie.

De stappen lopen achter elkaar. Gaat een eerdere stap mis, dan stopt het en komt
de code niet verder. De echte server-deploy is nu nog een placeholder en moet
worden ingevuld zodra de servers klaarstaan.

## False positives

Soms meldt een tool iets dat bij ons niet echt gevaarlijk is. De afspraak:
- Is het echt een probleem, dan lossen we het op (library updaten of code aanpassen).
- Is het een terechte uitzondering, dan zetten we het uit met een reden erbij, zo
  precies mogelijk op dat ene punt. Voor Grype gebeurt dat in .grype.yaml, voor
  CodeQL door het alert in de Security-tab af te wijzen met een reden.

We zetten dus nooit een hele controle uit, alleen losse meldingen met uitleg.
