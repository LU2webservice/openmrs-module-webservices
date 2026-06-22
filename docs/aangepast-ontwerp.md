# Aangepast ontwerp — webservices.rest module

> **Wat is dit?** Het herontwerp van de onderhoudbaarheids-hotspots uit de
> [onderhoudbaarheid-analyse](onderhoudbaarheid-analyse.md). Per hotspot beschrijf ik het
> probleem (huidig ontwerp), het **aangepaste ontwerp**, de toegepaste **ontwerpprincipes en
> -patronen**, de **afgewogen alternatieven** en de **motivatie op kwaliteitseisen** (ISO/IEC 25010).
> Het is een ontwerpvoorstel: het sluit direct aan op het [verbeterplan](improvements.md) (O1–O6).

## Aanpak en kwaliteitseisen

De analyse liet zien dat de onderhoudbaarheid over de hele linie goed is, maar dat het risico
**geconcentreerd** zit in een handvol plekken: `ConversionUtil.convert()` (cyclomatische
complexiteit 48), de `search()`-handlers (CC 21–31) en 16 lege `catch`-blokken. Het herontwerp
richt zich op die plekken en wordt gestuurd door drie meetbare kwaliteitseisen uit ISO/IEC 25010:

| Kwaliteitseis (ISO 25010) | Doel in dit ontwerp |
|---|---|
| **Analyseerbaarheid** | Geen methode boven CC 10; gedrag per geval lokaal te begrijpen |
| **Wijzigbaarheid** | Een nieuw type/resource toevoegen zonder bestaande methoden te wijzigen (Open/Closed) |
| **Herbruikbaarheid** | Gedeelde logica één keer; geen copy-paste tussen versies |

Leidende **ontwerpprincipes**: SOLID (vooral **Open/Closed** en **Single Responsibility**),
**DRY** en **fail-fast** (geen stille fouten).

---

## Implementaties

### 1. `ConversionUtil.convert()` — van mega-conditional naar Strategy + Registry

**Huidig ontwerp (probleem).** `convert()` is één methode van ~118 regels met CC 48: een lange
keten van `if/else` en `instanceof`-checks die per doeltype een andere conversie kiest. Elke
nieuwe conversie betekent: déze methode openbreken. Dat schendt Open/Closed, is moeilijk te
testen (48 paden) en is meteen de klasse met de meeste code smells (17).

**Aangepast ontwerp.** Vervang de conditional door **polymorfisme**: elke conversie wordt een
eigen `Converter`-strategie, geregistreerd in een `ConverterRegistry`. `convert()` zoekt de
juiste strategie op en delegeert.

```text
// VOOR — één methode, alle gevallen
Object convert(src, targetType) {
    if (targetType == Date.class) { ... }
    else if (targetType == Locale.class) { ... }
    else if (src instanceof Collection) { ... }
    else if (...) { ... }            // tientallen takken, CC 48
}

// NA — Strategy + Registry (Open/Closed)
interface Converter<T> { boolean supports(Class<?> t); T convert(Object src); }

class ConverterRegistry {
    private final List<Converter<?>> converters;        // bv. via Spring geïnjecteerd
    Converter<?> find(Class<?> t) {
        return converters.stream().filter(c -> c.supports(t)).findFirst()
               .orElseThrow(() -> new ConversionException(t));   // fail-fast
    }
}

class ConversionUtil {
    Object convert(Object src, Class<?> targetType) {
        return registry.find(targetType).convert(src);  // CC ~2
    }
}
// DateConverter, LocaleConverter, CollectionConverter, ... elk CC laag en los testbaar
```

**Toegepaste patronen/principes:** *Strategy* (uitwisselbare conversie-algoritmen), *Factory/Registry*
(opzoeken van de juiste strategie), refactoring *Replace Conditional with Polymorphism* en
*Extract Class*. Principe: **Open/Closed** (nieuwe converter = nieuwe klasse, `convert()` blijft
ongemoeid) en **Single Responsibility** (één converter per type).

**Afgewogen alternatieven.**

