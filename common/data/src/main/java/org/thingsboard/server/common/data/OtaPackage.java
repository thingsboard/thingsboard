/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.InputStream;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(value = { "data" })
public class OtaPackage extends OtaPackageInfo {

    private static final long serialVersionUID = 3091601761339422546L;

    @ApiModelProperty(position = 16, value = "OTA Package data.", readOnly = true)
    private transient InputStream data;

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
}
