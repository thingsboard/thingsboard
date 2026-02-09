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
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class TenantFields extends AbstractEntityFields {

    private String additionalInfo;
    private String country;
    private String state;
    private String city;
    private String address;
    private String address2;
    private String zip;
    private String phone;
    private String email;
    private String region;

    public TenantFields(UUID id, long createdTime, String name, Long version,
                        JsonNode additionalInfo, String country, String state, String city, String address,
                        String address2, String zip, String phone, String email, String region) {
        super(id, createdTime, name, version);
        this.additionalInfo = getText(additionalInfo);
        this.country = country;
        this.state = state;
        this.city = city;
        this.address = address;
        this.address2 = address2;
        this.zip = zip;
        this.phone = phone;
        this.email = email;
        this.region = region;
    }

    @Override
    public UUID getTenantId() {
        return getId();
    }
}
