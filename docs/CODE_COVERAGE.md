# Code coverage

In dit bestand leg ik uit hoe we de testdekking van onze code meten en bewaken. Welke
tool we gebruiken, hoe het in de build en in de CI zit, welke ondergrens we hebben
gekozen en waarom, en hoe je het rapport vindt. Het sluit aan op de pijplijn die in
[CICD.md](CICD.md) staat beschreven.

## Wat is code coverage en waarom doen we dit

Code coverage meet welk deel van de code daadwerkelijk wordt uitgevoerd als de tests
draaien. Een dekking van 80% wil zeggen: van alle code wordt 80% door minstens een test
geraakt, en 20% niet.

Het zegt niet of de tests goed zijn, maar wel of er code is die helemaal niet getest
wordt. Dat is een blinde vlek. Bij een ziekenhuissysteem dat patientgegevens verwerkt
wil je die blinde vlekken klein houden. In Nederland vraagt de norm NEN 7510 om veilig
en aantoonbaar ontwikkelen. Kunnen laten zien hoeveel van je code getest is, en bewaken
dat dit niet stilletjes wegzakt, hoort daarbij.

We gebruiken JaCoCo, de standaardtool voor code coverage in Java. Hij draait volledig
binnen onze eigen build en CI. Er gaat geen code of meetdata naar een externe partij.

## Hoe het is opgezet

De module bestaat uit twee code-delen: omod-common (de basis met framework en
hulpklassen) en omod (vrijwel alle echte REST-code en het grootste deel van de tests).
Daarnaast is er integration-tests, maar daar zit geen productiecode in.

JaCoCo doet het volgende (zie de pom.xml-bestanden):

1. Tijdens de tests bijhouden welke code wordt uitgevoerd (prepare-agent).
2. Per module een rapport maken (report).
3. In de omod-module de ondergrens bewaken (check): zakt de dekking van die module
   onder de norm, dan faalt de build. Dit is de gate.
4. In een aparte module coverage-report een gecombineerd rapport maken
   (report-aggregate): het totaalcijfer over beide modules samen.

Waarom samenvoegen: veel klassen in omod-common worden pas getest door de tests in
omod. Als je omod-common los meet, lijkt die maar 16% getest, terwijl het in
werkelijkheid 72% is. Pas als je de meetdata van beide modules samenvoegt, krijg je een
eerlijk cijfer.

Waarom een aparte coverage-report-module: de report-aggregate van JaCoCo rapporteert
alleen de modules waar de module die hem draait van afhankelijk is. Daarom is er een
kleine extra module die zowel omod-common als omod als afhankelijkheid heeft. Alleen zo
komen beide in een rapport terecht. Dit is de gangbare manier voor projecten met
meerdere modules.

## De ondergrens: 80%

De grens staat als een property boven in pom.xml:

```xml
<jacoco.minimum.coverage>0.80</jacoco.minimum.coverage>
```

De gemeten stand, met de tests van beide modules samen:

| Onderdeel | Dekking |
|-----------|---------|
| omod (waar de gate op let) | 86,6% |
| Gecombineerd, omod-common en omod samen (337 klassen) | 82,8% |

De gate bewaakt de omod-module, want daar zit vrijwel alle REST-code. Het gecombineerde
rapport laat het totaalcijfer over beide modules zien.

Waarom 80%, en niet een ander getal:

We hebben eerst gemeten en daarna pas de grens gekozen. We zitten nu rond de 83%, en de
gate let op een module die op 86% staat. We leggen de lat op 80%: een rond getal, net
onder de huidige stand. Een grens die je uit de lucht grijpt is namelijk of te laag,
zodat hij niets bewaakt, of te hoog, zodat de build meteen faalt en iemand de check
gaat uitzetten.

Het doel is niet om precies 80% te halen, want daar zitten we al ruim boven. Het doel is
voorkomen dat de dekking ongemerkt wegzakt als er later nieuwe, ongeteste code bij komt.
Door de lat een paar procent onder de huidige stand te leggen vangen we een echte
verslechtering wel (als iemand veel ongeteste code toevoegt, duikt de dekking onder 80%
en stopt de build), maar breekt een kleine, normale wijziging de build niet onnodig.

80% is bovendien een gangbare norm voor goed getest. De laatste procenten richting 100%
kosten onevenredig veel moeite, want dan zit je triviale of gegenereerde code te testen.
Dat levert vooral schijnzekerheid en frustratie op, geen echte kwaliteit. En een te
strenge grens werkt averechts: als de build steeds rood staat om iets onbenulligs, gaan
mensen de check omzeilen, en dan ben je slechter af dan met een haalbare grens.

Keuze: de gate blokkeert de build. Waarom: dit gaat over de testdekking van onze eigen
code, en daar gaan wij zelf over. Dat is precies het soort check dat volgens
[CICD.md](CICD.md) mag blokkeren, in tegenstelling tot bijvoorbeeld lekken in
OpenMRS-libraries waar wij niet bij kunnen.

Omdat de grens een property is, kun je hem later in een regel bijstellen, bijvoorbeeld
naar 85% als de dekking structureel groeit.

## Het rapport als artefact uit de CI

In [ci.yml](../.github/workflows/ci.yml) draait de build als `mvn clean verify`. Daardoor
wordt het rapport gemaakt en draait de gate. Dit gebeurt alleen bij een pull request: de
gate hoort voor de merge, en het uitrollen daarna is puur shippen. Na de build wordt het
rapport bewaard als artefact code-coverage-report.

Je vindt het onder Actions, bij de run van CI / Deploy Pipeline, onderaan bij Artifacts.
Pak code-coverage-report uit en open
coverage-report/target/site/jacoco-aggregate/index.html in een browser voor het
overzicht per package en per klasse.

## Zelf draaien

```bash
mvn clean verify
# rapport: coverage-report/target/site/jacoco-aggregate/index.html
```

Wil je wel meten maar de build niet laten falen op de grens, dan kun je de grens op de
commandoregel overschrijven:

```bash
mvn clean verify -Djacoco.minimum.coverage=0
```
