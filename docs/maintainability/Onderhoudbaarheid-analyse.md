# Onderhoudbaarheid-analyse — webservices.rest module

> **Wat is dit bestand?**
> Een **systematische analyse van de onderhoudbaarheid** van de OpenMRS REST Webservices
> module, onderbouwd met **echte, reproduceerbare metrieken**. Ik meet de code met
> standaardtools (PMD, PMD-CPD, JaCoCo) plus een transparant complexiteitsscript, koppel de
> uitkomsten aan de onderhoudbaarheids-subkenmerken van **ISO/IEC 25010**, wijs de
> **hotspots** aan en verbind die met het bestaande [verbeterplan](Improvements.md).
>
> Alles in dit document is op **2026-06-16** echt gedraaid op branch `code-tests-logging`;
> de commando's staan erbij zodat het na te rekenen is.

| | |
|---|---|
| **Module** | OpenMRS REST Webservices Module v3.2.0 (`/ws/rest/v1/...`) |
| **Scope** | Productiecode in `omod` en `omod-common` (geen test- of integratiecode) |
| **Datum meting** | 2026-06-16 |
| **Tools** | PMD 6.55.0 (smells), PMD-CPD (duplicatie), JaCoCo 0.8.13 (testdekking), McCabe-script (complexiteit) |
| **Norm** | ISO/IEC 25010 (onderhoudbaarheid) · NEN 7510-2:2024 8.28 (veilig coderen) |

---

## 1. Doel en aanpak

Onderhoudbaarheid bepaalt hoeveel moeite het kost om de module te begrijpen, te wijzigen en
uit te breiden zonder nieuwe fouten te introduceren. Voor een EPD-module die de centrale
REST-ingang naar klinische data is, is dat direct een **kwaliteits- en veiligheidsfactor**:
slecht onderhoudbare code is moeilijker veilig te houden.

Ik analyseer langs de vijf onderhoudbaarheids-subkenmerken van **ISO/IEC 25010**:

| Subkenmerk | Wat het betekent | Gemeten met |
|---|---|---|
| **Modulariteit** | Zijn componenten losjes gekoppeld? | Modulestructuur, duplicatie |
| **Herbruikbaarheid** | Kan code hergebruikt worden i.p.v. gekopieerd? | CPD-duplicatie |
| **Analyseerbaarheid** | Hoe makkelijk begrijp je de code / vind je een fout? | Complexiteit, omvang, smells |
| **Wijzigbaarheid** | Hoe risicovol is een wijziging? | Complexiteit, koppeling, smells |
| **Testbaarheid** | Hoe goed is de code te testen / getest? | JaCoCo-dekking |

> **Reproduceerbaarheid.** De metingen draaien lokaal/offline, zonder externe dienst.

```bash
# Code smells + duplicatie (genereert target/pmd.xml en target/cpd.xml per module)
mvn org.apache.maven.plugins:maven-pmd-plugin:3.21.2:pmd \
    org.apache.maven.plugins:maven-pmd-plugin:3.21.2:cpd -pl omod,omod-common

# Testdekking
mvn clean verify           # rapport: coverage-report/target/site/jacoco-aggregate/index.html
```

---

## 2. Omvang (volume)

De basis: hoe groot is het systeem?

| Metriek | Waarde |
|---|---|
| Productie-Java-bestanden (`omod` + `omod-common`) | **345** |
| Regels code (incl. commentaar/wit) | **44.111** |
| Methoden | **2.350** |
| Gemiddelde methodelengte | ~19 regels |

**Interpretatie:** een middelgrote module. De omvang op zich is geen probleem; het volume is
verdeeld over veel kleine klassen (gemiddeld ~128 regels per bestand), wat past bij de
resource-per-domeinobject-opzet van OpenMRS.

---

## 3. Complexiteit (analyseerbaarheid & wijzigbaarheid)

Cyclomatische complexiteit (McCabe) telt het aantal onafhankelijke paden door een methode:
`1 + aantal beslispunten` (`if`, `for`, `while`, `case`, `catch`, `&&`, `||`, `?`). Hoe hoger,
hoe moeilijker te begrijpen en te testen. Vuistregel: 1–10 prima, 11–20 aandacht, 21+ riskant.

| Metriek | Waarde | Oordeel |
|---|---|---|
| Gemiddelde CC per methode | **2,05** | Zeer goed |
| Mediaan CC | **1** | Zeer goed |
| Maximale CC | **48** | 1 uitschieter |

**Verdeling over 2.350 methoden:**

| Complexiteit | Aantal | Aandeel |
|---|:---:|:---:|
| 1–5 (laag) | 2.228 | **94%** |
| 6–10 (matig) | 85 | 3% |
| 11–20 (hoog) | 29 | 1% |
| 21+ (zeer hoog) | 8 | 0,3% |

