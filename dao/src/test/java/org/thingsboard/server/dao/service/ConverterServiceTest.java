// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DaoSqlTest
public class ConverterServiceTest extends AbstractServiceTest {

    @Autowired
    ConverterService converterService;

    private final JsonNode CUSTOM_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");
    private IdComparator<Converter> idComparator = new IdComparator<>();

    @Test
    public void testSaveConverter() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = converterService.saveConverter(converter);

        Assert.assertNotNull(savedConverter);
        Assert.assertNotNull(savedConverter.getId());
        Assert.assertTrue(savedConverter.getCreatedTime() > 0);
        Assert.assertEquals(converter.getTenantId(), savedConverter.getTenantId());
        Assert.assertEquals(converter.getName(), savedConverter.getName());

        savedConverter.setName("My new converter");

        converterService.saveConverter(savedConverter);
        Converter foundConverter = converterService.findConverterById(savedConverter.getTenantId(), savedConverter.getId());
        Assert.assertEquals(foundConverter.getName(), savedConverter.getName());

        converterService.deleteConverter(savedConverter.getTenantId(), savedConverter.getId());
    }

    @Test
    public void testSaveConverterWithEmptyName() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setType(ConverterType.UPLINK);
        Assertions.assertThrows(DataValidationException.class, () -> {
            converterService.saveConverter(converter);
        });
    }

    @Test
    public void testSaveConverterWithEmptyTenant() {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        Assertions.assertThrows(DataValidationException.class, () -> {
            converterService.saveConverter(converter);
        });
    }

    @Test
    public void testSaveConverterWithInvalidTenant() {
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setTenantId(new TenantId(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            converterService.saveConverter(converter);
        });
    }

    @Test
    public void testUpdateConverterType() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = converterService.saveConverter(converter);
        savedConverter.setType(ConverterType.DOWNLINK);
        Assertions.assertThrows(DataValidationException.class, () -> {
            converterService.saveConverter(savedConverter);
        });
    }

    @Test
    public void testFindConverterById() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = converterService.saveConverter(converter);
        Converter foundConverter = converterService.findConverterById(savedConverter.getTenantId(), savedConverter.getId());
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
        converterService.deleteConverter(savedConverter.getTenantId(), savedConverter.getId());
    }

    @Test
    public void testFindConverterByName() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = converterService.saveConverter(converter);
        Converter converter2 = new Converter(converter);
        Assert.assertThrows(DataValidationException.class, () -> converterService.saveConverter(converter2));
        Converter foundConverter = converterService.findConverterByName(savedConverter.getTenantId(), savedConverter.getName()).get();
        Assert.assertNotNull(foundConverter);
        Assert.assertEquals(savedConverter, foundConverter);
        converterService.deleteConverter(savedConverter.getTenantId(), savedConverter.getId());
    }

    @Test
    public void testDeleteConverter() {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        Converter savedConverter = converterService.saveConverter(converter);
        Converter foundConverter = converterService.findConverterById(savedConverter.getTenantId(), savedConverter.getId());
        Assert.assertNotNull(foundConverter);
        converterService.deleteConverter(savedConverter.getTenantId(), savedConverter.getId());
        foundConverter = converterService.findConverterById(savedConverter.getTenantId(), savedConverter.getId());
        Assert.assertNull(foundConverter);
    }

    @Test
    public void testFindTenantConverters() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<Converter> converters = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Converter converter = new Converter();
            converter.setTenantId(tenantId);
            converter.setName("Converter" + i);
            converter.setType(ConverterType.UPLINK);
            converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
            converters.add(converterService.saveConverter(converter));
        }

        List<Converter> loadedConverters = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Converter> pageData;
        do {
            pageData = converterService.findTenantConverters(tenantId, null, pageLink);
            loadedConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(converters, idComparator);
        Collections.sort(loadedConverters, idComparator);

        Assert.assertEquals(converters, loadedConverters);

        converterService.deleteConvertersByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = converterService.findTenantConverters(tenantId, null, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

}
