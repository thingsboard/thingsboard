// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.blob;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.BlobEntityId;

@Data
public class BlobEntityWithCustomerInfo extends BlobEntityInfo {

    @Schema(description = "Title of the customer", example = "Company A")
    private String customerTitle;

    @Schema(description = "Parameter that specifies if customer is public", accessMode = Schema.AccessMode.READ_ONLY, type = "boolean")
    private boolean customerIsPublic;

    public BlobEntityWithCustomerInfo() {
        super();
    }

    public BlobEntityWithCustomerInfo(BlobEntityId blobEntityId) {
        super(blobEntityId);
    }

    public BlobEntityWithCustomerInfo(BlobEntityInfo blobEntityInfo, String customerTitle, boolean customerIsPublic) {
        super(blobEntityInfo);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }

}
