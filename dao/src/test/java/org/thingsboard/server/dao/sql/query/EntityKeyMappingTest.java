package org.thingsboard.server.dao.sql.query;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class )
@SpringBootTest(classes = EntityKeyMapping.class)
public class EntityKeyMappingTest {

    @Autowired
    private EntityKeyMapping entityKeyMapping;

    private static final List<String> result = List.of("device1", "device2", "device3");

    @Test
    public void testSplitToList() {
        String value = "device1, device2, device3";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testReplaceSingleQuote() {
        String value = "'device1', 'device2', 'device3'";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testReplaceDoubleQuote() {
        String value = "\"device1\", \"device2\", \"device3\"";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSplitWithoutSpace() {
        String value = "\"device1\"    ,    \"device2\"    ,    \"device3\"";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSaveSpacesBetweenString() {
        String value = "device 1 , device 2  ,         device 3";
        List<String> result = List.of("device 1", "device 2", "device 3");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSaveQuoteInString() {
        String value = "device ''1 , device \"\"2  ,         device \"'3";
        List<String> result = List.of("device ''1", "device \"\"2", "device \"'3");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testNotDeleteQuoteWhenDifferentStyle() {

        String value = "\"device1\", 'device2', \"device3\"";
        List<String> result = List.of("\"device1\"", "'device2'", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "'device1', \"device2\", \"device3\"";
        result = List.of("'device1'", "\"device2\"", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "device1, 'device2', \"device3\"";
        result = List.of("device1", "'device2'", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);


        value = "'device1', device2, \"device3\"";
        result = List.of("'device1'", "device2", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "device1, \"device2\", \"device3\"";
        result = List.of("device1", "\"device2\"", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);


        value = "\"device1\", device2, \"device3\"";
        result = List.of("\"device1\"", "device2", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }
}