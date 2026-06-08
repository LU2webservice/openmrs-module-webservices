# Uitleg CI/CD en security

In dit bestand leg ik uit hoe onze pipeline werkt. Welke onderdelen er zijn, wat ze
doen, en vooral: welke keuzes we hebben gemaakt en waarom. Aan het eind staat ook
wat we doen met meldingen die niet kloppen of die we niet kunnen oplossen (false
positives).

## Waarom we hier streng in zijn

Dit is een module voor OpenMRS, een systeem dat ziekenhuizen gebruiken om
patientgegevens bij te houden. Patientgegevens zijn gevoelig. Als daar iets misgaat,
zijn de gevolgen groot. Daarom controleren we bij elke wijziging automatisch of de
code veilig is en blijft werken.

In Nederland geldt voor zorgsystemen de norm NEN 7510. Die norm vraagt onder andere
om veilig ontwikkelen, om het op tijd vinden van kwetsbaarheden, en om het kunnen
laten zien hoe je software in elkaar zit. Onze pipeline helpt daarbij: we scannen
onze eigen code, we scannen de externe libraries, we houden een lijst bij van alle
onderdelen, en we testen alles voordat het naar productie gaat.

## Welke eis zit waar

| Eis | Hoe het is gedaan | Bestand |
|-----|-------------------|---------|
| CI/CD tooling | Build en tests bij elke pull request, automatisch uitrollen na een merge | ci.yml en deploy.yml |
| SAST | CodeQL scant onze eigen code | codeql.yml |
| SBOM | Syft maakt een lijst van alle onderdelen | sbom.yml |
| SCA | Grype scant externe libraries op bekende lekken | sca.yml |
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
        |  alle verplichte checks groen en iemand keurt de pull request goed
        v
   merge naar main
        |
        v
   deploy.yml   dev -> test -> prod
