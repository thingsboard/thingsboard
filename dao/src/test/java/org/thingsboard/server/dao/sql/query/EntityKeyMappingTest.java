/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.query;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;

@RunWith(SpringRunner.class )
@SpringBootTest(classes = EntityKeyMapping.class)
public class EntityKeyMappingTest {

    @Autowired
    private EntityKeyMapping entityKeyMapping;

    private static final List<String> result = List.of("device1", "device2", "device3");

    @Test
    public void testSplitToList() {
        String value = "device1, device2, device3";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    @Test
    public void testReplaceSingleQuote() {
        String value = "'device1', 'device2', 'device3'";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    @Test
    public void testReplaceDoubleQuote() {
        String value = "\"device1\", \"device2\", \"device3\"";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    @Test
    public void testSplitWithoutSpace() {
        String value = "\"device1\"    ,    \"device2\"    ,    \"device3\"";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    @Test
    public void testSaveSpacesBetweenString() {
        String value = "device 1 , device 2  ,         device 3";
        List<String> result = List.of("device 1", "device 2", "device 3");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    @Test
    public void testSaveQuoteInString() {
        String value = "device ''1 , device \"\"2  ,         device \"'3";
        List<String> result = List.of("device ''1", "device \"\"2", "device \"'3");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    @Test
    public void testNotDeleteQuoteWhenDifferentStyle() {

        String value = "\"device1\", 'device2', \"device3\"";
        List<String> result = List.of("\"device1\"", "'device2'", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);

        value = "'device1', \"device2\", \"device3\"";
        result = List.of("'device1'", "\"device2\"", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);

        value = "device1, 'device2', \"device3\"";
        result = List.of("device1", "'device2'", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);


        value = "'device1', device2, \"device3\"";
        result = List.of("'device1'", "device2", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);

        value = "device1, \"device2\", \"device3\"";
        result = List.of("device1", "\"device2\"", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);


        value = "\"device1\", device2, \"device3\"";
        result = List.of("\"device1\"", "device2", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }
}