# Code coverage

Hoe we testdekking meten en bewaken in deze module: welke tool, welke ondergrens en
waarom, en hoe het rapport uit de CI komt. Sluit aan op [CICD.md](CICD.md).

## In het kort

> Bij elke pull request controleert JaCoCo of minstens **80%** van onze code door de
> tests wordt uitgevoerd. Haalt het dat niet, dan faalt de build. We staan nu op
> **~83%**; de grens dient als bewaking tegen achteruitgang. Het rapport wordt als
> artefact bewaard.

## Wat en waarom

Code coverage meet welk deel van de code daadwerkelijk wordt uitgevoerd tijdens de
tests. Het zegt niet of de tests *goed* zijn, maar wél of er code is die helemaal niet
getest wordt — een blinde vlek. Bij een zorgsysteem dat patiëntgegevens verwerkt wil je
die klein houden. NEN 7510 vraagt om aantoonbaar en veilig ontwikkelen; kunnen laten
zien hoeveel van je code getest is, en bewaken dat dit niet wegzakt, hoort daarbij.

We gebruiken **JaCoCo**, de standaardtool voor Java/Maven. Hij draait volledig binnen
onze eigen build en CI — er gaat geen code of meetdata naar een externe partij.

## Hoe het is opgezet

De module heeft twee code-delen: `omod-common` (basis) en `omod` (vrijwel alle
REST-code én de meeste tests). JaCoCo doet drie dingen (zie `pom.xml`):

1. **meten** tijdens de tests (`prepare-agent`);
2. een **rapport** maken per module (`report`);
3. in de `omod`-module de meetdata van beide modules **samenvoegen** (`merge` +
   `report-aggregate`) en de ondergrens **bewaken** (`check`).

**Waarom samenvoegen?** Veel klassen in `omod-common` worden pas getest door de tests
in `omod`. Los gemeten lijkt `omod-common` daardoor maar 16% getest, terwijl het in
werkelijkheid 73% is. Pas samengevoegd krijg je een eerlijk cijfer. Daarom bewaken we
de gecombineerde meetdata, niet elke module apart.

## De ondergrens: 80%

Staat als één property boven in `pom.xml`:

```xml
<jacoco.minimum.coverage>0.80</jacoco.minimum.coverage>
```

Gemeten stand (unit tests van beide modules samen):

| Onderdeel | Instructie-dekking |
|-----------|--------------------|
| `omod`-klassen — **dit bewaakt de gate** | **86,6%** |
| Gecombineerd (omod-common + omod) | 82,8% |

Waarom 80%:

- **Gemeten, niet gegokt.** De stand is ~83–86%; we leggen de lat op een rond getal er
  net onder. Dat geeft ruimte voor normale schommeling (en het kleine verschil tussen
  lokaal Java 8 en de CI op Java 17) zonder bij elke wijziging te knellen.
- **Bewaking tegen achteruitgang**, geen doel om te "halen" — we staan er al ruim boven.
- **80% is een gangbare norm.** 90–100% eisen dwingt tot zinloze tests op triviale code.
- **Eén regel om bij te stellen** (de property), bv. later naar 85%.

De gate blokkeert bewust: testdekking van onze eigen code is iets dat wij zelf in de
hand hebben — precies het soort check dat volgens [CICD.md](CICD.md) mág blokkeren.

## Rapport als CI-artefact

In [ci.yml](../.github/workflows/ci.yml) draait de build als `mvn clean verify`, zodat
het rapport wordt gemaakt en de gate draait. Dit gebeurt **alleen bij een pull
request** (de gate hoort vóór de merge; de deploy daarna is puur uitrollen). Het rapport
wordt bewaard als artefact **`code-coverage-report`**.

Vinden: **Actions → de `CI / Deploy Pipeline`-run → onderaan bij Artifacts →
`code-coverage-report`**. Uitpakken en `jacoco-aggregate/index.html` openen voor het
klikbare overzicht per package en klasse.

## Zelf draaien

```bash
mvn clean verify                          # bouwen + tests + rapport + de 80%-gate
# rapport: omod/target/site/jacoco-aggregate/index.html

mvn clean verify -Djacoco.minimum.coverage=0   # wel meten, niet falen op de grens
```

> Deze module bouwt officieel op **Java 17** (zoals de CI); de maatgevende cijfers komen
> uit de CI-run.
