/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.common.data.oauth2.OAuth2MobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.common.data.oauth2.TenantNameStrategyType;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
public class OAuth2ServiceImpl extends AbstractEntityService implements OAuth2Service {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CLIENT_REGISTRATION_ID = "Incorrect clientRegistrationId ";
    public static final String INCORRECT_DOMAIN_NAME = "Incorrect domainName ";
    public static final String INCORRECT_DOMAIN_SCHEME = "Incorrect domainScheme ";

    @Autowired
    private OAuth2ParamsDao oauth2ParamsDao;
    @Autowired
    private OAuth2RegistrationDao oauth2RegistrationDao;
    @Autowired
    private OAuth2DomainDao oauth2DomainDao;
    @Autowired
    private OAuth2MobileDao oauth2MobileDao;

    @Override
    public List<OAuth2ClientInfo> getOAuth2Clients(String domainSchemeStr, String domainName, String pkgName, PlatformType platformType) {
        log.trace("Executing getOAuth2Clients [{}://{}] pkgName=[{}] platformType=[{}]", domainSchemeStr, domainName, pkgName, platformType);
        if (domainSchemeStr == null) {
            throw new IncorrectParameterException(INCORRECT_DOMAIN_SCHEME);
        }
        SchemeType domainScheme;
        try {
            domainScheme = SchemeType.valueOf(domainSchemeStr.toUpperCase());
        } catch (IllegalArgumentException e){
            throw new IncorrectParameterException(INCORRECT_DOMAIN_SCHEME);
        }
        validateString(domainName, INCORRECT_DOMAIN_NAME + domainName);
        return oauth2RegistrationDao.findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(
                Arrays.asList(domainScheme, SchemeType.MIXED), domainName, pkgName, platformType)
                .stream()
                .map(OAuth2Utils::toClientInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveOAuth2Info(OAuth2Info oauth2Info) {
        log.trace("Executing saveOAuth2Info [{}]", oauth2Info);
        oauth2InfoValidator.accept(oauth2Info);
        oauth2ParamsDao.deleteAll();
        oauth2Info.getOauth2ParamsInfos().forEach(oauth2ParamsInfo -> {
            OAuth2Params oauth2Params = OAuth2Utils.infoToOAuth2Params(oauth2Info);
            OAuth2Params savedOauth2Params = oauth2ParamsDao.save(TenantId.SYS_TENANT_ID, oauth2Params);
            oauth2ParamsInfo.getClientRegistrations().forEach(registrationInfo -> {
                OAuth2Registration registration = OAuth2Utils.toOAuth2Registration(savedOauth2Params.getId(), registrationInfo);
                oauth2RegistrationDao.save(TenantId.SYS_TENANT_ID, registration);
            });
            oauth2ParamsInfo.getDomainInfos().forEach(domainInfo -> {
                OAuth2Domain domain = OAuth2Utils.toOAuth2Domain(savedOauth2Params.getId(), domainInfo);
                oauth2DomainDao.save(TenantId.SYS_TENANT_ID, domain);
            });
            if (oauth2ParamsInfo.getMobileInfos() != null) {
                oauth2ParamsInfo.getMobileInfos().forEach(mobileInfo -> {
                    OAuth2Mobile mobile = OAuth2Utils.toOAuth2Mobile(savedOauth2Params.getId(), mobileInfo);
                    oauth2MobileDao.save(TenantId.SYS_TENANT_ID, mobile);
                });
            }
        });
    }

    @Override
    public OAuth2Info findOAuth2Info() {
        log.trace("Executing findOAuth2Info");
        OAuth2Info oauth2Info = new OAuth2Info();
        List<OAuth2Params> oauth2ParamsList = oauth2ParamsDao.find(TenantId.SYS_TENANT_ID);
        oauth2Info.setEnabled(oauth2ParamsList.stream().anyMatch(param -> param.isEnabled()));
        List<OAuth2ParamsInfo> oauth2ParamsInfos = new ArrayList<>();
        oauth2Info.setOauth2ParamsInfos(oauth2ParamsInfos);
        oauth2ParamsList.stream().sorted(Comparator.comparing(BaseData::getUuidId)).forEach(oauth2Params -> {
            List<OAuth2Registration> registrations = oauth2RegistrationDao.findByOAuth2ParamsId(oauth2Params.getId().getId());
            List<OAuth2Domain> domains = oauth2DomainDao.findByOAuth2ParamsId(oauth2Params.getId().getId());
            List<OAuth2Mobile> mobiles = oauth2MobileDao.findByOAuth2ParamsId(oauth2Params.getId().getId());
            oauth2ParamsInfos.add(OAuth2Utils.toOAuth2ParamsInfo(registrations, domains, mobiles));
        });
        return oauth2Info;
    }

    @Override
    public OAuth2Registration findRegistration(UUID id) {
        log.trace("Executing findRegistration [{}]", id);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        return oauth2RegistrationDao.findById(null, id);
    }

    @Override
    public String findAppSecret(UUID id, String pkgName) {
        log.trace("Executing findAppSecret [{}][{}]", id, pkgName);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        validateString(pkgName, "Incorrect package name");
        return oauth2RegistrationDao.findAppSecret(id, pkgName);
    }


    @Override
    public List<OAuth2Registration> findAllRegistrations() {
        log.trace("Executing findAllRegistrations");
        return oauth2RegistrationDao.find(TenantId.SYS_TENANT_ID);
    }

    private final Consumer<OAuth2Info> oauth2InfoValidator = oauth2Info -> {
        if (oauth2Info == null
                || oauth2Info.getOauth2ParamsInfos() == null) {
            throw new DataValidationException("OAuth2 param infos should be specified!");
        }
        for (OAuth2ParamsInfo oauth2Params : oauth2Info.getOauth2ParamsInfos()) {
            if (oauth2Params.getDomainInfos() == null
                    || oauth2Params.getDomainInfos().isEmpty()) {
                throw new DataValidationException("List of domain configuration should be specified!");
            }
            for (OAuth2DomainInfo domainInfo : oauth2Params.getDomainInfos()) {
                if (StringUtils.isEmpty(domainInfo.getName())) {
                    throw new DataValidationException("Domain name should be specified!");
                }
                if (domainInfo.getScheme() == null) {
                    throw new DataValidationException("Domain scheme should be specified!");
                }
            }
            oauth2Params.getDomainInfos().stream()
                    .collect(Collectors.groupingBy(OAuth2DomainInfo::getName))
                    .forEach((domainName, domainInfos) -> {
                        if (domainInfos.size() > 1 && domainInfos.stream().anyMatch(domainInfo -> domainInfo.getScheme() == SchemeType.MIXED)) {
                            throw new DataValidationException("MIXED scheme type shouldn't be combined with another scheme type!");
                        }
                        domainInfos.stream()
                                .collect(Collectors.groupingBy(OAuth2DomainInfo::getScheme))
                                .forEach((schemeType, domainInfosBySchemeType) -> {
                                    if (domainInfosBySchemeType.size() > 1) {
                                        throw new DataValidationException("Domain name and protocol must be unique within OAuth2 parameters!");
                                    }
                                });
                    });
            if (oauth2Params.getMobileInfos() != null) {
                for (OAuth2MobileInfo mobileInfo : oauth2Params.getMobileInfos()) {
                    if (StringUtils.isEmpty(mobileInfo.getPkgName())) {
                        throw new DataValidationException("Package should be specified!");
                    }
                    if (StringUtils.isEmpty(mobileInfo.getAppSecret())) {
                        throw new DataValidationException("Application secret should be specified!");
                    }
                    if (mobileInfo.getAppSecret().length() < 16) {
                        throw new DataValidationException("Application secret should be at least 16 characters!");
                    }
                }
                oauth2Params.getMobileInfos().stream()
                        .collect(Collectors.groupingBy(OAuth2MobileInfo::getPkgName))
                        .forEach((pkgName, mobileInfos) -> {
                            if (mobileInfos.size() > 1) {
                                throw new DataValidationException("Mobile app package name must be unique within OAuth2 parameters!");
                            }
                        });
            }
            if (oauth2Params.getClientRegistrations() == null || oauth2Params.getClientRegistrations().isEmpty()) {
                throw new DataValidationException("Client registrations should be specified!");
            }
            for (OAuth2RegistrationInfo clientRegistration : oauth2Params.getClientRegistrations()) {
                if (StringUtils.isEmpty(clientRegistration.getClientId())) {
                    throw new DataValidationException("Client ID should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientSecret())) {
                    throw new DataValidationException("Client secret should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAuthorizationUri())) {
                    throw new DataValidationException("Authorization uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAccessTokenUri())) {
                    throw new DataValidationException("Token uri should be specified!");
                }
                if (CollectionUtils.isEmpty(clientRegistration.getScope())) {
                    throw new DataValidationException("Scope should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getUserNameAttributeName())) {
                    throw new DataValidationException("User name attribute name should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientAuthenticationMethod())) {
                    throw new DataValidationException("Client authentication method should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getLoginButtonLabel())) {
                    throw new DataValidationException("Login button label should be specified!");
                }
                OAuth2MapperConfig mapperConfig = clientRegistration.getMapperConfig();
                if (mapperConfig == null) {
                    throw new DataValidationException("Mapper config should be specified!");
                }
                if (mapperConfig.getType() == null) {
                    throw new DataValidationException("Mapper config type should be specified!");
                }
                if (mapperConfig.getType() == MapperType.BASIC) {
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
                    if (basicConfig == null) {
                        throw new DataValidationException("Basic config should be specified!");
                    }
                    if (StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                        throw new DataValidationException("Email attribute key should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == null) {
                        throw new DataValidationException("Tenant name strategy should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                            && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                        throw new DataValidationException("Tenant name pattern should be specified!");
                    }
                }
                if (mapperConfig.getType() == MapperType.GITHUB) {
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
                    if (basicConfig == null) {
                        throw new DataValidationException("Basic config should be specified!");
                    }
                    if (!StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                        throw new DataValidationException("Email attribute key cannot be configured for GITHUB mapper type!");
                    }
                    if (basicConfig.getTenantNameStrategy() == null) {
                        throw new DataValidationException("Tenant name strategy should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                            && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                        throw new DataValidationException("Tenant name pattern should be specified!");
                    }
                }
                if (mapperConfig.getType() == MapperType.CUSTOM) {
                    OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
                    if (customConfig == null) {
                        throw new DataValidationException("Custom config should be specified!");
                    }
                    if (StringUtils.isEmpty(customConfig.getUrl())) {
                        throw new DataValidationException("Custom mapper URL should be specified!");
                    }
                }
            }
        }
    };
}