**Interpretatie:** de overgrote meerderheid (94%) is laag-complex en dus goed analyseerbaar.
Het risico concentreert zich in een **kleine, goed afgebakende set** van 8 methoden (0,3%).
Dat is gunstig: gericht refactoren van die hotspots levert het meeste op.

### 3.1 Complexiteits-hotspots (top 8)

| CC | Bestand | Methode | Regels | Type |
|:--:|---|---|:--:|---|
| **48** | `ConversionUtil.java` | `convert()` | 118 | Centrale conversie-helper |
| **40** | `ObsResource1_8.java` | `setValue()` | 118 | Domein (Obs-waarde) |
| **38** | `SwaggerSpecificationCreator.java` | `testOperationImplemented()` | 171 | Swagger-generatie |
| **31** | `OrderSearchHandler2_3.java` | `search()` | 123 | Zoek-handler |
| **28** | `ConceptSearchHandler1_8.java` | `search()` | 115 | Zoek-handler |
| **25** | `RelationshipSearchHandler1_8.java` | `search()` | 72 | Zoek-handler |
| **21** | `SimpleObjectConverter.java` | `marshal()` | 33 | Serialisatie |
| **21** | `OrderSearchHandler2_2.java` | `search()` | 85 | Zoek-handler |

**Patroon:** de zwaarste methoden zijn (a) twee centrale helpers (`ConversionUtil.convert`,
`SimpleObjectConverter.marshal`) en (b) de `search()`-methoden van de zoek-handlers. Dit zijn
precies de plekken waar je bij een wijziging het meest oplet — en, niet toevallig, ook waar de
inputvalidatie-vraag uit de [attack surface](../security/attack-surface.md) speelt (T-2).

---

## 4. Duplicatie (herbruikbaarheid & modulariteit)

Gemeten met PMD-CPD (copy-paste-detector). Duplicatie is slecht onderhoudbaar: een fix moet je
dan op meerdere plekken doen, met het risico dat je er één vergeet.

| Metriek | Waarde | Oordeel |
|---|---|---|
| Duplicatie-blokken | **15** | Laag |
| Gedupliceerde regels (som) | **383** | ~0,9% van de codebase |
| Grootste blok | **56 regels** | Aandachtspunt |

### 4.1 Grootste duplicaten

| Regels | Tussen bestanden |
|:--:|---|
| 56 | `LocationResource2_0` ↔ `PersonAddressResource2_0` |
| 41 | `DrugSearchByMappingHandler1_10` ↔ `DrugsSearchByMappingHandler1_10` |
| 34 | `DefinitionProperty` ↔ `Parameter` |
| 33 | `OrderSearchHandler2_2` ↔ `OrderSearchHandler2_3` |
| 29 | `ModuleActionResource1_8` ↔ `ModuleResource1_8` |

**Interpretatie:** met ~0,9% is de duplicatie **laag** (een gangbare drempel is 3–5%). De
duplicaten zijn bovendien grotendeels **verklaarbaar**: OpenMRS ondersteunt meerdere
platformversies naast elkaar (bv. `OrderSearchHandler2_2` vs `2_3`), waardoor bewust een
vorige versie wordt gekopieerd om backward-compatibiliteit niet te breken. Het duo
`DrugSearchByMappingHandler` / `DrugsSearchByMappingHandler` (let op de "s") is wél een
kandidaat om samen te voegen.

---

## 5. Code smells (analyseerbaarheid)

PMD met de standaard quickstart-ruleset (best practices + error-prone + design).

| Metriek | Waarde |
|---|---|
| Totaal meldingen | **87** |
| Prioriteit 1–2 (kritiek/hoog) | **0** |
| Prioriteit 3 (middel) | 53 |
| Prioriteit 4 (laag) | 34 |

**Top-meldingen:**

| Aantal | Regel | Onderhoudbaarheidsimpact |
|:--:|---|---|
| 17 | `UselessParentheses` | Cosmetisch — leesbaarheid |
| 16 | `EmptyCatchBlock` | **Aandacht** — stil opgeslokte fouten verbergen problemen |
| 13 | `UnnecessaryModifier` | Cosmetisch |
| 12 | `UnnecessaryFullyQualifiedName` | Cosmetisch |
| 7 | `SimplifiedTernary` | Leesbaarheid |
| 4 | `UnusedPrivateMethod` | Dode code |
| 3 | `UnusedFormalParameter` | Dode code / misleidende API |

