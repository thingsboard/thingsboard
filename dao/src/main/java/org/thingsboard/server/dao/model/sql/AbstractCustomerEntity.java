// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractCustomerEntity<T extends Customer> extends BaseVersionedEntity<T> {

    public static final Map<String, String> customerColumnMap = new HashMap<>();

    static {
        customerColumnMap.put("name", "title");
    }

    @Column(name = ModelConstants.CUSTOMER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.CUSTOMER_PARENT_CUSTOMER_ID_PROPERTY)
    private UUID parentCustomerId;

    @Column(name = ModelConstants.CUSTOMER_TITLE_PROPERTY)
    private String title;

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

    @Column(name = ModelConstants.CUSTOMER_IS_PUBLIC_PROPERTY)
    private boolean isPublic;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CUSTOMER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    @Column(name = ModelConstants.CUSTOM_MENU_ID_PROPERTY)
    private UUID customMenuId;

    public AbstractCustomerEntity() {
        super();
    }

    public AbstractCustomerEntity(T customer) {
        super(customer);
        this.tenantId = customer.getTenantId().getId();
        if (customer.getParentCustomerId() != null) {
            this.parentCustomerId = customer.getParentCustomerId().getId();
        }
        this.title = customer.getTitle();
        this.country = customer.getCountry();
        this.state = customer.getState();
        this.city = customer.getCity();
        this.address = customer.getAddress();
        this.address2 = customer.getAddress2();
        this.zip = customer.getZip();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
        this.additionalInfo = customer.getAdditionalInfo();
        this.isPublic = customer.isPublic();
        if (customer.getExternalId() != null) {
            this.externalId = customer.getExternalId().getId();
        }
        if (customer.getCustomMenuId() != null) {
            this.customMenuId = customer.getCustomMenuId().getId();
        }
    }

    public AbstractCustomerEntity(CustomerEntity customerEntity) {
        super(customerEntity);
        this.tenantId = customerEntity.getTenantId();
        this.parentCustomerId = customerEntity.getParentCustomerId();
        this.title = customerEntity.getTitle();
        this.country = customerEntity.getCountry();
        this.state = customerEntity.getState();
        this.city = customerEntity.getCity();
        this.address = customerEntity.getAddress();
        this.address2 = customerEntity.getAddress2();
        this.zip = customerEntity.getZip();
        this.phone = customerEntity.getPhone();
        this.email = customerEntity.getEmail();
        this.additionalInfo = customerEntity.getAdditionalInfo();
        this.isPublic = customerEntity.isPublic();
        this.externalId = customerEntity.getExternalId();
        this.customMenuId = customerEntity.getCustomMenuId();
    }

    protected Customer toCustomer() {
        Customer customer = new Customer(new CustomerId(this.getUuid()));
        customer.setCreatedTime(createdTime);
        customer.setVersion(version);
        customer.setTenantId(TenantId.fromUUID(tenantId));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTitle(title);
        customer.setCountry(country);
        customer.setState(state);
        customer.setCity(city);
        customer.setAddress(address);
        customer.setAddress2(address2);
        customer.setZip(zip);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            customer.setExternalId(new CustomerId(externalId));
        }
        if (customMenuId != null) {
            customer.setCustomMenuId(new CustomMenuId(customMenuId));
        }
        return customer;
    }

}
