// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;

@Schema
@EqualsAndHashCode(callSuper = true)
public class OtaPackageExportData extends EntityExportData<OtaPackage> {

    @Override
    public EntityType getEntityType() { return EntityType.OTA_PACKAGE; }

    /*
     * OtaPackage is not a versioned entity; its 'version' field is part of the domain model (not used for optimistic locking)
     * We override both methods to ensure 'version' is not ignored during (de)serialization.
     */
    @JsonIgnoreProperties(value = {"tenantId", "createdTime"}, ignoreUnknown = true)
    @Override
    public OtaPackage getEntity() {
        return super.getEntity();
    }

    @JsonIgnoreProperties(value = {"tenantId", "createdTime"}, ignoreUnknown = true)
    @Override
    public void setEntity(OtaPackage entity) {
        super.setEntity(entity);
    }

}