| Alternatief | Voordeel | Nadeel | Besluit |
|---|---|---|---|
| Conditional houden + alleen *Extract Method* per tak | Klein, weinig risico | CC blijft hoog in `convert()`; schendt OCP nog steeds | Afgewezen — lost de hotspot niet op |
| **Strategy + Registry** | CC valt naar ~2; nieuw type zonder wijziging; los testbaar | Meer klassen | **Gekozen** — beste analyseerbaarheid + wijzigbaarheid |
| Externe lib (Spring `ConversionService`) | Geen eigen code | Grote afhankelijkheid; herschrijft de hele REST-conversielaag; regressierisico | Afgewezen — te ingrijpend voor de winst |

---

### 2. `search()`-handlers — Template Method tegen complexiteit én duplicatie

**Huidig ontwerp (probleem).** Vijf van de acht complexiteits-hotspots zijn `search()`-methoden
(`OrderSearchHandler2_2/2_3`, `ConceptSearchHandler1_8`, …). Elke handler herhaalt hetzelfde
skelet: parameters lezen → valideren → query bouwen → pagineren → resultaat verpakken. Dat geeft
zowel hoge complexiteit per methode als duplicatie tussen handlers (o.a. `OrderSearchHandler2_2`
↔ `2_3`, 33 regels).

**Aangepast ontwerp.** Trek het vaste skelet omhoog in een abstracte basisklasse
(**Template Method**); per handler blijven alleen de variabele stappen over als hooks.

```text
abstract class BaseSearchHandler implements SearchHandler {
    public final PageableResult search(RequestContext ctx) {   // vast skelet, final
        Map<String,String> p = readParams(ctx);
        validate(p);                       // hook
        Criteria q = buildQuery(p, ctx);   // hook (variabel deel)
        return paginate(q, ctx);           // gedeeld
    }
    protected abstract void validate(Map<String,String> p);
    protected abstract Criteria buildQuery(Map<String,String> p, RequestContext ctx);
}

class OrderSearchHandler2_3 extends BaseSearchHandler {
    protected Criteria buildQuery(...) { /* alleen het order-specifieke deel */ }
}
```

**Toegepaste patronen/principes:** *Template Method* (vast algoritme-skelet, variabele stappen),
refactoring *Extract Superclass* en *Pull Up Method*. Principe: **DRY** (skelet één keer) en
**Single Responsibility** (handler beschrijft alleen zijn eigen query).

**Afgewogen alternatieven.**

| Alternatief | Afweging | Besluit |
|---|---|---|
| **Template Method** (overerving) | Skelet vast afgedwongen; minste duplicatie | **Gekozen** voor het gedeelde skelet — sluit aan op de bestaande `SearchHandler`-interface |
| Helper-/utilityklasse (compositie) | "Composition over inheritance"; flexibeler | Handlers moeten zelf de helper aanroepen → skelet niet afgedwongen, drift mogelijk | Deels — voor `paginate()` als losse helper |
| Niets doen (versie-duplicatie accepteren) | Backward-compat veilig | Blijvende onderhoudslast bij elke versie | Afgewezen voor de niet-versie-duplicaten |

> Nuance: een deel van de duplicatie is **versie-gerelateerd** (`2_2` vs `2_3`) en bewust, om
> backward-compatibiliteit niet te breken. Het herontwerp richt zich op het **gedeelde skelet**,
> niet op het samenvoegen van versies die uiteen moeten kunnen lopen.

---

### 3. Lege `catch`-blokken — centrale, fail-fast foutafhandeling

**Huidig ontwerp (probleem).** 16 lege `catch`-blokken (o.a. in `ConversionUtil` en
`SettingsFormController`) slokken uitzonderingen stil op. Dat schaadt de analyseerbaarheid: een
fout verdwijnt zonder spoor, en het verbergt de eerdere bevindingen.

**Aangepast ontwerp.** Pas **fail-fast** toe: vang alleen wat je echt kunt afhandelen, log de
rest, of laat 'm door naar een **centrale exception handler**. De REST-laag heeft al een
`BaseRestController`/exception-mapping; nieuwe en bestaande fouten worden daar consistent op een
HTTP-status gemapt in plaats van per `catch` genegeerd.

```text
// VOOR
try { value = parse(input); } catch (Exception e) { /* stil */ }

// NA — geen stille fout
try { value = parse(input); }
catch (ParseException e) { throw new ConversionException("ongeldige waarde", e); } // → centrale handler → HTTP 400
```

