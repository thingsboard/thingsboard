/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2Service;

import java.util.*;
import java.util.stream.Collectors;

public class BaseOAuth2ServiceTest extends AbstractServiceTest {
    private static final OAuth2ClientsParams EMPTY_PARAMS = new OAuth2ClientsParams(false, new ArrayList<>());

    @Autowired
    protected OAuth2Service oAuth2Service;

    @Before
    public void beforeRun() {
        Assert.assertTrue(oAuth2Service.findAllClientRegistrationInfos().isEmpty());
    }

    @After
    public void after() {
        oAuth2Service.saveOAuth2Params(EMPTY_PARAMS);
        Assert.assertTrue(oAuth2Service.findAllClientRegistrationInfos().isEmpty());
        Assert.assertTrue(oAuth2Service.findOAuth2Params().getDomainsParams().isEmpty());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveHttpAndMixedDomainsTogether() {
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build()
        ));
        oAuth2Service.saveOAuth2Params(clientsParams);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveHttpsAndMixedDomainsTogether() {
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTPS).build(),
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build()
        ));
        oAuth2Service.saveOAuth2Params(clientsParams);
    }

    @Test
    public void testCreateAndFindParams() {
        OAuth2ClientsParams clientsParams = createDefaultClientsParams();
        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundClientsParams);
        // TODO ask if it's safe to check equality on AdditionalProperties
        Assert.assertEquals(clientsParams, foundClientsParams);
    }

    @Test
    public void testDisableParams() {
        OAuth2ClientsParams clientsParams = createDefaultClientsParams();
        clientsParams.setEnabled(true);
        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundClientsParams);
        Assert.assertEquals(clientsParams, foundClientsParams);

        clientsParams.setEnabled(false);
        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundDisabledClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertEquals(clientsParams, foundDisabledClientsParams);
    }

    @Test
    public void testClearDomainParams() {
        OAuth2ClientsParams clientsParams = createDefaultClientsParams();
        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundClientsParams);
        Assert.assertEquals(clientsParams, foundClientsParams);

        oAuth2Service.saveOAuth2Params(EMPTY_PARAMS);
        OAuth2ClientsParams foundAfterClearClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundAfterClearClientsParams);
        Assert.assertEquals(EMPTY_PARAMS, foundAfterClearClientsParams);
    }

    @Test
    public void testUpdateClientsParams() {
        OAuth2ClientsParams clientsParams = createDefaultClientsParams();
        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundClientsParams);
        Assert.assertEquals(clientsParams, foundClientsParams);

        OAuth2ClientsParams newClientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("another-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("test-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto()
                        ))
                        .build()
        ));
        oAuth2Service.saveOAuth2Params(newClientsParams);
        OAuth2ClientsParams foundAfterUpdateClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundAfterUpdateClientsParams);
        Assert.assertEquals(newClientsParams, foundAfterUpdateClientsParams);
    }

    @Test
    public void testGetOAuth2Clients() {
        List<ClientRegistrationDto> firstGroup = Lists.newArrayList(
                validClientRegistrationDto(),
                validClientRegistrationDto(),
                validClientRegistrationDto(),
                validClientRegistrationDto()
        );
        List<ClientRegistrationDto> secondGroup = Lists.newArrayList(
                validClientRegistrationDto(),
                validClientRegistrationDto()
        );
        List<ClientRegistrationDto> thirdGroup = Lists.newArrayList(
                validClientRegistrationDto()
        );
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(firstGroup)
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(secondGroup)
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTPS).build(),
                                DomainInfo.builder().name("fifth-domain").scheme(SchemeType.HTTP).build()
                        ))
                        .clientRegistrations(thirdGroup)
                        .build()
        ));

        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundClientsParams);
        Assert.assertEquals(clientsParams, foundClientsParams);

        List<OAuth2ClientInfo> firstGroupClientInfos = firstGroup.stream()
                .map(clientRegistrationDto -> new OAuth2ClientInfo(
                        clientRegistrationDto.getLoginButtonLabel(), clientRegistrationDto.getLoginButtonIcon(), null))
                .collect(Collectors.toList());
        List<OAuth2ClientInfo> secondGroupClientInfos = secondGroup.stream()
                .map(clientRegistrationDto -> new OAuth2ClientInfo(
                        clientRegistrationDto.getLoginButtonLabel(), clientRegistrationDto.getLoginButtonIcon(), null))
                .collect(Collectors.toList());
        List<OAuth2ClientInfo> thirdGroupClientInfos = thirdGroup.stream()
                .map(clientRegistrationDto -> new OAuth2ClientInfo(
                        clientRegistrationDto.getLoginButtonLabel(), clientRegistrationDto.getLoginButtonIcon(), null))
                .collect(Collectors.toList());

        List<OAuth2ClientInfo> nonExistentDomainClients = oAuth2Service.getOAuth2Clients("http", "non-existent-domain");
        Assert.assertTrue(nonExistentDomainClients.isEmpty());

        List<OAuth2ClientInfo> firstDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "first-domain");
        Assert.assertEquals(firstGroupClientInfos.size(), firstDomainHttpClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    firstDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> firstDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "first-domain");
        Assert.assertTrue(firstDomainHttpsClients.isEmpty());

        List<OAuth2ClientInfo> fourthDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "fourth-domain");
        Assert.assertEquals(secondGroupClientInfos.size(), fourthDomainHttpClients.size());
        secondGroupClientInfos.forEach(secondGroupClientInfo -> {
            Assert.assertTrue(
                    fourthDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(secondGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(secondGroupClientInfo.getName()))
            );
        });
        List<OAuth2ClientInfo> fourthDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "fourth-domain");
        Assert.assertEquals(secondGroupClientInfos.size(), fourthDomainHttpsClients.size());
        secondGroupClientInfos.forEach(secondGroupClientInfo -> {
            Assert.assertTrue(
                    fourthDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(secondGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(secondGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> secondDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "second-domain");
        Assert.assertEquals(firstGroupClientInfos.size() + secondGroupClientInfos.size(), secondDomainHttpClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });
        secondGroupClientInfos.forEach(secondGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(secondGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(secondGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> secondDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "second-domain");
        Assert.assertEquals(firstGroupClientInfos.size() + thirdGroupClientInfos.size(), secondDomainHttpsClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });
        thirdGroupClientInfos.forEach(thirdGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(thirdGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(thirdGroupClientInfo.getName()))
            );
        });
    }

    @Test
    public void testGetOAuth2ClientsForHttpAndHttps() {
        List<ClientRegistrationDto> firstGroup = Lists.newArrayList(
                validClientRegistrationDto(),
                validClientRegistrationDto(),
                validClientRegistrationDto(),
                validClientRegistrationDto()
        );
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(firstGroup)
                        .build()
        ));

        oAuth2Service.saveOAuth2Params(clientsParams);
        OAuth2ClientsParams foundClientsParams = oAuth2Service.findOAuth2Params();
        Assert.assertNotNull(foundClientsParams);
        Assert.assertEquals(clientsParams, foundClientsParams);

        List<OAuth2ClientInfo> firstGroupClientInfos = firstGroup.stream()
                .map(clientRegistrationDto -> new OAuth2ClientInfo(
                        clientRegistrationDto.getLoginButtonLabel(), clientRegistrationDto.getLoginButtonIcon(), null))
                .collect(Collectors.toList());

        List<OAuth2ClientInfo> firstDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "first-domain");
        Assert.assertEquals(firstGroupClientInfos.size(), firstDomainHttpClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    firstDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> firstDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "first-domain");
        Assert.assertEquals(firstGroupClientInfos.size(), firstDomainHttpsClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    firstDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });
    }

    @Test
    public void testGetDisabledOAuth2Clients() {
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build()
        ));

        oAuth2Service.saveOAuth2Params(clientsParams);

        List<OAuth2ClientInfo> secondDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "second-domain");
        Assert.assertEquals(5, secondDomainHttpClients.size());

        clientsParams.setEnabled(false);
        oAuth2Service.saveOAuth2Params(clientsParams);

        List<OAuth2ClientInfo> secondDomainHttpDisabledClients = oAuth2Service.getOAuth2Clients("http", "second-domain");
        Assert.assertEquals(0, secondDomainHttpDisabledClients.size());
    }

    @Test
    public void testFindAllClientRegistrationInfos() {
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTPS).build(),
                                DomainInfo.builder().name("fifth-domain").scheme(SchemeType.HTTP).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto()
                        ))
                        .build()
        ));

        oAuth2Service.saveOAuth2Params(clientsParams);
        List<OAuth2ClientRegistrationInfo> foundClientRegistrationInfos = oAuth2Service.findAllClientRegistrationInfos();
        Assert.assertEquals(6, foundClientRegistrationInfos.size());
        clientsParams.getDomainsParams().stream()
                .flatMap(domainParams -> domainParams.getClientRegistrations().stream())
                .forEach(clientRegistrationDto ->
                        Assert.assertTrue(
                                foundClientRegistrationInfos.stream()
                                        .anyMatch(clientRegistrationInfo -> clientRegistrationInfo.getClientId().equals(clientRegistrationDto.getClientId()))
                        )
                );
    }

    @Test
    public void testFindClientRegistrationById() {
        OAuth2ClientsParams clientsParams = new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTPS).build(),
                                DomainInfo.builder().name("fifth-domain").scheme(SchemeType.HTTP).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto()
                        ))
                        .build()
        ));

        oAuth2Service.saveOAuth2Params(clientsParams);
        List<OAuth2ClientRegistrationInfo> clientRegistrationInfos = oAuth2Service.findAllClientRegistrationInfos();
        clientRegistrationInfos.forEach(clientRegistrationInfo -> {
            OAuth2ClientRegistrationInfo foundClientRegistrationInfo = oAuth2Service.findClientRegistrationInfo(clientRegistrationInfo.getUuidId());
            Assert.assertEquals(clientRegistrationInfo, foundClientRegistrationInfo);
        });
    }

    private OAuth2ClientsParams createDefaultClientsParams() {
        return new OAuth2ClientsParams(true, Lists.newArrayList(
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build(),
                OAuth2ClientsDomainParams.builder()
                        .domainInfos(Lists.newArrayList(
                                DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validClientRegistrationDto(),
                                validClientRegistrationDto()
                        ))
                        .build()
        ));
    }

    private ClientRegistrationDto validClientRegistrationDto() {
        return ClientRegistrationDto.builder()
                .clientId(UUID.randomUUID().toString())
                .clientSecret(UUID.randomUUID().toString())
                .authorizationUri(UUID.randomUUID().toString())
                .accessTokenUri(UUID.randomUUID().toString())
                .scope(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .userInfoUri(UUID.randomUUID().toString())
                .userNameAttributeName(UUID.randomUUID().toString())
                .jwkSetUri(UUID.randomUUID().toString())
                .clientAuthenticationMethod(UUID.randomUUID().toString())
                .loginButtonLabel(UUID.randomUUID().toString())
                .loginButtonIcon(UUID.randomUUID().toString())
                .additionalInfo(mapper.createObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .mapperConfig(
                        OAuth2MapperConfig.builder()
                                .allowUserCreation(true)
                                .activateUser(true)
                                .type(MapperType.CUSTOM)
                                .custom(
                                        OAuth2CustomMapperConfig.builder()
                                                .url(UUID.randomUUID().toString())
                                                .build()
                                )
                                .build()
                )
                .build();
    }
}