**Interpretatie:** **geen enkele prioriteit-1/2-melding** — er zijn geen ernstige
structurele smells. Het meeste is cosmetisch (overbodige haakjes/modifiers). Het enige
inhoudelijke aandachtspunt is **`EmptyCatchBlock` (16×)**: lege `catch`-blokken slokken
uitzonderingen op en maken fouten lastig te traceren. De zwaarste smell-concentratie zit in
`ConversionUtil.java` (17 meldingen) — dezelfde klasse als de complexiteits-hotspot, dus een
duidelijke prioriteit.

---

## 6. Testbaarheid (testability)

Onderhoudbaarheid en testbaarheid versterken elkaar: testbare code is doorgaans minder complex
en beter gemoduleerd, en een hoge testdekking maakt wijzigingen veiliger.

| Metriek | Waarde | Bron |
|---|---|---|
| Gecombineerde instructie-dekking | **82,8%** | JaCoCo-aggregaat |
| Dekking `omod` (gate-module) | **86,6%** | JaCoCo |
| Coverage-gate (faalt build eronder) | **80%** | `pom.xml` |
| Geautomatiseerde tests | **1.910** | volledige testrun |

Details en de onderbouwing van de 80%-drempel staan in [CODE_COVERAGE.md](../CODE_COVERAGE.md).
**Interpretatie:** ruim boven de norm; de codebase is goed testbaar en de gate beschermt
tegen sluipende verslechtering.

---

## 7. Samenvattend onderhoudbaarheidsoordeel (ISO 25010)

| Subkenmerk | Score | Onderbouwing (metriek) |
|---|:---:|---|
| **Modulariteit** | Goed | Veel kleine klassen (~128 regels); lage duplicatie (~0,9%) |
| **Herbruikbaarheid** | Goed | 15 duplicatieblokken, grotendeels versie-gerelateerd en verklaarbaar |
| **Analyseerbaarheid** | Goed | Gem. CC 2,05; 94% methoden laag-complex; 0 kritieke smells |
| **Wijzigbaarheid** | Voldoende–Goed | Sterk gemiddelde, maar 8 hotspot-methoden (CC 21–48) verhogen het wijzigingsrisico lokaal |
| **Testbaarheid** | Goed | 82,8%/86,6% dekking met een blokkerende gate |

**Totaalbeeld:** de module is **goed onderhoudbaar**. De kwaliteit is over de hele linie hoog;
het risico is **niet diffuus maar geconcentreerd** in een handvol complexe methoden en één
klasse (`ConversionUtil`). Dat maakt verbetering goedkoop en gericht.

---

## 8. Concrete verbeteracties (gekoppeld aan het verbeterplan)

Deze acties volgen rechtstreeks uit de metingen hierboven. Ze sluiten aan op §7 van
[Improvements.md](Improvements.md) (onderhoudbaarheid & testbaarheid).

| # | Actie | Aanleiding (metriek) | Effort |
|:--:|---|---|:--:|
| O1 | Refactor `ConversionUtil.convert()` (CC 48) — opsplitsen per doeltype | Hoogste complexiteit én meeste smells (17) in één klasse | Middel |
| O2 | Vereenvoudig de `search()`-handlers (CC 21–31) — gedeelde basislogica extraheren | 5 van de top-8 hotspots zijn `search()`-methoden | Middel |
| O3 | Vervang de 16 `EmptyCatchBlock`'s door logging of een bewuste comment | Stil opgeslokte fouten schaden analyseerbaarheid | Klein |
| O4 | Voeg `DrugSearchByMappingHandler` en `DrugsSearchByMappingHandler` samen | 41 regels duplicatie, niet versie-gerelateerd | Klein |
| O5 | Ruim cosmetische smells op (overbodige haakjes/modifiers/imports) | 87 → richting 0; houdt toekomstige scans schoon | Klein |
| O6 | Verhoog de coverage-gate naar 85% zodra structureel gehaald | Huidige 82,8% ligt al boven 80% | Klein |

> Geen van deze acties is een security-blocker — ze verbeteren de **onderhoudbaarheid**, niet
> de veiligheid. De security-prioriteiten staan apart in de
> [security backlog](../security/Security_Backlog_Pentest_Rapport.md).

---

## 9. Herleidbaarheid

| Onderdeel | Locatie |
|---|---|
| Smells-rapport (PMD) | `omod/target/pmd.xml`, `omod-common/target/pmd.xml` |
| Duplicatie-rapport (CPD) | `omod/target/cpd.xml`, `omod-common/target/cpd.xml` |
| Testdekking (JaCoCo) | `coverage-report/target/site/jacoco-aggregate/index.html` |
| Complexiteitsmethode | McCabe: `1 + aantal {if,for,while,case,catch,&&,||,?}` per methode |
| Verbeterplan (acties) | [Improvements.md](Improvements.md) §7 |
| Testbaarheid-onderbouwing | [CODE_COVERAGE.md](../CODE_COVERAGE.md) |
