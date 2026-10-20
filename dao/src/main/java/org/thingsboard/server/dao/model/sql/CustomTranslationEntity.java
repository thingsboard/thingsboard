// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.io.Serializable;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_TRANSLATION_LOCALE_CODE;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.CUSTOM_TRANSLATION_TABLE_NAME)
@IdClass(CustomTranslationCompositeKey.class)
public class CustomTranslationEntity implements ToData<CustomTranslation>, Serializable {

    @Id
    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Id
    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Id
    @Column(name = CUSTOM_TRANSLATION_LOCALE_CODE)
    private String localeCode;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CUSTOM_TRANSLATION_VALUE)
    private JsonNode value;

    public CustomTranslationEntity(CustomTranslation customTranslation) {
        this.tenantId = customTranslation.getTenantId().getId();
        if (customTranslation.getCustomerId() != null) {
            this.customerId = customTranslation.getCustomerId().getId();
        } else {
            this.customerId = EntityId.NULL_UUID;
        }
        this.localeCode = customTranslation.getLocaleCode();
        if (customTranslation.getValue() != null) {
            this.value = customTranslation.getValue();
        }
    }

    @Override
    public CustomTranslation toData() {
        CustomTranslation customTranslation = new CustomTranslation();
        customTranslation.setTenantId(TenantId.fromUUID(tenantId));
        if (!EntityId.NULL_UUID.equals(customerId)) {
            customTranslation.setCustomerId(new CustomerId(customerId));
        }
        customTranslation.setLocaleCode(localeCode);
        if (value != null) {
            customTranslation.setValue(value);
        }
        return customTranslation;
    }
}
