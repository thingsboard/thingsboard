/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DaoSqlTest
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
                var listAdd = ["thigsboard", 4, 67];
                return {
                        list: list.clone(),
                        length: list.length(),
                        memorySize: list.memorySize(),
                        indOf1: list.indexOf("B", 1),
                        indOf2: list.indexOf(2, 2),
                        sStr: list.validateClazzInArrayIsOnlyString()
                       }
                """;
        ArrayList list = new ArrayList<>(List.of(67, 2, 2, 2));
        LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", list);
        expected.put("length", list.size());
        expected.put("memorySize", 32L);
        expected.put("indOf1", -1);
        expected.put("indOf2", 2);
        expected.put("sStr", false);
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
    public void dpubleToString_Test() throws ExecutionException, InterruptedException {
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
        expected.put("dd16", "0x1.e83f862142b5bp19");
        expected.put("dd10", "999932,19156013");
        Object actual = invokeScript(evalScript(decoderStr), msgStr);
        assertEquals(expected, actual);
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
    public void stringToBytes_Test() throws ExecutionException, InterruptedException {
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

