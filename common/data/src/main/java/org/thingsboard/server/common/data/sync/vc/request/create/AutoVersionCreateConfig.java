// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.vc.request.create;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class AutoVersionCreateConfig extends VersionCreateConfig {

    @Serial
    private static final long serialVersionUID = 8245450889383315551L;

    private String branch;

    @JsonIgnore
    public AutoVersionCreateConfig copy() {
        AutoVersionCreateConfig result = new AutoVersionCreateConfig();
        result.setBranch(this.branch);
        result.setSaveAttributes(this.isSaveAttributes());
        result.setSaveRelations(this.isSaveRelations());
        result.setSaveCredentials(this.isSaveCredentials());
        result.setSavePermissions(this.isSavePermissions());
        result.setSaveGroupEntities(this.isSaveGroupEntities());
        return result;
    }

}
