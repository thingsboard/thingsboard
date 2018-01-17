/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.application;

import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationDao extends Dao<Application>{
    /**
     * Find application by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of application objects
     */
    List<Application> findApplicationsByTenantId(UUID tenantId, TextPageLink pageLink);


    /**
     * Find applications by tenantId and  name.
     *
     * @param tenantId the tenantId
     * @param name the application name
     * @return the optional application object
     */
    Optional<Application> findApplicationByTenantIdAndName(UUID tenantId, String name);


    /**
     * Find application by tenantId and device type.
     *
     * @param tenantId the tenantId
     * @param deviceType the device type
     * @return the list of application objects
     */
    List<Application> findApplicationByDeviceType(UUID tenantId, String deviceType);


    /**
     * Find application by tenantId and rule id.
     *
     * @param tenantId the tenantId
     * @param ruleId the ruleId
     * @return the list of applications
     */
    List<Application> findApplicationByRuleId(UUID tenantId, UUID ruleId);


    /**
     * Find application by tenantId and dashboardId.
     *
     * @param tenantId the tenantId
     * @param dashboardId the dashboardId
     * @return the list of application
     */
    List<Application> findApplicationsByDashboardId(UUID tenantId, UUID dashboardId);

}