**Toegepaste patronen/principes:** *fail-fast*, gecentraliseerde foutafhandeling
(*Exception Handler* / `@ControllerAdvice`-stijl). Principe: geen "swallow", herleidbaarheid.

**Afgewogen alternatieven.** Per `catch` lokaal loggen (snel, maar versnipperd en inconsistente
HTTP-statussen) versus één centrale handler (consistente foutrespons, sluit aan op de bestaande
mapping) — **gekozen: centraal**, met lokaal loggen alleen waar de fout écht lokaal hoort.

---

### 4. `DrugSearchByMappingHandler` / `DrugsSearchByMappingHandler` — samenvoegen

**Huidig ontwerp (probleem).** Twee handlers verschillen alleen in de "s" in de naam en delen 41
regels identieke code. Dit is **niet** versie-gerelateerd, dus pure duplicatie.

**Aangepast ontwerp.** Eén handler behouden, de andere laten delegeren of verwijderen
(*Extract/Move Method*, **DRY**). Alternatief — beide laten staan met een gedeelde private helper
— is zwakker omdat de dubbele klasse blijft bestaan; daarom samenvoegen.

---

## Ontwerppatronen

Samenvatting van de toegepaste patronen, principes en refactorings, met het kwaliteitskenmerk dat
elk bedient.

| Patroon / principe | Waar toegepast | Type | Kwaliteitskenmerk (ISO 25010) |
|---|---|---|---|
| **Strategy** | `Converter`-strategieën i.p.v. `convert()`-conditional | Ontwerppatroon (gedrag) | Wijzigbaarheid, analyseerbaarheid |
| **Factory / Registry** | `ConverterRegistry.find(type)` | Ontwerppatroon (creatie) | Wijzigbaarheid |
| **Template Method** | `BaseSearchHandler.search()` skelet | Ontwerppatroon (gedrag) | Herbruikbaarheid, analyseerbaarheid |
| **Replace Conditional with Polymorphism** | wegwerken CC-48-keten | Refactoringpatroon | Analyseerbaarheid |
| **Extract Class / Superclass, Pull Up Method** | converters, search-basis | Refactoringpatroon | Modulariteit, herbruikbaarheid |
| **Exception Handler (centraal)** | lege `catch` → centrale mapping | Ontwerppatroon (architectuur) | Analyseerbaarheid, betrouwbaarheid |
| **SOLID — Open/Closed** | nieuw type/resource zonder wijziging | Ontwerpprincipe | Wijzigbaarheid |
| **SOLID — Single Responsibility** | één converter/handler per verantwoordelijkheid | Ontwerpprincipe | Analyseerbaarheid |
| **DRY** | gedeeld skelet, samengevoegde handlers | Ontwerpprincipe | Herbruikbaarheid |
| **Fail-fast** | geen stille `catch` | Ontwerpprincipe | Betrouwbaarheid |

### Onderbouwing van de keuzes (samengevat)

Elke keuze is gemotiveerd op een meetbare kwaliteitseis, niet op smaak:

- **Strategy boven "Extract Method op de conditional"**: alleen extracten haalt de complexiteit
  niet weg en houdt de OCP-schending; Strategy brengt `convert()` naar CC ~2 én maakt nieuwe types
  toevoegbaar zonder bestaande code te raken.
- **Template Method boven volledige compositie**: de bestaande `SearchHandler`-interface en de
  wens om het skelet áf te dwingen maken overerving hier passend; pure compositie zou het skelet
  optioneel maken en drift toelaten.
- **Centrale exception handling boven per-`catch`-logging**: consistente HTTP-foutcodes en één
  plek om te onderhouden, in lijn met de al aanwezige REST-foutmapping.

### Restrisico en scope

Dit is een **ontwerpvoorstel**; de hotspots zijn nog niet geherstructureerd. De refactors raken
veelgebruikte kerncode (`ConversionUtil`, search-handlers), dus elke wijziging moet door de
bestaande testsuite (1.910 tests) en de coverage-gate (80%) heen voordat ze landt — zo borgt het
herontwerp de onderhoudbaarheid zonder de bestaande werking te breken.
