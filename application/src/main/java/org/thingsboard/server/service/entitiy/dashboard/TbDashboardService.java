// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.dashboard;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;

import java.util.Set;

public interface TbDashboardService extends SimpleTbEntityService<Dashboard> {

    Dashboard assignDashboardToCustomer(Dashboard dashboard, Customer customer, User user) throws ThingsboardException;

    Dashboard assignDashboardToPublicCustomer(Dashboard dashboard, User user) throws ThingsboardException;

    Dashboard unassignDashboardFromPublicCustomer(Dashboard dashboard, User user) throws ThingsboardException;

    Dashboard updateDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws ThingsboardException;

    Dashboard addDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws ThingsboardException;

    Dashboard removeDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws ThingsboardException;

    Dashboard asignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, Edge edge, User user) throws ThingsboardException;

    Dashboard unassignDashboardFromEdge(Dashboard dashboard, Edge edge, User user) throws ThingsboardException;

    Dashboard unassignDashboardFromCustomer(Dashboard dashboard, Customer customer, User user) throws ThingsboardException;

}
