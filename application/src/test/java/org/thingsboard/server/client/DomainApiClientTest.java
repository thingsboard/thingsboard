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
package org.thingsboard.server.client;

import org.junit.After;
import org.junit.Test;
import org.thingsboard.client.ApiException;
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
                client.deleteDomain(domain.getId().getId());
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

            Domain created = client.saveDomain(domain, null);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(domain.getName(), created.getName());
            assertEquals(false, created.getOauth2Enabled());

            createdDomains.add(created);
        }

        // list tenant domains with text search
        PageDataDomainInfo filteredDomains = client.getTenantDomainInfos(100, 0,
                "domain.", null, null);
        assertNotNull(filteredDomains);
        assertEquals(5, filteredDomains.getData().size());

        // get domain info by id
        Domain searchDomain = createdDomains.get(2);
        DomainInfo fetchedInfo = client.getDomainInfoById(searchDomain.getId().getId());
        assertEquals(searchDomain.getName(), fetchedInfo.getName());
        assertEquals(searchDomain.getOauth2Enabled(), fetchedInfo.getOauth2Enabled());
        assertNotNull(fetchedInfo.getOauth2ClientInfos());

        // update domain
        Domain domainToUpdate = createdDomains.get(3);
        domainToUpdate.setPropagateToEdge(true);
        Domain updatedDomain = client.saveDomain(domainToUpdate, null);
        assertEquals(true, updatedDomain.getPropagateToEdge());

        // delete domain
        UUID domainToDeleteId = createdDomains.get(0).getId().getId();
        createdDomains.remove(0);
        client.deleteDomain(domainToDeleteId);

        // verify deletion
        assertReturns404(() ->
                client.getDomainInfoById(domainToDeleteId)
        );

        PageDataDomainInfo domainsAfterDelete = client.getTenantDomainInfos(100, 0,
                "domain.", null, null);
        assertEquals(4, domainsAfterDelete.getData().size());
    }

}
