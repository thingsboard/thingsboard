// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.After;
import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteDomainArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDomainInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDomainInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDomainArgs;
import org.thingsboard.client.model.Domain;
import org.thingsboard.client.model.DomainInfo;
import org.thingsboard.client.model.PageDataDomainInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class DomainApiClientTest extends AbstractApiClientTest {

    List<Domain> createdDomains = new ArrayList<>();

    @After
    public void afterDomainTest() {
        createdDomains.forEach(domain -> {
            try {
                client.deleteDomain(DeleteDomainArgs.builder()
                        .id(domain.getId().getId())
                        .build());
            } catch (ApiException e) {
                // ignore
            }
        });
    }

    @Test
    public void testDomainLifecycle() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");

        long timestamp = System.currentTimeMillis();

        // create 5 domains
        for (int i = 0; i < 5; i++) {
            Domain domain = new Domain();
            domain.setName("domain." + i + ".com");
            domain.setOauth2Enabled(false);
            domain.setPropagateToEdge(false);

            Domain created = client.saveDomain(SaveDomainArgs.builder()
                    .domain(domain)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(domain.getName(), created.getName());
            assertEquals(false, created.getOauth2Enabled());

            createdDomains.add(created);
        }

        // list tenant domains with text search
        PageDataDomainInfo filteredDomains = client.getDomainInfos(GetDomainInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch("domain.")
                .build());
        assertNotNull(filteredDomains);
        assertEquals(5, filteredDomains.getData().size());

        // get domain info by id
        Domain searchDomain = createdDomains.get(2);
        DomainInfo fetchedInfo = client.getDomainInfoById(GetDomainInfoByIdArgs.builder()
                .id(searchDomain.getId().getId())
                .build());
        assertEquals(searchDomain.getName(), fetchedInfo.getName());
        assertEquals(searchDomain.getOauth2Enabled(), fetchedInfo.getOauth2Enabled());
        assertNotNull(fetchedInfo.getOauth2ClientInfos());

        // update domain
        Domain domainToUpdate = createdDomains.get(3);
        domainToUpdate.setPropagateToEdge(true);
        Domain updatedDomain = client.saveDomain(SaveDomainArgs.builder()
                .domain(domainToUpdate)
                .build());
        assertEquals(true, updatedDomain.getPropagateToEdge());

        // delete domain
        UUID domainToDeleteId = createdDomains.get(0).getId().getId();
        createdDomains.remove(0);
        client.deleteDomain(DeleteDomainArgs.builder()
                .id(domainToDeleteId)
                .build());

        // verify deletion
        assertReturns404(() ->
                client.getDomainInfoById(GetDomainInfoByIdArgs.builder()
                        .id(domainToDeleteId)
                        .build())
        );

        PageDataDomainInfo domainsAfterDelete = client.getDomainInfos(GetDomainInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch("domain.")
                .build());
        assertEquals(4, domainsAfterDelete.getData().size());
    }

}
