// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteDomainArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDomainArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveWebSelfRegistrationParamsArgs;
import org.thingsboard.client.model.Domain;
import org.thingsboard.client.model.DomainId;
import org.thingsboard.client.model.SelfRegistrationParams;
import org.thingsboard.client.model.SignUpField;
import org.thingsboard.client.model.SignUpFieldId;
import org.thingsboard.client.model.V2CaptchaParams;
import org.thingsboard.client.model.WebSelfRegistrationParams;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class SelfRegistrationApiClientTest extends AbstractApiClientTest {

    @Test
    public void testWebSelfRegistrationParamsLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        Domain domain = createDomain("selfreg-" + ts + ".example.com");
        UUID domainId = domain.getId().getId();

        WebSelfRegistrationParams saved =
                client.saveWebSelfRegistrationParams(SaveWebSelfRegistrationParamsArgs.builder()
                        .webSelfRegistrationParams(buildParams(domainId, "Test Registration " + ts))
                        .build());
        assertNotNull(saved);
        assertEquals("Test Registration " + ts, saved.getTitle());

        SelfRegistrationParams fetched = client.getWebSelfRegistrationParams();
        assertNotNull(fetched);
        assertEquals("Test Registration " + ts, fetched.getTitle());

        WebSelfRegistrationParams updatedParams = buildParams(domainId, "Updated Registration " + ts);
        WebSelfRegistrationParams updated = client.saveWebSelfRegistrationParams(SaveWebSelfRegistrationParamsArgs.builder()
                .webSelfRegistrationParams(updatedParams)
                .build());
        assertNotNull(updated);
        assertEquals("Updated Registration " + ts, updated.getTitle());

        SelfRegistrationParams refetched = client.getWebSelfRegistrationParams();
        assertEquals("Updated Registration " + ts, refetched.getTitle());

        client.deleteWebSelfRegistrationParams();

        client.deleteDomain(DeleteDomainArgs.builder()
                .id(domainId)
                .build());
    }

    private Domain createDomain(String name) throws ApiException {
        Domain domain = new Domain();
        domain.setName(name);
        return client.saveDomain(SaveDomainArgs.builder()
                .domain(domain)
                .build());
    }

    private WebSelfRegistrationParams buildParams(UUID domainId, String title) {
        V2CaptchaParams captcha = new V2CaptchaParams()
                .siteKey("test-site-key")
                .secretKey("test-secret-key");

        SignUpField emailField = new SignUpField()
                .id(SignUpFieldId.EMAIL)
                .label("Email");
        SignUpField passwordField = new SignUpField()
                .id(SignUpFieldId.PASSWORD)
                .label("Password");

        WebSelfRegistrationParams params = new WebSelfRegistrationParams();
        params.setTitle(title);
        params.setCaptcha(captcha);
        params.setSignUpFields(List.of(emailField, passwordField));
        params.setPermissions(List.of());
        params.setDomainId(new DomainId().id(domainId));
        params.setPrivacyPolicy("Test privacy policy text");
        params.setTermsOfUse("Test terms of use text");
        return params;
    }

}
