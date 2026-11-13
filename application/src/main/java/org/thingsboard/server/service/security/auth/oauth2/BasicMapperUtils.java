// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicMapperUtils {
    private static final String START_PLACEHOLDER_PREFIX = "%{";
    private static final String END_PLACEHOLDER_PREFIX = "}";

    public static OAuth2User getOAuth2User(String email, Map<String, Object> attributes, OAuth2MapperConfig config) {
        OAuth2User oauth2User = new OAuth2User();
        oauth2User.setEmail(email);
        oauth2User.setTenantName(getTenantName(email, attributes, config));
        if (!StringUtils.isEmpty(config.getBasic().getLastNameAttributeKey())) {
            String lastName = getStringAttributeByKey(attributes, config.getBasic().getLastNameAttributeKey());
            oauth2User.setLastName(lastName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getFirstNameAttributeKey())) {
            String firstName = getStringAttributeByKey(attributes, config.getBasic().getFirstNameAttributeKey());
            oauth2User.setFirstName(firstName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getCustomerNamePattern())) {
            StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
            String customerName = sub.replace(config.getBasic().getCustomerNamePattern());
            oauth2User.setCustomerName(customerName);
        }
        oauth2User.setAlwaysFullScreen(config.getBasic().isAlwaysFullScreen());
        if (!StringUtils.isEmpty(config.getBasic().getDefaultDashboardName())) {
            oauth2User.setDefaultDashboardName(config.getBasic().getDefaultDashboardName());
        }
        if (!StringUtils.isEmpty(config.getBasic().getParentCustomerNamePattern())) {
            StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
            String parentCustomerName = sub.replace(config.getBasic().getParentCustomerNamePattern());
            oauth2User.setParentCustomerName(parentCustomerName);
        }
        if (config.getBasic().getUserGroupsNamePattern() != null && !config.getBasic().getUserGroupsNamePattern().isEmpty()) {
            List<String> userGroupNamePatterns = config.getBasic().getUserGroupsNamePattern();
            List<String> userGroups = new ArrayList<>();
            for (String userGroupNamePattern : userGroupNamePatterns) {
                StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
                String userGroupName = sub.replace(userGroupNamePattern);
                userGroups.add(userGroupName);
            }
            oauth2User.setUserGroups(userGroups);
        }
        return oauth2User;
    }

    public static String getTenantName(String email, Map<String, Object> attributes, OAuth2MapperConfig config) {
        switch (config.getBasic().getTenantNameStrategy()) {
            case EMAIL:
                return email;
            case DOMAIN:
                return email.substring(email .indexOf("@") + 1);
            case CUSTOM:
                StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
                return sub.replace(config.getBasic().getTenantNamePattern());
            default:
                throw new RuntimeException("Tenant Name Strategy with type " + config.getBasic().getTenantNameStrategy() + " is not supported!");
        }
    }

    public static String getStringAttributeByKey(Map<String, Object> attributes, String key) {
        String result = null;
        try {
            result = (String) attributes.get(key);
        } catch (Exception e) {
            log.warn("Can't convert attribute to String by key " + key);
        }
        return result;
    }
}
