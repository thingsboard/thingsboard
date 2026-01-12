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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class OtaPackageInfo extends BaseDataWithAdditionalInfo<OtaPackageId> implements HasName, HasTenantId, HasTitle, ExportableEntity<OtaPackageId> {

    @Serial
    private static final long serialVersionUID = 3168391583570815419L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the ota package can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Device Profile Id. Device Profile Id of the ota package can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private DeviceProfileId deviceProfileId;
    @Schema(description = "OTA Package type.", example = "FIRMWARE", accessMode = Schema.AccessMode.READ_ONLY)
    private OtaPackageType type;
    @Length(fieldName = "title")
    @NoXss
    @Schema(description = "OTA Package title.", example = "fw", accessMode = Schema.AccessMode.READ_ONLY)
    private String title;
    @Length(fieldName = "version")
    @NoXss
    @Schema(description = "OTA Package version.", example = "1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String version;
    @Length(fieldName = "tag")
    @NoXss
    @Schema(description = "OTA Package tag.", example = "fw_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String tag;
    @Length(fieldName = "url")
    @NoXss
    @Schema(description = "OTA Package url.", example = "http://thingsboard.org/fw/1", accessMode = Schema.AccessMode.READ_ONLY)
    private String url;
    @Schema(description = "Indicates OTA Package 'has data'. Field is returned from DB ('true' if data exists or url is set).  If OTA Package 'has data' is 'false' we can not assign the OTA Package to the Device or Device Profile.", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean hasData;
    @Length(fieldName = "file name")
    @NoXss
    @Schema(description = "OTA Package file name.", example = "fw_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String fileName;
    @NoXss
    @Length(fieldName = "contentType")
    @Schema(description = "OTA Package content type.", example = "APPLICATION_OCTET_STREAM", accessMode = Schema.AccessMode.READ_ONLY)
    private String contentType;
    @Schema(description = "OTA Package checksum algorithm.", example = "CRC32", accessMode = Schema.AccessMode.READ_ONLY)
    private ChecksumAlgorithm checksumAlgorithm;
    @Length(fieldName = "checksum", max = 1020)
    @Schema(description = "OTA Package checksum.", example = "0xd87f7e0c", accessMode = Schema.AccessMode.READ_ONLY)
    private String checksum;
    @Schema(description = "OTA Package data size.", example = "8", accessMode = Schema.AccessMode.READ_ONLY)
    private Long dataSize;

    private OtaPackageId externalId;

    public OtaPackageInfo() {
        super();
    }

    public OtaPackageInfo(OtaPackageId id) {
        super(id);
    }

    public OtaPackageInfo(OtaPackageInfo otaPackageInfo) {
        super(otaPackageInfo);
        this.tenantId = otaPackageInfo.getTenantId();
        this.deviceProfileId = otaPackageInfo.getDeviceProfileId();
        this.type = otaPackageInfo.getType();
        this.title = otaPackageInfo.getTitle();
        this.version = otaPackageInfo.getVersion();
        this.tag = otaPackageInfo.getTag();
        this.url = otaPackageInfo.getUrl();
        this.hasData = otaPackageInfo.isHasData();
        this.fileName = otaPackageInfo.getFileName();
        this.contentType = otaPackageInfo.getContentType();
        this.checksumAlgorithm = otaPackageInfo.getChecksumAlgorithm();
        this.checksum = otaPackageInfo.getChecksum();
        this.dataSize = otaPackageInfo.getDataSize();
        this.externalId = otaPackageInfo.getExternalId();
    }

    @Schema(description = "JSON object with the ota package Id. " +
            "Specify existing ota package Id to update the ota package. " +
            "Referencing non-existing ota package id will cause error. " +
            "Omit this field to create new ota package.")
    @Override
    public OtaPackageId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the ota package creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @JsonIgnore
    public boolean hasUrl() {
        return StringUtils.isNotEmpty(url);
    }

    @Schema(description = "OTA Package description.", example = "Description for the OTA Package fw_1.0")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}
