# TBEL (ThingsBoard Expression Language) — referência completa

> Fonte: `thingsboard.io/docs/user-guide/tbel/` (Overview, Language Guide, Helper
> Functions) e `thingsboard.io/docs/user-guide/calculated-fields/script/`, capturado em
> 2026-08-26. TBEL é usado em: Script Filter, Switch e Script Transformation nodes do
> Rule Engine, e em Script Calculated Fields.

## O que é

TBEL é uma linguagem de scripting leve para transformação de dados IoT, baseada em
**MVEL** (não é JavaScript completo) com sandbox de segurança e limite de memória por
execução. Comparado ao antigo engine Nashorn (JS), TBEL tem custo de inicialização
desprezível e isolamento de memória forte — por isso é a opção recomendada sobre
"JavaScript" nos Script nodes sempre que a sintaxe suportada for suficiente.

Restrição chave: **não é permitido instanciar classes Java diretamente**, só chamar
métodos estáticos:

```
java.util.Collections.reverse(list); // permitido
list = new java.util.ArrayList();    // NÃO permitido
```

Acesso a classes é limitado essencialmente a `java.lang`/`java.util` (ex: `Math.sqrt(4)`).

## Tipos de dados

```
125            // int
0353           // octal
0xAFF0         // hex
10.503         // double
94.92d         // double explícito
14.5f          // float
104.39484B     // BigDecimal
8.4I           // BigInteger
true / false
null / nil
"texto" / 'texto'         // strings, escapes \\ \n \r \u####
{"temperature": 42}        // Map
["A", "B", "C"]             // List
toSet(["B","A","C","A"])    // Set
new int[3]                  // array primitivo
```

Conversão automática de tipo em comparação: `"123" == 123` → `true`.

## Operadores

- Aritméticos: `+ - * /`
- Comparação: `== != > < >= <=`
- Lógicos: `&& || !`
- Ternário: `(condicao) ? valor1 : valor2` (precisa de parênteses fora de statement explícito)
- Bitwise: `& | ^ ~ << >> >>>`  (ex: `0xFF & 0x0F`)
- Safe navigation: `map.?nonExistingKey.smth` (evita NullPointerException)

## Controle de fluxo

```tbel
// if / else if / else
if (temperature > 0) {
    return "Greater than zero!";
} else if (temperature == -1) {
    return "Minus one!";
} else {
    return "Something else!";
}

// foreach
var numbers = [1, 2, 3];
var sum = 0;
foreach (n : numbers) { sum += n; }

// for
for (var i = 0; i < 100; i++) { sum += i; }

// while / until
while (isTrue()) { doSomething(); }
until (isFalse()) { doSomething(); }

// do-while / do-until
do { x = something(); } while (x != null);
do { x = something(); } until (x == null);
```

Declaração de variável: `var a = 2;` — múltiplos statements exigem `;`.

## Funções definidas pelo usuário

```tbel
function sum(list) {
    var result = 0;
    for (var i = 0; i < list.length; i++) {
        result += list[i];
    }
    return result;
}
var total = sum(array);
```

## Maps

```tbel
var map = {"temperature": 42, "nested": "508"};

map.temperature;             // dot notation
map.get("temperature");
map["temperature"];          // bracket notation
map.?nonExistingKey.smth;    // safe navigation

map.temperature = 0;
map.put("humidity", 73);
map.putIfAbsent("temperature1", 73);
map.replace("temperature", 56);
map.replace("temperature", 56, 42);   // replace só se valor atual == 56
map.remove("temperature");
map.putAll({"test": 12, "input": {"http": 130}});

var keys = map.keys();
var values = map.values();
var size = map.size();
var memorySize = map.memorySize();
map.sortByKey();
var sorted = map.sortByValue();
isMap(map);

foreach (element : map.entrySet()) {
    element.getKey();
    element.getValue();
}
foreach (value : map) { /* itera valores */ }

var unmodifiable = original.toUnmodifiable();
unmodifiable.put("x", 1);   // lança erro
```

## Lists

