// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.SaveTrendzConfigArgs;
import org.thingsboard.client.model.TrendzConfiguration;
import org.thingsboard.client.model.TrendzHealthcheckResult;
import org.thingsboard.client.model.TrendzSynchronizationResult;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class TrendzApiClientTest extends AbstractApiClientTest {

    private static final String FAKE_TRENDZ_URL = "http://trendz.test:8888";
    private static final String FAKE_TB_URL = "http://thingsboard.test:8080";

    @Test
    public void testTrendzConfigLifecycle() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");

        TrendzConfiguration saved = saveFakeConfig();
        assertNotNull(saved);
        assertEquals(FAKE_TRENDZ_URL, saved.getTrendzUrl());
        assertEquals(FAKE_TB_URL, saved.getTbUrl());

        TrendzConfiguration fetched = client.getTrendzConfig();
        assertNotNull(fetched);
        assertEquals(FAKE_TRENDZ_URL, fetched.getTrendzUrl());
        assertEquals(FAKE_TB_URL, fetched.getTbUrl());

        fetched.setTrendzUrl("http://trendz-updated.test:8888");
        TrendzConfiguration updated = client.saveTrendzConfig(SaveTrendzConfigArgs.builder()
                .trendzConfiguration(fetched)
                .build());
        assertNotNull(updated);
        assertEquals("http://trendz-updated.test:8888", updated.getTrendzUrl());
    }

    @Test
    public void testConnectToTrendz() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");

        saveFakeConfig();

        try {
            TrendzSynchronizationResult result = client.connectToTrendz();
            assertNotNull(result);
        } catch (ApiException e) {
            assertTrue("connectToTrendz returned unexpected HTTP status: " + e.getCode(),
                    e.getCode() >= 400);
        }
    }

    @Test
    public void testPublicConnectToTrendz() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");

        saveFakeConfig();

        try {
            client.publicConnectToTrendz();
        } catch (ApiException e) {
            assertTrue("publicConnectToTrendz returned unexpected HTTP status: " + e.getCode(),
                    e.getCode() >= 400);
        }
    }

    @Test
    public void testGetTrendzSyncResult() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");
        saveFakeConfig();

        try {
            TrendzSynchronizationResult result = client.getTrendzSyncResult();
            assertNotNull(result);
        } catch (ApiException e) {
            assertTrue("getTrendzSyncResult returned unexpected HTTP status: " + e.getCode(),
                    e.getCode() >= 400);
        }
    }

    @Test
    public void testPerformTrendzHealthcheck() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");
        saveFakeConfig();

        try {
            TrendzHealthcheckResult result = client.performTrendzHealthcheck();
            assertNotNull(result);
        } catch (ApiException e) {
            assertTrue("performTrendzHealthcheck returned unexpected HTTP status: " + e.getCode(),
                    e.getCode() >= 400);
        }
    }

    private TrendzConfiguration saveFakeConfig() throws ApiException {
        TrendzConfiguration config = new TrendzConfiguration();
        config.setTrendzUrl(FAKE_TRENDZ_URL);
        config.setTbUrl(FAKE_TB_URL);
        return client.saveTrendzConfig(SaveTrendzConfigArgs.builder()
                .trendzConfiguration(config)
                .build());
    }

}
