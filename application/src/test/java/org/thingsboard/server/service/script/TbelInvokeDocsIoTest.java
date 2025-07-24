/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.script;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbDate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TbelInvokeDocsIoTest extends AbstractTbelInvokeTest {

    private String decoderStr;
    private String msgStr;

    private final String msgMapStr = """
            {"temperature": 42, "nested" : "508"};
            """;
    private final LinkedHashMap<String, Object> expectedMap = new LinkedHashMap<>(Map.of("temperature", 42, "nested", "508"));

    // Simple Property Expression
    @Test
    void simplePropertyBooleanExpression_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"temperature": 15, "humidity" : 30}
                """;
        decoderStr = "return (msg.temperature > 10 && msg.temperature < 20) || (msg.humidity > 10 && msg.humidity < 60);";
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        boolean expected = true;
        assertEquals(expected, actual);
    }

    // Multiple statements
    @Test
    public void multipleStatements_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                var a = 2;
                var b = 2;
                return a + b;
                """;
        Integer expected = 4;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // Maps
    @Test
    public void mapsChangeValueKey_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.temperature = 0;
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        expected.put("temperature", 0);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }


    @Test
    public void mapsCheckExistenceKey_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                if(msg.temperature != null){
                    msg.temperature = 2;
                }
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        expected.put("temperature", 2);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsNullSafeExpressionsUsing_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.existingKey = {};
                msg.existingKey.smth = 20;
                if(msg.?nonExistingKey.smth > 10){
                    msg.temperature = 2;
                }
                if(msg.?existingKey.smth > 10){
                    msg.nested = "100";
                }
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        expected.put("nested", "100");
        LinkedHashMap<String, Object> existingKey = new LinkedHashMap<>(Map.of("smth", 20));
        expected.put("existingKey", existingKey);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsIterateThrough_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                foreach(element : msg.entrySet()){
                  if(element.getKey() == null){
                    return raiseError("Bad getKey");
                  }
                  if(element.key == null){
                    return raiseError("Bad key");
                  }
                  if(element.getValue() == null){
                    return raiseError("Bad getValue");
                  }
                  if(element.value == null){
                    return raiseError("Bad value");
                  }
                }
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsImplicitIterationWithoutEntrySet() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                foreach(element : msg){
                  if(element.getKey() == null){
                    return raiseError("Bad getKey");
                  }
                  if(element.key == null){
                    return raiseError("Bad key");
                  }
                  if(element.getValue() == null){
                    return raiseError("Bad getValue");
                  }
                  if(element.value == null){
                    return raiseError("Bad value");
                  }
                }
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsGetInfoSize_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                var size = msg.size();
                return size;
                """;
        Integer expected = 2;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsGetInfoMemorySize_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                var memorySize = msg.memorySize();
                return memorySize;
                """;
        Long expected = 24L;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsGetInfoPut_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.humidity = 73;
                msg.put("humidity", 74);
                msg.putIfAbsent("temperature1", 73);
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        expected.put("humidity", 74);
        expected.putIfAbsent("temperature1", 73);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsChangeValuePut_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.temperature = 73;
                msg.putIfAbsent("temperature1", 73);
                var put1 = msg.put("temperature", 74);
                msg.putIfAbsent("humidity", 73);
                var putIfAbsent1 = msg.putIfAbsent("humidity", 74);
                return {map: msg,
                        put1: put1,
                        putIfAbsent1: putIfAbsent1
                       };
                """;
        LinkedHashMap<String, Object> expectedMapChangeValue = expectedMap;
        expectedMapChangeValue.replace("temperature", 42, 74);
        expectedMapChangeValue.put("humidity", 73);
        expectedMapChangeValue.putIfAbsent("temperature1", 73);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("map", expectedMapChangeValue);
        expected.put("put1", 73);
        expected.put("putIfAbsent1", 73);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsChangeValueReplace_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.put("humidity", 73);
                msg.putIfAbsent("temperature1", 73);
                var replace = msg.replace("temperature", 56);
                var replace1 = msg.replace("temperature", 56, 45);
                var replace2 = msg.replace("temperature", 48, 56);
                return {map: msg,
                        replace: replace,
                        replace1: replace1,
                        replace2: replace2
                       }
                """;
        LinkedHashMap<String, Object> expectedMapReplaceValue = expectedMap;
        expectedMapReplaceValue.replace("temperature", 42, 45);
        expectedMapReplaceValue.put("humidity", 73);
        expectedMapReplaceValue.putIfAbsent("temperature1", 73);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("map", expectedMapReplaceValue);
        expected.put("replace", 42);
        expected.put("replace1", true);
        expected.put("replace2", false);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsRemoveEntryFromMapByKey_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.put("humidity", 73);
                msg.putIfAbsent("temperature1", 73);
                msg.remove("temperature");
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        expected.put("humidity", 73);
        expected.putIfAbsent("temperature1", 73);
        expected.remove("temperature");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsGetKeysValues_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.put("humidity", 73); 
                msg.putIfAbsent("temperature1", 73);
                msg.remove("temperature");
                var keys = msg.keys();
                var values = msg.values();
                return {keys: keys,
                        values: values
                       }
                """;
        LinkedHashMap<String, Object> expectedMapKeysValues = expectedMap;
        expectedMapKeysValues.put("humidity", 73);
        expectedMapKeysValues.putIfAbsent("temperature1", 73);
        expectedMapKeysValues.remove("temperature");
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("keys", expectedMapKeysValues.keySet().stream().toList());
        expected.put("values", expectedMapKeysValues.values().stream().toList());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void mapsSort_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.put("humidity", 73);
                msg.putIfAbsent("temperature1", 73);
                msg.remove("temperature");
                msg.sortByKey();
                var mapSortByKey = msg.clone();
                var sortByValue = msg.sortByValue(); 
                return {mapSortByKey: mapSortByKey,
                        sortByValue: sortByValue,
                        mapSortByValue: msg
                       }
                """;
        LinkedHashMap<String, Object> expectedMapSortByKey = new LinkedHashMap<>();
        expectedMapSortByKey.put("humidity", 73);
        expectedMapSortByKey.put("nested", "508");
        expectedMapSortByKey.put("temperature1", 73);
        LinkedHashMap<String, Object> expectedMapSortByValue = new LinkedHashMap<>();
        expectedMapSortByValue.put("humidity", 73);
        expectedMapSortByValue.put("temperature1", 73);
        expectedMapSortByValue.put("nested", "508");
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("mapSortByKey", expectedMapSortByKey);
        expected.put("mapSortByValue", expectedMapSortByValue);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
        assertEquals(expected.get("mapSortByKey"), ((LinkedHashMap) actual).get("mapSortByKey"));
        assertEquals(((LinkedHashMap) expected.get("mapSortByKey")).keySet().toArray()[0], ((LinkedHashMap) ((LinkedHashMap) actual).get("mapSortByKey")).keySet().toArray()[0]);
        assertEquals(((LinkedHashMap) expected.get("mapSortByKey")).keySet().toArray()[1], ((LinkedHashMap) ((LinkedHashMap) actual).get("mapSortByKey")).keySet().toArray()[1]);
        assertEquals(((LinkedHashMap) expected.get("mapSortByKey")).keySet().toArray()[2], ((LinkedHashMap) ((LinkedHashMap) actual).get("mapSortByKey")).keySet().toArray()[2]);
        assertEquals(expected.get("mapSortByValue"), ((LinkedHashMap) actual).get("mapSortByValue"));
        assertEquals(((LinkedHashMap) expected.get("mapSortByValue")).keySet().toArray()[0], ((LinkedHashMap) ((LinkedHashMap) actual).get("mapSortByValue")).keySet().toArray()[0]);
        assertEquals(((LinkedHashMap) expected.get("mapSortByValue")).keySet().toArray()[1], ((LinkedHashMap) ((LinkedHashMap) actual).get("mapSortByValue")).keySet().toArray()[1]);
        assertEquals(((LinkedHashMap) expected.get("mapSortByValue")).keySet().toArray()[2], ((LinkedHashMap) ((LinkedHashMap) actual).get("mapSortByValue")).keySet().toArray()[2]);
    }

    @Test
    public void mapsAddNewEntry_Test() throws ExecutionException, InterruptedException {
        msgStr = msgMapStr;
        decoderStr = """
                msg.put("humidity", 73);
                msg.putIfAbsent("temperature1", 73);
                msg.remove("temperature");
                var mapAdd = {"test": 12, "input" : {"http": 130}};
                msg.putAll(mapAdd);
                return msg;
                """;
        LinkedHashMap<String, Object> expected = expectedMap;
        expected.put("humidity", 73);
        expected.putIfAbsent("temperature1", 73);
        expected.remove("temperature");
        LinkedHashMap<String, Object> expectedMapAdd = new LinkedHashMap<>();
        expectedMapAdd.put("test", 12);
        LinkedHashMap<String, Object> expectedInput = new LinkedHashMap<>();
        expectedInput.put("http", 130);
        expectedMapAdd.put("input", expectedInput);
        expected.putAll(expectedMapAdd);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // Lists
    @Test
    public void listsCreateNewList_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["A", "B", "C"]}
                """;
        decoderStr = """
                var list = msg.list;
                var list0 = list[0];
                var listSize = list.size();
                var smthForeach = "";
                foreach (item : list) {
                  smthForeach += item;
                }
                var smthForLoop= "";
                for (var i =0; i < list.size; i++) {
                  smthForLoop += list[i];
                }
                return {list: list,
                        list0: list0,
                        listSize: listSize,
                        smthForeach: smthForeach,
                        smthForLoop: smthForLoop
                       }
                """;
        List expectedList = new ArrayList<>(List.of("A", "B", "C"));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", expectedList);
        expected.put("list0", expectedList.get(0));
        expected.put("listSize", expectedList.size());
        AtomicReference<String> smth = new AtomicReference<>("");
        expectedList.forEach(s -> smth.updateAndGet(v -> v + s));
        expected.put("smthForeach", smth.get());
        expected.put("smthForLoop", smth.get());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    /**
     * add/push
     * delete
     */
    @Test
    public void listAddPushDelete_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["A", "B", "C", "B", "C", "hello", 34567]}
                """;
        decoderStr = """
                var list = msg.list;
                var msgRez = {};
                // add/push
                msgRez.put("list", list.clone());
                var listAdd = ["thigsboard", 4, 67];
                var addAll = list.addAll(listAdd);
                msgRez.put("addAll", addAll);
                msgRez.put("addAllList", list.clone());
                var addAllIndex = list.addAll(2, listAdd);
                msgRez.put("addAllIndex", addAllIndex);
                msgRez.put("addAllIndexList", list.clone());
                var addIndex = list.add(3, "thigsboard");
                if(addIndex != null) {
                  msgRez.put("addIndex", addIndex);
                };
                msgRez.put("addIndexList", list.clone());
                var push = list.push("thigsboard");
                msgRez.put("push", push);
                if(push != null) {
                  msgRez.put("push", push);
                }
                msgRez.put("pushList", list.clone());
                var unshift = list.unshift("r");
                msgRez.put("unshift", unshift);
                msgRez.put("unshiftList", list.clone());
                var unshiftQ = ["Q", 4];
                msgRez.put("unshift", list.unshift(unshiftQ));
                msgRez.put("unshiftQunList", list.clone());
                // delete
                msgRez.put("removeIndex2", list.remove(2));
                msgRez.put("removeIndex2List", list.clone());
                msgRez.put("removeValueC", list.remove("C"));
                msgRez.put("removeValueCList", list.clone());
                msgRez.put("shift", list.shift());
                msgRez.put("shiftList", list.clone());
                msgRez.put("pop", list.pop());
                msgRez.put("popList", list.clone());
                msgRez.put("splice3", list.splice(3));
                msgRez.put("splice3List", list.clone());
                msgRez.put("splice2", list.splice(2, 2));
                msgRez.put("splice2List", list.clone());
                msgRez.put("splice1_4", list.splice(1, 4, "start", 5, "end"));
                msgRez.put("splice1_4List", list.clone());
                return msgRez;
                """;
        ArrayList list = new ArrayList<>(List.of("A", "B", "C", "B", "C", "hello", 34567));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        // add/push
        expected.put("list", list.clone());
        List listAdd = new ArrayList<>(List.of("thigsboard", 4, 67));
        expected.put("addAll", list.addAll(listAdd));
        expected.put("addAllList", list.clone());
        expected.put("addAllIndex", list.addAll(2, listAdd));
        expected.put("addAllIndexList", list.clone());
        list.add(3, "thigsboard");
        expected.put("addIndexList", list.clone());
        expected.put("push", list.add("thigsboard"));
        expected.put("pushList", list.clone());
        list.add(0, "r");
        expected.put("unshiftList", list.clone());
        ArrayList unshiftQ = new ArrayList<>(List.of("Q", 4));
        list.add(0, unshiftQ);
        expected.put("unshiftQunList", list.clone());
        // delete
        expected.put("removeIndex2", list.remove(2));
        expected.put("removeIndex2List", list.clone());
        expected.put("removeValueC", list.remove("C"));
        expected.put("removeValueCList", list.clone());
        expected.put("shift", list.remove(0));
        expected.put("shiftList", list.clone());
        expected.put("pop", list.remove(list.size() - 1));
        expected.put("popList", list.clone());
        expected.put("splice3", splice(list, 3));
        expected.put("splice3List", list.clone());
        expected.put("splice2", splice(list, 2, 2));
        expected.put("splice2List", list.clone());
        expected.put("splice1_4", splice(list, 1, 4, "start", 5, "end"));
        expected.put("splice1_4List", list.clone());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void listChange_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["r", "start", 5, "end"]}
                """;
        decoderStr = """
                var list = msg.list;
                var msgRez = {};
                msgRez.put("list", list.clone());
                msgRez.put("set", list.set(3, "65"));
                msgRez.put("setList", list.clone());
                list[1] = "98";
                msgRez.put("listChangeIndex1List", list.clone());
                list[0] = 2096;
                msgRez.put("listChangeIndex0List", list.clone());
                return msgRez;
                """;
        ArrayList list = new ArrayList<>(List.of("r", "start", 5, "end"));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", list.clone());
        expected.put("set", list.set(3, "65"));
        expected.put("setList", list.clone());
        list.set(1, "98");
        expected.put("listChangeIndex1List", list.clone());
        list.set(0, 2096);
        expected.put("listChangeIndex0List", list.clone());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void listSort_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": [2096, "98", 5, "65"]}
                """;
        decoderStr = """
                var list = msg.list;
                var msgRez = {};
                msgRez.put("list", list.clone());
                msgRez.put("sort", list.sort());
                msgRez.put("sortList", list.clone());
                msgRez.put("sortTrue", list.sort(true));
                msgRez.put("sortTrueList", list.clone());
                msgRez.put("sortFalse", list.sort(false));
                msgRez.put("sortFalseList", list.clone());
                msgRez.put("reverse", list.reverse());
                msgRez.put("reverseList", list.clone());
                msgRez.put("fill", list.fill(67));
                msgRez.put("fillList", list.clone());
                msgRez.put("fill4", list.fill(4, 1));
                msgRez.put("fill4List", list.clone());
                msgRez.put("fill4_6", list.fill(2, 1, 4));
                msgRez.put("fill4_6List", list.clone());
                return msgRez;
                """;
        ArrayList list = new ArrayList<>(List.of(2096, "98", 5, "65"));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", list.clone());
        list.sort(numericCompAsc);
        expected.put("sortList", list.clone());
        expected.put("sortTrueList", list.clone());
        list.sort(numericCompDesc);
        expected.put("sortFalseList", list.clone());
        Collections.reverse(list);
        expected.put("reverseList", list.clone());
        expected.put("fill", fill(list, 67));
        expected.put("fillList", list.clone());
        expected.put("fill4", fill(list, 4, 1));
        expected.put("fill4List", list.clone());
        expected.put("fill4_6", fill(list, 2, 1, 4));
        expected.put("fill4_6List", list.clone());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void listNewListOrStringSort_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": [67, 2, 2, 2]}
                """;
        decoderStr = """
                var list = msg.list;
                var listAdd = ["thigsboard", 4, 67];
                var msgRez = {};
                msgRez.put("list", list.clone());
                msgRez.put("toSorted", list.toSorted());
                msgRez.put("toSortedList", list.clone());
                msgRez.put("toSortedTrue", list.toSorted(true));
                msgRez.put("toSortedTrueList", list.clone());
                msgRez.put("toSortedFalse", list.toSorted(false));
                msgRez.put("toSortedFalseList", list.clone());
                msgRez.put("toReversed", list.toReversed());
                msgRez.put("toReversedList", list.clone());
                msgRez.put("slice", list.slice());
                msgRez.put("sliceList", list.clone());
                msgRez.put("slice4", list.slice(3));
                msgRez.put("slice4List", list.clone());
                msgRez.put("slice1_5", list.slice(0,2));
                msgRez.put("slice1_5List", list.clone());
                msgRez.put("with1", list.with(1, 69));
                msgRez.put("with1List", list.clone());
                msgRez.put("concat", list.concat(listAdd));
                msgRez.put("concatList", list.clone());
                msgRez.put("join", list.join());
                msgRez.put("joinList", list.clone());
                msgRez.put("toSpliced2", list.toSpliced(1, 0, "Feb"));
                msgRez.put("toSpliced2List", list.clone());
                msgRez.put("toSpliced0_2", list.toSpliced(0, 2));
                msgRez.put("toSpliced0_2List", list.clone());
                msgRez.put("toSpliced2_2", list.toSpliced(2, 2));
                msgRez.put("toSpliced2_2List", list.clone());
                msgRez.put("toSpliced4_5", list.toSpliced(2, 4, "start", 5, "end"));
                msgRez.put("toSpliced4_5List", list.clone());
                return msgRez;
                """;
        ArrayList list = new ArrayList<>(List.of(67, 2, 2, 2));
        ArrayList oldList = (ArrayList) list.clone();
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", oldList);
        list.sort(numericCompAsc);
        expected.put("toSorted", list);
        expected.put("toSortedList", oldList);
        expected.put("toSortedTrue", list);
        expected.put("toSortedTrueList", oldList);
        list = (ArrayList) oldList.clone();
        list.sort(numericCompDesc);
        expected.put("toSortedFalse", list);
        expected.put("toSortedFalseList", oldList);
        list = (ArrayList) oldList.clone();
        Collections.reverse(list);
        expected.put("toReversed", list);
        expected.put("toReversedList", oldList);
        list = (ArrayList) oldList.clone();
        expected.put("slice", list);
        expected.put("sliceList", oldList);
        expected.put("slice4", list.subList(3, 4));
        expected.put("slice4List", oldList);
        expected.put("slice1_5", list.subList(0, 2));
        expected.put("slice1_5List", oldList);
        list = (ArrayList) oldList.clone();
        list.set(1, 69);
        list.add(2);
        expected.put("with1", list);
        expected.put("with1List", oldList);
        list = (ArrayList) oldList.clone();
        List listAdd = new ArrayList<>(List.of("thigsboard", 4, 67));
        list.addAll(listAdd);
        expected.put("concat", list);
        expected.put("concatList", oldList);
        list = (ArrayList) oldList.clone();
        String join = list.toString().substring(1, list.toString().length() - 1).replaceAll(" ", "");
        expected.put("join", join);
        expected.put("joinList", oldList);
        list.set(1, "Feb");
        list.add(2);
        expected.put("toSpliced2", list);
        expected.put("toSpliced2List", oldList);
        list = (ArrayList) oldList.clone();
        list.remove(0);
        list.remove(0);
        expected.put("toSpliced0_2", list);
        expected.put("toSpliced0_2List", oldList);
        list = (ArrayList) oldList.clone();
        list.remove(2);
        list.remove(2);
        expected.put("toSpliced2_2", list);
        expected.put("toSpliced2_2List", oldList);
        list = (ArrayList) oldList.clone();
        list.remove(2);
        list.remove(2);
        list.add("start");
        list.add(5);
        list.add("end");
        expected.put("toSpliced4_5", list);
        expected.put("toSpliced4_5List", oldList);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void listGetInfo_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": [67, 2, 2, 2]}
                """;
        decoderStr = """
                var list = msg.list;
                return {
                        list: list.clone(),
                        length: list.length(),
                        memorySize: list.memorySize(),
                        indOf1: list.indexOf("B", 1),
                        indOf2: list.indexOf(2, 2),
                        sStr: list.validateClazzInArrayIsOnlyNumber()
                       }
                """;
        ArrayList list = new ArrayList<>(List.of(67, 2, 2, 2));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", list);
        expected.put("length", list.size());
        expected.put("memorySize", 32L);
        expected.put("indOf1", -1);
        expected.put("indOf2", 2);
        expected.put("sStr", true);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // Arrays
    @Test
    public void arrays_Test() throws ExecutionException, InterruptedException {
        String str = "My String";
        msgStr = String.format("""
                {"str": "%s"}
                """, str);
        decoderStr = """
                // Create new array
                var array = new int[3];
                array[0] = 1;
                array[1] = 2;
                array[2] = 3;
                var str = msg.str;
                var str0 = str[0];
                function sum(list){
                    var result = 0;
                    for(var i = 0; i < list.length; i++){
                        result += list[i];
                    }
                    return result;
                };
                var sum = sum(array);
                return {
                        array: array,
                        str: str,
                        str0: str0,
                        sum: sum
                       }
                """;
        ArrayList list = new ArrayList<>(List.of(1, 2, 3));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("array", list);
        expected.put("str", str);
        expected.put("str0", str.toCharArray()[0]);
        expected.put("sum", 6);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }


    // Sets
    @Test
    public void setsCreateNewSetFromMap_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["B", "A", "C", "A"]}
                """;
        decoderStr = """
                var originalMap = {};
                var set1 = originalMap.entrySet();         // create new Set from map, Empty
                var set2 = set1.clone();                   // clone new Set, Empty
                var result1 = set1.addAll(msg.list);       // addAll list, no sort, size = 3 ("A" - duplicate)
                return {set1: set1,
                        set2: set2,
                        result1: result1
                       }
                """;
        Set expectedSet1 = new LinkedHashSet(List.of("B", "A", "C", "A"));
        Set expectedSet2 = new LinkedHashSet();
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("set1", expectedSet1);
        expected.put("set2", expectedSet2);
        expected.put("result1", true);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected.toString(), actual.toString());
    }

     @Test
    public void setsCreateNewSetFromCreateSetTbMethod_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["B", "A", "C", "A"]}
                """;
        decoderStr = """
                var set1 = toSet(msg.list);       // create new Set from toSet() with list, no sort, size = 3 ("A" - duplicate)
                var set2 = newSet();               // create new Set from newSet(), Empty
                return {set1: set1,
                        set2: set2
                       }
                """;
        Set expectedSet1 = new LinkedHashSet(List.of("B", "A", "C", "A"));
        Set expectedSet2 = new LinkedHashSet();
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("set1", expectedSet1);
        expected.put("set2", expectedSet2);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected.toString(), actual.toString());
    }

     @Test
    public void setsForeachForLoop_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["A", "B", "C"]}
                """;
        decoderStr = """
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
                return {
                        set2: set2,
                        set2_0: set2_0,
                        set2Size: set2Size,
                        smthForeach: smthForeach,
                        smthForLoop: smthForLoop
                       }
                """;
        Set expectedSet2 = new LinkedHashSet(List.of("A", "B", "C"));
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("set2", expectedSet2);
        expected.put("set2_0", expectedSet2.toArray()[0]);
        expected.put("set2Size", expectedSet2.size());
        AtomicReference<String> smth = new AtomicReference<>("");
        expectedSet2.forEach(s -> smth.updateAndGet(v -> v + s));
        expected.put("smthForeach", smth.get());
        expected.put("smthForLoop", smth.get());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected.toString(), actual.toString());
    }

    /**
     * add
     * delete/remove
     * setCreate, setCreatList
     */
    @Test
    public void setsAddRemove_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["B", "C", "A", "B", "C", "hello", 34567]}
                """;
        decoderStr = """
                // add
                var setAdd = toSet(["thigsboard", 4, 67]);      // create new, size = 3
                var setAdd1_value = setAdd.clone();                   // clone setAdd, size = 3
                var setAdd2_result = setAdd.add(35);                  // add value = 35, result = true
                var setAdd2_value = setAdd.clone();                   // clone setAdd (fixing the result add = 35), size = 4
                var setAddList1 = toSet(msg.list);              // create new from list without duplicate value ("B" and "C" - only one), size = 5
                var setAdd3_result = setAdd.addAll(setAddList1);      // add all without duplicate values, result = true
                var setAdd3_value = setAdd.clone();                   // clone setAdd (with addAll), size = 9  
                var setAdd4_result = setAdd.add(35);                  // add duplicate value = 35,  result = false  
                var setAdd4_value = setAdd.clone();                   // clone setAdd (after add duplicate value = 35), size = 9  
                var setAddList2 = toSet(msg.list);              // create new from list without duplicate value ("B" and "C" - only one), start: size = 5, finish: size = 7       
                var setAdd5_result1 = setAddList2.add(72);            // add is not duplicate value = 72,  result = true   
                var setAdd5_result2 = setAddList2.add(72);            // add duplicate value = 72,  result = false   
                var setAdd5_result3 = setAddList2.add("hello25");     // add  is not duplicate value = "hello25",  result = true    
                var setAdd5_value = setAddList2.clone();              // clone setAddList2, size = 7 
                var setAdd6_result = setAdd.addAll(setAddList2);      // add all with duplicate values, result = true  
                var setAdd6_value = setAdd.clone();                   // clone setAdd (after addAll setAddList2), before size = 9, after size = 11, added only is not duplicate values {"hello25", 72}  
                
                // remove
                var setAdd7_value = setAdd6_value.clone();            // clone setAdd6_value, before size = 11, after remove value = 4 size = 10
                var setAdd7_result = setAdd7_value.remove(4);         // remove value = 4, result = true   
                var setAdd8_value = setAdd7_value.clone();            // clone setAdd7_value, before size = 10, after clear size = 0
                setAdd8_value.clear();                                // setAdd8_value clear, result size = 0  
                return {
                   "setAdd1_value": setAdd1_value, 
                   "setAdd2_result": setAdd2_result, 
                   "setAdd2_value": setAdd2_value, 
                   "setAddList1": setAddList1, 
                   "setAdd3_result": setAdd3_result, 
                   "setAdd3_value": setAdd3_value, 
                   "setAdd4_result": setAdd4_result, 
                   "setAdd4_value": setAdd4_value,                    
                   "setAdd5_result1": setAdd5_result1, 
                   "setAdd5_result2": setAdd5_result2,
                   "setAdd5_result3": setAdd5_result3,
                   "setAddList2": setAddList2,  
                   "setAdd5_value": setAdd5_value, 
                   "setAdd6_result": setAdd6_result, 
                   "setAdd6_value": setAdd6_value, 
                   "setAdd7_result": setAdd7_result, 
                   "setAdd7_value": setAdd7_value, 
                   "setAdd8_value": setAdd8_value 
                };
                """;
        ArrayList<Object> list = new ArrayList<>(List.of("B", "C", "A", "B", "C", "hello", 34567));
        ArrayList<Object> listAdd = new ArrayList<>(List.of("thigsboard", 4, 67));
        Set<Object> setAdd = new LinkedHashSet<>(listAdd);
        Set setAdd1_value = new LinkedHashSet<>(setAdd);
        boolean setAdd2_result = setAdd.add(35);
        Set<Object> setAdd2_value = new LinkedHashSet<>(setAdd);
        Set<Object> setAddList1 = new LinkedHashSet<>(list);
        boolean setAdd3_result = setAdd.addAll(setAddList1);
        Set<Object> setAdd3_value = new LinkedHashSet<>(setAdd);
        boolean setAdd4_result = setAdd.add(35);
        Set<Object> setAdd4_value = new LinkedHashSet<>(setAdd);
        Set<Object> setAddList2 = new LinkedHashSet<>(list);
        boolean setAdd5_result1 = setAddList2.add(72);
        boolean setAdd5_result2 = setAddList2.add(72);
        boolean setAdd5_result3 = setAddList2.add("hello25");
        Set<Object> setAdd5_value = new LinkedHashSet<>(setAddList2);
        boolean setAdd6_result = setAdd.addAll(setAddList2);
        Set<Object> setAdd6_value = new LinkedHashSet<>(setAdd);
        // remove
        Set<Object> setAdd7_value = new LinkedHashSet<>(setAdd6_value);
        boolean setAdd7_result = setAdd7_value.remove(4);
        Set<Object> setAdd8_value = new LinkedHashSet<>(setAdd7_value);
        setAdd8_value.clear();

        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("setAdd1_value", setAdd1_value);
        expected.put("setAdd2_result", setAdd2_result);
        expected.put("setAdd2_value", setAdd2_value);
        expected.put("setAddList1", setAddList1);
        expected.put("setAdd3_result", setAdd3_result);
        expected.put("setAdd3_value", setAdd3_value);
        expected.put("setAdd4_result", setAdd4_result);
        expected.put("setAdd4_value", setAdd4_value);
        expected.put("setAdd5_result1", setAdd5_result1);
        expected.put("setAdd5_result2", setAdd5_result2);
        expected.put("setAdd5_result3", setAdd5_result3);
        expected.put("setAddList2", setAddList2);
        expected.put("setAdd5_value", setAdd5_value);
        expected.put("setAdd6_result", setAdd6_result);
        expected.put("setAdd6_value", setAdd6_value);
        expected.put("setAdd7_result", setAdd7_result);
        expected.put("setAdd7_value", setAdd7_value);
        expected.put("setAdd8_value", setAdd8_value);

        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void setsSort_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["C", "B", "A", 34567, "B", "C", "hello", 34]}
                """;
        decoderStr = """
                var set1 = toSet(msg.list);               // create new from method toSet(List list) no sort, size = 6 ("A" and "C" is duplicated)
                var set2 = toSet(msg.list);               // create new from method toSet(List list) no sort, size = 6 ("A" and "C" is duplicated)
                var set1_asc = set1.clone();                    // clone set1, size = 6
                var set1_desc = set1.clone();                   // clone set1, size = 6
                set1.sort();                                    // sort set1 -> asc
                set1_asc.sort(true);                            // sort set1_asc -> asc
                set1_desc.sort(false);                          // sort set1_desc -> desc
                var set3 = set2.toSorted();                     // toSorted set3 -> asc
                var set3_asc = set2.toSorted(true);             // toSorted set3 -> asc
                var set3_desc = set2.toSorted(false);           // toSorted set3 -> desc
                return {
                   "set1": set1,
                   "set1_asc": set1_asc,
                   "set1_desc": set1_desc,
                   "set2": set2,
                   "set3": set3,
                   "set3_asc": set3_asc,
                   "set3_desc": set3_desc,
                }
                """;
        ArrayList<Object> list = new ArrayList<>(List.of("C", "B", "A", 34567, "hello", 34));
        Set<Object> expected = new LinkedHashSet<>(list);
        ArrayList<Object> listSortAsc = new ArrayList<>(List.of(34, 34567, "A", "B", "C", "hello"));
        Set<Object> expectedAsc = new LinkedHashSet<>(listSortAsc);
        ArrayList<Object> listSortDesc = new ArrayList<>(List.of("hello", "C", "B", "A", 34567, 34));
        Set<Object> expectedDesc = new LinkedHashSet<>(listSortDesc);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expectedAsc.toString(), ((LinkedHashMap<?, ?>)actual).get("set1").toString());
        assertEquals(expectedAsc.toString(), ((LinkedHashMap<?, ?>)actual).get("set1_asc").toString());
        assertEquals(expectedDesc.toString(), ((LinkedHashMap<?, ?>)actual).get("set1_desc").toString());
        assertEquals(expected.toString(), ((LinkedHashMap<?, ?>)actual).get("set2").toString());
        assertEquals(expectedAsc.toString(), ((LinkedHashMap<?, ?>)actual).get("set3").toString());
        assertEquals(expectedAsc.toString(), ((LinkedHashMap<?, ?>)actual).get("set3_asc").toString());
        assertEquals(expectedDesc.toString(), ((LinkedHashMap<?, ?>)actual).get("set3_desc").toString());
    }

    @Test
    public void setsContains_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["C", "B", "A", 34567, "B", "C", "hello", 34]}
                """;
        decoderStr = """
                var set1 = toSet(msg.list);               // create new from method toSet(List list) no sort, size = 6  ("A" and "C" is duplicated)
                var result1 = set1.contains("A");               // return true
                var result2 = set1.contains("H");               // return false
                return {
                   "set1": set1,
                   "result1": result1,
                   "result2": result2
                }
                """;
        List<Object> listOrigin = new ArrayList<>(List.of("C", "B", "A", 34567, "B", "C", "hello", 34));
        Set<Object> expectedSet = new LinkedHashSet<>(listOrigin);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expectedSet.toString(), ((LinkedHashMap<?, ?>)actual).get("set1").toString());
        assertEquals(true, ((LinkedHashMap<?, ?>)actual).get("result1"));
        assertEquals(false, ((LinkedHashMap<?, ?>)actual).get("result2"));
    }

    @Test
    public void setsToList_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["C", "B", "A", 34567, "B", "C", "hello", 34]}
                """;
        decoderStr = """
                var set1 = toSet(msg.list);               // create new from method toSet(List list) no sort, size = 6  ("A" and "C" is duplicated)
                var tolist = set1.toList();                     // create new List from Set, size = 6
                return {
                   "list": msg.list,
                   "set1": set1,
                   "tolist": tolist
                }
                """;
        List<Object> listOrigin = new ArrayList<>(List.of("C", "B", "A", 34567, "B", "C", "hello", 34));
        Set<Object> expectedSet = new LinkedHashSet<>(listOrigin);
        List<Object> expectedToList = new ArrayList<>(expectedSet);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(listOrigin.toString(), ((LinkedHashMap<?, ?>)actual).get("list").toString());
        assertEquals(expectedSet.toString(), ((LinkedHashMap<?, ?>)actual).get("set1").toString());
        assertEquals(expectedToList.toString(), ((LinkedHashMap<?, ?>)actual).get("tolist").toString());
    }

    @Test
    public void arraysWillCauseArrayIndexOutOfBoundsException_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {}
                """;
        decoderStr = """
                var array = new int[3];
                array[0] = 1;
                array[1] = 2;
                array[2] = 3;
                array[3] = 4;
                return array;
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Invalid statement: 4")
                .hasMessageContaining("[Line: 5, Column: 12]");
    }

    // Using Java Classes
    @Test
    public void sqrt_Test() throws ExecutionException, InterruptedException {
        int i = 4;
        msgStr = String.format("""
                {"i": %d}
                """, i);
        decoderStr = """
                var foo = Math.sqrt(msg.i);
                return{
                       i: msg.i,
                       foo: foo
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("i", i);
        expected.put("foo", Math.sqrt(i));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // Convert Number to Hexadecimal/Octal/Binary String
    @Test
    public void byteIntegerToString_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var b16 = Integer.toString(0x1A, 16);
                    var b10 = Integer.toString(0x1A, 10);
                    var b8 = Integer.toString(0x1A, 8);
                    var b2 = Integer.toString(0x1A, 2);
                    return{
                       b16: b16,
                       b10: b10,
                       b8: b8,
                       b2: b2
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("b16", "1a");
        expected.put("b10", "26");
        expected.put("b8", "32");
        expected.put("b2", "11010");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void integerIntegerToString_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var i16 = Integer.toString(-255, 16);
                    var i10 = Integer.toString(-255, 10);
                    var i8 = Integer.toString(-255, 8);
                    var i2 = Integer.toString(-255, 2);
                    return{
                       i16: i16,
                       i10: i10,
                       i8: i8,
                       i2: i2
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("i16", "-ff");
        expected.put("i10", "-255");
        expected.put("i8", "-377");
        expected.put("i2", "-11111111");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void floatToString_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var f0 =7823764.8374;
                    var f16 = Float.toHexString(f0);
                    var f10 = f0.toString();
                    return{
                       f16: f16,
                       f10: f10
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("f16", "0x1.dd8654p22");
        expected.put("f10", "7823764.8374");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void longToString_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                        var l16 = Long.toString(9223372036854775807, 16);
                        var l10 = Long.toString(9223372036854775807, 10);
                        var l8 = Long.toString(9223372036854775807, 8);
                        var l2 = Long.toString(9223372036854775807, 2);
                    return{
                       l16: l16,
                       l10: l10,
                       l8: l8,
                       l2: l2
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("l16", "7fffffffffffffff");
        expected.put("l10", "9223372036854775807");
        expected.put("l8", "777777777777777777777");
        expected.put("l2", "111111111111111111111111111111111111111111111111111111111111111");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void doubleToString_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var dd0 = 99993219.156013e-002;
                    var dd16 = Double.toHexString(dd0);
                    var dd10 = String.format("%.8f",dd0);
                    return{
                       dd16: dd16,
                       dd10: dd10
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        Double dd0 = 99993219.156013e-002d;
        expected.put("dd16", "0x1.e83f862142b5bp19");
        expected.put("dd10", String.format("%.8f", dd0));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected.toString(), actual.toString());
    }

    // Flow Control
    @Test
    public void ifTthenElseBlocks_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"temperature": 20}
                """;
        decoderStr = """
                if (msg.temperature > 0) {
                   return "Greater than zero!";
                } else if (msg.temperature == -1) {
                   return "Minus one!";
                } else {
                   return "Something else!";
                }
                """;
        String expected = "Greater than zero!";
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void ternarySstatements_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"temperature": 20}
                """;
        decoderStr = """
                return msg.temperature > 0 ? "Yes" : "No";
                """;
        String expected = "Yes";
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void foreach_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var numbers = [1, 2, 3];
                    var sum = 0;
                    foreach (n : numbers) {
                       sum+=n;
                    }
                    return sum;
                """;
        Integer expected = 6;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void foreachStringsIiterable_Test() throws ExecutionException, InterruptedException {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        msgStr = String.format("""
                {"str": "%s"}
                """, str);
        decoderStr = """
                    int l = msg.str.length();
                    var array = new int[l];
                    var i = 0;
                    foreach (c : msg.str) {
                       array[i] = (int)c;
                       i++;
                    }
                    return array;
                """;
        List expected = new ArrayList<>(List.of(str.chars().boxed().toArray(Integer[]::new)));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void forLoop_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var sum = 0;
                    for (var i =0; i < 100; i++) {\s
                       sum += i;
                    }
                    return sum;
                """;
        Integer expected = 4950;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void doWhile_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var x = "doWhile";
                    var a = null;
                    do {
                       x = a;
                    }
                    while (x != null);
                    return x;
                """;
        String expected = null;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void doUntil_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var x = "doWhile";
                    var a = null;
                    do {
                       x = a;
                    }
                    until (x == null);
                    return x;
                """;
        String expected = null;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void standardWhile_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var x = "doWhile";
                    var a = null;
                    while (x != null) {
                       x = a;
                    }
                    return x;
                """;
        String expected = null;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void standardUntil_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var x = "doWhile";
                    var a = null;
                    until (x == null) {
                       x = a;
                    }
                    return x;
                """;
        String expected = null;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // Helper functions
    @Test
    public void atobBtoa_Test() throws ExecutionException, InterruptedException {
        String str = "Hello, world";
        msgStr = String.format("""
                {"str": "%s"}
                """, str);
        decoderStr = """
                    var encodedData = btoa(msg.str);
                    var decodedData = atob(encodedData);
                    return{
                       "btoa": encodedData,
                       "atob": decodedData
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("btoa", Base64.getEncoder().encodeToString(str.getBytes()));
        expected.put("atob", str);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void toFixed_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    return{
                       toFixed1: toFixed(0.345, 1),
                       toFixed2: toFixed(0.345, 2)
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("toFixed1", 0.3);
        expected.put("toFixed2", 0.35);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void toInt_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    return{
                       toInt1: toInt(0.3),
                       toInt2: toInt(0.5),
                       toInt3: toInt(2.7)
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("toInt1", 0);
        expected.put("toInt2", 1);
        expected.put("toInt3", 3);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void stringToBytesBinaryString_Test() throws ExecutionException, InterruptedException {
        String base64Str = "eyJoZWxsbyI6ICJ3b3JsZCJ9";
        String inputStr = "hello, world";
        msgStr = String.format("""
                {
                 "base64Str": "%s",
                 "inputStr": "%s"
                }
                """, base64Str, inputStr);
        decoderStr = """
                    var bytesStr = atob(msg.base64Str);
                    var charsetStr = "UTF8";
                    return{
                       bytesStr: stringToBytes(bytesStr),
                       inputStr: stringToBytes(msg.inputStr),
                       inputStrUTF8: stringToBytes(msg.inputStr, charsetStr)
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("bytesStr", bytesToList(Base64.getDecoder().decode(base64Str)));
        expected.put("inputStr", bytesToList(inputStr.getBytes()));
        expected.put("inputStrUTF8", bytesToList(inputStr.getBytes(StandardCharsets.UTF_8)));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void stringToBytesObjectFromJsonAsString_Test() throws ExecutionException, InterruptedException {
        String inputStr = "hello, world";
        msgStr = "{}";
        decoderStr = String.format("""
                    var dataMap = {};
                    dataMap.inputStr = "%s";
                    var dataJsonStr = JSON.stringify(dataMap);
                    var dataJson = JSON.parse(dataJsonStr);
                    return stringToBytes(dataJson.inputStr);
                """, inputStr);
        List expected = bytesToList(inputStr.getBytes());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void bytesToString_Test() throws ExecutionException, InterruptedException {
        byte[] inputBytes = new byte[]{0x48, 0x45, 0x4C, 0x4C, 0x4F};
        msgStr = "{}";
        decoderStr = """
                var bytes = [0x48,0x45,0x4C,0x4C,0x4F];
                return bytesToString(bytes);
                """;
        String expected = new String(inputBytes);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void decodeToJson_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var base64Str = "eyJoZWxsbyI6ICJ3b3JsZCJ9";
                    var bytesStr = atob(base64Str);
                    var bytes = stringToBytes(bytesStr);
                    return decodeToJson(bytes);
                """;
        String expected = "{\"hello\":\"world\"}";
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, JacksonUtil.toString(actual));
    }

    @Test
    public void isTypeInValue_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    return{
                       "binary": isBinary("1100110"),
                       "notBinary": isBinary("2100110"),
                       "octal": isOctal("4567734"),
                       "notOctal": isOctal("8100110"),
                       "decimal": isDecimal("4567039"),
                       "notDecimal": isDecimal("C100110"),
                       "hexadecimal": isHexadecimal("F5D7039"),
                       "notHexadecimal": isHexadecimal("K100110"),
                       "nan": isNaN(0.0 / 0.0),
                       "number": isNaN(1.0)
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("binary", 2);
        expected.put("notBinary", -1);
        expected.put("octal", 8);
        expected.put("notOctal", -1);
        expected.put("decimal", 10);
        expected.put("notDecimal", -1);
        expected.put("hexadecimal", 16);
        expected.put("notHexadecimal", -1);
        expected.put("nan", true);
        expected.put("number", false);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void encodeDecodeUri_Test() throws ExecutionException, InterruptedException {
        String uriOriginal = "-_.!~*'();/?:@&=+$,#ht://example.ж д a/path with spaces/?param1=Київ 1&param2=Україна2";
        String uriEncode = "-_.!~*'();/?:@&=+$,#ht://example.%D0%B6%20%D0%B4%20a/path%20with%20spaces/?param1=%D0%9A%D0%B8%D1%97%D0%B2%201&param2=%D0%A3%D0%BA%D1%80%D0%B0%D1%97%D0%BD%D0%B02";
        msgStr = "{}";
        decoderStr = String.format("""
                    var uriOriginal = "%s";
                    var uriEncode = "%s";
                    return{
                       "encodeURI": encodeURI(uriOriginal),
                       "decodeURI": decodeURI(uriEncode)
                      }
                """, uriOriginal, uriEncode);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("encodeURI", uriEncode);
        expected.put("decodeURI", uriOriginal);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void raiseError_Test() throws ExecutionException, InterruptedException {
        String message = "frequency_weighting_type must be 0, 1 or 2.";
        msgStr = "{}";
        decoderStr = String.format("""
                    var message = "%s";
                    return raiseError(message);
                """, message);
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining(message);

        Integer value = 4;
        message = "frequency_weighting_type must be 0, 1 or 2. A value of " + value + " is invalid.";
        decoderStr = String.format("""
                    var message = "%s";
                    return raiseError(message);
                """, message);
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining(message);
    }

    @Test
    public void printUnsignedBytes_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var hexStrBe = "D8FF";
                    var listBe = hexToBytes(hexStrBe);
                    return printUnsignedBytes(listBe);
                """;
        ArrayList expected = new ArrayList<>(List.of(216, 255));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
        decoderStr = """
                    var hexStrLe = "FFD8";
                    var listLe = hexToBytes(hexStrLe);
                    return printUnsignedBytes(listLe);
                """;
        expected = new ArrayList<>(List.of(255, 216));
        actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void pad_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        // padStart
        decoderStr = """
                    var str1 = "010011";
                    var str2 ="1001010011";
                    var fullNumber = "203439900FFCD5581";
                    var last4Digits = fullNumber.substring(11);
                    return {
                        padStart1: padStart(str1, 8, '0'),
                        padStart2: padStart(str2, 8, '0'),
                        padStart16: padStart(str2, 16, '*'),
                        padStartFullNumber: padStart(last4Digits, fullNumber.length(), '*')
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("padStart1", "00010011");
        expected.put("padStart2", "1001010011");
        expected.put("padStart16", "******1001010011");
        expected.put("padStartFullNumber", "***********CD5581");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
        // padEnd
        decoderStr = """
                    var str1 = "010011";
                    var str2 ="1001010011";
                    var fullNumber = "203439900FFCD5581";
                    var last4Digits = fullNumber.substring(0, 11);
                    return {
                        padEnd1: padEnd(str1, 8, '0'),
                        padEnd2: padEnd(str2, 8, '0'),
                        padEnd16: padEnd(str2, 16, '*'),
                        padEndFullNumber: padEnd(last4Digits, fullNumber.length(), '*')
                    }
                """;
        expected = new LinkedHashMap<>();
        expected.put("padEnd1", "01001100");
        expected.put("padEnd2", "1001010011");
        expected.put("padEnd16", "1001010011******");
        expected.put("padEndFullNumber", "203439900FF******");
        actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void numberToRadixStringIntLongFloatDouble_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var i = 0x7FFFFFFF;
                    return {
                        "intToHex1": intToHex(i, true, true),
                        "intToHex2": intToHex(171, true, false),
                        "intToHex3": intToHex(0xABCDEF, false, true, 4),
                        "intToHex4": intToHex(0xABCD, false, false, 2),
                        "longToHex1": longToHex(9223372036854775807, true, true),
                        "longToHex2": longToHex(0x7A12BCD3, true, true, 4),
                        "longToHex3": longToHex(0x7A12BCD3, false, false, 4),
                        "floatToHex1": floatToHex(123456789.00),
                        "floatToHex2": floatToHex(123456789.00, false),
                        "doubleToHex1": doubleToHex(1729.1729d),
                        "doubleToHex2": doubleToHex(1729.1729d, false)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("intToHex1", "0x7FFFFFFF");
        expected.put("intToHex2", "AB");
        expected.put("intToHex3", "0xCDAB");
        expected.put("intToHex4", "AB");
        expected.put("longToHex1", "0x7FFFFFFFFFFFFFFF");
        expected.put("longToHex2", "0xBCD3");
        expected.put("longToHex3", "127A");
        expected.put("floatToHex1", "0x4CEB79A3");
        expected.put("floatToHex2", "0xA379EB4C");
        expected.put("doubleToHex1", "0x409B04B10CB295EA");
        expected.put("doubleToHex2", "0xEA95B20CB1049B40");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void intLongToRadixString_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    return {
                        "bin1": intLongToRadixString(58, 2),
                        "bin2": intLongToRadixString(9223372036854775807, 2),
                        "octal1": intLongToRadixString(13158, 8),
                        "octal2": intLongToRadixString(-13158, 8),
                        "decimal": intLongToRadixString(-13158, 10),
                        "hexDecimal1": intLongToRadixString(13158, 16),
                        "hexDecimal2": intLongToRadixString(-13158, 16),
                        "hexDecimal3": intLongToRadixString(9223372036854775807, 16),
                        "hexDecimal4": intLongToRadixString(-13158, 16, true, true)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("bin1", "00111010");
        expected.put("bin2", "0111111111111111111111111111111111111111111111111111111111111111");
        expected.put("octal1", "31546");
        expected.put("octal2", "1777777777777777746232");
        expected.put("decimal", "-13158");
        expected.put("hexDecimal1", "3366");
        expected.put("hexDecimal2", "FFCC9A");
        expected.put("hexDecimal3", "7FFFFFFFFFFFFFFF");
        expected.put("hexDecimal4", "0xFFCC9A");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void parseHex_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    return {
                        "hexToInt1": parseHexToInt("BBAA"),
                        "hexToInt2": parseHexToInt("BBAA", true),
                        "hexToInt3": parseHexToInt("AABB", false),
                        "hexToInt4": parseHexToInt("BBAA", false),
                        "hexToFloat1": parseHexToFloat("41EA62CC"),
                        "hexToFloat2": parseHexToFloat("41EA62CC", true),
                        "hexToFloat3": parseHexToFloat("41EA62CC", false),
                        "hexToFloat4": parseHexToFloat("CC62EA41", false),
                        "hexIntLongToFloat1": parseHexIntLongToFloat("0x0A", true),
                        "hexIntLongToFloat2": parseHexIntLongToFloat("0x0A", false),
                        "hexIntLongToFloat3": parseHexIntLongToFloat("0x00000A", true),
                        "hexIntLongToFloat4": parseHexIntLongToFloat("0x0A0000", false),
                        "hexIntLongToFloat5": parseHexIntLongToFloat("0x000A0A", true),
                        "hexIntLongToFloat6": parseHexIntLongToFloat("0x0A0A00", false),
                        "hexToDouble1": parseHexToDouble("409B04B10CB295EA"),
                        "hexToDouble2": parseHexToDouble("409B04B10CB295EA", false),
                        "hexToDouble3": parseHexToDouble("409B04B10CB295EA", true),
                        "hexToDouble4": parseHexToDouble("EA95B20CB1049B40", false)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("hexToInt1", 48042);
        expected.put("hexToInt2", 48042);
        expected.put("hexToInt3", 48042);
        expected.put("hexToInt4", 43707);
        expected.put("hexToFloat1", 29.29824f);
        expected.put("hexToFloat2", 29.29824f);
        expected.put("hexToFloat3", -5.948442E7f);
        expected.put("hexToFloat4", 29.29824f);
        expected.put("hexIntLongToFloat1", 10.0f);
        expected.put("hexIntLongToFloat2", 10.0f);
        expected.put("hexIntLongToFloat3", 10.0f);
        expected.put("hexIntLongToFloat4", 10.0f);
        expected.put("hexIntLongToFloat5", 2570.0f);
        expected.put("hexIntLongToFloat6", 2570.0f);
        expected.put("hexToDouble1", 1729.1729);
        expected.put("hexToDouble2", -2.7208640774822924E205);
        expected.put("hexToDouble3", 1729.1729);
        expected.put("hexToDouble4", 1729.1729);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void parseBytes_Test() throws ExecutionException, InterruptedException {
        byte[] bytesExecutionArrayList = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
        msgStr = "{}";
        decoderStr = """
                    var bytes = [0xBB, 0xAA];
                    var list = [-69, 83];
                    var intValByte = [0xAA, 0xBB, 0xCC, 0xDD];
                    var longValByte = [64, -101, 4, -79, 12, -78, -107, -22];
                    var bytesFloat = [0x0A];
                    var floatValByte = [0x41, 0xEA, 0x62, 0xCC];
                    var floatValList = [65, -22, 98, -52];
                    var intValByteFromInt = [0x00, 0x00, 0x00, 0x0A];
                    var dataAT101 = "0x01756403671B01048836BF7701F000090722050000";
                    var byteAT101 = hexToBytes(dataAT101);
                    var offsetLatInt = 9;
                    var coordinatesAsHex = "0x32D009423F23B300B0106E08D96B6C00";
                    var coordinatesasBytes = hexToBytes(coordinatesAsHex);
                    var offsetLatLong = 0;
                    var factor = 1e15;
                    var bytesExecutionArrayList = [0xAA, 0xBB, 0xCC, 0xDD];
                    return {
                        "bytesToHex1": bytesToHex(bytes),
                        "bytesToHex2": bytesToHex(list),
                        "bytesToInt1": parseBytesToInt(intValByte, 0, 3),
                        "bytesToInt2": parseBytesToInt(intValByte, 0, 3, true),
                        "bytesToInt3": parseBytesToInt(intValByte, 0, 3, false),
                        "bytesToLong1": parseBytesToLong(longValByte, 0, 8),
                        "bytesToLong2": parseBytesToLong(longValByte, 0, 8, false),
                        "bytesToFloat1": parseBytesToFloat(bytesFloat),
                        "bytesToFloat2": parseBytesToFloat(floatValByte, 0),
                        "bytesToFloat3": parseBytesToFloat(floatValByte, 0, 2, false),
                        "bytesToFloat4": parseBytesToFloat(floatValByte, 0, 2, true),
                        "bytesToFloat5": parseBytesToFloat(floatValByte, 0, 3, false),
                        "bytesToFloat6": parseBytesToFloat(floatValByte, 0, 3, true),
                        "bytesToFloat7": parseBytesToFloat(floatValByte, 0, 4, false),
                        "bytesToFloat8": parseBytesToFloat(floatValList, 0),
                        "bytesToFloat9": parseBytesToFloat(floatValList, 0, 4, false),
                        "bytesIntToFloat1": parseBytesIntToFloat(intValByteFromInt, 3, 1, true),
                        "bytesIntToFloat2": parseBytesIntToFloat(intValByteFromInt, 3, 1, false),
                        "bytesIntToFloat3": parseBytesIntToFloat(intValByteFromInt, 2, 2, true),
                        "bytesIntToFloat4": parseBytesIntToFloat(intValByteFromInt, 2, 2, false),
                        "bytesIntToFloat5": parseBytesIntToFloat(intValByteFromInt, 0, 4, true),
                        "bytesIntToFloat6": parseBytesIntToFloat(intValByteFromInt, 0, 4, false),
                        "bytesIntToFloat7": parseBytesIntToFloat(byteAT101, offsetLatInt, 4, false) / 1000000,
                        "bytesIntToFloat8": parseBytesIntToFloat(byteAT101, offsetLatInt + 4, 4, false) / 1000000,
                        "bytesLongToDouble1": parseBytesLongToDouble(coordinatesasBytes, offsetLatLong, 8, false) / factor,
                        "bytesLongToDouble2": parseBytesLongToDouble(coordinatesasBytes, offsetLatLong + 8, 8, false) / factor,
                        "bytesLongToExecutionArrayList": bytesToExecutionArrayList(bytesExecutionArrayList)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("bytesToHex1", "BBAA");
        expected.put("bytesToHex2", "BB53");
        expected.put("bytesToInt1", 11189196);
        expected.put("bytesToInt2", 11189196);
        expected.put("bytesToInt3", 13417386);
        expected.put("bytesToLong1", 4655319798286292458L);
        expected.put("bytesToLong2", -1543131529725306048L);
        expected.put("bytesToFloat1", 1.4E-44f);
        expected.put("bytesToFloat2", 29.29824f);
        expected.put("bytesToFloat3", 8.4034E-41f);
        expected.put("bytesToFloat4", 2.3646E-41f);
        expected.put("bytesToFloat5", 9.083913E-39f);
        expected.put("bytesToFloat6", 6.053388E-39f);
        expected.put("bytesToFloat7", -5.948442E7f);
        expected.put("bytesToFloat8", 29.29824f);
        expected.put("bytesToFloat9", -5.948442E7f);
        expected.put("bytesIntToFloat1", 10.0f);
        expected.put("bytesIntToFloat2", 10.0f);
        expected.put("bytesIntToFloat3", 10.0f);
        expected.put("bytesIntToFloat4", 2560.0f);
        expected.put("bytesIntToFloat5", 10.0f);
        expected.put("bytesIntToFloat6", 1.6777216E8f);
        expected.put("bytesIntToFloat7", 24.62495f);
        expected.put("bytesIntToFloat8", 118.030576f);
        expected.put("bytesLongToDouble1", 50.422775429058610d);
        expected.put("bytesLongToDouble2", 30.517877378257072d);
        expected.put("bytesLongToExecutionArrayList", bytesToList(bytesExecutionArrayList));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // hexToBytes List or Array
    @Test
    public void hexToBytes_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var validInputList = "0x01752B0367FA000500010488FFFFFFFFFFFFFFFF33";
                    var validInputArray = "AABBCCDDEE";
                    return {
                        "hexToBytes": hexToBytes(validInputList),
                        "hexToBytesArray": hexToBytesArray(validInputArray),
                    }
                """;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("hexToBytes", bytesToList(new byte[]{1, 117, 43, 3, 103, -6, 0, 5, 0, 1, 4, -120, -1, -1, -1, -1, -1, -1, -1, -1, 51}));
        // [-86, -69, -52, -35, -18] == new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE}
        expected.put("hexToBytesArray", bytesToList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE}));
        assertEquals(expected, actual);
    }

    // parseBinaryArray
    @Test
    public void parseBinaryArray_Test() throws ExecutionException, InterruptedException {
        byte[] value = new byte[]{1, 0, 0, 1, 1, 1};
        msgStr = "{}";
        decoderStr = """
                    // parseByteToBinaryArray
                    var byteVal = 0x39;
                    var value = parseByteToBinaryArray(byteVal, 6, false);
                    var actualLowCurrent1Alarm = value[0];
                    var actualHighCurrent1Alarm = value[1];
                    var actualLowCurrent2Alarm = value[2];
                    var actualHighCurrent2Alarm = value[3];
                    var actualLowCurrent3Alarm = value[4];
                    var actualHighCurrent3Alarm = value[5];
                    // parseBytesToBinaryArray
                    var bytesVal = [0xCE, 0xB2];
                    // parseLongToBinaryArray
                    var longValue = 52914L;
                    return {
                        // parseByteToBinaryArray
                        "parseByteToBinaryArray_All": parseByteToBinaryArray(byteVal),
                        "parseByteToBinaryArray_1": parseByteToBinaryArray(byteVal, 3),
                        "parseByteToBinaryArray_2": parseByteToBinaryArray(byteVal, 8, false),
                        "parseByteToBinaryArray_3": parseByteToBinaryArray(byteVal, 5, false),
                        "parseByteToBinaryArray_4": parseByteToBinaryArray(byteVal, 4, false),
                        "parseByteToBinaryArray_5": parseByteToBinaryArray(byteVal, 3, false),
                        "parseByteToBinaryArray_value":  parseByteToBinaryArray(byteVal, 6, false),
                        "parseByteToBinaryArray_value0": value[0],
                        "parseByteToBinaryArray_value1": value[1],
                        "parseByteToBinaryArray_value2": value[2],
                        "parseByteToBinaryArray_value3": value[3],
                        "parseByteToBinaryArray_value4": value[4],
                        "parseByteToBinaryArray_value5": value[5],
                        //  parseBytesToBinaryArray
                        "parseBytesToBinaryArray_All": parseBytesToBinaryArray(bytesVal),
                        "parseBytesToBinaryArray_1": parseBytesToBinaryArray(bytesVal, 15),
                        "parseBytesToBinaryArray_2": parseBytesToBinaryArray(bytesVal, 14),
                        "parseBytesToBinaryArray_3": parseBytesToBinaryArray(bytesVal, 2),
                        // parseLongToBinaryArray
                        "parseLongToBinaryArray_1": parseLongToBinaryArray(longValue),
                        "parseLongToBinaryArray_2": parseLongToBinaryArray(longValue, 16),
                        // parseBinaryArrayToInt
                        "parseBinaryArrayToIn_1": parseBinaryArrayToInt([1, 0, 0, 1, 1, 1, 1, 1]),
                        "parseBinaryArrayToIn_2": parseBinaryArrayToInt([1, 0, 0, 1, 1, 1, 1, 1], 1, 7)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        // parseByteToBinaryArray
        expected.put("parseByteToBinaryArray_All", bytesToList(new byte[]{0, 0, 1, 1, 1, 0, 0, 1}));
        expected.put("parseByteToBinaryArray_1", bytesToList(new byte[]{0, 0, 1}));
        expected.put("parseByteToBinaryArray_2", bytesToList(new byte[]{1, 0, 0, 1, 1, 1, 0, 0}));
        expected.put("parseByteToBinaryArray_3", bytesToList(new byte[]{1, 0, 0, 1, 1}));
        expected.put("parseByteToBinaryArray_4", bytesToList(new byte[]{1, 0, 0, 1}));
        expected.put("parseByteToBinaryArray_5", bytesToList(new byte[]{1, 0, 0}));
        expected.put("parseByteToBinaryArray_value", bytesToList(value));
        expected.put("parseByteToBinaryArray_value0", value[0]);
        expected.put("parseByteToBinaryArray_value1", value[1]);
        expected.put("parseByteToBinaryArray_value2", value[2]);
        expected.put("parseByteToBinaryArray_value3", value[3]);
        expected.put("parseByteToBinaryArray_value4", value[4]);
        expected.put("parseByteToBinaryArray_value5", value[5]);
        //  parseBytesToBinaryArray
        expected.put("parseBytesToBinaryArray_All", bytesToList(new byte[]{1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0}));
        expected.put("parseBytesToBinaryArray_1", bytesToList(new byte[]{1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0}));
        expected.put("parseBytesToBinaryArray_2", bytesToList(new byte[]{0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0}));
        expected.put("parseBytesToBinaryArray_3", bytesToList(new byte[]{1, 0}));
        // parseLongToBinaryArray
        expected.put("parseLongToBinaryArray_1", bytesToList(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0}));
        expected.put("parseLongToBinaryArray_2", bytesToList(new byte[]{1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0}));
        // parseBinaryArrayToInt
        expected.put("parseBinaryArrayToIn_1", -97);
        expected.put("parseBinaryArrayToIn_2", 31);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // parseNumber
    @Test
    public void parseInt_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """                    
                    return {
                        "parseInt_01": parseInt("0"),
                        "parseInt_02": parseInt("473"),
                        "parseInt_03": parseInt("+42"),
                        "parseInt_04": parseInt("-0", 10),
                        "parseInt_05": parseInt("-0xFF"),
                        "parseInt_06": parseInt("-FF", 16),
                        "parseInt_07": parseInt("1100110", 2),
                        "parseInt_08": parseInt("2147483647", 10),
                        "parseInt_09": parseInt("-2147483648", 10),
                        "parseInt_10": parseInt("Kona", 27)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("parseInt_01", 0);
        expected.put("parseInt_02", 473);
        expected.put("parseInt_03", 42);
        expected.put("parseInt_04", 0);
        expected.put("parseInt_05", -255);
        expected.put("parseInt_06", -255);
        expected.put("parseInt_07", 102);
        expected.put("parseInt_08", 2147483647);
        expected.put("parseInt_09", -2147483648);
        expected.put("parseInt_10", 411787);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);

        decoderStr = """
                    return parseInt("2147483648", 10);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseInt(\"2147483648\", 10): For input string: \"2147483648\"")
                .hasMessageContaining("[Line: 1, Column: 12]");

        decoderStr = """
                    return parseInt("99", 8);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseInt(\"99\", 8): Failed radix [8] for value: \"99\"")
                .hasMessageContaining("[Line: 1, Column: 12]");

        decoderStr = """
                    return parseInt("Kona", 10);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseInt(\"Kona\", 10): Failed radix [10] for value: \"Kona\"")
                .hasMessageContaining("[Line: 1, Column: 12]");
    }

    @Test
    public void parseLong_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """                    
                    return {
                        "parseLong_01": parseLong("0"),
                        "parseLong_02": parseLong("473"),
                        "parseLong_03": parseLong("+42"),
                        "parseLong_04": parseLong("-0", 10),
                        "parseLong_05": parseLong("-0xFFFF"), 
                        "parseLong_06": parseLong("-FFFF", 16),
                        "parseLong_07": parseLong("11001101100110", 2),
                        "parseLong_08": parseLong("777777777777777777777", 8),
                        "parseLong_09": parseLong("KonaLong", 27),
                        "parseLong_10": parseLong("9223372036854775807", 10),
                        "parseLong_11": parseLong("-9223372036854775808", 10),
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("parseLong_01", 0L);
        expected.put("parseLong_02", 473L);
        expected.put("parseLong_03", 42L);
        expected.put("parseLong_04", 0L);
        expected.put("parseLong_05", -65535L);
        expected.put("parseLong_06", -65535L);
        expected.put("parseLong_07", 13158L);
        expected.put("parseLong_08", 9223372036854775807L);
        expected.put("parseLong_09", 218840926543L);
        expected.put("parseLong_10", 9223372036854775807L);
        expected.put("parseLong_11", -9223372036854775808L);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);

        decoderStr = """
                    return parseLong("9223372036854775808", 10);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseLong(\"9223372036854775808\", 10): For input string: \"9223372036854775808\"")
                .hasMessageContaining("[Line: 1, Column: 12]");

        decoderStr = """
                    return parseLong("0xFGFFFFFF", 16);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseLong(\"0xFGFFFFFF\", 16): Failed radix [16] for value: \"0xFGFFFFFF\"")
                .hasMessageContaining("[Line: 1, Column: 12]");

        decoderStr = """
                    return parseLong("FFFFFFFFF", 16);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseLong(\"FFFFFFFFF\", 16): The hexadecimal value: \"FFFFFFFFF\"")
                .hasMessageContaining("[Line: 1, Column: 12]");

        decoderStr = """
                    return parseLong("1787", 8);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseLong(\"1787\", 8): Failed radix [8] for value: \"1787\"")
                .hasMessageContaining("[Line: 1, Column: 12]");

        decoderStr = """
                    return parseLong("KonaLong", 10);
                """;
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: parseLong(\"KonaLong\", 10): Failed radix [10] for value: \"KonaLong\"")
                .hasMessageContaining("[Line: 1, Column: 12]");
    }

    @Test
    public void parseFloat_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """                    
                    return parseFloat("4.2");
                """;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(4.2f, actual);
    }

    @Test
    public void parseDouble_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """                    
                    return parseDouble("4.2");
                """;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(4.2d, actual);
    }

    // Bitwise Operations
    @Test
    public void bitwiseOperationsBoolean_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """     
                    var x = true;
                    var y = false;               
                    return {
                        "andResult": x & y,
                        "orResult": x | y,
                        "xorResult": x ^ y,                    
                        "leftShift": x << y,                    
                        "rightShift": x >> y,                    
                        "rightUnShift": x >>> y                    
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("andResult", 0);
        expected.put("orResult", 1);
        expected.put("xorResult", 1);
        expected.put("leftShift", 1);
        expected.put("rightShift", 1);
        expected.put("rightUnShift", 1);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void bitwiseOperationsMix_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """     
                    var x = true;
                    var y = false;
                    var i = 10;
                    var b = -14;
                    var l = 9223372036854775807;            
                    return {
                        "andResult": x & b,
                        "orResult": i | y,
                        "xorResult": i ^ l,                
                        "leftShift": l << i,                   
                        "rightShift": l >> b,                    
                        "rightUnShift": i >>> x                    
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("andResult", 0);
        expected.put("orResult", 10);
        expected.put("xorResult", 9223372036854775797L);
        expected.put("leftShift", -1024L);
        expected.put("rightShift", 8191L);
        expected.put("rightUnShift", 5);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }


    // base64
    @Test
    public void base64_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """                    
                    return {
                        "base64ToHex": base64ToHex("Kkk="),
                        "bytesToBase64": bytesToBase64([42, 73]),
                        "base64ToBytes": base64ToBytes("Kkk="),                   
                        "base64ToBytesList": base64ToBytesList("AQIDBAU=")                    
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("base64ToHex", "2A49");
        expected.put("bytesToBase64", "Kkk=");
        expected.put("base64ToBytes", bytesToList(new byte[]{42, 73}));
        expected.put("base64ToBytesList", bytesToList(new byte[]{1, 2, 3, 4, 5}));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // toFlatMap
    @Test
    public void toFlatMap_Test() throws ExecutionException, InterruptedException {
        msgStr = """
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
                    };
                """;
        decoderStr = """                    
                    return {
                        "toFlatMap": toFlatMap(msg),
                        "toFlatMapFalse": toFlatMap(msg, false),
                        "toFlatMapExcludeList": toFlatMap(msg, ["key1", "key3"]),
                        "toFlatMapExcludeListFalse": toFlatMap(msg, ["key2", "key4"], false)
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        LinkedHashMap<String, Object> expectedJson = new LinkedHashMap<>();
        expectedJson.put("key1", "value1");
        expectedJson.put("key2", 12);
        expectedJson.put("key3.key4", "value4");
        expectedJson.put("key3.key5", 34);
        expectedJson.put("key3.key6.0.key7", "value7");
        expectedJson.put("key3.key6.1.key8", "value8");
        expectedJson.put("key3.key6.2", "just_string_value_in_array");
        expectedJson.put("key3.key6.3", 56);
        expectedJson.put("key3.key9.key10.0", "value10_1");
        expectedJson.put("key3.key9.key10.1", "value10_2");
        expectedJson.put("key3.key9.key10.2", "value10_3");
        expectedJson.put("key_to_overwrite", "root_value");
        expectedJson.put("key11.key_to_overwrite", "second_level_value");
        expected.put("toFlatMap", expectedJson);
        LinkedHashMap<String, Object> expectedJsonFalse = new LinkedHashMap<>();
        expectedJsonFalse.put("key1", "value1");
        expectedJsonFalse.put("key2", 12);
        expectedJsonFalse.put("key4", "value4");
        expectedJsonFalse.put("key5", 34);
        expectedJsonFalse.put("key7", "value7");
        expectedJsonFalse.put("key8", "value8");
        expectedJsonFalse.put("key6.2", "just_string_value_in_array");
        expectedJsonFalse.put("key6.3", 56);
        expectedJsonFalse.put("key10.0", "value10_1");
        expectedJsonFalse.put("key10.1", "value10_2");
        expectedJsonFalse.put("key10.2", "value10_3");
        expectedJsonFalse.put("key_to_overwrite", "second_level_value");
        expected.put("toFlatMapFalse", expectedJsonFalse);
        LinkedHashMap<String, Object> expectedJsonExcludeList = new LinkedHashMap<>();
        expectedJsonExcludeList.put("key2", 12);
        expectedJsonExcludeList.put("key_to_overwrite", "root_value");
        expectedJsonExcludeList.put("key11.key_to_overwrite", "second_level_value");
        expected.put("toFlatMapExcludeList", expectedJsonExcludeList);
        LinkedHashMap<String, Object> expectedJsonExcludeListFalse = new LinkedHashMap<>();
        expectedJsonExcludeListFalse.put("key1", "value1");
        expectedJsonExcludeListFalse.put("key5", 34);
        expectedJsonExcludeListFalse.put("key7", "value7");
        expectedJsonExcludeListFalse.put("key8", "value8");
        expectedJsonExcludeListFalse.put("key6.2", "just_string_value_in_array");
        expectedJsonExcludeListFalse.put("key6.3", 56);
        expectedJsonExcludeListFalse.put("key10.0", "value10_1");
        expectedJsonExcludeListFalse.put("key10.1", "value10_2");
        expectedJsonExcludeListFalse.put("key10.2", "value10_3");
        expectedJsonExcludeListFalse.put("key_to_overwrite", "second_level_value");
        expected.put("toFlatMapExcludeListFalse", expectedJsonExcludeListFalse);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    // tbDate
    @Test
    public void tbDateStringTZ_Test() throws ExecutionException, InterruptedException {
        String s2 = "2023-08-06T12:04:05.123";
        msgStr = "{}";
        decoderStr = String.format("""
                    var d1 = new Date("2023-08-06T04:04:05.123Z");          // TZ => "UTC"
                    var d2 = new Date("%s");                                // TZ = ZoneId.systemDefault() instant
                    var d3 = new Date("2023-08-06T04:04:05.00-04");         // TZ => "-04"
                    var d4 = new Date("2023-08-06T04:04:05.00+02:00");      // TZ => "+02:00"
                    var d5 = new Date("2023-08-06T04:04:05.00+03:00:00");   // TZ => "+03:00:00"
                    var d6 = new Date("Tue, 3 Jun 2008 11:05:30 GMT");      // TZ => "GMT"
                    var d7 = new Date("Tue, 3 Jun 2008 11:05:30 +043056");  // TZ => "+043056"
                    return {
                        "dIso_1": d1.toISOString(),
                        "dIso_2": d2.toISOString(),
                        "dIso_3": d3.toISOString(),
                        "dIso_4": d4.toISOString(),
                        "dIso_5": d5.toISOString(),
                        "dIso_6": d6.toISOString(),
                        "dIso_7": d7.toISOString()
                    }
                """, s2);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("dIso_1", "2023-08-06T04:04:05.123Z");
        TbDate d2 = new TbDate(s2);
        expected.put("dIso_2", d2.toISOString());
        expected.put("dIso_3", "2023-08-06T08:04:05Z");
        expected.put("dIso_4", "2023-08-06T02:04:05Z");
        expected.put("dIso_5", "2023-08-06T01:04:05Z");
        expected.put("dIso_6", "2008-06-03T11:05:30Z");
        expected.put("dIso_7", "2008-06-03T06:34:34Z");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void tbDateStringPattern_Test() throws ExecutionException, InterruptedException {
        String s3 = "2023-08-06 12:04:05.000";
        String pattern3 = "yyyy-MM-dd HH:mm:ss.SSS";
        msgStr = "{}";
        decoderStr = String.format("""
                    var pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX";
                    var d1 = new Date("2023-08-06 04:04:05.000Z", pattern);         // Pattern without TZ => ZoneId "UTC"
                    var d2 = new Date("2023-08-06 04:04:05.000-04:00", pattern);    // Pattern with TZ => "-04:00"
                    var d3 = new Date("%s","%s");                                   // Pattern without TZ, TZ = ZoneId.systemDefault() instant
                    return {
                        "dIso_1": d1.toISOString(),
                        "dIso_2": d2.toISOString(),
                        "dIso_3": d3.toISOString()
                    }
                """, s3, pattern3);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("dIso_1", "2023-08-06T04:04:05Z");
        expected.put("dIso_2", "2023-08-06T08:04:05Z");
        TbDate d3 = new TbDate(s3, pattern3);
        expected.put("dIso_3", d3.toISOString());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void tbDateStringPatternLocale_Test() throws ExecutionException, InterruptedException {
        String sDe = "12:15:30 PM, So. 10/09/2022";
        String sUs = "12:15:30 PM, Sun 10/09/2022";
        var pattern = "hh:mm:ss a, EEE M/d/uuuu";
        msgStr = "{}";
        decoderStr = String.format("""
                    var pattern = "%s";
                    var d1 = new Date("%s", pattern, "de");     // Pattern without TZ, TZ = ZoneId.systemDefault() instant
                    var d2 = new Date("%s", pattern, "en-US");  // Pattern without TZ, TZ = ZoneId.systemDefault() instant
                    return {
                        "dIso_1": d1.toISOString(),
                        "dLocal_1": d1.toLocaleString("de"),
                        "dIso_2": d2.toISOString(),
                        "dLocal_2": d2.toLocaleString("en-US")
                    }
                """, pattern, sDe, sUs);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        TbDate d1 = new TbDate(sDe, pattern, "de");
        expected.put("dIso_1", d1.toISOString());
        expected.put("dLocal_1", d1.toLocaleString("de"));
        TbDate d2 = new TbDate(sUs, pattern, "en-US");
        expected.put("dIso_2", d2.toISOString());
        expected.put("dLocal_2", d2.toLocaleString("en-US"));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);

        decoderStr = String.format("""
                   var pattern = "%s";
                   var d = new Date("02:15:30 PM, Sun 10/09/2022", pattern, "de");
                   return d;
                """, pattern);
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("[Error: could not create constructor")
                .hasMessageContaining("[Line: 2, Column: 16]");
    }


    @Test
    public void tbDateStringPatternLocaleTZ_Test() throws ExecutionException, InterruptedException {
        String s = "2023-08-06 04:04:05.000+02:00";
        var pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX";
        msgStr = "{}";
        decoderStr = String.format("""
                    var d = new Date("%s", "%s", "de", "America/New_York"); // Pattern with TZ => "+02:00" but `Time Zone` as parameter = "America/New_York" = "-04:00"
                    return {
                        "dIso": d.toISOString(),
                        "dLocal_de": d.toLocaleString("de"),
                        "dLocal_us": d.toLocaleString("en-US")
                    }
                """, s, pattern);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        TbDate d = new TbDate(s, pattern, "de", "America/New_York");
        expected.put("dIso", d.toISOString());
        expected.put("dLocal_de", d.toLocaleString("de"));
        expected.put("dLocal_us", d.toLocaleString("en-US"));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void tbDateInstantYearMonthDateHrsMinSecond_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var d1 = new Date(2023, 8, 6, 4, 4, 5, "America/New_York");
                    var d2 = new Date(2023, 8, 6, 4, 4, 5, "Europe/Berlin");
                    return {
                        "dLocal_1": d1.toLocaleString(),
                        "dIso_1": d1.toISOString(),
                        "date_1": d1.toString(),
                        "dLocal_2": d2.toLocaleString(),
                        "dLocal_2_us": d2.toLocaleString("en-us", "America/New_York"),
                        "dIso_2": d2.toISOString(),
                        "date_2": d2.toString()
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        TbDate d1 = new TbDate(2023, 8, 6, 4, 4, 5, "America/New_York");
        expected.put("dLocal_1", d1.toLocaleString());
        expected.put("dIso_1", d1.toISOString());
        expected.put("date_1", d1.toString());
        TbDate d2 = new TbDate(2023, 8, 6, 4, 4, 5, "Europe/Berlin");
        expected.put("dLocal_2", d2.toLocaleString());
        expected.put("dLocal_2_us", "8/5/23, 10:04:05 PM");
        expected.put("dIso_2", d2.toISOString());
        expected.put("date_2", d2.toString());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void tbDateLocale_Test() throws ExecutionException, InterruptedException {
        String s2 = "2023-08-06T04:04:05.000";
        msgStr = "{}";
        decoderStr = String.format("""
                     // Input date Without TZ (TZ Default = ZoneId.systemDefault())
                     var d1 = new Date(2023, 8, 6, 4, 4, 5);                     // Parameters (int year, int month, int dayOfMonth, int hours, int minutes, int seconds) => TZ Default = ZoneId.systemDefault()
                     var d2 = new Date("%s");                                    // Parameter (String 'yyyy-MM-ddThh:mm:ss.ms') => TZ Default = ZoneId.systemDefault()
                     // Input date With TZ (TZ = parameter TZ or 'Z' equals 'UTC')
                     var d3 = new Date(2023, 8, 6, 4, 4, 5, "Europe/Berlin");    // Parameters (int year, int month, int dayOfMonth, int hours, int minutes, int seconds, TZ) => TZ "Europe/Berlin"
                     return {
                         "dLocal_1_us": d1.toLocaleString("en-US"),
                         "dIso_1": d1.toISOString(),
                         "d1": d1.toString(),
                         "dIso_2": d2.toISOString(),
                         "dLocal_2_de": d2.toLocaleString("de"),
                         "dLocal_2_utc": d2.toLocaleString("UTC"),
                         "dIso_3": d3.toISOString(),
                         "dLocal_3_utc": d3.toLocaleString("UTC"),
                         "dLocal_3_us": d3.toLocaleString("en-us"),
                         "dLocal_3_de": d3.toLocaleString("de")
                     }
                """, s2);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        TbDate d1 = new TbDate(2023, 8, 6, 4, 4, 5);
        expected.put("dLocal_1_us", "8/6/23, 4:04:05 AM");
        expected.put("dIso_1", d1.toISOString());
        expected.put("d1", d1.toString());
        TbDate d2 = new TbDate(s2);
        expected.put("dIso_2", d2.toISOString());
        expected.put("dLocal_2_de", "06.08.23, 04:04:05");
        expected.put("dLocal_2_utc", "2023-08-06 04:04:05");
        TbDate d3 = new TbDate(2023, 8, 6, 4, 4, 5, "Europe/Berlin");
        expected.put("dIso_3", d3.toISOString());
        expected.put("dLocal_3_utc", d3.toLocaleString("UTC"));
        expected.put("dLocal_3_us", d3.toLocaleString("en-us"));
        expected.put("dLocal_3_de", d3.toLocaleString("de"));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void tbDateLocaleTZ_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var d = new Date(2023, 8, 6, 4, 4, 5, "Europe/Berlin"); // Parameters (int year, int month, int dayOfMonth, int hours, int minutes, int seconds, TZ) => TZ "Europe/Berlin";
                    return {
                        "dIso": d.toISOString(),
                        "dLocal_utc": d.toLocaleString("UTC"),
                        "dLocal_us": d.toLocaleString("en-us", "America/New_York"),
                        "dLocal_de": d.toLocaleString("de", "Europe/Berlin")
                    }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        TbDate d = new TbDate(2023, 8, 6, 4, 4, 5, "Europe/Berlin");
        expected.put("dIso", d.toISOString());
        expected.put("dLocal_utc", d.toLocaleString("UTC"));
        expected.put("dLocal_us", "8/5/23, 10:04:05 PM");
        expected.put("dLocal_de", "06.08.23, 04:04:05");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void tbDateLocalPatternMap_Test() throws ExecutionException, InterruptedException {
        String s1 = "2023-08-06T04:04:05.00Z";
        String s2 = "2023-08-06T04:04:05.000";
        String options1_2Str = "{\"timeZone\":\"Europe/Berlin\"}";
        msgStr = "{}";
        decoderStr = String.format("""
                    var d1 = new Date("%s");                                // TZ => "UTC"
                    var d2 = new Date("%s");                                // TZ => Default = ZoneId.systemDefault
                    var options1_2 = %s;                                  // TZ = "+02:00"
                    var options1_2Str = JSON.stringify(options1_2);
                    var d3 = new Date(2023, 8, 6, 4, 4, 5);                 // TZ => Default = ZoneId.systemDefault
                    var options3 = {"timeZone":"Europe/Berlin", "pattern": "M-d/yyyy, h:mm=ss a"};
                    var options3Str = JSON.stringify(options3);
                    var d4 = new Date(2023, 8, 6, 4, 4, 5, "UTC");          // TZ => "UTC"
                    var options4 = {"timeZone":"Europe/Berlin","dateStyle":"full","timeStyle":"full"};
                    var options4Str = JSON.stringify(options4);
                    return {
                        "dIso_1": d1.toISOString(),
                        "dLocal_1": d1.toLocaleString(),
                        "dLocal_1_options": d1.toLocaleString("en-US", options1_2Str),
                        "dIso_2": d2.toISOString(),
                        "dLocal_2_options": d2.toLocaleString("en-US", options1_2Str);,
                        "dIso_3": d3.toISOString(),
                        "dLocal_3_options": d3.toLocaleString("en-US", options3Str),
                        "dIso_4": d4.toISOString(),
                        "dLocal_4_options_ua": d4.toLocaleString("uk-UA", options4Str),
                        "dLocal_4_options_us": d4.toLocaleString("en-US", options4Str),
                        "dLocal_4_options_de": d4.toLocaleString("de", options4Str)
                    }
                """, s1, s2, options1_2Str);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        TbDate d1 = new TbDate(s1);
        expected.put("dIso_1", "2023-08-06T04:04:05Z");
        expected.put("dLocal_1", d1.toLocaleString());
        expected.put("dLocal_1_options", d1.toLocaleString("en-US", options1_2Str));
        TbDate d2 = new TbDate(s2);
        expected.put("dIso_2", d2.toISOString());
        expected.put("dLocal_2_options", d2.toLocaleString("en-US", options1_2Str));
        TbDate d3 = new TbDate(2023, 8, 6, 4, 4, 5);
        String options3Str = "{\"timeZone\":\"Europe/Berlin\", \"pattern\": \"M-d/yyyy, h:mm=ss a\"}";
        expected.put("dIso_3", d3.toISOString());
        expected.put("dLocal_3_options", d3.toLocaleString("en-US", options3Str));
        TbDate d4 = new TbDate(2023, 8, 6, 4, 4, 5, "UTC");
        String options4Str = "{\"timeZone\":\"Europe/Berlin\",\"dateStyle\":\"full\",\"timeStyle\":\"full\"}";
        expected.put("dIso_4", "2023-08-06T04:04:05Z");
        expected.put("dLocal_4_options_ua", d4.toLocaleString("uk-UA", options4Str));
        expected.put("dLocal_4_options_us", d4.toLocaleString("en-US", options4Str));
        expected.put("dLocal_4_options_de", d4.toLocaleString("de", options4Str));
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void toUnmodifiableExecutionArrayList_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = String.format("""
                var original = [];
                original.add(0x35);
                var unmodifiable = original.toUnmodifiable();
                msg.result = unmodifiable;
                return {msg: msg};
                """);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        List expectedList = Arrays.asList(0x35);
        LinkedHashMap<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("result", expectedList);
        expected.put("msg", expectedResult);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);

        decoderStr = String.format("""
                var original = [];
                original.add(0x67);
                var unmodifiable = original.toUnmodifiable();
                unmodifiable.add(0x35);                                
                msg.result = unmodifiable;
                return {msg: msg};
                """);
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: unmodifiable.add(0x35): List is unmodifiable");
    }


    @Test
    public void toUnmodifiableExecutionHashMap_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = String.format("""
                var original = {};
                original.putIfAbsent("entry1", 73);
                var unmodifiable = original.toUnmodifiable();
                msg.result = unmodifiable;
                return {msg: msg};
                """);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        LinkedHashMap<String, Object> expectedMap = new LinkedHashMap<>(Map.of("entry1", 73));
        LinkedHashMap<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("result", expectedMap);
        expected.put("msg", expectedResult);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);

        decoderStr = String.format("""
                var original = {};
                original.humidity = 73;
                var unmodifiable = original.toUnmodifiable();
                unmodifiable.put("temperature1", 96);
                msg.result = unmodifiable;
                return {msg: msg};
                """);
        assertThatThrownBy(() -> {
            invokeScript(evalScript(decoderStr), msgStr);
        }).hasMessageContaining("Error: unmodifiable.put(\"temperature1\", 96): Map is unmodifiable");
    }

    @Test
    public void tbDateFunction_Test() throws ExecutionException, InterruptedException {
        String stringDateUTC = "2024-01-01T10:00:00.00Z";
        TbDate d = new TbDate(stringDateUTC);

        msgStr = "{}";
        decoderStr = String.format("""
                var d = new Date("%s");              // TZ => "UTC"
                var dIsoY1 = d.toISOString();                           // return 2024-01-01T10:00:00Z
                d.addYears(1);
                var dIsoY2 = d.toISOString();                           // return 2025-01-01T10:00:00Z
                d.addYears(-2);
                var dIsoY3 = d.toISOString();                         // return 2023-01-01T10:00:00Z
                d.addMonths(2);
                var dIsoM1 = d.toISOString();                           // return 2023-03-01T10:00:00Z
                d.addMonths(10);
                var dIsoM2 = d.toISOString();                           // return 2024-01-01T10:00:00Z
                d.addMonths(-13);
                var dIsoM3 = d.toISOString();                           // return 2022-12-01T10:00:00Z
                d.addWeeks(4);
                var dIsoW1 = d.toISOString();                           // return 2022-12-29T10:00:00Z
                d.addWeeks(-5);
                var dIsoW2 = d.toISOString();                           // return 2022-11-24T10:00:00Z
                d.addDays(6);
                var dIsoD1 = d.toISOString();                           // return 2022-11-30T10:00:00Z
                d.addDays(45);
                var dIsoD2 = d.toISOString();                           // return 2023-01-14T10:00:00Z
                d.addDays(-50);
                var dIsoD3 = d.toISOString();                           // return 2022-11-25T10:00:00Z
                d.addHours(23);
                var dIsoH1 = d.toISOString();                           // return 2022-11-26T09:00:00Z
                d.addHours(-47);
                var dIsoH2 = d.toISOString();                           // return 2022-11-24T10:00:00Z
                d.addMinutes(59);
                var dIsoMin1 = d.toISOString();                         // return 2022-11-24T10:59:00Z
                d.addMinutes(-60);
                var dIsoMin2 = d.toISOString();                         // return 2022-11-24T09:59:00Z
                d.addSeconds(59);
                var dIsoS1 = d.toISOString();                           // return 2022-11-24T09:59:59Z
                d.addSeconds(-60);
                var dIsoS2 = d.toISOString();                           // return 2022-11-24T09:58:59Z
                d.addNanos(999999);
                var dIsoN1 = d.toISOString();                           // return 2022-11-24T09:58:59.000999999Z
                d.addNanos(-1000000);
                var dIsoN2 = d.toISOString();                           // return 2022-11-24T09:58:58.999999999Z
                    return {
                        "dIsoY1": dIsoY1,
                        "dIsoY2": dIsoY2,
                        "dIsoY3": dIsoY3,
                        "dIsoM1": dIsoM1,
                        "dIsoM2": dIsoM2,
                        "dIsoM3": dIsoM3,
                        "dIsoW1": dIsoW1,
                        "dIsoW2": dIsoW2,
                        "dIsoD1": dIsoD1,
                        "dIsoD2": dIsoD2,
                        "dIsoD3": dIsoD3,
                        "dIsoH1": dIsoH1,
                        "dIsoH2": dIsoH2,
                        "dIsoMin1": dIsoMin1,
                        "dIsoMin2": dIsoMin2,
                        "dIsoS1": dIsoS1,
                        "dIsoS2": dIsoS2,
                        "dIsoN1": dIsoN1,
                        "dIsoN2": dIsoN2
                    }
                """, stringDateUTC);
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("dIsoY1", d.toISOString());
        d.addYears(1);
        expected.put("dIsoY2", d.toISOString());
        d.addYears(-2);
        expected.put("dIsoY3", d.toISOString());
        d.addMonths(2);
        expected.put("dIsoM1", d.toISOString());
        d.addMonths(10);
        expected.put("dIsoM2", d.toISOString());
        d.addMonths(-13);
        expected.put("dIsoM3", d.toISOString());
        d.addWeeks(4);
        expected.put("dIsoW1", d.toISOString());
        d.addWeeks(-5);
        expected.put("dIsoW2", d.toISOString());
        d.addDays(6);
        expected.put("dIsoD1", d.toISOString());
        d.addDays(45);
        expected.put("dIsoD2", d.toISOString());
        d.addDays(-50);
        expected.put("dIsoD3", d.toISOString());
        d.addHours(23);
        expected.put("dIsoH1", d.toISOString());
        d.addHours(-47);
        expected.put("dIsoH2", d.toISOString());
        d.addMinutes(59);
        expected.put("dIsoMin1", d.toISOString());
        d.addMinutes(-60);
        expected.put("dIsoMin2", d.toISOString());
        d.addSeconds(59);
        expected.put("dIsoS1", d.toISOString());
        d.addSeconds(-60);
        expected.put("dIsoS2", d.toISOString());
        d.addNanos(999999);
        expected.put("dIsoN1", d.toISOString());
        d.addNanos(-1000000);
        expected.put("dIsoN2", d.toISOString());
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void isMap_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {}
                """;
        decoderStr = """
                return isMap(msg);
                """;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertInstanceOf(Boolean.class, actual);
        assertTrue((Boolean) actual);
        decoderStr = """
                return isList(msg);
                """;
        actual = invokeScript(evalScript(decoderStr), msgStr);
        assertInstanceOf(Boolean.class, actual);
        assertFalse((Boolean) actual);
    }

    @Test
    public void isList_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {}
                """;
        decoderStr = String.format("""
                var list = [];
                list.add(0x35);
                return isList(list);
                """);
    }

    @Test
    public void isSet_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {"list": ["C", "B", "A", 34567, "B", "C", "hello", 34]}
                """;
        decoderStr = """
                    return isSet(toSet(msg.list));        // return true
                """;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertInstanceOf(Boolean.class, actual);
        assertTrue((Boolean) actual);
    }

    @Test
    public void isArray_Test() throws ExecutionException, InterruptedException {
        msgStr = """
                {}
                """;
        decoderStr = """
                var array = new int[3];
                array[0] = 1;
                array[1] = 2;
                array[2] = 3;
                return isArray(array);
                """;
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertInstanceOf(Boolean.class, actual);
        assertTrue((Boolean) actual);
    }

    @Test
    public void isInsidePolygon_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var perimeter = "[[[37.7810,-122.4210],[37.7890,-122.3900],[37.7700,-122.3800],[37.7600,-122.4000],[37.7700,-122.4250],[37.7810,-122.4210]],[[37.7730,-122.4050],[37.7700,-122.3950],[37.7670,-122.3980],[37.7690,-122.4100],[37.7730,-122.4050]]]";
                    return{
                       outsidePolygon: isInsidePolygon(37.8000, -122.4300, perimeter),
                       insidePolygon: isInsidePolygon(37.7725, -122.4010, perimeter),
                       insideHole: isInsidePolygon(37.7700, -122.4030, perimeter)
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("outsidePolygon", false);
        expected.put("insidePolygon", true);
        expected.put("insideHole", false);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    @Test
    public void isInsideCircle_Test() throws ExecutionException, InterruptedException {
        msgStr = "{}";
        decoderStr = """
                    var perimeter = "{\\"latitude\\":37.7749,\\"longitude\\":-122.4194,\\"radius\\":3000,\\"radiusUnit\\":\\"METER\\"}";
                    return{
                       outsideCircle: isInsideCircle(37.8044, -122.2712, perimeter),
                       insideCircle: isInsideCircle(37.7599, -122.4148, perimeter)
                      }
                """;
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("outsideCircle", false);
        expected.put("insideCircle", true);
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
    }

    private List splice(List oldList, int start, int deleteCount, Object... values) {
        start = initStartIndex(oldList, start);
        deleteCount = deleteCount < 0 ? 0 : Math.min(deleteCount, (oldList.size() - start));
        List removed = new ArrayList<>();
        while (deleteCount > 0) {
            removed.add(oldList.remove(start));
            deleteCount--;
        }
        int insertIdx = start;
        for (Object e : values) {
            oldList.add(insertIdx++, e);
        }
        return removed;
    }

    private List splice(List oldList, int start) {
        return this.splice(oldList, start, oldList.size() - start);
    }

    private List fill(List oldList, Object value) {
        return fill(oldList, value, 0);
    }

    private List fill(List oldList, Object value, int start) {
        return fill(oldList, value, start, oldList.size());
    }

    private List fill(List oldList, Object value, int start, int end) {
        start = initStartIndex(oldList, start);
        end = initEndIndex(oldList, end);

        if (start < oldList.size() && end > start) {
            for (int i = start; i < end; ++i) {
                oldList.set(i, value);
            }
        }
        return oldList;
    }

    private int initStartIndex(List oldList, int start) {
        return start < -oldList.size() ? 0 :
                start < 0 ? start + oldList.size() :
                        start;
    }

    private int initEndIndex(List oldList, int end) {
        return end < -oldList.size() ? 0 :
                end < 0 ? end + oldList.size() :
                        Math.min(end, oldList.size());
    }

    private static final Comparator numericCompAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return first.compareTo(second);
        }
    };

    private static final Comparator numericCompDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return second.compareTo(first);
        }
    };

    private static List<Byte> bytesToList(byte[] bytes) {
        List<Byte> list = new ArrayList<>();
        for (byte aByte : bytes) {
            list.add(aByte);
        }
        return list;
    }
}

