package org.thingsboard.server.common.data.oauth2;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class OAuth2BasicMapperConfig {
    private final String emailAttributeKey;
    private final String firstNameAttributeKey;
    private final String lastNameAttributeKey;
    private final String tenantNameStrategy;
    private final String tenantNamePattern;
    private final String customerNamePattern;
    private final String defaultDashboardName;
    private final boolean alwaysFullScreen;
}
