// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class BlobEntityWithCustomerInfoEntity extends AbstractBlobEntityInfoEntity<BlobEntityWithCustomerInfo> {

    public static final Map<String,String> blobEntityWithCustomerInfoColumnMap = new HashMap<>();
    static {
        blobEntityWithCustomerInfoColumnMap.put("customerTitle", "c.title");
    }

    private String customerTitle;
    private boolean customerIsPublic;

    public BlobEntityWithCustomerInfoEntity() {
        super();
    }

    public BlobEntityWithCustomerInfoEntity(BlobEntityInfoEntity blobEntityInfoEntity,
                                            String customerTitle,
                                            Object customerAdditionalInfo) {
        super(blobEntityInfoEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @Override
    public BlobEntityWithCustomerInfo toData() {
        return new BlobEntityWithCustomerInfo(super.toBlobEntityInfo(), customerTitle, customerIsPublic);
    }

}
