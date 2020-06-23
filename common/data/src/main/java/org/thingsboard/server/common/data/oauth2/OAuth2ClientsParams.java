package org.thingsboard.server.common.data.oauth2;

import lombok.*;

import java.util.List;

@EqualsAndHashCode
@Data
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ClientsParams {
    private String domainName;
    private String adminSettingsId;

    private List<OAuth2ClientRegistration> clientRegistrations;
}