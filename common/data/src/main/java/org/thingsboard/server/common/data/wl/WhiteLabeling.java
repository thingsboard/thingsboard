// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.setJson;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WhiteLabeling implements Serializable {

    private static final long serialVersionUID = 2628323657987010348L;

    private TenantId tenantId;
    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "type", max = 50)
    private WhiteLabelingType type;

    @NoXss
    @Length(fieldName = "settings", max = 10000000)
    private transient JsonNode settings;

    private DomainId domainId;

    @JsonIgnore
    private byte[] settingsBytes;

    public JsonNode getSettings() {
        return getJson(() -> settings, () -> settingsBytes);
    }

    public void setSettings(JsonNode settings) {
        setJson(settings, json -> this.settings = json, bytes -> this.settingsBytes = bytes);
    }
}
