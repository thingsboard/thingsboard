package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code secret} is set only once {@code activated} is true. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommunityGrantLicenseClaimResponse(boolean activated, String secret) {
}
