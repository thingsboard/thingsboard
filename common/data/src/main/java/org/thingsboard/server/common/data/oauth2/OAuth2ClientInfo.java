// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.OAuth2ClientId;

import java.util.List;

@Data
@Schema
@EqualsAndHashCode(callSuper = true)
public class OAuth2ClientInfo extends BaseData<OAuth2ClientId> implements HasName {

    @Schema(description = "Oauth2 client registration title (e.g. My google)")
    private String title;

    @Schema(description = "Oauth2 client provider name (e.g. Google)")
    private String providerName;
    @Schema(description = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    private List<PlatformType> platforms;

    public OAuth2ClientInfo() {
        super();
    }

    public OAuth2ClientInfo(OAuth2ClientId id) {
        super(id);
    }

    public OAuth2ClientInfo(OAuth2Client oAuth2Client) {
        super(oAuth2Client);
        this.title = oAuth2Client.getTitle();
        this.providerName =  oAuth2Client.getAdditionalInfoField("providerName", JsonNode::asText,"");
        this.platforms = oAuth2Client.getPlatforms();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }
}
