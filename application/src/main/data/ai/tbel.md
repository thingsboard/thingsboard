* TOC
{:toc}


ThingsBoard supports user-defined functions (UDF) for data processing in the [Rule Engine]() and [Data Converters](). 
The original programming language for the UDF is JavaScript. It is popular, well-known, and simple. We plan to support JavaScript forever.
Nevertheless, we have decided to provide an alternative to JavaScript. You may find our motivation below.

## Motivation

ThingsBoard is written in Java and currently uses Java 11. There are two ways to execute the JS function in ThingsBoard:  

A) use remote JS Executor microservice written in Node.js. It is the default way for ThingsBoard to run in a cluster/microservices mode;

B) use local JS Executor powered by Nashorn JS engine that runs inside the JVM. It is the default way for ThingsBoard while deployed in monolithic mode.

The Nashorn JS engine is now deprecated and is removed from Java 16. It is also relatively slow. Some users already called Nashorn to be the platform's kryptonite.
That is why it was absolutely clear that we need a replacement to Nashorn. 
Many users suggested [GraalVM](https://www.graalvm.org/) for its built-in polyglot feature, but it was not suitable for us for multiple reasons.
The most important reason is security and the ability to control every aspect of the UDF execution.
Besides, most UDFs are relatively simple functions that transform or filter data, and we want to find a more effective way to execute them.   

Our search for existing Script/Expression Language (EL) implementations led us to the [MVEL](https://github.com/mvel/mvel).
The ThingsBoard Expression Language (TBEL) is basically a [fork](https://github.com/thingsboard/tbel) of MVEL with some important security constraints, 
built-in memory management, and frequently used helper functions that are specific to ThingsBoard.
 
### TBEL vs Nashorn

TBEL is lightweight and is super fast compared to Nashorn. For example, the execution of 1000 simple scripts like:
"return msg.temperature > 20" took 16 seconds for Nashorn and 12 milliseconds for MVEL. More than 1000 times faster.
We have forked the TBEL codebase and added additional security improvements to ensure no CPU or memory abuse. 
So, no need to run Nashorn with the sandbox environment.

Of course, TBEL is not as powerful as JS, but the majority of the use cases do not need this.
{% if docsPrefix contains 'paas/' %}
The one who requires JS flexibility may use remote [JS Executors](/docs/pe/reference/msa/#javascript-executor-microservices) as usual.
{% else %}
The one who requires JS flexibility may use remote [JS Executors](/docs/{{docsPrefix}}reference/msa/#javascript-executor-microservices) as usual.
{% endif %}

### TBEL vs JS Executors

{% if docsPrefix contains 'paas/' %}
[JS Executors](/docs/pe/reference/msa/#javascript-executor-microservices) is a separate microservice based on Node.js.
{% else %}
[JS Executors](/docs/{{docsPrefix}}reference/msa/#javascript-executor-microservices) is a separate microservice based on Node.js.
{% endif %}
It is powerful and supports the latest JS language standards. 
However, there is a noticeable overhead to execute JS functions remotely. 
The Rule Engine and JS executors communicate through the queue. 
This process consumes resources and introduces a relatively small latency (a few ms). 
The latter may become a problem if you have multiple rule nodes chained into one rule chain.  

TBEL execution consumes much less resources and has no extra latency for inter-process communications.

## TBEL Language Guide

TBEL is used to evaluate expressions written using Java syntax. Unlike Java however, TBEL is dynamically typed (with optional typing), meaning that the source code does not require type qualification.
The TBEL expression can be as simple as a single identifier, or as complicated as an expression with method calls and inline collections.


### Syntax Constraints for Loops and Conditions

In TBEL (ThingsBoard Expression Language), certain syntax restrictions are enforced to ensure compatibility and reliability within the scripting environment. Use <code style="color: sienna;">foreach</code> instead of traditional <code style="color: sienna;">for</code> loops, and always wrap <code style="color: sienna;">ternary (? :) expressions</code> in parentheses when used without an explicit <code style="color: sienna;">if</code> statement.

✅ Allowed:

```java
foreach (ValueType value : map) {
    // Process each value
}
```
{: .copy-code}

❌ Not allowed:

```java
for (ValueType value : map) {
    // Process each value
}
```
{: .copy-code}

✅ Correct:

```java
var msgNew = {
        keyNew: (msg.humidity > 50 ? 3 : 0)    // if (msg.humidity > 50) == true, result msgNew = { keyNew: : 3  } 
     };
```
{: .copy-code}

❌ Incorrect:

```java
var msgNew = {
        keyNew: msg.humidity > 50 ? 3 : 0       // result msgNew = { 3 : 0  }
     };
```
{: .copy-code}

### Simple Property Expression

```java
msg.temperature
```
{: .copy-code}

In this expression, we simply have a single identifier (msg.temperature), which by itself, is what we refer to in TBEL as a property expression, 
in that the only purpose of the expression is to extract a property out of a variable or context object.

TBEL can even be used for evaluating a boolean expression. 
Assuming you are using TBEL in the Rule Engine to define a simple script [filter node](https://thingsboard.io/docs/{{docsPrefix}}user-guide/rule-engine-2-0/filter-nodes/#script-filter-node):

```java
return msg.temperature > 10;
```
{: .copy-code}

Like Java, TBEL supports the full gamut of operator precedence rules, including the ability to use bracketing to control execution order.

```java
return (msg.temperature > 10 && msg.temperature < 20) || (msg.humidity > 10 && msg.humidity < 60);
```
{: .copy-code}

### Multiple statements

You may write scripts with an arbitrary number of statements using the semi-colon to denote the termination of a statement. 
This is required in all cases except in cases where there is only one statement, or for the last statement in a script.

```java
var a = 2;
var b = 2;
return a + b
```
{: .copy-code}

Note the lack of a semi-colon after 'a + b'. New lines are not substitutes for the use of the semi-colon in MVEL.

### Value Coercion

MVEL’s type coercion system is applied in cases where two incomparable types are presented by attempting to coerce the ‘’right’‘value to that of the type of the’‘left’’ value, and then vice-versa.

For example:

```java
"123" == 123;
```
{: .copy-code}

This expression is *true* in TBEL because the type coercion system will coerce the untyped number *123* to a String in order to perform the comparison.

### Maps

TBEL allows you to create Maps. We use our own implementation of the Map to control memory usage.
That is why TBEL allows only inline creation of maps.

```java
// Create new map
var map = {"temperature": 42, "nested" : "508"};
```
{: .copy-code}

There are two syntax variations for iterating over a map:
1. Explicit entrySet() iteration:

```java
// Iterate through the map
foreach(element : map.entrySet()){
        // Get the key
        element.getKey();
        //or 
        element.key;
        
        // Get the value
        element.getValue();
        //or 
        element.value;
}

```
{: .copy-code}

2. Implicit iteration without entrySet():

```java
// Iterate through the map without entrySet()
foreach (ValueType value : map) {
        // Process each value
}
```
{: .copy-code}

Most common operation with the map:

**Examples:**

```java
// Change value of the key
map.temperature = 0;

// Check existance of the key
if(map.temperature != null){
// <you code>
}
// Null-Safe expressions using ?
if(map.?nonExistingKey.smth > 10){
// <you code>
}

// get Info
var size = map.size();                           // return  2 
var memorySize = map.memorySize();               // return 24 

map.humidity = 73;                              // return nothing => map = {"temperature": 0, "nested": "508", "humidity": 73}
map.put("humidity", 73);                        // return nothing => map ={"temperature": 0, "nested": "508", "humidity": 73}
map.putIfAbsent("temperature1", 73);            // return nothing => map = {"temperature": 0, "nested": "508", "humidity": 73, "temperature1": 73}

//change Value   
map.temperature = 73;                           // return nothing => map = {"temperature": 73, "nested": "508", "humidity": 73, "temperature1": 73}        
var put1 = map.put("temperature", 73);          // return put1 = 73 => map = {"temperature": 73, "nested": "508", "humidity": 73, "temperature1": 73}
var putIfAbsent1 = map.putIfAbsent("temperature", 73); // return putIfAbsent1 = 73 => map = {"temperature": 73, "nested": "508", "humidity": 73, "temperature1": 73}       
var replace = map.replace("temperature", 56);          // return 73 => map = {"temperature": 56, "nested": "508", "humidity": 73, "temperature1": 73}       
var replace1 = map.replace("temperature", 56, 42);     // return true => map = {"temperature": 42, "nested": "508", "humidity": 73, "temperature1": 73}       
var replace3 = map.replace("temperature", 48, 56);     // return false => map = {"temperature": 42, "nested": "508", "humidity": 73, "temperature1": 73}       

// remove Entry from the map by key
map.remove("temperature");                             // return nothing => map = {"nested": "508", "humidity": 73, "temperature1": 73}

// get Keys/Values  
var keys = map.keys();                                 // return ["nested", "humidity", "temperature1"]      
var values = map.values();                             // return ["508", 73, 73]      

// sort
map.sortByKey();                                       // return nothing => map = {"humidity": 73, "nested": "508", "temperature1": 73}                                      
var sortByValue = map.sortByValue();                   // return nothing => map = {"humidity": 73, "temperature1": 73, "nested": "508"}

// add new Entry/(new key/new value)
var mapAdd = {"test": 12, "input" : {"http": 130}};
map.putAll(mapAdd);

// isMap(Object obj): checks if the given object is an instance of Map
isMap(mapAdd);                                          // return true

```
{: .copy-code}


#### Map - Unmodifiable

TBEL use our own implementation of an unmodifiable map.

**Examples:**

```java
var original = {};
original.humidity = 73;
var unmodifiable = original.toUnmodifiable();   // Create an ExecutionHashMap that is unmodifiable.
unmodifiable.put("temperature1", 96);
return unmodifiable;      // An error occurs with the message: 'Error: unmodifiable.put("temperature1", 96): Map is unmodifiable'."
```
{: .copy-code}

### Lists

TBEL allows you to create Lists. We use our own implementation of the List to control memory usage of the script.
That is why TBEL allows only inline creation of lists. For example:

```java
// Create new list
var list = ["A", "B", "C"];
// List elelement access
list[0];
// Size of the list
list.size();
// Foreach 
foreach (item : list) { 
    var smth = item;
}
// For loop 
for (var i =0; i < list.size; i++) { 
    var smth = list[i];
}
```
{: .copy-code}

In our own list implementation The *Tbel* library uses most of the standard JavaScript methods from the [JavaScript Array](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/concat).

**Examples:**

```java
var list = ["A", "B", "C", "B", "C", "hello", 34567];
var listAdd = ["thigsboard", 4, 67];

    // add/push
var addAll = list.addAll(listAdd);         // return true    => list = ["A", "B", "C", "B", "C", "hello", 34567, "thigsboard", 4, 67]
var addAllI = list.addAll(2, listAdd);     // return true    => list = ["A", "B", "thigsboard", 4, 67, "C", "B", "C", "hello", 34567, "thigsboard", 4, 67]
var add = list.add(3, "thigsboard");       // return nothing => list = ["A", "B", "thigsboard", "thigsboard", 4, 67, "C", "B", "C", "hello", 34567, "thigsboard", 4, 67]
var push = list.push("thigsboard");        // return true    => list = ["A", "B", "thigsboard", "thigsboard", 4, 67, "C", "B", "C", "hello", 34567, "thigsboard", 4, 67, "thigsboard"]
var unshift = list.unshift("r");           // return nothing => list = ["r", "A", "B", "thigsboard", "thigsboard", 4, 67, "C", "B", "C", "hello", 34567, "thigsboard", 4, 67, "thigsboard"]
var unshiftQ = ["Q", 4];
var unshiftQun = list.unshift(unshift);    // return nothing => list = [[["Q", 4], "r", "B", "thigsboard", "thigsboard", 4, 67, "C", "B", "C", "hello", 34567, "thigsboard", 4, 67, "thigsboard"]
    // delete  
var removeIndex2 = list.remove(2);         // return "A" => list = [["Q", 4], "r", "B", "thigsboard", "thigsboard", 4, 67, "C", "B", "C", "hello", 34567, "thigsboard", 4, 67, "thigsboard"]    
var removeValueC = list.remove("C");       // return true => list = [["Q", 4], "r", "B", "thigsboard", "thigsboard", 4, 67, "B", "C", "hello", 34567, "thigsboard", 4, 67, "thigsboard"] 
var shift = list.shift();                  // return ["Q", 4] => list = ["r", "B", "thigsboard", "thigsboard", 4, 67, "B", "C", "hello", 34567, "thigsboard", 4, 67, "thigsboard"]
var pop = list.pop();                      // return "thigsboard" => list =  ["r", "B", "thigsboard", "thigsboard", 4, 67, "B", "C", "hello", 34567, "thigsboard", 4, 67]
var splice3 = list.splice(3);              // return ["thigsboard", 4, 67, "B", "C", "hello", 34567, "thigsboard", 4, 67] => list = ["r", "B", "thigsboard"]
var splice2 = list.splice(2, 2);           // return ["thigsboard"] => list = ["r", "B"]
var splice1_4 = list.splice(1, 4, "start", 5, "end"); // return ["B"]  => list =  ["r", "start", 5, "end"]

    // change
var set = list.set(3, "65");                 // return "end"" => list =  ["r", "start", 5, "65"]
list[1] = "98";                              // return nothing => list = ["r", "98", 5, "65"] 
list[0] = 2096;                              // return nothing => list = [2096, "98", 5, "65"]

list.sort();                                 // return nothing => list = [5, "65", "98", 2096] (sort Asc)
list.sort(true);                             // return nothing => list = [5, "65", "98", 2096] (sort Asc)
list.sort(false);                            // return nothing => list = [2096, "98", "65", 5] (sort Desc)
list.reverse();                              // return nothing => list = [5, "65", "98", 2096]
var fill = list.fill(67);                    // return new List [67, 67, 67, 67] => list = [67, 67, 67, 67]
var fill4 = list.fill(4, 1);                 // return new List [67, 4, 4, 4] => list = [67, 4, 4, 4]
var fill4_6 = list.fill(2, 1, 4);            // return new List [67, 2, 2, 2] => list = [67, 2, 2, 2]

    // return new List/new String 
var toSorted = list.toSorted();            // return new List [2, 2, 2, 67] (sort Asc) 
var toSorted_true = list.toSorted(true);   // return new List [2, 2, 2, 67] (sort Asc)  
var toSorted_false = list.toSorted(false); // return new List [67, 2, 2, 2] (sort Desc)
var toReversed = list.toReversed();        // return new List [2, 2, 2, 67] 
var slice = list.slice();                  // return new List [67, 2, 2, 2]  
var slice4 = list.slice(3);                // return new List [2]   
var slice1_5 = list.slice(0,2);            // return new List [67, 2]  
var with1 = list.with(1, 69);               // return new List [67, 69, 2, 2, 2] => list = [67, 2, 2, 2]
var concat = list.concat(listAdd);         // return new List [67, 2, 2, 2, "thigsboard", 4, 67] => list = [67, 2, 2, 2]
var join = list.join();                    // return new String "67,2,2,2" => list = [67, 2, 2, 2]        
var toSpliced2 = list.toSpliced(1, 0, "Feb"); // return new List [67, "Feb", 2, 2, 2] => list = [67, 2, 2, 2] 
var toSpliced0_2 = list.toSpliced(0, 2);     // return new List [2, 2] => list = [67, 2, 2, 2] 
var toSpliced2_2 = list.toSpliced(2, 2);     // return new List [67, 2] => list = [67, 2, 2, 2] 
var toSpliced4_5 = list.toSpliced(2, 4, "start", 5, "end"); // return new List[67, 2, "start", 5, "end"] => list = [67, 2, 2, 2] 

    // get Info        
var length = list.length();                 // return  4 
var memorySize = list.memorySize();         // return 32L 
var indOf1 = list.indexOf("B", 1);          // return -1  
var indOf2 = list.indexOf(2, 2);            // return 2  
var sNumber = list.validateClazzInArrayIsOnlyNumber(); // return true

// isList(Object obj): checks if the given object is an instance of List
isList(list);                               // return true
```
{: .copy-code}

{% capture difference %}
**Note**:
<br>
*sort/toSorted* operations:
- If the list consists only of a numeric value (12234 or a string in the form of a numeric value "12234") - the sort/toSorted... operation is used as a type of numeric value;
- If the list consists of one or more non-numeric values ("123K45" or "FEB" or {345: "re56"}) - the sort/toSorted... operation is interpreted as sorting the String;
{% endcapture %}
{% include templates/info-banner.md content=difference %}

#### List - Unmodifiable

TBEL use our own implementation of an unmodifiable list.

**Examples:**

```java
var original = [];
original.add(0x67);
var unmodifiable = original.toUnmodifiable();   // Create an ExecutionArrayList that is unmodifiable.
unmodifiable.add(0x35);
return unmodifiable;      // An error occurs with the message: 'Error: unmodifiable.add(0x35): List is unmodifiable'."
```
{: .copy-code}

### Sets

TBEL allows you to create Sets. We use our own implementation of the Set to control memory usage of the script.
That is why TBEL allows only inline creation of sets. 

In TBEL there are two supported ways to create a Set as ExecutionLinkedHashSet:

- Using entrySet() from a Map. You can obtain a Set of key-value pairs directly from a Map:

```java
var list = ["B", "A", "C", "A"];
var originalMap = {};
var set1 = originalMap.entrySet();         // create new Set from map, Empty
var set2 = set1.clone();                   // clone new Set, Empty
var result1 = set1.addAll(list);           // addAll list, no sort, size = 3 ("A" - duplicate), result = true
```
{: .copy-code}

- Using the toSet(List list) method. You can convert a List to a Set by calling the built-in newSet() method:

```java
var list = ["B", "A", "C", "A"];
var set1 = toSet(list);             // create new Set from newSet() with list, no sort, size = 3 ("A" - duplicate)
var set2 = newSet();                 // create new Set from newSet(), Empty
```
{: .copy-code}

- Supported Methods

| Method                  | Description                                                          |
|:------------------------|:---------------------------------------------------------------------|
| `add(value)`            | Adds a value to the set                                              |
| `remove(value)`         | Removes the value from the set                                       |
| `clear()`               | Removes all elements                                                 |
| `contains(value)`       | Checks if the set contains a value                                   |
| `size()`                | Returns the number of elements                                       |
| `toList()`              | Converts the set to a `List`                                         |
| `sort()`                | Sorts the set in ascending order (modifies the original set)         |
| `sort(boolean asc)`     | Sorts the set in ascending or descending order (in-place)            |
| `toSorted()`            | Returns a new sorted list in ascending order (original is unchanged) |
| `toSorted(boolean asc)` | Returns a new sorted list in desired order                           |


**Examples:**

- Foreach and For-Loop

```java
// Create a new set with value
var set = createSet(["A", "B", "C"]);
var set2 = toSet(msg.list);       // create new from list, size = 3
var set2_0 = set2.toArray()[0];         // return "A", value with index = 0 from Set 
var set2Size = set2.size();             // return size = 3
var smthForeach = "";
foreach (item : set2) {                 // foreach for Set
    smthForeach += item;                  // return "ABC"
}
var smthForLoop= "";
var set2Array = set2.toArray();         // for loop for Set (Set to array))
for (var i =0; i < set2.size; i++) {
    smthForLoop += set2Array[i];        // return "ABC"            
}
```
{: .copy-code}

- Add/Remove

```java
// add
var list = ["B", "C", "A", "B", "C", "hello", 34567];
var setAdd = toSet(["thigsboard", 4, 67]);      // create new, size = 3
var setAdd1_value = setAdd.clone();                   // clone setAdd, size = 3
var setAdd2_result = setAdd.add(35);                  // add value = 35, result = true
var setAdd2_value = setAdd.clone();                   // clone setAdd (fixing the result add = 35), size = 4
var setAddList1 = toSet(list);                  // create new from list without duplicate value ("B" and "C" - only one), size = 5
var setAdd3_result = setAdd.addAll(setAddList1);      // add all without duplicate values, result = true
var setAdd3_value = setAdd.clone();                   // clone setAdd (with addAll), size = 9  
var setAdd4_result = setAdd.add(35);                  // add duplicate value = 35,  result = false  
var setAdd4_value = setAdd.clone();                   // clone setAdd (after add duplicate value = 35), size = 9  
var setAddList2 = toSet(list);                  // create new from list without duplicate value ("B" and "C" - only one), start: size = 5, finish: size = 7       
var setAdd5_result1 = setAddList2.add(72);            // add is not duplicate value = 72,  result = true   
var setAdd5_result2 = setAddList2.add(72);            // add duplicate value = 72,  result = false   
var setAdd5_result3 = setAddList2.add("hello25");     // add  is not duplicate value = "hello25",  result = true    

// addAll
var setAdd5_value = setAddList2.clone();              // clone setAddList2, size = 7 
var setAdd6_result = setAdd.addAll(setAddList2);      // add all with duplicate values, result = true  
var setAdd6_value = setAdd.clone();                   // clone setAdd (after addAll setAddList2), before size = 9, after size = 11, added only is not duplicate values {"hello25", 72}  

// remove
var setAdd7_value = setAdd6_value.clone();            // clone setAdd6_value, before size = 11, after remove value = 4 size = 10
var setAdd7_result = setAdd7_value.remove(4);         // remove value = 4, result = true   
var setAdd8_value = setAdd7_value.clone();            // clone setAdd7_value, before size = 10, after clear size = 0
setAdd8_value.clear();                                // setAdd8_value clear, result size = 0  
```
{: .copy-code}

- sort/toSorted

```java
var list = ["C", "B", "A", 34567, "B", "C", "hello", 34];
var set1 = toSet(list);                   // create new from method toSet(List list) no sort, size = 6  ("A" and "C" is duplicated)
var set2 = toSet(list);                   // create new from method toSet(List list) no sort, size = 6  ("A" and "C" is duplicated)
var set1_asc = set1.clone();                    // clone set1, size = 6
var set1_desc = set1.clone();                   // clone set1, size = 6
set1.sort();                                    // sort set1 -> asc
set1_asc.sort(true);                            // sort set1_asc -> asc
set1_desc.sort(false);                          // sort set1_desc -> desc
var set3 = set2.toSorted();                     // toSorted set3 -> asc new Set
var set3_asc = set2.toSorted(true);             // toSorted set3 -> asc new Set
var set3_desc = set2.toSorted(false);           // toSorted set3 -> desc new Set
```
{: .copy-code}

- contains/toList/isSet

```java
var list = ["C", "B", "A", 34567, "B", "C", "hello", 34];
var set1 = toSet(list);                   // create new from method toSet(List list) no sort, size = 6  ("A" and "C" is duplicated)
var result1 = set1.contains("A");               // return true
var result2 = set1.contains("H");               // return false
var tolist = set1.toList();                     // create new List from Set, size = 6
var result3 = isSet(set1);                      // rerturn true
var result4 = isSet(tolist);                    // rerturn false
```
{: .copy-code}

#### Set - Unmodifiable

TBEL use our own implementation of an unmodifiable set.

**Examples:**

```java
var original = createSet();
original.add(0x67);
var unmodifiable = original.toUnmodifiable();   // Create an ExecutionArrayList that is unmodifiable.
unmodifiable.add(0x35);
return unmodifiable;      // An error occurs with the message: 'Error: unmodifiable.add(0x35): set is unmodifiable'."
```
{: .copy-code}

### Arrays

TBEL allows you to create Arrays. To control the memory usage, we permit only arrays of primitive types. String arrays are automatically converted to lists.

```java
// Create new array
var array = new int[3];
array[0] = 1;
array[1] = 2;
array[2] = 3;

var str = "My String";
var str0 = str[0]; // returns 'M';

function sum(list){
    var result = 0;
    for(var i = 0; i < list.length; i++){
        result += list[i];
    }
    return result;
};

var sum = sum(array); // returns 6

// isArray(Object obj): checks if the given object is an array
isArray(array);       // return true
```

```java
// Will cause ArrayIndexOutOfBoundsException.
var array = new int[3];
array[0] = 1;
array[1] = 2;
array[2] = 3;

array[3] = 4; return: Error with message:"Invalid statement: 4" and "[Line: 5, Column: 12]"
```
{: .copy-code}

### Literals

A literal is used to represent a fixed-value in the source of a particular script.

**String literals**

String literals may be denoted by single or double quotes.

```
"This is a string literal"
'This is also string literal'
```
{: .copy-code}

String escape sequences:

* \\ - Double escape allows rendering of single backslash in string.
* \n - Newline
* \r - Return
* \u#### - Unicode character (Example: \uAE00)
* \### - Octal character (Example: \73)

**Numeric literals**

Integers can be represented in decimal (base 10), octal (base 8), or hexadecimal (base 16).

A decimal integer can be expressed as any number that does not start with zero.

```java
125 // decimal
```
{: .copy-code}

An octal representation of an integer is possible by prefixing the number with a zero, followed by digits ranging from 0 to 7.

```java
0353 // octal
```
{: .copy-code}

Hexidecimal is represented by prefixing the integer with 0x followed by numbers ranging from 0-9..A-F.

```java
0xAFF0 // hex
```
{: .copy-code}

A floating point number consists of a whole number and a factional part denoted by the point/period character, with an optional type suffix.

```java
10.503 // a double
94.92d // a double
14.5f // a float
```
{: .copy-code}

You can represent `*`BigInteger`*` and `*`BigDecimal`*` literals by using the suffixes `B` and `I` (uppercase is mandatory).

```
104.39484B // BigDecimal
8.4I // BigInteger
```
{: .copy-code}

Boolean literals are represented by the reserved keywords `true` and `false`.

The null literal is denoted by the reserved keywords `null` or `nil`.

### Using Java Classes

The TBEL implementation allows the usage of **some** Java classes from the `java.util` and `java.lang` packages. For example:

```java
var foo = Math.sqrt(4);
```
{: .copy-code}


#### Convert Number to Hexadecimal/Octal/Binary String
- Byte

```java
var b16 = Integer.toString(0x1A, 16); // Hexadecimal "1a"
var b10 = Integer.toString(0x1A, 10); // Decimal     "26"
var b8 = Integer.toString(0x1A, 8);   // Octal       "32"
var b2 = Integer.toString(0x1A, 2);   // Binary     "11010"
```
{: .copy-code}

- Integer

```java
var i16 = Integer.toString(-255, 16); // Hexadecimal "-ff"
var i10 = Integer.toString(-255, 10); // Decimal     "-255"
var i8 = Integer.toString(-255, 8);   // Octal       "-377"
var i2 = Integer.toString(-255, 2);   // Binary      "-11111111"
```
{: .copy-code}

- Float

```java
var f0 =7823764.8374;  
var f16 = Float.toHexString(f0);           // Hexadecimal "0x1.dd8654p22" 
var f10 = f0.toString();                   // Decimal     "7823764.8374"
```
{: .copy-code}

- Long

```java
var l16 = Long.toString(9223372036854775807, 16); // Hexadecimal "7fffffffffffffff"
var l10 = Long.toString(9223372036854775807, 10); // Decimal     "9223372036854775807"
var l8 = Long.toString(9223372036854775807, 8);   // Octal       "777777777777777777777"
var l2 = Long.toString(9223372036854775807, 2);   // Binary      "111111111111111111111111111111111111111111111111111111111111111"
```
{: .copy-code}

- Double

```java
var dd0 = 99993219.156013e-002;
var dd16 = Double.toHexString(dd0);              // Hexadecimal "0x1.e83f862142b5bp19"
var dd10 = String.format("%.8f",dd0);            // Decimal     "999932,19156013"
```
{: .copy-code}

For the security reasons, the usage of those classes is constrained. You are able to call both static and non-static methods, but you are not able to assign the instance of the class to the variable:

```java
var list = ["A", "B", "C"];
java.util.Collections.reverse(list); // allowed
list = new java.util.ArrayList(); // Not allowed
```
{: .copy-code}

To simplify migration from JavaScript, we introduced a JSON class with static methods JSON.stringify and JSON.parse, which function similarly to their JavaScript counterparts. 

For example:

```java
var metadataStr = JSON.stringify(metadata);
var metadata = JSON.parse(metadataStr);
```
{: .copy-code}

For the same purpose:
- Added [decodeToJson](#decodetojson) method.
- Added [Date](#tbdate)class, which can be used without specifying a package name.
- In the `tbel` library, the  [map](#maps) and [list](#lists) classes have extended support for their primary methods, similar to Java.
 
### Flow Control

**If-Then-Else**

TBEL supports full, C/Java-style if-then-else blocks. For example:

```java
if (temperature > 0) {
   return "Greater than zero!";
} else if (temperature == -1) { 
   return "Minus one!";
} else { 
   return "Something else!";
}
```
{: .copy-code}

**Ternary statements**

Ternary statements are supported just as in Java:

```java
temperature > 0 ? "Yes" : "No";
```
{: .copy-code}

Nested ternary statements are also supported.

**Foreach**

One of the most powerful features in TBEL is its foreach operator. It is similar to the foreach operator in Java 1.5 in both syntax and functionality. 
It accepts two parameters separated by a colon. The first is the local variable for the current element, and the second is the collection or array to be iterated.

For example:

```java
var numbers = [1, 2, 3];
var sum = 0;
foreach (n : numbers) {
   sum+=n;
}
```
{: .copy-code}

Since TBEL treats Strings as iterable objects, you can iterate a String (character by character) with a foreach block:

```java
var str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
foreach (c : str) {
   //do something 
}
```
{: .copy-code}

**For Loop**

```java
var sum = 0;
for (var i =0; i < 100; i++) { 
   sum += i;
}
```
{: .copy-code}

**Do While, Do Until**

`do while` and `do until` are implemented in TBEL, following the same convention as Java, with `until` being the inverse of `while`.

```java
do { 
   x = something();
} 
while (x != null);
```
{: .copy-code}

... is semantically equivalent to ...

```java
do {
   x = something();
}
until (x == null);
```
{: .copy-code}

**While, Until**

TBEL implements standard while, with the addition of the inverse until.

```java
while (isTrue()) {
   doSomething();
}
```
{: .copy-code}

... or ...

```java
until (isFalse()) {
   doSomething();
}
```
{: .copy-code}

## Helper functions

### btoa

Creates a Base64-encoded ASCII string from a binary string (i.e., a string in which each character in the string is treated as a byte of binary data)

**Syntax:**

*String btoa(String input)*

**Parameters:**

<ul>
  <li><b>input:</b> <code>string</code> - The binary string to encode.</li>
</ul>

**Return value:**

Returns an ASCII string that represents the Base64 encoding of the input.

**Examples:**

```java
var encodedData = btoa("Hello, world"); // encode a string
var decodedData = atob(encodedData); // decode the string
```
{: .copy-code}

### atob

Decodes a string of data which has been encoded using Base64 encoding.

**Syntax:**

*String atob(String input)*

**Parameters:**

<ul>
  <li><b>input:</b> <code>string</code> - A binary string containing base64-encoded data.</li>
</ul>

**Return value:**

An ASCII string containing decoded data from encodedData.

**Examples:**

```java
var encodedData = btoa("Hello, world"); // encode a string
var decodedData = atob(encodedData); // decode the string
```
{: .copy-code}

### toFixed

Rounds the double value towards "nearest neighbor".

**Syntax:**

*double toFixed(double value, int precision)*

**Parameters:**

<ul>
  <li><b>value:</b> <code>double</code> - the double value.</li>
  <li><b>precision:</b> <code>int</code> - the precision.</li>
</ul>

**Return value:**

Rounded double

**Examples:**

```java
return toFixed(0.345, 1); // returns 0.3
return toFixed(0.345, 2); // returns 0.35
```
{: .copy-code}

### toInt

Rounds the double value to the nearest integer.

**Syntax:**

*double toInt(double value)*

**Parameters:**

<ul>
  <li><b>value:</b> <code>double</code> - the double value.</li>
</ul>

**Return value:**

Rounded integer

**Examples:**

```java
return toInt(0.3);   // returns 0
return toInt(0.5);   // returns 1
return toInt(2.7);   // returns 3
```
{: .copy-code}

### stringToBytes

Converts input binary string to the list of bytes.

**Syntax:**

*List<Byte> stringToBytes(String input[, String charsetName])*

**Parameters:**

<ul>
  <li><b>input:</b> <code>Binary string</code> - string in which each character in the string is treated as a byte of binary data.</li>
  <li><b>charsetName:</b> <code>String</code> - optional Charset name. UTF-8 by default.</li>
</ul>

**Return value:**

A list of bytes.

**Examples with:** <code>Binary string</code>

```java
var base64Str = "eyJoZWxsbyI6ICJ3b3JsZCJ9"; // Base 64 representation of the '{"hello": "world"}' 
var bytesStr = atob(base64Str);
return stringToBytes(bytesStr); // Returns [123, 34, 104, 101, 108, 108, 111, 34, 58, 32, 34, 119, 111, 114, 108, 100, 34, 125]

var inputStr = "hello world";
return stringToBytes(inputStr);  // Returns  [104, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100]
var charsetStr = "UTF8";
return stringToBytes(inputStr, charsetStr);  // Returns  [104, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100]
```
{: .copy-code}

**Examples with:** <code>Object from Json as String</code>

```java
var dataMap = {};
dataMap.inputStr = "hello world";
var dataJsonStr = JSON.stringify(dataMap);
var dataJson = JSON.parse(dataJsonStr);
return stringToBytes(dataJson.inputStr); // Returns  [104, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100]
```
{: .copy-code}

### bytesToString

Creates a string from the list of bytes

**Syntax:**

*String bytesToString(List<Byte> bytesList[, String charsetName])*

**Parameters:**

<ul>
  <li><b>bytesList:</b> <code>List of Bytes</code> - A list of bytes.</li>
  <li><b>charsetName:</b> <code>String</code> - optional Charset name. UTF-8 by default.</li>
</ul>

**Return value:**

A string constructed from the specified byte list.

**Examples:**

```java
var bytes = [0x48,0x45,0x4C,0x4C,0x4F];
return bytesToString(bytes); // Returns "HELLO"
```
{: .copy-code}

### decodeToString

Alias for the [bytesToString](#bytestostring)

### decodeToJson

Decodes a list of bytes to the JSON document.

**Syntax:**

*String decodeToJson(List<Byte> bytesList)*

**Parameters:**

<ul>
  <li><b>bytesList:</b> <code>List of Bytes</code> - A list of bytes.</li>
</ul>

**Return value:**

A JSON object or primitive.

**Examples:**

```java
var base64Str = "eyJoZWxsbyI6ICJ3b3JsZCJ9"; // Base 64 representation of the '{"hello": "world"}'
var bytesStr = atob(base64Str);
var bytes = stringToBytes(bytesStr);
return decodeToJson(bytes); // Returns '{"hello": "world"}'
```
{: .copy-code}

### isTypeInValue
#### isBinary

Validates whether a string represents a binary (base-2) value.

**Syntax:**

*int isBinary(String str)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>String</code> - The string to be checked if it represents a binary number. UTF-8 is the default character set.</li>
</ul>

**Return value:**

An integer value indicating the result:
- '2' if the string is binary.
- '-1' if the string is not binary.

**Examples:**

```java
return isBinary("1100110"); // Returns 2
return isBinary("2100110"); // Returns -1
```
{: .copy-code}

#### isOctal

Validates whether a string represents an octal (base-8) value.

**Syntax:**

*int isOctal(String str)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>String</code> - The string to be checked if it represents a octal number. UTF-8 is the default character set.</li>
</ul>

**Return value:**

An integer value indicating the result:
- '8' if the string is octal.
- '-1' if the string is not octal.

**Examples:**

```java
return isOctal("4567734"); // Returns 8
return isOctal("8100110"); // Returns -1
```
{: .copy-code}

#### isDecimal

Validates whether a string represents a decimal (base-10) value.

**Syntax:**

*int isDecimal(String str)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>String</code> - The string to be checked if it represents a decimal number. UTF-8 is the default character set..</li>
</ul>

**Return value:**

An integer value indicating the result:
- '10' if the string is decimal.
- '-1' if the string is not decimal.

**Examples:**

```java
return isDecimal("4567039"); // Returns 10
return isDecimal("C100110"); // Returns -1
```
{: .copy-code}

#### isHexadecimal

Validates whether a string represents a hexadecimal (base-16) value.

**Syntax:**

*int isHexadecimal(String str)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>String</code> - The string to be checked if it represents a hexadecimal number. UTF-8 is the default character set.</li>
</ul>

**Return value:**

An integer value indicating the result:
- '16' if the string is hexadecimal.
- '-1' if the string is not hexadecimal.

**Examples:**

```java
return isHexadecimal("F5D7039"); // Returns 16
return isHexadecimal("K100110"); // Returns -1
```
{: .copy-code}

#### isNaN

Checks whether a double value is "Not-a-Number" (NaN).

**Syntax:**

*boolean isNaN(double value)*

**Parameters:**

<ul>
  <li><b>value:</b> <code>double</code> - The value to be checked for NaN.</li>
</ul>

**Return value:**

A boolean indicating the result:
- `true` if the value is NaN.
- `false` if the value is a valid number.

**Examples:**

```java
return isNaN(0.0 / 0.0); // Returns true
return isNaN(1.0);       // Returns false
```
{: .copy-code}

### encodeDecodeUri
#### encodeURI
{% capture difference %}
**Note**:
<br>
The encodeURI() function:
- The encodeURI() function escapes characters as UTF-8 code units, with each octet encoded in the format %XX, left-padded with 0 if necessary. Lone surrogates in UTF-16 do not represent valid Unicode characters.
- The Tbel library includes most standard JavaScript methods, such as [encodeURI()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI).
{% endcapture %}
{% include templates/info-banner.md content=difference %}

- *encodeURI()* escapes all characters except:

```java
A–Z a–z 0–9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , #
```

**Syntax:**

*String encodeURI(String uri)*

**Parameters:**

<ul>
  <li><b>uri:</b> <code>String</code> - A string to be encoded as a URI.</li>
</ul>

**Return value:**

A new string representing the provided string encoded as a URI.

**Examples:**

```java
var uriOriginal = "-_.!~*'();/?:@&=+$,#ht://example.ж д a/path with spaces/?param1=Київ 1&param2=Україна2";
return encodeURI(uriOriginal); // Returns "-_.!~*'();/?:@&=+$,#ht://example.%D0%B6%20%D0%B4%20a/path%20with%20spaces/?param1=%D0%9A%D0%B8%D1%97%D0%B2%201&param2=%D0%A3%D0%BA%D1%80%D0%B0%D1%97%D0%BD%D0%B02"
```
{: .copy-code}

#### decodeURI
{% capture difference %}
**Note**:
<br>
The decodeURI() function:
- The decodeURI() function decodes a Uniform Resource Identifier (URI) that was previously encoded using encodeURI() or a similar method.
- The Tbel library incorporates most standard JavaScript methods, including [decodeURI()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/decodeURI).
{% endcapture %}
{% include templates/info-banner.md content=difference %}

**Syntax:**

*String decodeURI(String uri)*

**Parameters:**

<ul>
  <li><b>uri:</b> <code>String</code> - A complete, encoded Uniform Resource Identifier.</li>
</ul>

**Return value:**

A new string that represents the unencoded version of the specified encoded Uniform Resource Identifier (URI).

**Examples:**

```java
var uriEncode = "-_.!~*'();/?:@&=+$,#ht://example.%D0%B6%20%D0%B4%20a/path%20with%20spaces/?param1=%D0%9A%D0%B8%D1%97%D0%B2%201&param2=%D0%A3%D0%BA%D1%80%D0%B0%D1%97%D0%BD%D0%B02";
return decodeURI(uriEncode); // Returns "-_.!~*'();/?:@&=+$,#ht://example.ж д a/path with spaces/?param1=Київ 1&param2=Україна2"
```
{: .copy-code}

### raiseError

**Syntax:**

*void raiseError(String message)*

**Parameters:**

<ul>
  <li><b>message:</b> <code>String</code> - Info about Exception message.</li>
</ul>

**Return value:**

new RuntimeException with msg = message;

**Examples:**

```java
var message = "frequency_weighting_type must be 0, 1 or 2.";
return raiseError(message);        // Returns "frequency_weighting_type must be 0, 1 or 2."
var value = 4;
message = "frequency_weighting_type must be 0, 1 or 2. A value of " + value + " is invalid.";
return raiseError(message, value); // Returns "frequency_weighting_type must be 0, 1 or 2. A value of 4 is invalid."
```
{: .copy-code}

### printUnsignedBytes

Converts List<Signed byte> to List<Unsigned integer>

**Syntax:**

*List<Integer> printUnsignedBytes(List<Byte> byteArray)*

**Parameters:**

<ul>
  <li><b>byteArray:</b> <code>List of byte</code> - the  list of byte values, where each integer represents a single byte</li>
</ul>

**Return value:**

List<Integer>

**Examples:**

```java
var hexStrBe = "D8FF";                     // [-40, -1]
var listBe = hexToBytes(hexStrBe);
return printUnsignedBytes(listBe);         // Returns [216, 255]

var hexStrLe = "FFD8";                     // [-1, -40]
var listLe = hexToBytes(hexStrLe);
return printUnsignedBytes(listLe);         // Returns [255, 216]
```
{: .copy-code}

### pad
#### padStart
- Pads this string with another string (multiple times, if needed) until the resulting string reaches the given length. 
- The padding is applied from the start of this string.

**Syntax:**

*String padEnd(String str, int targetLength, char padString)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>String</code> - String values pads this string with a given string (repeated, if needed)</li>
  <li><b>targetLength:</b> <code>List of byte</code> - The length of the resulting string once the current str has been padded. If the value is less than or equal to str.length, then str is returned as-is.</li>
  <li><b>padString:</b> <code>List of byte</code> - The string to pad the current str with. If padString is too long to stay within the targetLength, it will be truncated from the end.</li>
</ul>

**Return value:**

String

**Examples:**

```java
var str = "010011";
return padStart(str, 8, '0');                           // Returns  "00010011"
var str ="1001010011";        
return padStart(str, 8, '0');                           // Returns  "1001010011"
return padStart(str, 16, '*');                          // Returns  "******1001010011"
var fullNumber = "203439900FFCD5581";
var last4Digits = fullNumber.substring(11);
return padStart(last4Digits, fullNumber.length(), '*'); // Returns "***********CD5581"
```
{: .copy-code}

#### padEnd
- Pads this string with a given string (repeated, if needed) so that the resulting string reaches a given length. 
- The padding is applied from the end of this string.

**Syntax:**

*String padEnd(String str, int targetLength, char padString)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>String</code> - String values pads this string with a given string (repeated, if needed)</li>
  <li><b>targetLength:</b> <code>List of byte</code> - The length of the resulting string once the current str has been padded. If the value is less than or equal to str.length, the current string will be returned as-is.</li>
  <li><b>padString:</b> <code>List of byte</code> - The string to pad the current str with. If padString is too long to stay within targetLength, it will be truncated: for left-to-right languages the left-most part and for right-to-left languages the right-most will be applied.</li>
</ul>

**Return value:**

String

**Examples:**

```java
var str = "010011";
padEnd(str, 8, '0');                           // Returns  "01001100"
        
var str ="1001010011";        
padEnd(str, 8, '0');                           // Returns  "1001010011"        
padEnd(str, 16, '*');                          // Returns  "1001010011******"
        
var fullNumber = "203439900FFCD5581";
var last4Digits = fullNumber.substring(0, 11);
padEnd(last4Digits, fullNumber.length(), '*'); // Returns "203439900FF******"
```
{: .copy-code}

### numberToRadixString
#### intToHex
#### longToHex
#### floatToHex
#### doubleToHex

Converts input number (int, long, float, double) to hexString.

**Syntax:**

*String intToHex(Integer i[, boolean bigEndian, boolean pref, int len])*
*String longToHex(Long l[, boolean bigEndian, boolean pref, int len])*
*String floatToHex(Float f[, boolean bigEndian])*
*String doubleToHex(Double d[, boolean bigEndian])*

**Parameters:**

<ul>
    <li><b>i, l, f, d:</b> <code>Number</code> - Format: Integer, Long, Float, Double.</li>
    <li><b>bigEndian:</b> <code>boolean</code> - optional, the big-endian (BE) byte order if true, little-endian (LE) otherwise. Default: true (BE).</li>
    <li><b>pref:</b> <code>boolean</code> - optional, the format output with "0x". Default: false</li>
    <li><b>len:</b> <code>int</code> - optional, the number of bytes of the number to convert to text.</li>
</ul>

**Return value:**

A string in HexDecimal format.

**Examples:**

```java
var i = 0x7FFFFFFF;                             // (2147483647);
intToHex(i, true, true);                        // Returns "0x7FFFFFFF"
intToHex(171, true, false);                     // Returns "AB"
intToHex(0xABCDEF, false, true, 4);             // Returns "0xCDAB"
intToHex(0xABCD, false, false, 2);              // Returns "AB"

longToHex(9223372036854775807, true, true);     // Returns "0x7FFFFFFFFFFFFFFF"
longToHex(0x7A12BCD3, true, true, 4);           // Returns "0xBCD3"
longToHex(0x7A12BCD3, false, false, 4);         // Returns "127A"

floatToHex(123456789.00);                       // Returns "0x4CEB79A3"
floatToHex(123456789.00, false);                // Returns "0xA379EB4C"

doubleToHex(1729.1729d);                        // Returns "0x409B04B10CB295EA"
doubleToHex(1729.1729d, false);                 // Returns "0xEA95B20CB1049B40"
```
{: .copy-code}

#### intLongToRadixString

Converts input number (int, long) to radix String (default Dec).

**Syntax:**

*String intLongToRadixString(Long number[, int radix, boolean bigEndian, boolean pref])*

**Parameters:**

<ul>
    <li><b>number:</b> <code>Number</code> - Format: Long.</li>
    <li><b>radix:</b> <code>int</code> - optional radix to use when parsing to string format(BinaryString, OctalString, DecimalString, HexDecimalString). Default: DecimalString.</li>
    <li><b>bigEndian:</b> <code>boolean</code> - optional the big-endian (BE) byte order if true, little-endian (LE) otherwise. Default: true (BE).</li>
    <li><b>pref:</b> <code>boolean</code> - optional format output with "0x". Default: false</li>
</ul>

**Return value:**

A string in BinaryString, OctalString, DecimalString, HexDecimalString format.

**Examples:**

```java
intLongToRadixString(58, 2);                         // Returns "00111010"
intLongToRadixString(9223372036854775807, 2);        // Returns "0111111111111111111111111111111111111111111111111111111111111111"
intLongToRadixString(13158, 8);                      // Returns "31546"
intLongToRadixString(-13158, 8);                     // Returns "1777777777777777746232"
intLongToRadixString(-13158, 10);                    // Returns "-13158"
intLongToRadixString(13158, 16);                     // Returns "3366"
intLongToRadixString(-13158, 16);                    // Returns "FFCC9A"
intLongToRadixString(9223372036854775807, 16);       // Returns "7FFFFFFFFFFFFFFF"
intLongToRadixString(-13158, 16, true, true);        // Returns "0xFFCC9A"
```
{: .copy-code}

### parseHex
#### parseHexToInt

Converts the hex string to integer.

**Syntax:**

*int parseHexToInt(String hex[, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>hex:</b> <code>string</code> - the hex string with big-endian byte order.</li>
  <li><b>bigEndian:</b> <code>boolean</code> -  optional the big-endian (BE) byte order if true, little-endian (LE) otherwise.</li>
</ul>

**Return value:**

Parsed integer value.

**Examples:**

```java
parseHexToInt("BBAA");        // Returns 48042
parseHexToInt("BBAA", true);  // Returns 48042
parseHexToInt("AABB", false); // Returns 48042
parseHexToInt("BBAA", false); // Returns 43707
```
{: .copy-code}

##### parseLittleEndianHexToInt

Alias for [parseHexToInt(hex, false)](#parsehextoint)

**Syntax:**

*int parseLittleEndianHexToInt(String hex)*

##### parseBigEndianHexToInt

Alias for [parseHexToInt(hex, true)](#parsehextoint)

**Syntax:**

*int parseBigEndianHexToInt(String hex)*

#### parseHexToFloat

Converts the hex string to float from HexString.

**Syntax:**

*Float parseHexToFloat(String hex[, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>hex:</b> <code>string</code> - the hex string with big-endian byte order.</li>
  <li><b>bigEndian:</b> <code>boolean</code> -  optional the big-endian (BE) byte order if true, little-endian (LE) otherwise.</li>
</ul>

**Return value:**

Parsed float value.

**Examples:**

```java
parseHexToFloat("41EA62CC");         // Returns 29.29824f
parseHexToFloat("41EA62CC", true);   // Returns 29.29824f
parseHexToFloat("41EA62CC", false);  // Returns -5.948442E7f
parseHexToFloat("CC62EA41", false);  // Returns 29.29824f
```
{: .copy-code}

##### parseLittleEndianHexToFloat

Alias for [parseHexToFloat(hex, false)](#parsehextofloat)

**Syntax:**

*float parseLittleEndianHexToFloat(String hex)*

##### parseBigEndianHexToFloat

Alias for [parseBigEndianHexToFloat(hex, true)](#parsehextofloat)

**Syntax:**

*float parseBigEndianHexToFloat(String hex)*

#### parseHexIntLongToFloat

{% capture difference %}
**Note**:
<br>
eg *"0x0A"* for *10.0f*:
- In this method, we process it as an integer.
{% endcapture %}
{% include templates/info-banner.md content=difference %}

Converts the hex string to float from HexString.

**Syntax:**

*Float parseHexIntLongToFloat(String hex, boolean bigEndian)*

**Parameters:**

<ul>
  <li><b>hex:</b> <code>string</code> - the hex string with big-endian byte order.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - the big-endian (BE) byte order if true, little-endian (LE) otherwise.</li>
</ul>

**Return value:**

Parsed float value.

**Examples:**

```java
return parseHexIntLongToFloat("0x0A", true);         // Returns 10.0f
return parseHexIntLongToFloat("0x0A", false);        // Returns 10.0f
return parseHexIntLongToFloat("0x00000A", true);     // Returns 10.0f
return parseHexIntLongToFloat("0x0A0000", false);    // Returns 10.0f
return parseHexIntLongToFloat("0x000A0A", true);     // Returns 2570.0f
return parseHexIntLongToFloat("0x0A0A00", false);    // Returns 2570.0f
```
{: .copy-code}

#### parseHexToDouble

Converts the hex string to Double.

**Syntax:**

*int parseHexToDouble(String hex[, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>hex:</b> <code>string</code> - the hex string with big-endian byte order.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional the big-endian (BE) byte order if true, little-endian (LE) otherwise.</li>
</ul>

**Return value:**

Parsed double value.

**Examples:**

```java
return parseHexToDouble("409B04B10CB295EA");            // Returns 1729.1729
return parseHexToDouble("409B04B10CB295EA", false);      // Returns -2.7208640774822924E205
return parseHexToDouble("409B04B10CB295EA", true);       // Returns 1729.1729
return parseHexToDouble("EA95B20CB1049B40", false);      // Returns 1729.1729
```
{: .copy-code}

##### parseLittleEndianHexToDouble

Alias for [parseHexToDouble(hex, false)](#parsehextodouble)

**Syntax:**

*double parseLittleEndianHexToDouble(String hex)*

##### parseBigEndianHexToDouble

Alias for [parseBigEndianHexToDouble(hex, true)](#parsehextodouble)

**Syntax:**

*double parseBigEndianHexToDouble(String hex)*

#### hexToBytes List or Array

##### hexToBytes

Converts the hex string to list of integer values, where each integer represents single byte.

**Syntax:**

*List<Integer> hexToBytes(String hex)*

**Parameters:**

<ul>
  <li><b>hex:</b> <code>string</code> - the hex string with big-endian byte order.</li>
</ul>

**Return value:**

Parsed list of integer values.

**Examples:**

```java
return hexToBytes("0x01752B0367FA000500010488FFFFFFFFFFFFFFFF33"); // Returns [1, 117, 43, 3, 103, -6, 0, 5, 0, 1, 4, -120, -1, -1, -1, -1, -1, -1, -1, -1, 51]
```
{: .copy-code}

###### hexToBytesArray

Converts the hex string to Array of single byte values.

**Syntax:**

*byte[] hexToBytesArray(String hex)*

**Parameters:**

<ul>
  <li><b>hex:</b> <code>string</code> - the hex string with big-endian byte order.</li>
</ul>

**Return value:**

Parsed array of single byte values.

**Examples:**

```java
return hexToBytesArray("AABBCCDDEE"); // Returns [(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE]
                                      //  Returns [-86, -69, -52, -35, -18]  
```
{: .copy-code}

### parseBytes
#### bytesToHex

Converts the list of integer values, where each integer represents a single byte, to the hex string.

**Syntax:**

*String bytesToHex(List<Integer> bytes)*
*String bytesToHex(byte[] bytes)*~~~~

**Parameters:**

<ul>
  <li><b>bytes:</b> <code>byte[]</code> or <code>List of integer</code> - the byte array or the list of integer values, where each integer represents a single byte</li>
</ul>

**Return value:**

Returns a hexadecimal string.

**Examples:**

```java
var bytes = [0xBB, 0xAA];
return bytesToHex(bytes); // Returns "BBAA"
var list = [-69, 83];
return bytesToHex(list);   // Returns "BB53"
```
{: .copy-code}

#### parseBytesToInt

Converts a byte array with the given offset to an int, length, and optional byte order.

**Syntax:**

*int parseBytesToInt([byte[] or List<Byte>] data[, int offset, int length, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>data:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>offset:</b> <code>int</code> - optional, the offset in the array.</li>
  <li><b>length:</b> <code>int</code> - optional, the length in bytes. Less then or equal to 4.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional, LittleEndian if false, BigEndian otherwise.</li>
</ul>

**Return value:**

integer value.

**Examples:**

```java
var bytes = [0xAA, 0xBB, 0xCC, 0xDD];
return parseBytesToInt(bytes, 0, 3); // Returns 11189196 in Decimal or 0xAABBCC  
return parseBytesToInt(bytes, 0, 3, true); // Returns 11189196 in Decimal or 0xAABBCC  
return parseBytesToInt(bytes, 0, 3, false); // Returns 13417386 in Decimal or 0xCCBBAA
```
{: .copy-code}

#### parseBytesToLong

Converts a byte array with the given offset to a long, length, and optional byte order.

**Syntax:**

*int parseBytesToInt([byte[] or List<Byte>] data[,int offset, int length, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>data:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>offset:</b> <code>int</code> - optional, the offset in the array.</li>
  <li><b>length:</b> <code>int</code> - optional, the length in bytes. Less then or equal to 4.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional, LittleEndian if false, BigEndian otherwise.</li>
</ul>

**Return value:**

long value.

**Examples:**

```java
var longValByte = [64, -101, 4, -79, 12, -78, -107, -22];
return parseBytesToLong(longValByte, 0, 8);         // Returns 4655319798286292458L  == 0x409B04B10CB295EAL  
return parseBytesToLong(longValByte, 0, 8, false);  // Returns -1543131529725306048L == 0xEA95B20CB1049B40L  

```
{: .copy-code}

#### parseBytesToFloat

{% capture difference %}
**Note**:
<br>
eg *"0x0A"* for *1.4E-44f*:
- In this method, we process it as an float.
{% endcapture %}
{% include templates/info-banner.md content=difference %}

Converts a byte array with the given offset to a floating-point number, length, and optional byte order.

**Syntax:**

*int parseBytesToFloat([byte[] or List<Byte>] data[, int offset, int length, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>data:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>offset:</b> <code>int</code> - optional, the offset in the array.</li>
  <li><b>length:</b> <code>int</code> - optional, the length in bytes. Less then or equal to 4.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional, LittleEndian if false, BigEndian otherwise.</li>
</ul>

**Return value:**

float value.

**Examples:**

```java
var bytes = [0x0A];
return parseBytesToFloat(bytes);                       // Returns 1.4E-44f  
var floatValByte = [0x41, 0xEA, 0x62, 0xCC];
return parseBytesToFloat(floatValByte, 0);             // Returns 29.29824f  
return parseBytesToFloat(floatValByte, 0, 2, false);   // Returns 8.4034E-41f 
return parseBytesToFloat(floatValByte, 0, 2, true);    // Returns 2.3646E-41f 
return parseBytesToFloat(floatValByte, 0, 3, false);   // Returns 9.083913E-39f  
return parseBytesToFloat(floatValByte, 0, 3, true);    // Returns 6.053388E-39f  
return parseBytesToFloat(floatValByte, 0, 4, false);   // Returns -5.948442E7f  
var floatValList = [65, -22, 98, -52];
return parseBytesToFloat(floatValList, 0);             // Returns 29.29824f  
return parseBytesToFloat(floatValList, 0, 4, false);   // Returns -5.948442E7f 

```
{: .copy-code}

#### parseBytesIntToFloat

{% capture difference %}
**Note**:
<br>
eg *"0x0A"* for *10.0f*, *"0x0A00"* for *2560.0f*:
- In this method, we process it as an integer.
- Converts a byte array to a floating-point number equal to the value that would be found if the hexadecimal string were converted to an Integer number.
{% endcapture %}
{% include templates/info-banner.md content=difference %}

Converts a byte array with the given offset to a floating-point number, length, and optional byte order.

**Syntax:**

*int parseBytesToFloat([byte[] or List<Byte>] data[, int offset, int length, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>data:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>offset:</b> <code>int</code> - optional, the offset in the array.</li>
  <li><b>length:</b> <code>int</code> - optional, the length in bytes. Less then or equal to 4.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional, LittleEndian if false, BigEndian otherwise.</li>
</ul>

**Return value:**

float value.

**Examples:**

```java
var intValByte = [0x00, 0x00, 0x00, 0x0A];
return parseBytesIntToFloat(intValByte, 3, 1, true);     // Returns 10.0f
return parseBytesIntToFloat(intValByte, 3, 1, false);    // Returns 10.0f
return parseBytesIntToFloat(intValByte, 2, 2, true);     // Returns 10.0f
return parseBytesIntToFloat(intValByte, 2, 2, false);    // Returns 2560.0f
return parseBytesIntToFloat(intValByte, 0, 4, true);     // Returns 10.0f 
return parseBytesIntToFloat(intValByte, 0, 4, false);    // Returns 1.6777216E8f
```
{: .copy-code}

**Examples (latitude, longitude):**

```java
var dataAT101 = "0x01756403671B01048836BF7701F000090722050000";
var byteAT101 = hexToBytes(dataAT101);
var offset = 9;
return parseBytesIntToFloat(byteAT101, offset, 4, false) / 1000000;     // Returns 24.62495f
return parseBytesIntToFloat(byteAT101, offset + 4, 4, false) / 1000000; // Returns 118.030576f

```
{: .copy-code}

#### parseBytesLongToDouble

{% capture difference %}
**Note**:
<br>
eg *"0x0A"* for *10.0d*, *"0x0A00"* for *2'560.0d*, *"0x0A0A0A0A"* for *168'430'090.0d*:
- In this method, we process it as an long.
- Converts a byte array to a floating-point number equal to the value that would be found if the hexadecimal string were converted to an Long number.
- Using double has enough precision for accurate lat/lon down to inches for 6-7 decimal places.
- The 6th decimal place  for lat/lon is for sub-foot accuracy.
{% endcapture %}
{% include templates/info-banner.md content=difference %}

Converts a byte array with the given offset to a double, length, and optional byte order.

**Syntax:**

*int parseBytesToDouble([byte[] or List<Byte>] data[, int offset, int length, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>data:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>offset:</b> <code>int</code> -optional, the offset in the array.</li>
  <li><b>length:</b> <code>int</code> - optional, the length in bytes. Less then or equal to 4.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional, LittleEndian if false, BigEndian otherwise.</li>
</ul>

**Return value:**

double value.

**Examples (latitude, longitude):**

```java
var coordinatesAsHex = "0x32D009423F23B300B0106E08D96B6C00";
var coordinatesasBytes = hexToBytes(coordinatesAsHex);
var offset = 0;
var factor = 1e15;
return parseBytesLongToDouble(coordinatesasBytes, offset, 8, false) / factor;     // Returns 50.422775429058610d, latitude
return parseBytesLongToDouble(coordinatesasBytes, offset + 8, 8, false) / factor; // Returns 30.517877378257072d, longitude

```
{: .copy-code}

#### bytesToExecutionArrayList

Converts a byte array to an Array List implementation of a byte array with the given offset, length, and optional byte order.

**Syntax:**

*ExecutionArrayList<Byte> bytesToExecutionArrayList([byte[] bytes)*

**Parameters:**

<ul>
  <li><b>bytes:</b> <code>byte[]</code> - the byte array.</li>
</ul>

**Return value:**

ExecutionArrayList<Byte> value.

**Examples:**

```java
var bytes = [0xAA, 0xBB, 0xCC, 0xDD];
return bytesToExecutionArrayList(bytes); // Returns ExecutionArrayList<Byte> value with size = 4,  includes: [-86, -69, -52, -35]
```
{: .copy-code}


### parseBinaryArray
#### parseByteToBinaryArray

Converts a byte to binary array.

Parses  one byte to binary array from the byte given the optional binLength and endianness.

**Syntax:**

*byte[] parseByteToBinaryArray(byte byteValue[, int binLength, boolean bigEndian])*

**Parameters:**

<ul>
  <li><b>byteValue:</b> <code>byte</code>  - the byte value.</li>
  <li><b>binLength:</b> <code>int</code> - optional, the length of the output binary array.</li>
  <li><b>bigEndian:</b> <code>boolean</code> - optional, LittleEndian if false, BigEndian otherwise.</li>
</ul>

**Return value:**

byte[] value.

**Examples:**

```java
var byteVal = 0x39;
return parseByteToBinaryArray(byteVal);               // Returns byte[8] value => [0, 0, 1, 1, 1, 0, 0, 1]
return parseByteToBinaryArray(byteVal, 3);            // Returns byte[3] value => [0, 0, 1]
return parseByteToBinaryArray(byteVal, 8, false);     // Returns byte[8] value => [1, 0, 0, 1, 1, 1, 0, 0]
return parseByteToBinaryArray(byteVal, 5, false);     // Returns byte[5] value => [1, 0, 0, 1, 1]
return parseByteToBinaryArray(byteVal, 4, false);     // Returns byte[4] value => [1, 0, 0, 1]        
return parseByteToBinaryArray(byteVal, 3, false);     // Returns byte[3] value => [1, 0, 0]

var value = parseByteToBinaryArray(byteVal, 6, false);     // Returns byte[6] value => [1, 0, 0, 1, 1, 1]
var actualLowCurrent1Alarm = value[0];
var actualHighCurrent1Alarm = value[1];
var actualLowCurrent2Alarm = value[2];
var actualHighCurrent2Alarm = value[3];
var actualLowCurrent3Alarm = value[4];
var actualHighCurrent3Alarm = value[5];
```
{: .copy-code}

#### parseBytesToBinaryArray

Converts a byte Array to binary Array from the byte Array/List with the given length.

**Syntax:**

*byte[] parseBytesToBinaryArray([byte[] or List<Byte>] byteValue[, int binLength])*

**Parameters:**

<ul>
  <li><b>byteValue:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>binLength:</b> <code>int</code> - optional, the length of the output binary array.</li>
</ul>

**Return value:**

byte[] value.

**Examples:**

```java
var bytesVal = [0xCE, 0xB2];
return parseBytesToBinaryArray(bytesVal);              // Returns byte[16] value => [1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0]
return parseBytesToBinaryArray(bytesVal, 15);          // Returns byte[15] value =>    [1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0]
return parseBytesToBinaryArray(bytesVal, 14);          // Returns byte[14] value =>       [0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0]
return parseBytesToBinaryArray(bytesVal, 2);           // Returns byte[2]  value =>                                           [1, 0]
```
{: .copy-code}

#### parseLongToBinaryArray

Converts a long value to binary Array from the long value with the given length.

**Syntax:**

*byte[] parseLongToBinaryArray(long longValue[, int binLength])*

**Parameters:**

<ul>
   <li><b>longValue:</b> <code>long</code> - the long value.</li>
  <li><b> binLength:</b> <code>int</code> - optional, the length of the output binary array.</li>
</ul>

**Return value:**

byte[] value.

**Examples:**

```java
var longValue = 52914L;
return parseLongToBinaryArray(longValue);        // Returns byte[64] value => [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0]
return parseLongToBinaryArray(longValue, 16);    // Returns byte[16] value => [1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0]
```
{: .copy-code}

#### parseBinaryArrayToInt

Converts a binary array to an int value from a binary array, optionally specifying an integer offset, length, and byte order.

**Syntax:**

*int parseBinaryArrayToInt([byte[] or List<Byte>] data[, int offset, int length])*

**Parameters:**

<ul>
  <li><b>data:</b> <code>byte[]</code> or <code>List of Byte</code> - the byte array.</li>
  <li><b>offset:</b> <code>int</code> - optional, the offset in the array.</li>
  <li><b>length:</b> <code>int</code> - optional, the length in bytes.</li>
</ul>

**Return value:**

int value.

**Examples:**

```java
var binaryArrayToInt = parseBinaryArrayToInt([1, 0, 0, 1, 1, 1, 1, 1]);  // Returns -97
var actualVolt = parseBinaryArrayToInt([1, 0, 0, 1, 1, 1, 1, 1], 1, 7);  // Returns 31
```
{: .copy-code}

### parseNumber
#### parseInt

Converts input string to integer.

**Syntax:**

*Integer parseInt(String str[, String radix])*

**Parameters:**

<ul>
  <li><b>str:</b> <code>string</code> - the String containing the integer representation to be parsed.</li>
  <li><b>radix:</b> <code>String</code> - optional radix to be used while parsing string.</li>
</ul>

**Return value:**

An integer value.

**Examples:**

```java
return parseInt("0");               // Returns 0
return parseInt("473");             // Returns 473
return parseInt("+42");             // Returns 42
return parseInt("-0", 10);          // Returns 0
return parseInt("-0xFF");           // Returns -255        
return parseInt("-FF", 16);         // Returns -255
return parseInt("1100110", 2);      // Returns 102
return parseInt("2147483647", 10);  // Returns 2147483647
return parseInt("-2147483648", 10); // Returns -2147483648
return parseInt("Kona", 27);        // Returns 411787        
return parseInt("2147483648", 10);  // throws a NumberFormatException
return parseInt("99", 8);           // throws a NumberFormatException
return parseInt("Kona", 10);        // throws a NumberFormatException
```
{: .copy-code}

#### parseLong

Converts input string to long.

**Syntax:**

*Long parseLong(String str[, String radix])*

**Parameters:**

<ul>
  <li><b>str:</b> <code>string</code> - the String containing the long representation to be parsed.</li>
  <li><b>radix:</b> <code>String</code> - optional radix to be used while parsing string.</li>
</ul>

**Return value:**

A long value.

**Examples:**

```java
return parseLong("0");                          // Returns 0L
return parseLong("473");                        // Returns 473L
return parseLong("+42");                        // Returns 42L
return parseLong("-0", 10);                     // Returns 0L
return parseLong("-0xFFFF");                    // Returns -65535L        
return parseLong("-FFFF", 16);                  // Returns -65535L
return parseLong("11001101100110", 2);          // Returns 13158L
return parseLong("777777777777777777777", 8);   // Returns 9223372036854775807L
return parseLong("KonaLong", 27);               // Returns 218840926543L
return parseLong("9223372036854775807", 10);    // Returns 9223372036854775807L
return parseLong("-9223372036854775808", 10);   // Returns -9223372036854775808L
return parseLong("9223372036854775808", 10);    // throws a NumberFormatException
return parseLong("0xFGFFFFFF", 16);             // throws a NumberFormatException
return parseLong("FFFFFFFF", 16);               // throws a NumberFormatException
return parseLong("1787", 8);                    // throws a NumberFormatException
return parseLong("KonaLong", 10);               // throws a NumberFormatException
return parseLong("KonaLong", 16);               // throws a NumberFormatException
```
{: .copy-code}

#### parseFloat

Converts input string to float.

**Syntax:**

*Integer parseFloat(String str)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>string</code> - the string to be parsed.</li>
</ul>

**Return value:**

A float value.

**Examples:**

```java
return parseFloat("4.2"); // Returns 4.2f
```
{: .copy-code}

#### parseDouble

Converts input string to double.

**Syntax:**

*Integer parseDouble(String str)*

**Parameters:**

<ul>
  <li><b>str:</b> <code>string</code> - the string to be parsed.</li>
</ul>

**Return value:**

A double precision value.

**Examples:**

```java
return parseDouble("4.2"); // Returns 4.2d
```
{: .copy-code}

### Bitwise Operations

Bitwise operations in ThingsBoard allow manipulation of individual bits within integer, long, and boolean values. These operations support mixed-type operands (Integer, Long, Boolean) without implicit type conversions, and the result type depends on the operation's outcome: either Integer or Long.

**Supported operators:**

```markdown
- AND `&`: Performs a bit-by-bit comparison, where the result bit is `1` if both bits are `1`.
- OR `|`: Sets a bit to `1` if at least one of the corresponding bits is `1`.
- XOR `^` Sets a bit to `1` if the corresponding bits are different.
- Left shift `<<`: Shifts bits to the left, effectively multiplying the number by `2^n`.
- Right shift `>>`: Shifts bits to the right, effectively dividing the number by `2^n` (preserving the sign).
- Unsigned Right shift `>>>`: Shifts bits to the right, filling with zeroes, regardless of the sign.
```

Supported Data Type Combinations

1. Integer - Integer: Produces an Integer result.
2. Long - Long: Produces a Long result.
3. Boolean - Boolean: Produces an Integer result.
3. Mixed Types (e.g., Integer - Long, Long - Boolean, Boolean - Integer): The operation ensures compatibility between input types, and the result type is determined based on precision or hierarchy:

- If one of the types is Long, the result is Long.
- If one type is Boolean and the other is Integer, the result is Integer.
- If both types are Boolean, the result is also Integer.

{% capture difference %}
**Note**:
<br>
eg *Boolean* - (Integer / Long) or (Integer / Long) - *Boolean*:
- Boolean values are interpreted as 1 (true) or 0 (false) and can interact with integers or longs directly.
{% endcapture %}
{% include templates/info-banner.md content=difference %}

**Examples:**

*Boolean - Boolean*

```java
var x = true;
var y = false;

var andResult = x & y; 
return andResult;       // Returns 0   
        
var orResult = x | y;  
return orResult;        // Returns 1 
        
var xorResult = x ^ y;  
return xorResult;       // Returns 1 
        
var leftShift = x << y; 
return leftShift;       // Returns 1 
        
var rightShift = x >> y;
return rightShift;      // Returns 1 
        
var rightUnShift = x >>> y;
return rightUnShift;     // Returns 1 
```
{: .copy-code}


*Mixed*

```java
var x = true;
var y = false;
var i = 10;
var b = -14;
var l = 9223372036854775807;

var andResult = x & b; 
return andResult;       // Returns 0   
        
var orResult = i | y;  
return orResult;        // Returns 10 
        
var xorResult = i ^ l;  
return xorResult;       // Returns 9223372036854775797L
        
var leftShift = l << i; 
return leftShift;       // Returns -1024L 
        
var rightShift = l >> b;
return rightShift;      // Returns 8191L 
        
var rightUnShift = i >>> x;
return rightUnShift;     // Returns 5 
```
{: .copy-code}

### base64
#### base64ToHex

Decodes the Base64 string, to hex string.

**Syntax:**

*String base64ToHex(String input)*

**Parameters:**

<ul>
  <li><b>input:</b> <code>String</code> - the Base64 string.</li>
</ul>

**Return value:**

Hex string

**Examples:**

```java
return base64ToHex("Kkk="); // Returns "2A49"
```
{: .copy-code}

#### bytesToBase64

Encodes a byte array into a Base64 string.

**Syntax:**

*String bytesToBase64(byte[] bytes)*

**Parameters:**

<ul>
  <li><b>bytes:</b> <code>List of integer</code> - the list of integer values, where each integer represents a single byte.</li>
</ul>

**Return value:**

Base64 string.

**Examples:**

```java
return bytesToBase64([42, 73]); // Returns "Kkk="
```
{: .copy-code}

#### base64 To Bytes Array or List
##### base64ToBytes

Decodes a Base64 string into a byte array.

**Syntax:**

*byte[] base64ToBytes(String input)*

**Parameters:**

<ul>
  <li><b>input:</b> <code>String</code> - the Base64 string.</li>
</ul>

**Return value:**

Byte array.

**Examples:**

```java
return base64ToBytes("Kkk="); // Returns [42, 73]
```
{: .copy-code}

###### base64ToBytesList

Decodes a Base64 string into a byte list.

**Syntax:**

*List<Byte> base64ToBytesList(String input)*

**Parameters:**

<ul>
  <li><b>input:</b> <code>String</code> - the Base64 string.</li>
</ul>

**Return value:**

Byte array.

**Examples:**

```java
return base64ToBytesList("AQIDBAU="); // Returns ExecutionArrayList<Byte> value with size = 5,  includes: [1, 2, 3, 4, 5]
```
{: .copy-code}

### toFlatMap

Iterates recursive over the given object and creates a single level json object.  
If the incoming message contains arrays, the key for transformed value will contain index of the element.  

**Syntax:**

```java
Map<String, Object> toFlatMap(Map<String, Object> json)
Map<String, Object> toFlatMap(Map<String, Object> json, boolean pathInKey)
Map<String, Object> toFlatMap(Map<String, Object> json, List<String> excludeList)
Map<String, Object> toFlatMap(Map<String, Object> json, List<String> excludeList, boolean pathInKey)
```
{: .copy-code}

**Parameters:**

 - **json** `Map<String, Object>` - input JSON object.  
 - **excludeList** `List<String>` - *optional* List with keys, for exclude from result. ***Default:*** `[]`.    
 - **pathInKey** `boolean` - *optional* Add path to keys. ***Default:*** `true`.  

{% capture warning %}
If parameter **pathInKey** is set to ***false*** - some keys can be overwritten by newly found values!  
Recommended to use with caution with objects that contains similar keys on different levels.  
{% endcapture %}
{% include templates/warn-banner.md content=warning %}

**Return value:**

JSON Object

**Examples:**

*Input:*

```json
{
    "key1": "value1",
    "key2": 12,
    "key3": {
        "key4": "value4",
        "key5": 34,
        "key6": [{
                "key7": "value7"
            },
            {
                "key8": "value8"
            },
            "just_string_value_in_array",
            56
        ],
        "key9": {
            "key10": ["value10_1", "value10_2", "value10_3"]
        }
    },
    "key_to_overwrite": "root_value",
    "key11": {
        "key_to_overwrite": "second_level_value"
    }
}
```

#### toFlatMap(json)

Expected behavior - get the single level object with paths in keys. 

```java
return toFlatMap(json);
```
{: .copy-code}

*Output:*

```json
{
    "key1": "value1",
    "key2": 12,
    "key3.key4": "value4",
    "key3.key5": 34,
    "key3.key6.0.key7": "value7",
    "key3.key6.1.key8": "value8",
    "key3.key6.2": "just_string_value_in_array",
    "key3.key6.3": 56,
    "key3.key9.key10.0": "value10_1",
    "key3.key9.key10.1": "value10_2",
    "key3.key9.key10.2": "value10_3",
    "key_to_overwrite": "root_value",
    "key11.key_to_overwrite": "second_level_value"
}
```
{: .copy-code}

#### toFlatMap(json, pathInKey)

Expected behavior - get the single level object without paths in keys.  
**key_to_overwrite** should contain the value from **key11.key_to_overwrite**.  

```java
return toFlatMap(json, false);
```
{: .copy-code}

*Output:*

```json
{
    "key1": "value1",
    "key2": 12,
    "key4": "value4",
    "key5": 34,
    "key7": "value7",
    "key8": "value8",
    "key6.2": "just_string_value_in_array",
    "key6.3": 56,
    "key10.0": "value10_1",
    "key10.1": "value10_2",
    "key10.2": "value10_3",
    "key_to_overwrite": "second_level_value"
}
```
{: .copy-code}

As you can see, **key_to_overwrite** is **second_level_value** instead of **root_value**.  
  
#### toFlatMap(json, excludeList)

Expected behavior - get the single level object with paths in keys, without keys **key1** and **key3**.  

```java
return toFlatMap(json, ["key1", "key3"]);
```
{: .copy-code}

*Output:*

```json
{
    "key2": 12,
    "key_to_overwrite": "root_value",
    "key11.key_to_overwrite": "second_level_value"
}
```

As you can see, **key1** and **key3** were removed from output. Excluding works on any level with incoming keys.   
  
#### toFlatMap(json, excludeList, pathInKey)

Expected behavior - get the single level object without paths in keys, without keys **key2** and **key4**.  
**key_to_overwrite** should contain the value from **key11.key_to_overwrite**.    

```java
return toFlatMap(json, ["key2", "key4"], false);
```
{: .copy-code}

*Output:*

```json
{
    "key1": "value1",
    "key5": 34,
    "key7": "value7",
    "key8": "value8",
    "key6.2": "just_string_value_in_array",
    "key6.3": 56,
    "key10.0": "value10_1",
    "key10.1": "value10_2",
    "key10.2": "value10_3",
    "key_to_overwrite": "second_level_value"
}
```

As you can see, **key2** and **key4** was excluded from the output and **key_to_overwrite** is **second_level_value**.  

### tbDate: 

The *Tbel* library uses all standard JavaScript methods in the [JavaScript Date](https://www.w3schools.com/jsref/jsref_getdate.asp), such as **"toLocaleString"**,  **"toISOString"** and other methods;

#### Input format data:
- String with Optional: pattern, locale, time zone
- ints (year, month and etc) with Optional: pattern, locale, time zone

Locale default: *UTC*;

Time zone Id default: *ZoneId.systemDefault()*;

**Return value:**

a Date object as a string, using locale default: "UTC", time zone Id default: ZoneId.systemDefault().

##### String with Optional: pattern, locale, time zone 

**Examples:**
- Input data format: Only one - String (Variable the Time Zone in the input parameter), without pattern:

```ruby
assuming: "2007-12-03T10:15:30.00Z"           TZ = UTC
           "2007-12-03T10:15:30.00"           TZ = ZoneId.systemDefault() instant
           "2007-12-03T10:15:30.00-04"        TZ = "-04"
           "2007-12-03T10:15:30.00+02:00"     TZ = "+02"
           "2007-12-03T10:15:30.00+03:00:00"  TZ = "+03"
```

```java
var d = new Date("2023-08-06T04:04:05.123Z");        // TZ => "UTC"
var dIso = d.toISOString();                          //  return "2023-08-06T04:04:05.123Z"   ZoneId "UTC"
```
{: .copy-code}

```java
var d = new Date("2023-08-06T12:04:05.123");        // TZ => Default
var dIso = d.toISOString();                         //  return "2023-08-06T16:04:05.123Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")                                                               
```
{: .copy-code}

```java
var d = new Date("2023-08-06T04:04:05.00-04");      //  TZ => "-04"
var dIso = d.toISOString();                         //  return "2023-08-06T08:04:05Z"   ZoneId "America/New_York" = "-04:00"
```
{: .copy-code}

```java
var d = new Date("2023-08-06T04:04:05.00+02:00");   //  TZ => "+02:00"
var dIso = d.toISOString();                         //  return "2023-08-06T02:04:05Z"   ZoneId "Europe/Berlin" = "+02:00"
```
{: .copy-code}

```java
var d = new Date("2023-08-06T04:04:05.00+03:00:00");   //  TZ => "+03:00:00"
var dIso = d.toISOString();                            //  return "2023-08-06T01:04:05Z"   ZoneId  "Europe/Kyiv" = "+03:00"
```
{: .copy-code}

```ruby
assuming RFC-1123 value: "Tue, 3 Jun 2008 11:05:30 GMT"         TZ = UTC
                         "Tue, 3 Jun 2008 11:05:30"             TZ = ZoneId.systemDefault() instant
                         "Tue, 3 Jun 2008 11:05:30 -03"         TZ = "-03"
                         "Tue, 3 Jun 2008 11:05:30 -0200"       TZ = "-02"                   
                         "Tue, 3 Jun 2008 11:05:30 +043056"     TZ = "+04:30:56"          
 ```

```java
var d = new Date("Tue, 3 Jun 2008 11:05:30 GMT");   //  TZ => "GMT"
var dIso = d.toISOString();                         //  return "2008-06-03T11:05:30Z"   ZoneId  "GMT" = "0"
```
{: .copy-code}

```java
var d = new Date("Tue, 3 Jun 2008 11:05:30 +043056");   //  TZ => "+043056"
var dIso = d.toISOString();                             //  return "2008-06-03T06:34:34Z"   ZoneId  "????" = "+04:30:56"
```
{: .copy-code}

- Input data format: String + Pattern:

```java
var pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX";
var d = new Date("2023-08-06 04:04:05.000Z", pattern);        // Pattern without TZ => ZoneId "UTC"
var dIso = d.toISOString();                                   // return "2023-08-06T04:04:05Z"    ZoneId "UTC" = "00:00"
```
{: .copy-code}


```java
var pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX";
var d = new Date("2023-08-06 04:04:05.000-04:00", pattern);   // Pattern with TZ => "-04:00"
var dIso = d.toISOString();                                   // return "2023-08-06T08:04:05Z"    ZoneId "America/New_York" = "-04:00"
```
{: .copy-code}

```java
var pattern = "yyyy-MM-dd HH:mm:ss.SSS";
var d = new Date("2023-08-06 12:04:05.000", pattern);         //  Pattern without TZ, TZ = ZoneId.systemDefault() instant
var dIso = d.toISOString();                                   //  return "2023-08-06T16:04:05Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
```
{: .copy-code}

- Input data format: String + Pattern + Locale:

```java
var pattern = "hh:mm:ss a, EEE M/d/uuuu";
var d = new Date("12:15:30 PM, So. 10/09/2022", pattern, "de");         //  Pattern without TZ, TZ = ZoneId.systemDefault() instant
var dIso = d.toISOString();                                             //  return "2022-10-09T16:15:30Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
var dLocal= d.toLocaleString("de");                                     //  return "09.10.22, 12:15:30"
```
{: .copy-code}

```java
var pattern = "hh:mm:ss a, EEE M/d/uuuu";
var d = new Date("12:15:30 PM, Sun 10/09/2022", pattern, "en-US");         //  Pattern without TZ, TZ = ZoneId.systemDefault() instant
var dIso = d.toISOString();                                                //  return "2022-10-09T16:15:30Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
var dLocal = d.toLocaleString("en-US");                                    //  return "10/9/22, 12:15:30 PM"
```
{: .copy-code}

```java
var pattern = "hh:mm:ss a, EEE M/d/uuuu";
var d = new Date("02:15:30 PM, Sun 10/09/2022", pattern, "de");             // return: Error: could not create constructor 
                                                                            // The pattern with input parameter of date does not match the locale
                                                                            // Locale = "de", input parameter "day of the week" is "Sun", should be "So.". 
```
{: .copy-code}
- Input data format: String + Pattern + Locale + Time Zone:

```java
var pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX";
var d = new Date("2023-08-06 04:04:05.000+02:00", pattern, "de", "America/New_York");    // Pattern with TZ => "+02:00" but `Time Zone` as parameter = "America/New_York" = "-04:00"
var dIso = d.toISOString();                                                         // return "2023-08-06T08:04:05Z"    ZoneId "America/New_York" = "-04:00"
var dLocal_de = d.toLocaleString("de");                                             // return "06.08.23, 04:04:05" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")   
var dLocal_us = d.toLocaleString("en-US");                                          // return "8/6/23, 4:04:05 AM"      
```
{: .copy-code}

##### Instant (year, month and etc) with Optional: time zone
```java
var d = new Date(2023, 8, 6, 4, 4, 5, "America/New_York");
var dLocal = d.toLocaleString();        //  return "2023-08-06 04:04:05" (Locale: "UTC", ZoneId "America/New_York")
var dIso = d.toISOString();             //  return "2023-08-06T08:04:05Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
var dDate = d;                          //  return "Sunday, August 6, 2023 at 4:04:05 AM Eastern Daylight Time"
```
{: .copy-code}

```java
var d = new Date(2023, 8, 6, 4, 4, 5, "Europe/Berlin");
var dLocal = d.toLocaleString();                                    // return "2023-08-05 22:04:05" (Locale: "UTC", (if ZoneId.systemDefault(): "America/New_York" = "-04:00"))
var dLocal_us = d.toLocaleString("en-us", "America/New_York");      // return "8/5/23, 10:04:05 PM" (Locale: "en-us", ZoneId "America/New_York")
var dIso = d.toISOString();                                         // return "2023-08-06T02:04:05Z"
var dDate = d;                                                      // return "Saturday, August 5, 2023 at 22:04:05 AM Eastern Daylight Time" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
```
{: .copy-code}

#### locale

**Syntax:**

*String toLocaleString(String locale)*

**Parameters:**

Time zone Id default: *ZoneId.systemDefault()*;
<ul>
  <li><b>locale:</b> <code>string</code> - Language-specific format to use.</li>
</ul>

**Return value:**

a Date object as a string, using locale settings, time zone Id default: ZoneId.systemDefault().

**Examples:**
_Input date Without TZ (TZ Default = ZoneId.systemDefault())_
```java
var d = new Date(2023, 8, 6, 4, 4, 5);          //  Parameters (int year, int month, int dayOfMonth, int hours, int minutes, int seconds) => TZ Default = ZoneId.systemDefault();
var dLocal = d.toLocaleString("en-US");         //  return "8/6/23, 4:04:05 AM" (Locale: "en-US")
var dIso = d.toISOString();                     //  return Default = ZoneId.systemDefault: "2023-08-06T08:04:05Z", (if ZoneId.systemDefault(): "America/New_York" = "-04:00");
var dDate = d;                                  //  return "Sunday, August 6, 2023 at 4:04:05 AM Eastern Daylight Time" (if ZoneId.systemDefault(): "America/New_York" = "-04:00");
```
{: .copy-code}
        
```java
var d = new Date("2023-08-06T04:04:05.000");     //  Parameter (String 'yyyy-MM-ddThh:mm:ss.ms') => TZ Default = ZoneId.systemDefault():
var dIso = d.toISOString();                      //  return "2023-08-06T08:04:05Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00");
var dLocal_de = d.toLocaleString("de");          //  return "06.08.23, 04:04:05"  (Locale: "de",  ZoneId.systemDefault());
var dLocal_utc = d.toLocaleString("UTC");        //  return "2023-08-06 04:04:05" (Locale: "UTC", ZoneId.systemDefault());
```
{: .copy-code}

_Input date With TZ (TZ = parameter TZ or 'Z' equals 'UTC')_
```java
var d = new Date(2023, 8, 6, 4, 4, 5, "Europe/Berlin"); //  Parameters (int year, int month, int dayOfMonth, int hours, int minutes, int seconds, TZ) => TZ "Europe/Berlin";
var dIso = d.toISOString();                             //  return "2023-08-06T02:04:05Z";
var dLocal1 = d.toLocaleString("UTC");                  //  return "2023-08-05 22:04:05" (Locale: "UTC",  (if ZoneId.systemDefault(): "America/New_York" = "-04:00"));
var dLocal2 = d.toLocaleString("en-us");                //  return "8/5/23, 10:04:05 PM" (Locale: "en-US", (if ZoneId.systemDefault(): "America/New_York" = "-04:00"));
var dLocal3 = d.toLocaleString("de");                   // return "05.08.23, 22:04:05"  (Locale: "de",   (if ZoneId.systemDefault(): "America/New_York" = "-04:00"));
```     
{: .copy-code}

#### locale, time zone

**Syntax:**

*String toLocaleString(String locale, String tz)*

**Parameters:**
<ul>
  <li><b>locale:</b> <code>string</code> - Language-specific format to use.</li>
  <li><b>tz:</b> <code>string</code> - Id local time zone.</li>  
</ul>

**Return value:**

a Date object as a string, using locale settings and Id time zone.

**Examples:**

```java
var d = new Date(2023, 8, 6, 4, 4, 5, "Europe/Berlin");         //  Parameters (int year, int month, int dayOfMonth, int hours, int minutes, int seconds, TZ) => TZ "Europe/Berlin";
var dIso = d.toISOString();                                     //  return "2023-08-06T02:04:05Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00");
var dLocal1 = d.toLocaleString("UTC");                          //  return "2023-08-05 22:04:05" (Locale: "UTC",   (if ZoneId.systemDefault(): "America/New_York" = "-04:00"));
var dLocal2 = d.toLocaleString("en-us", "America/New_York");    //  return "8/5/23, 10:04:05 PM" (Locale: "en-US", ZoneId "America/New_York" = "-04:00")
var dLocal3 = d.toLocaleString("de", "Europe/Berlin");          //  return "06.08.23, 04:04:05"  (Locale: "de",    ZoneId "Europe/Berlin" = "+02:00")
```
{: .copy-code}

#### local, pattern (With Map options)

**Syntax:**

*String toLocaleString(String locale, String optionsStr)*

**Parameters:**
<ul>
  <li><b>locale:</b> <code>string</code> - Language-specific format to use.</li>
  <li><b>pattern:</b> <code>string</code> - Pattern, id time zone, Date/Time string format to use.</li>  
</ul>

**Return value:**

a Date object as a string, using locale settings, {"timeZone": "Id time zone",
                                                   "dateStyle": "full/long/medium/short",
                                                   "timeStyle": "full/long/medium/short",
                                                   "pattern": "M/d/yyyy, h:mm:ss a"}.

**Examples:**

```java
var d = new Date("2023-08-06T04:04:05.00Z");         // TZ => "UTC"
var dIso = d.toISOString();                          // return "2023-08-06T04:04:05Z"
var dLocal1 = d.toLocaleString();                    // return "2023-08-06 00:04:05" (Locale: Default "UTC", ZoneId: (if ZoneId.systemDefault(): "America/New_York" = "-04:00"));

var options = {"timeZone":"Europe/Berlin"};       // TZ = "+02:00"
var optionsStr = JSON.stringify(options);       
var dLocal2 = d.toLocaleString("en-US", optionsStr); // "8/6/23, 6:04:05 AM"  (Locale:  "en-US",  options: ZoneId  => "Europe/Berlin" = "+02:00");
```
{: .copy-code}

```java
var d = new Date("2023-08-06T04:04:05.000");         // TZ => Default = ZoneId.systemDefault
var dIso = d.toISOString();                          // return "2023-08-06T08:04:05Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
var options = {"timeZone":"Europe/Berlin"};
var optionsStr = JSON.stringify(options);
var dLocal1 = d.toLocaleString("en-US", optionsStr); // return "8/6/23, 10:04:05 AM" (Locale: "en-US",  options: ZoneId  => "Europe/Berlin" = "+02:00");
```
{: .copy-code}

```java
var d = new Date(2023, 8, 6, 4, 4, 5);               // TZ => Default = ZoneId.systemDefault
var dIso = d.toISOString();                          // return "2023-08-06T08:04:05Z" (if ZoneId.systemDefault(): "America/New_York" = "-04:00")
var options = {"timeZone":"Europe/Berlin", "pattern": "M-d/yyyy, h:mm=ss a"};
var optionsStr = JSON.stringify(options);
var dLocal1 = d.toLocaleString("en-US", optionsStr); // return "8-6/2023, 10:04=05 AM" (options: pattern,  Locale = "en-US", ZoneId  => "Europe/Berlin" = "+02:00");
```
{: .copy-code}

```java
var d = new Date(2023, 8, 6, 4, 4, 5, "UTC");        // TZ => "UTC"
var dIso = d.toISOString();                          // return "2023-08-06T04:04:05Z"       
var options = {"timeZone":"Europe/Berlin","dateStyle":"full","timeStyle":"full"};
var optionsStr = JSON.stringify(options);
var dLocal1 = d.toLocaleString("uk-UA", optionsStr); // return  "неділя, 6 серпня 2023 р. о 06:04:05 за центральноєвропейським літнім часом"
var dLocal2 = d.toLocaleString("en-US", optionsStr); // return  "Sunday, August 6, 2023 at 6:04:05 AM Central European Summer Time"
var dLocal3 = d.toLocaleString("de", optionsStr);    // return  "Sonntag, 6. August 2023 um 06:04:05 Mitteleuropäische Sommerzeit"
```
{: .copy-code}

##### functions to add time units separately: years, months, weeks, days, hours, minutes, seconds, and milliseconds.

**Examples:**

```java
var stringDateUTC = "2024-01-01T10:00:00.00Z";
var d = new Date(stringDateUTC);            // TZ => "UTC"
var dIso = d.toISOString();                           // return 2024-01-01T10:00:00Z 
        
d.addYears(1);
d.toISOString();                                     // return 2025-01-01T10:00:00Z

d.addYears(-2);
d.toISOString();                                     // return 2023-01-01T10:00:00Z

d.addMonths(2);
d.toISOString();                                     // return 2023-03-01T10:00:00Z

d.addMonths(10);
d.toISOString();                                     // return 2024-01-01T10:00:00Z

d.addMonths(-13);
d.toISOString();                                     // return 2022-12-01T10:00:00Z

d.addWeeks(4);
d.toISOString();                                     // return 2022-12-29T10:00:00Z

d.addWeeks(-5);
d.toISOString();                                     // return 2022-11-24T10:00:00Z

d.addDays(6);
d.toISOString();                                     // return 2022-11-30T10:00:00Z

d.addDays(45);
d.toISOString();                                     // return 2023-01-14T10:00:00Z

d.addDays(-50);
d.toISOString();                                     // return 2022-11-25T10:00:00Z

d.addHours(23);
d.toISOString();                                     // return 2022-11-26T09:00:00Z

d.addHours(-47);
d.toISOString();                                     // return 2022-11-24T10:00:00Z

d.addMinutes(59);
d.toISOString();                                     // return 2022-11-24T10:59:00Z
        
d.addMinutes(-60);
d.toISOString();                                     // return 2022-11-24T09:59:00Z

d.addSeconds(59);
d.toISOString();                                     // return 2022-11-24T09:59:59Z

d.addSeconds(-60);
d.toISOString();                                     // return 2022-11-24T09:58:59Z

d.addNanos(999999);
d.toISOString();                                     // return 2022-11-24T09:58:59.000999999Z

d.addNanos(-1000000);
d.toISOString();                                     // return 2022-11-24T09:58:58.999999999Z
```
{: .copy-code}

### Geofencing utility functions

#### isInsidePolygon

Checks whether the given geographic coordinate is within the perimeter of the given polygon.

**Syntax:**

*boolean isInsidePolygon(double latitude, double longitude, String perimeter)*

**Parameters:**

<ul>
  <li><b>latitude:</b> <code>double</code> – The latitude of the coordinate.</li>
  <li><b>longitude:</b> <code>double</code> – The longitude of the coordinate.</li>
  <li><b>perimeter:</b> <code>String</code> – A string that represents a polygon using a nested array format:
    <ul>
      <li>The first array defines the outer boundary of the polygon.</li>
      <li>Any additional arrays define inner boundaries (holes).</li>
      <li>Each point is defined as a pair <code>[latitude, longitude]</code>.</li>
    </ul>
  </li>
</ul>

**Return value:**

A boolean indicating the result:
- `true` if the coordinate is inside the polygon. 
- `false` otherwise.

**Examples:**

```java
var perimeter = "[[[37.7810,-122.4210],[37.7890,-122.3900],[37.7700,-122.3800],[37.7600,-122.4000],[37.7700,-122.4250],[37.7810,-122.4210]],[[37.7730,-122.4050],[37.7700,-122.3950],[37.7670,-122.3980],[37.7690,-122.4100],[37.7730,-122.4050]]]";
// Outside the polygon
return isInsidePolygon(37.8000, -122.4300, perimeter); // Returns false
// Inside the polygon
return isInsidePolygon(37.7725, -122.4010, perimeter); // Returns true
// Inside the hole
return isInsidePolygon(37.7700, -122.4030, perimeter); // Returns false
```
{: .copy-code}

#### isInsideCircle

Checks whether the given geographic coordinate is within a circular perimeter.

**Syntax:**

*boolean isInsideCircle(double latitude, double longitude, String perimeter)*

**Parameters:**

<ul>
  <li><b>latitude:</b> <code>double</code> – The latitude of the coordinate.</li>
  <li><b>longitude:</b> <code>double</code> – The longitude of the coordinate.</li>
  <li><b>perimeter:</b> <code>String</code> – A JSON string representing a circle with fields: 
    <ul>
      <li><code>latitude</code>: latitude of the circle center.</li>
      <li><code>longitude</code>: longitude of the circle center.</li>
      <li><code>radius</code>: radius of the circle.</li>
      <li><code>radiusUnit</code>: optional, defines the unit of the radius. Defaults to <code>METER</code>. Supported values are: <code>METER</code>, <code>KILOMETER</code>, <code>FOOT</code>, <code>MILE</code>, <code>NAUTICAL_MILE</code>.</li>
    </ul>
  </li>
</ul>

**Return value:**

A boolean indicating the result:
- `true` if the coordinate is inside the circle.
- `false` otherwise.

**Examples:**

```java
var perimeter = "{\"latitude\":37.7749,\"longitude\":-122.4194,\"radius\":3000,\"radiusUnit\":\"METER\"}";
// Outside the circle
return isInsideCircle(37.8044, -122.2712, perimeter); // Returns false
// Inside the circle
return isInsideCircle(37.7599, -122.4148, perimeter); // Returns true
```
{: .copy-code}
