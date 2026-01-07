/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