```

Eerst wordt alles gecontroleerd. Pas als dat goed gaat en iemand de pull request
goedkeurt, mag de code naar main. Daarna rolt deploy.yml het vanzelf uit.

## De belangrijkste keuzes op een rij

Hieronder eerst kort de keuzes en het waarom. Daarna leg ik per workflow alles uit.

1. We controleren bij elke pull request, niet pas achteraf. Zo komt foute of onveilige
   code het systeem niet in.
2. We gebruiken aparte tools voor aparte vragen. CodeQL voor onze eigen code, Grype
   voor externe libraries, Syft voor de inventarisatie. Elk doet waar hij goed in is.
3. Niet elke check blokkeert. Checks die jij zelf kunt oplossen, blokkeren wel.
   Checks die afgaan op problemen in OpenMRS-libraries die wij niet kunnen updaten,
   blokkeren niet, maar tonen het probleem wel.
4. We zetten nooit een hele check uit. Als een melding niet klopt of niet op te lossen
   is, zetten we alleen die ene melding uit, met een reden erbij.
5. Uitrollen gaat stap voor stap: eerst dev, dan test, dan productie. Gaat een stap
   mis, dan stopt het en komt het niet verder.

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

Keuze: we testen niet alleen of de code bouwt, maar starten ook een echte
OpenMRS-omgeving en draaien daar de integratietests tegenaan. Waarom: een ziekenhuis
draait niet op losse stukjes code, maar op het geheel. We willen weten dat de module
ook echt werkt in een draaiend systeem, niet alleen dat hij compileert.

Keuze: stap 2 (Dependency Review) blokkeert wel. Waarom: dit gaat over een nieuwe
library die in deze pull request wordt toegevoegd. Dat is een bewuste keuze van de
maker, en die kun je dus zelf oplossen door een veiligere versie te kiezen.

## codeql.yml: eigen code scannen (SAST)

Draait bij elke push en pull request naar main, en verder elke maandag.

1. De code wordt opgehaald en CodeQL start.
2. De module wordt gebouwd.
3. CodeQL leest onze eigen Java-code en zoekt naar onveilige stukken, zoals
   SQL-injectie of onveilig omgaan met bestanden.

De resultaten staan in GitHub onder Security, bij Code scanning.

Keuze: we draaien naast de standaard security-controles ook de uitgebreide set
(security-and-quality). Waarom: voor een zorgsysteem willen we zwakke plekken liever
te vroeg dan te laat zien.

Keuze: testcode en de map target scannen we niet mee (paths-ignore). Waarom: dat is
geen code die naar productie gaat. Wel meescannen zou alleen maar ruis geven.

Keuze: deze check blokkeert wel. Waarom: dit zijn lekken in onze eigen code. Die
kunnen en moeten we zelf oplossen voordat het verder gaat.

## sbom.yml: lijst van onderdelen (SBOM)

Draait bij elke push en pull request naar main.

1. De module wordt gebouwd.
2. Syft maakt een lijst van alle onderdelen en libraries die erin zitten. Ook de
   libraries die door andere libraries worden meegetrokken.
3. Die lijst wordt 90 dagen bewaard.

Keuze: we maken deze lijst automatisch bij elke wijziging. Waarom: als er later een
nieuw lek bekend wordt, kun je in deze lijst meteen opzoeken of dat onderdeel in onze
build zit. Voor NEN 7510 is het belangrijk dat je weet welke onderdelen je in huis
hebt. Zonder lijst weet je dat niet.

## sca.yml: externe libraries scannen (SCA)

Draait bij elke push en pull request naar main, en verder elke maandag.

1. De module wordt gebouwd.
2. Grype controleert alle libraries tegen een database met bekende lekken.
3. De resultaten gaan naar de Security-tab en worden als rapport bewaard.

Verschil met stap 2 van ci.yml: die kijkt alleen naar libraries die in de pull
request veranderen. Grype scant alles, en ook elke week. Zo zien we ook lekken die
pas later bekend worden in libraries die er al langer in zitten.

Keuze: deze check blokkeert niet (fail-build staat op false). Dit is een bewuste
keuze en hij is belangrijk, dus hier de volledige uitleg.

OpenMRS brengt heel veel libraries mee. Een flink deel daarvan heeft bekende lekken
die wij niet kunnen oplossen, omdat ze niet van ons zijn maar van OpenMRS zelf. Als
we de pijplijn daarop zouden laten falen, zou bijna elke pull request blijven hangen
op iets wat wij toch niet kunnen veranderen. Dan ga je de check uitzetten of negeren,
en dat is juist onveilig.

Daarom kiezen we ervoor: de scan draait altijd, toont alle lekken in de Security-tab,
maar blokkeert het werk niet. Zo houden we het overzicht zonder dat we onszelf
klemzetten. De lekken zijn niet verstopt; ze staan zichtbaar in de Security-tab en we
gaan ze af volgens het beleid hieronder.

Let op: het aantal meldingen kan hoog zijn, omdat Grype alle onderliggende libraries
van OpenMRS meeneemt. Dat is normaal. De aanpak is niet om ze allemaal een voor een
weg te werken, maar om gericht te kijken: eerst de zwaarste lekken (critical) en de
libraries die wij zelf in onze pom.xml hebben gezet. Die kunnen we mogelijk wel
oplossen. De rest, in OpenMRS zelf, houden we in de gaten via de Security-tab en de
SBOM.

## deploy.yml: uitrollen (OTAP)

Draait pas nadat de code naar main is gemerged, dus als alle verplichte checks groen
waren.

1. Dev: de module wordt gebouwd en klaargezet.
2. Test: de module gaat op een testomgeving en de integratietests draaien daar nog
   een keer tegen een echte OpenMRS-stack.
3. Prod: pas als test goed gaat, gaat het naar productie.

Keuze: we rollen in stappen uit (dev, test, prod) en niet in een keer naar productie.
Waarom: bij een ziekenhuissysteem wil je een fout vangen voordat hij bij echte
patientgegevens komt. Gaat een eerdere stap mis, dan stopt het en komt het niet
verder. De echte deploy naar de server is nu nog een placeholder en vullen we in
zodra de servers er zijn.

## Wat blokkeert wel en wat niet

In de branch protection van main staat welke checks verplicht groen moeten zijn
voordat je kunt mergen. Dit zijn ze:

- Build & smoke test (uit ci.yml)
- Analyze (java) (uit codeql.yml)
- SBOM met Syft (uit sbom.yml)

De SCA-scan (Grype dependency scan) staat hier bewust niet bij als verplicht, en de
workflow zelf laat de pijplijn ook niet falen. De reden staat hierboven bij sca.yml:
hij kan afgaan op een lek in een OpenMRS-library die wij niet kunnen updaten. Het lek
blijft wel zichtbaar in de Security-tab, dus we verliezen geen zicht.

De regel die we hierin volgen: een check blokkeert alleen als het iets is dat wij
zelf kunnen oplossen. Onze eigen code en onze eigen nieuwe libraries: blokkeren. Iets
diep in OpenMRS waar wij niet bij kunnen: tonen, niet blokkeren.

## False positives

Soms geeft een tool een melding die bij ons niet echt gevaarlijk is, of iets dat we
zelf niet kunnen oplossen omdat het uit OpenMRS komt. De afspraak:

- Kunnen we het echt oplossen, dan doen we dat. De library updaten of de code
  aanpassen.
- Is het een terechte uitzondering of kunnen we het niet oplossen, dan zetten we
  die ene melding uit, met een reden en een datum erbij.

Hoe we dat doen:

- CodeQL: het alert in de Security-tab afwijzen (Dismiss) met een reden.
- Grype: het alert in de Security-tab afwijzen (Dismiss) met een reden.

We zetten dus nooit een hele check uit, alleen losse meldingen met uitleg. Zo kun je
later altijd terugvinden waarom iets is genegeerd. Dat is ook wat NEN 7510 vraagt:
niet zomaar dingen wegklikken, maar bewust een afweging maken en die vastleggen.

## Over de checks bij een pull request

Bij een pull request zie je soms meer regels dan er workflows zijn. Dat klopt:

- De vier workflows hierboven geven elk hun eigen check.
- Daarnaast maakt GitHub zelf nog regels met de naam "Code scanning results", zodra
  CodeQL en Grype hun resultaten naar de Security-tab sturen. Dat zijn geen extra
  workflows.
