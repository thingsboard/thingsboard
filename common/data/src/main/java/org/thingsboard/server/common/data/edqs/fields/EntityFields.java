// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface EntityFields {

    Logger log = LoggerFactory.getLogger(EntityFields.class);

    UUID getId();

    default UUID getTenantId() {
        return null;
    }

    default UUID getCustomerId() {
        return null;
    }

    default List<UUID> getAssignedCustomerIds() {
        return Collections.emptyList();
    }

    default long getCreatedTime() {
        return 0;
    }

    default String getName() {
        return "";
    }

    default String getType() {
        return "";
    }

    default String getLabel() {
        return "";
    }

    default String getAdditionalInfo() {
        return "";
    }

    default String getEmail() {
        return "";
    }

    default String getCountry() {
        return "";
    }

    default String getState() {
        return "";
    }

    default String getCity() {
        return "";
    }

    default String getAddress() {
        return "";
    }

    default String getAddress2() {
        return "";
    }

    default String getZip() {
        return "";
    }

    default String getPhone() {
        return "";
    }

    default String getRegion() {
        return "";
    }

    default String getFirstName() {
        return "";
    }

    default String getLastName() {
        return "";
    }

    default boolean isEdgeTemplate() {
        return false;
    }

    default String getConfiguration() {
        return "";
    }

    default String getSchedule() {
        return "";
    }

    default EntityId getOriginatorId() {
        return null;
    }

    default String getQueueName() {
        return "";
    }

    default String getServiceId() {
        return "";
    }

    default boolean isDefault() {
        return false;
    }

    default UUID getOwnerId() {
        return null;
    }

    default Long getVersion() {
        return null;
    }

    default String getAsString(String key) {
        return switch (key) {
            case "id" -> getId().toString();
            case "createdTime" -> Long.toString(getCreatedTime());
            case "title" -> getName();
            case "type" -> getType();
            case "label" -> getLabel();
            case "additionalInfo" -> getAdditionalInfo();
            case "email" -> getEmail();
            case "country" -> getCountry();
            case "state" -> getState();
            case "city" -> getCity();
            case "address" -> getAddress();
            case "address2" -> getAddress2();
            case "zip" -> getZip();
            case "phone" -> getPhone();
            case "region" -> getRegion();
            case "firstName" -> getFirstName();
            case "lastName" -> getLastName();
            case "edgeTemplate" -> Boolean.toString(isEdgeTemplate());
            case "configuration" -> getConfiguration();
            case "schedule" -> getSchedule();
            case "originatorId" -> getOriginatorId().getId().toString();
            case "originatorType" -> getOriginatorId().getEntityType().toString();
            case "queueName" -> getQueueName();
            case "serviceId" -> getServiceId();
            default -> {
                log.warn("Unknown field '{}'", key);
                yield null;
            }
        };
    }

}