```tbel
var list = ["A", "B", "C"];
list[0];        // "A"
list.size();

list.add(3, "thingsboard");
list.push("thingsboard");
list.unshift("r");
list.addAll(["thingsboard", 4, 67]);
list.addAll(2, ["x", "y"]);

var removed = list.remove(2);
list.remove("C");
var first = list.shift();
var last = list.pop();
var spliced = list.splice(3);
list.splice(2, 2);
list.splice(1, 4, "start", 5, "end");

list.set(3, "65");
list[1] = "98";
list.sort();
list.sort(true);   // asc
list.sort(false);  // desc
list.reverse();
list.fill(67);
list.fill(4, 1);
list.fill(2, 1, 4);

// não-mutantes (retornam nova lista)
var sorted = list.toSorted();
var sortedDesc = list.toSorted(false);
var reversed = list.toReversed();
var sliced = list.slice(0, 2);
var replaced = list.with(1, 69);
var merged = list.concat(otherList);
var str = list.join();
var spliced = list.toSpliced(1, 0, "Feb");

var length = list.length();
var memorySize = list.memorySize();
var idx = list.indexOf("B", 1);
isList(list);

var unmodifiable = original.toUnmodifiable();
```

## Sets

```tbel
var set1 = toSet(["B", "A", "C", "A"]);   // de uma lista, deduplicando
var set2 = newSet();                      // vazio
// Não existe createSet(): as duas construtoras são newSet() e toSet(list).

set.add(35);
set.addAll(otherSet);
set.remove(4);
set.clear();
set.contains("A");
set.size();
set.toArray();
set.toList();
set.clone();
isSet(set);

set.sort();
set.sort(true);
set.sort(false);
var sorted = set.toSorted();

foreach (item : set) { /* ... */ }
```

## Arrays e Strings

```tbel
var array = new int[3];
array[0] = 1; array[1] = 2; array[2] = 3;

var str = "My String";
var ch = str[0];    // 'M' — acesso por índice retorna caractere
isArray(array);      // true

foreach (c : "ABCDEFGHIJKLMNOPQRSTUVWXYZ") { /* itera caracteres */ }
```

## JSON

```tbel
var metadataStr = JSON.stringify(metadata);
var metadata = JSON.parse(metadataStr);
```

## Helper functions — referência

### Encoding
- `btoa(input)` → Base64 a partir de string binária
- `atob(input)` → decodifica Base64

### Números
- `toFixed(value, precision)` → arredonda: `toFixed(0.345, 2)` = `0.35`
- `toInt(value)` → arredonda para inteiro: `toInt(2.7)` = `3`
- `parseInt/parseLong/parseFloat/parseDouble(str)` → parse de string para número
- `isNaN(value)` → `true`/`false`

### Strings / bytes
- `stringToBytes(input, [charset])` → List de bytes (UTF-8 default)
- `bytesToString(bytesList, [charset])` / `decodeToString(bytesList)` → String
- `decodeToJson(bytesList)` → objeto/valor JSON
- `padStart(str, len, padChar)` / `padEnd(str, len, padChar)`
- `encodeURI(uri)` / `decodeURI(uri)`

### Validação de formato
- `isBinary(str)` → `2` se binário, `-1` caso contrário
- `isOctal(str)` → `8` / `-1`
- `isDecimal(str)` → `10` / `-1`
- `isHexadecimal(str)` → `16` / `-1`

### Hex / binário / base
- `intToHex/longToHex/floatToHex/doubleToHex(value, [bigEndian], [pref], [len])`
- `intLongToRadixString(number, [radix], ...)` — binário(2)/octal(8)/decimal(10)/hex(16)
- `parseHexToInt/parseHexToFloat/parseHexToDouble(hex, [bigEndian])`
- `parseHexIntLongToFloat(hex, bigEndian)`
- `hexToBytes(hex)` / `hexToBytesArray(hex)` / `bytesToHex(bytesList)`
- `parseBytesToInt/parseBytesToLong/parseBytesToFloat/parseBytesLongToDouble(bytesList, [bigEndian])`
- `parseBytesIntToFloat(bytesList, [bigEndian])`
- `parseByteToBinaryArray(b)` / `parseBytesToBinaryArray(bytesList)` / `parseLongToBinaryArray(value)`
- `parseBinaryArrayToInt(binaryArray)`
- `printUnsignedBytes(byteArray)` → representação 0–255

### Base64
- `base64ToHex(str)` / `bytesToBase64(bytesList)` / `base64ToBytes(str)` / `base64ToBytesList(str)`

### JSON utilitário
- `toFlatMap(json, [pathInKey], [excludeList])` → achata JSON aninhado em map de nível único
  (ex: `{"nested":{"humidity":73}}` → `{"nested.humidity": 73}`)

