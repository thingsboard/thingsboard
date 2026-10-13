// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteConverterArgs;
import org.thingsboard.client.api.ThingsboardApi.GetConverterByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetConvertersArgs;
import org.thingsboard.client.api.ThingsboardApi.GetConvertersByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveConverterArgs;
import org.thingsboard.client.model.Converter;
import org.thingsboard.client.model.ConverterType;
import org.thingsboard.client.model.PageDataConverter;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class ConverterApiClientTest extends AbstractApiClientTest {

    public static final JsonNode TEST_DECODER = OBJECT_MAPPER.createObjectNode().put("scriptLang", "TBEL")
            .put("decoder", "")
            .put("tbelDecoder", "// Decode an uplink message from a buffer\\n// payload - array of bytes\\n// metadata - key/value object\\n\\n/** Decoder **/\\n\\n// decode payload to string\\nvar payloadStr = decodeToString(payload);\\n\\n// decode payload to JSON\\n// var data = decodeToJson(payload);\\n\\nvar deviceName = 'Device A';\\nvar deviceType = 'thermostat';\\nvar customerName = 'Customer C';\\nvar groupName = 'thermostat devices';\\nvar manufacturer = 'Example corporation';\\n// use assetName and assetType instead of deviceName and deviceType\\n// to automatically create assets instead of devices.\\n// var assetName = 'Asset A';\\n// var assetType = 'building';\\n\\n// Result object with device/asset attributes/telemetry data\\nvar result = {\\n// Use deviceName and deviceType or assetName and assetType, but not both.\\n   deviceName: deviceName,\\n   deviceType: deviceType,\\n// assetName: assetName,\\n// assetType: assetType,\\n// customerName: customerName,\\n   groupName: groupName,\\n   attributes: {\\n       model: 'Model A',\\n       serialNumber: 'SN111',\\n       integrationName: metadata['integrationName'],\\n       manufacturer: manufacturer\\n   },\\n   telemetry: {\\n       temperature: 42,\\n       humidity: 80,\\n       rawData: payloadStr\\n   }\\n};\\n\\n/** Helper functions 'decodeToString' and 'decodeToJson' are already built-in **/\\n\\nreturn result;\",\"encoder\":null,\"tbelEncoder\":null,\"updateOnlyKeys\":[\"manufacturer\"]}");
    public static final JsonNode TEST_ENCODER = OBJECT_MAPPER.createObjectNode().put("scriptLang", "TBEL")
            .put("encoder", "")
            .put("tbelEncoder", "// Encode a downlink message to a buffer\\n// data - key/value object with device/asset attributes/telemetry to encode\\n// metadata - key/value object\\n\\n/** Encoder **/\\n\\nvar payloadStr = 'Hello';\\nvar payload = encodeToBuffer(payloadStr);\\n\\n/** Helper function 'encodeToBuffer' is already built-in **/\\n\\nreturn payload;");

    @Test
    public void testConverterLifecycle() throws Exception {
        long ts = System.currentTimeMillis();

        Converter created = client.saveConverter(SaveConverterArgs.builder()
                .converter(buildConverter(TEST_PREFIX + ts, ConverterType.UPLINK))
                .build());
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(TEST_PREFIX + ts, created.getName());
        assertEquals(ConverterType.UPLINK, created.getType());

        String converterId = created.getId().getId().toString();

        Converter fetched = client.getConverterById(GetConverterByIdArgs.builder()
                .converterId(converterId)
                .build());
        assertNotNull(fetched);
        assertEquals(converterId, fetched.getId().getId().toString());
        assertEquals(created.getName(), fetched.getName());

        fetched.setName(TEST_PREFIX + ts + "_updated");
        fetched.setDebugMode(true);
        Converter updated = client.saveConverter(SaveConverterArgs.builder()
                .converter(fetched)
                .build());
        assertEquals(TEST_PREFIX + ts + "_updated", updated.getName());

        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(converterId)
                .build());

        assertReturns404(() -> client.getConverterById(GetConverterByIdArgs.builder()
                .converterId(converterId)
                .build()));
    }

    @Test
    public void testGetConverters() throws Exception {
        long ts = System.currentTimeMillis();
        List<String> createdIds = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String name = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + ts + "_" + i;
            ConverterType type = (i % 2 == 0) ? ConverterType.UPLINK : ConverterType.DOWNLINK;
            Converter c = client.saveConverter(SaveConverterArgs.builder()
                    .converter(buildConverter(name, type))
                    .build());
            createdIds.add(c.getId().getId().toString());
        }

        PageDataConverter allPage = client.getConverters(GetConvertersArgs.builder()
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(allPage);
        assertNotNull(allPage.getData());
        assertTrue(allPage.getTotalElements() >= 10);

        PageDataConverter filtered = client.getConverters(GetConvertersArgs.builder()
                .pageSize("100")
                .page("0")
                .textSearch(TEST_PREFIX_2)
                .build());
        assertNotNull(filtered);
        assertEquals(5, filtered.getData().size());
        assertTrue(filtered.getData().stream()
                .allMatch(c -> c.getName().contains(TEST_PREFIX_2)));

        PageDataConverter page1 = client.getConverters(GetConvertersArgs.builder()
                .pageSize("3")
                .page("0")
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertEquals(3, page1.getData().size());
        assertTrue(page1.getHasNext());

        for (String id : createdIds) {
            client.deleteConverter(DeleteConverterArgs.builder()
                    .converterId(id)
                    .build());
        }
    }

    @Test
    public void testGetConvertersByIdsV2() throws Exception {
        long ts = System.currentTimeMillis();

        Converter c1 = client.saveConverter(SaveConverterArgs.builder()
                .converter(buildConverter(TEST_PREFIX + ts + "_a", ConverterType.UPLINK))
                .build());
        Converter c2 = client.saveConverter(SaveConverterArgs.builder()
                .converter(buildConverter(TEST_PREFIX + ts + "_b", ConverterType.DOWNLINK))
                .build());

        String id1 = c1.getId().getId().toString();
        String id2 = c2.getId().getId().toString();

        List<Converter> result = client.getConvertersByIds(GetConvertersByIdsArgs.builder()
                .converterIds(List.of(id1, id2))
                .build());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().getId().toString().equals(id1)));
        assertTrue(result.stream().anyMatch(c -> c.getId().getId().toString().equals(id2)));

        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(id1)
                .build());
        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(id2)
                .build());
    }

    @Test
    public void testNonExistentConverterReturns404() {
        String randomId = UUID.randomUUID().toString();
        assertReturns404(() -> client.getConverterById(GetConverterByIdArgs.builder()
                .converterId(randomId)
                .build()));
        assertReturns404(() -> client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(randomId)
                .build()));
    }

    private Converter buildConverter(String name, ConverterType type) {
        Converter converter = new Converter();
        converter.setName(name);
        converter.setType(type);
        converter.setConfiguration(type == ConverterType.UPLINK ? TEST_DECODER : TEST_ENCODER);
        return converter;
    }

}
