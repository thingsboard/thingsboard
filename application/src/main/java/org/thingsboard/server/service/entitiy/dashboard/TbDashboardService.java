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
package org.thingsboard.server.service.entitiy.dashboard;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface  TbDashboardService extends SimpleTbEntityService<Dashboard, DashboardId> {

    Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, Customer customer, SecurityUser user) throws ThingsboardException;

    Dashboard assignDashboardToPublicCustomer(TenantId tenantId, DashboardId dashboardId, SecurityUser user) throws ThingsboardException;

    Dashboard updateDashboardCustomers(TenantId tenantId, Dashboard dashboard, String[] strCustomerIds, SecurityUser user) throws ThingsboardException;

    Dashboard addDashboardCustomers(TenantId tenantId, Dashboard dashboard,  String[] strCustomerIds, SecurityUser user) throws ThingsboardException;

    Dashboard removeDashboardCustomers(TenantId tenantId, Dashboard dashboard,  String[] strCustomerIds, SecurityUser user) throws ThingsboardException;

    Dashboard asignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, Edge edge, SecurityUser user) throws ThingsboardException;

    Dashboard unassignDeviceFromEdge(Dashboard dashboard, Edge edge, SecurityUser user) throws ThingsboardException;

}
