// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.converter;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.converter.ConverterDao;

import java.util.Optional;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JpaConverterDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ConverterDao converterDao;

    @Test
    public void testFindConvertersByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID converterId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            saveConverter(converterId, tenantId, "CONVERTER_" + i, ConverterType.UPLINK);
        }
        assertEquals(60, converterDao.find(TenantId.SYS_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0,"CONVERTER_");
        PageData<Converter> converters1 = converterDao.findByTenantId(tenantId1, pageLink);
        assertEquals(20, converters1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Converter> converters2 = converterDao.findByTenantId(tenantId1, pageLink);
        assertEquals(10, converters2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Converter> converters3 = converterDao.findByTenantId(tenantId1, pageLink);
        assertEquals(0, converters3.getData().size());
    }

    @Test
    public void testFindConverterByTenantIdAndName() {
        UUID converterId1 = Uuids.timeBased();
        UUID converterId2 = Uuids.timeBased();
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        String name = "TEST_CONVERTER";
        saveConverter(converterId1, tenantId1, name, ConverterType.UPLINK);
        saveConverter(converterId2, tenantId2, name, ConverterType.UPLINK);

        Optional<Converter> converterOpt1 = converterDao.findConverterByTenantIdAndName(tenantId2, name);
        assertTrue("Optional expected to be non-empty", converterOpt1.isPresent());
        assertEquals(converterId2, converterOpt1.get().getId().getId());

        Optional<Converter> converterOpt2 = converterDao.findConverterByTenantIdAndName(tenantId2, "NON_EXISTENT_NAME");
        assertFalse("Optional expected to be empty", converterOpt2.isPresent());
    }

    @Test
    public void testFindConvertersByTenantIdAndNameAndType() {
        UUID converterId1 = Uuids.timeBased();
        UUID converterId2 = Uuids.timeBased();
        UUID tenantId = Uuids.timeBased();
        String name = "TEST_CONVERTER";
        saveConverter(converterId1, tenantId, name, ConverterType.UPLINK);
        saveConverter(converterId2, tenantId, name, ConverterType.DOWNLINK);

        Optional<Converter> converterOpt1 = converterDao.findConverterByTenantIdAndNameAndType(tenantId, name, ConverterType.UPLINK);
        assertTrue("Optional expected to be non-empty", converterOpt1.isPresent());
        assertEquals(converterId1, converterOpt1.get().getId().getId());

        Optional<Converter> converterOpt2 = converterDao.findConverterByTenantIdAndNameAndType(tenantId, name, ConverterType.DOWNLINK);
        assertTrue("Optional expected to be non-empty", converterOpt2.isPresent());
        assertEquals(converterId2, converterOpt2.get().getId().getId());
    }

    private void saveConverter(UUID id, UUID tenantId, String name, ConverterType type) {
        Converter converter = new Converter();
        converter.setId(new ConverterId(id));
        converter.setTenantId(new TenantId(tenantId));
        converter.setName(name);
        converter.setType(type);
        converterDao.save(new TenantId(tenantId), converter);
    }
}
