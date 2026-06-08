# Uitleg CI/CD en security

In dit bestand leg ik uit hoe de pipeline werkt. Welke workflows er zijn, wat ze
doen en in welke volgorde. Ook leg ik uit wat we doen met meldingen die niet
kloppen (false positives).

## Welke eis zit waar

| Eis | Hoe het is gedaan | Bestand |
|-----|-------------------|---------|
| CI/CD tooling | Build en tests bij elke pull request, en automatisch uitrollen na een merge | ci.yml en deploy.yml |
| SAST | CodeQL scant onze eigen code | codeql.yml |
| SBOM | Syft maakt een lijst van alle onderdelen | sbom.yml |
| SCA | Grype scant externe libraries op lekken | sca.yml |
| False positives | Beleid (zie onderaan) | dit bestand |

Alle workflows staan in de map .github/workflows.

## De volgorde

```
Pull request naar main
        |
        |-- ci.yml      build plus unit- en integratietests
        |-- codeql.yml  scan eigen code (SAST)
        |-- sbom.yml    lijst van onderdelen (SBOM)
        |-- sca.yml     scan externe libraries (SCA)
        |
        |  alle checks groen en iemand keurt de pull request goed
        v
   merge naar main
        |
        v
   deploy.yml   dev -> test -> prod
```

Eerst wordt alles gecontroleerd. Pas als dat goed gaat en iemand de pull request
goedkeurt, mag de code naar main. Daarna rolt deploy.yml het vanzelf uit.

## ci.yml: bouwen en testen

Draait zodra iemand een pull request naar main opent.

1. De code wordt opgehaald.
2. Er wordt gekeken of er een nieuwe library bij komt met een bekend lek. Is dat
   ernstig (high), dan stopt het hier.
3. Java 17 wordt klaargezet en de module wordt gebouwd met Maven, samen met de
   unit tests.
4. Met Docker wordt een hele OpenMRS-omgeving gestart, de gebouwde module gaat
   erin, en de integratietests draaien tegen dat draaiende systeem.
5. De testrapporten en de gebouwde module worden bewaard zodat je ze kunt
   downloaden.
6. De Docker-omgeving wordt weer afgebroken.

Hiermee weet je dat de code bouwt en nog werkt.

## codeql.yml: eigen code scannen (SAST)

Draait bij elke push en pull request naar main, en verder elke maandag.

1. De code wordt opgehaald en CodeQL start.
2. De module wordt gebouwd.
3. CodeQL leest onze eigen Java-code en zoekt naar onveilige stukken, zoals
   SQL-injectie of onveilig omgaan met bestanden.

De resultaten staan in GitHub onder Security, bij Code scanning. Testcode slaan we
over, zodat we geen onnodige meldingen krijgen.

## sbom.yml: lijst van onderdelen (SBOM)

Draait bij elke push en pull request naar main.

1. De module wordt gebouwd.
2. Syft maakt een lijst van alle onderdelen en libraries die erin zitten. Ook de
   libraries die door andere libraries worden meegetrokken.
3. Die lijst wordt 90 dagen bewaard.

Waarom dit handig is: als er later een nieuw lek bekend wordt, kun je in deze lijst
gelijk opzoeken of dat onderdeel in onze build zit.

## sca.yml: externe libraries scannen (SCA)

Draait bij elke push en pull request naar main, en verder elke maandag.

1. De module wordt gebouwd.
2. Grype controleert alle libraries tegen een database met bekende lekken.
3. De resultaten gaan naar de Security-tab en worden als rapport bewaard.

Deze check laat de pipeline bewust niet falen (fail-build staat op false). Een lek
in een OpenMRS-dependency die we zelf niet kunnen updaten mag de build namelijk niet
blokkeren. Het lek blijft wel zichtbaar in de Security-tab.

Verschil met stap 2 van ci.yml: die kijkt alleen naar libraries die in de pull
request veranderen. Grype scant alles, en ook elke week. Zo zien we ook lekken die
pas later bekend worden in libraries die er al langer in zitten.

## deploy.yml: uitrollen (OTAP)

Draait pas nadat de code naar main is gemerged, dus als alle checks groen waren.

1. Dev: de module wordt gebouwd en klaargezet.
2. Test: de module gaat op een testomgeving en de integratietests draaien daar nog
   een keer tegen een echte OpenMRS-stack.
3. Prod: pas als test goed gaat, gaat het naar productie.

De stappen lopen achter elkaar. Gaat een eerdere stap mis, dan stopt het. De echte
deploy naar de server is nu nog een placeholder en vullen we in zodra de servers er
zijn.

## False positives

Soms geeft een tool een melding die bij ons niet echt gevaarlijk is, of iets dat we
zelf niet kunnen oplossen omdat het uit OpenMRS komt. De afspraak:

- Kunnen we het echt oplossen, dan doen we dat. De library updaten of de code
  aanpassen.
- Is het een terechte uitzondering of kunnen we het niet oplossen, dan zetten we
  die ene melding uit met een reden en een datum erbij.

Voor CodeQL en Grype doen we dat door het alert in de Security-tab af te wijzen met
een reden. We zetten dus nooit een hele check uit, alleen losse meldingen met
uitleg. Zo kun je later terugvinden waarom iets is genegeerd.

## Over de checks bij een pull request

Bij een pull request zie je soms meer regels dan er workflows zijn. Dat klopt:

- De vier workflows hierboven geven elk hun eigen check.
- Daarnaast maakt GitHub zelf nog regels met de naam "Code scanning results", zodra
  CodeQL en Grype hun resultaten naar de Security-tab sturen. Dat zijn geen extra
  workflows.

In de branch protection van main staan de checks die verplicht groen moeten zijn
voor je kunt mergen: Build & smoke test, Analyze (java) en SBOM met Syft. De
SCA-scan (Grype) staat daar bewust niet bij als verplicht en laat de pipeline ook
niet falen, omdat hij kan afgaan op een lek in een OpenMRS-dependency die we zelf
niet kunnen updaten. Het lek blijft wel zichtbaar in de Security-tab.
