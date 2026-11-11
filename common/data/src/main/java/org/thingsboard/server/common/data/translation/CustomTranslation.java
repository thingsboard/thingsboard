// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.translation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.setJson;

@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder=true)
@EqualsAndHashCode
@Slf4j
public class CustomTranslation implements Serializable {

    private TenantId tenantId;
    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "localeCode", max = 5)
    private String localeCode;

    @NoXss
    @Length(fieldName = "value", max = 1000000)
    private transient JsonNode value;

    @JsonIgnore
    private byte[] valueBytes;

    public JsonNode getValue() {
        return getJson(() -> value, () -> valueBytes);
    }

    public void setValue(JsonNode value) {
        setJson(value, json -> this.value = json, bytes -> this.valueBytes = bytes);
    }

}
