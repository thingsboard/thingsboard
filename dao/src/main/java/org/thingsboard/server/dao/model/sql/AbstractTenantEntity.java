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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractTenantEntity<T extends Tenant> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

    @Column(name = ModelConstants.TENANT_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.TENANT_REGION_PROPERTY)
    private String region;

    @Column(name = ModelConstants.COUNTRY_PROPERTY)
    private String country;

    @Column(name = ModelConstants.STATE_PROPERTY)
    private String state;

    @Column(name = ModelConstants.CITY_PROPERTY)
    private String city;

    @Column(name = ModelConstants.ADDRESS_PROPERTY)
    private String address;

    @Column(name = ModelConstants.ADDRESS2_PROPERTY)
    private String address2;

    @Column(name = ModelConstants.ZIP_PROPERTY)
    private String zip;

    @Column(name = ModelConstants.PHONE_PROPERTY)
    private String phone;

    @Column(name = ModelConstants.EMAIL_PROPERTY)
    private String email;

    @Type(type = "json")
    @Column(name = ModelConstants.TENANT_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.TENANT_TENANT_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantProfileId;

    public AbstractTenantEntity() {
        super();
    }

    public AbstractTenantEntity(Tenant tenant) {
        if (tenant.getId() != null) {
            this.setUuid(tenant.getId().getId());
        }
        this.setCreatedTime(tenant.getCreatedTime());
        this.title = tenant.getTitle();
        this.region = tenant.getRegion();
        this.country = tenant.getCountry();
        this.state = tenant.getState();
        this.city = tenant.getCity();
        this.address = tenant.getAddress();
        this.address2 = tenant.getAddress2();
        this.zip = tenant.getZip();
        this.phone = tenant.getPhone();
        this.email = tenant.getEmail();
        this.additionalInfo = tenant.getAdditionalInfo();
        if (tenant.getTenantProfileId() != null) {
            this.tenantProfileId = tenant.getTenantProfileId().getId();
        }
    }

    public AbstractTenantEntity(TenantEntity tenantEntity) {
        this.setId(tenantEntity.getId());
        this.setCreatedTime(tenantEntity.getCreatedTime());
        this.title = tenantEntity.getTitle();
        this.region = tenantEntity.getRegion();
        this.country = tenantEntity.getCountry();
        this.state = tenantEntity.getState();
        this.city = tenantEntity.getCity();
        this.address = tenantEntity.getAddress();
        this.address2 = tenantEntity.getAddress2();
        this.zip = tenantEntity.getZip();
        this.phone = tenantEntity.getPhone();
        this.email = tenantEntity.getEmail();
        this.additionalInfo = tenantEntity.getAdditionalInfo();
        this.tenantProfileId = tenantEntity.getTenantProfileId();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    protected Tenant toTenant() {
        Tenant tenant = new Tenant(new TenantId(this.getUuid()));
        tenant.setCreatedTime(createdTime);
        tenant.setTitle(title);
        tenant.setRegion(region);
        tenant.setCountry(country);
        tenant.setState(state);
        tenant.setCity(city);
        tenant.setAddress(address);
        tenant.setAddress2(address2);
        tenant.setZip(zip);
        tenant.setPhone(phone);
        tenant.setEmail(email);
        tenant.setAdditionalInfo(additionalInfo);
        if (tenantProfileId != null) {
            tenant.setTenantProfileId(new TenantProfileId(tenantProfileId));
        }
        return tenant;
    }


}
