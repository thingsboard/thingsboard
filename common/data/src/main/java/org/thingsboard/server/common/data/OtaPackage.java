// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.Serial;
import java.nio.ByteBuffer;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class OtaPackage extends OtaPackageInfo {

    @Serial
    private static final long serialVersionUID = 3091601761339422546L;

    @Schema(description = "OTA Package data.", accessMode = Schema.AccessMode.READ_ONLY)
    private transient ByteBuffer data;

    public OtaPackage() {
        super();
    }

    public OtaPackage(OtaPackageId id) {
        super(id);
    }

    public OtaPackage(OtaPackage otaPackage) {
        super(otaPackage);
        this.data = otaPackage.getData();
    }

    public OtaPackage(OtaPackageInfo otaPackageInfo) {
        super(otaPackageInfo);
        this.data = null;
    }

}
