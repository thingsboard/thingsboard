// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerInfo extends Customer {

    @Valid
    @Schema(description = "Owner name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    @Valid
    @Schema(description = "Groups", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;

    public CustomerInfo() {
        super();
    }

    public CustomerInfo(Customer customer, String ownerName, List<EntityInfo> groups) {
        super(customer);
        this.ownerName = ownerName;
        this.groups = groups;
    }
}
