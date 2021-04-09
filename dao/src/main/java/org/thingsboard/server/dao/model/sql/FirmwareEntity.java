/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_CHECKSUM_ALGORITHM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_CHECKSUM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_CONTENT_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_DATA_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_FILE_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TITLE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_VERSION_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = FIRMWARE_TABLE_NAME)
public class FirmwareEntity extends BaseSqlEntity<Firmware> implements SearchTextEntity<Firmware> {

    @Column(name = FIRMWARE_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = FIRMWARE_TITLE_COLUMN)
    private String title;

    @Column(name = FIRMWARE_VERSION_COLUMN)
    private String version;

    @Column(name = FIRMWARE_FILE_NAME_COLUMN)
    private String fileName;

    @Column(name = FIRMWARE_CONTENT_TYPE_COLUMN)
    private String contentType;

    @Column(name = FIRMWARE_CHECKSUM_ALGORITHM_COLUMN)
    private String checksumAlgorithm;

    @Column(name = FIRMWARE_CHECKSUM_COLUMN)
    private String checksum;

    @Column(name = FIRMWARE_DATA_COLUMN, columnDefinition = "BINARY")
    private byte[] data;

    @Type(type = "json")
    @Column(name = ModelConstants.FIRMWARE_ADDITIONAL_INFO_COLUMN)
    private JsonNode additionalInfo;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    public FirmwareEntity() {
        super();
    }

    public FirmwareEntity(Firmware firmware) {
        this.createdTime = firmware.getCreatedTime();
        this.setUuid(firmware.getUuidId());
        this.tenantId = firmware.getTenantId().getId();
        this.title = firmware.getTitle();
        this.version = firmware.getVersion();
        this.fileName = firmware.getFileName();
        this.contentType = firmware.getContentType();
        this.checksumAlgorithm = firmware.getChecksumAlgorithm();
        this.checksum = firmware.getChecksum();
        this.data = firmware.getData().array();
        this.additionalInfo = firmware.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Firmware toData() {
        Firmware firmware = new Firmware(new FirmwareId(id));
        firmware.setCreatedTime(createdTime);
        firmware.setTenantId(new TenantId(tenantId));
        firmware.setTitle(title);
        firmware.setVersion(version);
        firmware.setFileName(fileName);
        firmware.setContentType(contentType);
        firmware.setChecksumAlgorithm(checksumAlgorithm);
        firmware.setChecksum(checksum);
        if (data != null) {
            firmware.setData(ByteBuffer.wrap(data));
        }
        firmware.setAdditionalInfo(additionalInfo);
        return firmware;
    }
}