### Datas (`tbDate`)
```tbel
new Date("2024-01-15")
new Date("2024-01-15", "yyyy-MM-dd", "en-US", "UTC")
new Date(2024, 1, 15, 14, 30, 45)   // ano, mês, dia, hora, min, seg, [timeZone]

date.addYears(n); date.addMonths(n); date.addWeeks(n); date.addDays(n);
date.addHours(n); date.addMinutes(n); date.addSeconds(n); date.addNanos(n);
// Todos são `public void`: modificam a data in-place e NÃO retornam nada.
// `x = date.addDays(1)` deixa x nulo — encadear/atribuir não funciona.
// Não existe addMilliseconds: a menor unidade é addNanos (use n * 1000000).
```

### Geofencing
- `isInsidePolygon(lat, lon, polygon)` → boolean
- `isInsideCircle(lat, lon, centerLat, centerLon, radiusKm)` → boolean

### Erros
- `raiseError(message)` → lança RuntimeException com a mensagem

## Calculated Fields — Simple vs. Script

Calculated Fields ficam associados a device/asset profile (não a um rule chain).
Existem **dois tipos**; escolher Simple primeiro e só subir para Script quando a lógica
não cabe numa expressão única:

- **Simple** (sem script): uma única **expressão matemática** produz um resultado.
  Operadores `+ - * /` e funções `abs`, `sqrt`, `pow`, `min`, `max`, `ln`, `log`, `ceil`,
  `floor`, `round`. Argumentos: **Latest telemetry** ou **Attribute**, de qualquer
  entidade (a própria, outro device/asset, customer, tenant ou owner). Saída: Time
  series ou Attribute, com `key`, `decimals` opcional, e (para time series) "Use latest
  timestamp". Bom para: métricas derivadas simples (ponto de orvalho, potência),
  normalização/calibração linear, conversão de unidade que o widget não cobre, clamping
  de valores, scoring simples. **Não usa TBEL** — não suporta `if`, loops, nem múltiplas
  chaves de saída.
- **Script**: usa TBEL (ver seções acima), suporta lógica condicional, iteração sobre
  janelas de telemetria (rolling), e retorno de múltiplas chaves/registros numa única
  execução (`function calculate(ctx, arg1, arg2, ...)`). Usar quando Simple não é
  suficiente — múltiplas saídas, lógica condicional, ou agregação (`.mean()`, `.merge()`
  etc.) sobre uma janela histórica.

TBEL detalhado abaixo é específico do tipo **Script**.

```javascript
function calculate(ctx, arg1, arg2, ...): object | object[]
```

Contexto `ctx`:

| Propriedade | Descrição |
|---|---|
| `ctx.latestTs` | timestamp mais recente entre os argumentos baseados em telemetria |
| `ctx.args.<arg>.ts` / `.value` | timestamp/valor de argumento de valor único |
| `ctx.args.<arg>.timeWindow` | `{ startTs, endTs }` de argumento tipo "rolling" |

Tipos de argumento: **Attribute** (valor único + fallback), **Latest telemetry** (valor
mais recente + fallback), **Time series rolling** (janela histórica com agregação).

Métodos de agregação em argumentos rolling: `.mean()`/`.avg()`, `.max()`, `.min()`,
`.sum()`, `.std()`, `.median()`, `.count()`, `.first()`, `.last()` — todos aceitam
`ignoreNaN` opcional (default `true`).

`.merge()` / `.mergeAll()` alinham timestamps entre séries diferentes, preenchendo
valores ausentes com o último valor conhecido; cada registro resultante vira
`{ ts, v1, v2, ... }`.

### Exemplo — conversão simples

```javascript
function calculate(ctx, temperatureF) {
  var temperatureC = (temperatureF - 32) / 1.8;
  return { "ts": ctx.latestTs, "values": { "temperatureC": toFixed(temperatureC, 2) } };
}
```

### Exemplo — cálculo cross-entity (densidade do ar)

```javascript
function calculate(ctx, altitude, temperature) {
  var avgTemperature = temperature.mean();
  var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15;
  var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude), 5.25588);
  var airDensity = pressure / (287.05 * temperatureK);
  return { "airDensity": toFixed(airDensity, 2) };
}
```

### Exemplo — detecção de anomalia com saída em array

```javascript
function calculate(ctx, temperature, defrost) {
  var merged = temperature.merge(defrost);
  var result = [];
  foreach(item: merged) {
    if (item.v1 > -5.0 && item.v2 == 0) {
      result.add({ ts: item.ts, values: { issue: { temperature: item.v1, defrostState: false } } });
    }
  }
  return result;
}
```
