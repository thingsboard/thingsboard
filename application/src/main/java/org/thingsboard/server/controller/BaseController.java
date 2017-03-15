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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
public abstract class BaseController {

    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    @Autowired
    protected RuleService ruleService;

    @Autowired
    protected PluginService pluginService;

    @Autowired
    protected ActorService actorService;


    @ExceptionHandler(ThingsboardException.class)
    public void handleThingsboardException(ThingsboardException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    private ThingsboardException handleException(Exception exception, boolean logException) {
        if (logException) {
            log.error("Error [{}]", exception.getMessage());
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
        } else {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.GENERAL);
        }
    }

    <T> T checkNotNull(T reference) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    void checkParameter(String name, String param) throws ThingsboardException {
        if (StringUtils.isEmpty(param)) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    void checkArrayParameter(String name, String[] params) throws ThingsboardException {
        if (params == null || params.length == 0) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    UUID toUUID(String id) {
        return UUID.fromString(id);
    }

    TimePageLink createPageLink(int limit, Long startTime, Long endTime, boolean ascOrder, String idOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TimePageLink(limit, startTime, endTime, ascOrder, idOffsetUuid);
    }


    TextPageLink createPageLink(int limit, String textSearch, String idOffset, String textOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TextPageLink(limit, textSearch, idOffsetUuid, textOffset);
    }

    protected SecurityUser getCurrentUser() throws ThingsboardException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

    void checkTenantId(TenantId tenantId) throws ThingsboardException {
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() != Authority.SYS_ADMIN &&
                (authUser.getTenantId() == null || !authUser.getTenantId().equals(tenantId))) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    protected TenantId getTenantId() throws ThingsboardException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerId(CustomerId customerId) throws ThingsboardException {
        try {
            validateId(customerId, "Incorrect customerId " + customerId);
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.SYS_ADMIN ||
                    (authUser.getAuthority() != Authority.TENANT_ADMIN &&
                            (authUser.getCustomerId() == null || !authUser.getCustomerId().equals(customerId)))) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            Customer customer = customerService.findCustomerById(customerId);
            checkCustomer(customer);
            return customer;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkCustomer(Customer customer) throws ThingsboardException {
        checkNotNull(customer);
        checkTenantId(customer.getTenantId());
    }

    User checkUserId(UserId userId) throws ThingsboardException {
        try {
            validateId(userId, "Incorrect userId " + userId);
            User user = userService.findUserById(userId);
            checkUser(user);
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkUser(User user) throws ThingsboardException {
        checkNotNull(user);
        checkTenantId(user.getTenantId());
        if (user.getAuthority() == Authority.CUSTOMER_USER) {
            checkCustomerId(user.getCustomerId());
        }
    }

    Device checkDeviceId(DeviceId deviceId) throws ThingsboardException {
        try {
            validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(deviceId);
            checkDevice(device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkDevice(Device device) throws ThingsboardException {
        checkNotNull(device);
        checkTenantId(device.getTenantId());
        if (device.getCustomerId() != null && !device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(device.getCustomerId());
        }
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, boolean modify) throws ThingsboardException {
        try {
            validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(widgetsBundleId);
            checkWidgetsBundle(widgetsBundle, modify);
            return widgetsBundle;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkWidgetsBundle(WidgetsBundle widgetsBundle, boolean modify) throws ThingsboardException {
        checkNotNull(widgetsBundle);
        if (widgetsBundle.getTenantId() != null && !widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetsBundle.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    WidgetType checkWidgetTypeId(WidgetTypeId widgetTypeId, boolean modify) throws ThingsboardException {
        try {
            validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
            WidgetType widgetType = widgetTypeService.findWidgetTypeById(widgetTypeId);
            checkWidgetType(widgetType, modify);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    void checkWidgetType(WidgetType widgetType, boolean modify) throws ThingsboardException {
        checkNotNull(widgetType);
        if (widgetType.getTenantId() != null && !widgetType.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetType.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    Dashboard checkDashboardId(DashboardId dashboardId) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            Dashboard dashboard = dashboardService.findDashboardById(dashboardId);
            checkDashboard(dashboard);
            return dashboard;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkDashboard(Dashboard dashboard) throws ThingsboardException {
        checkNotNull(dashboard);
        checkTenantId(dashboard.getTenantId());
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() == Authority.CUSTOMER_USER) {
            if (dashboard.getCustomerId() == null || dashboard.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
        if (dashboard.getCustomerId() != null && !dashboard.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(dashboard.getCustomerId());
        }
    }

    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            ComponentDescriptor componentDescriptor = checkNotNull(componentDescriptorService.getComponent(clazz));
            return componentDescriptor;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkPluginActionsByPluginClazz(String pluginClazz) throws ThingsboardException {
        try {
            checkComponentDescriptorByClazz(pluginClazz);
            log.debug("[{}] Lookup plugin actions", pluginClazz);
            return componentDescriptorService.getPluginActions(pluginClazz);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected PluginMetaData checkPlugin(PluginMetaData plugin) throws ThingsboardException {
        checkNotNull(plugin);
        SecurityUser authUser = getCurrentUser();
        TenantId tenantId = plugin.getTenantId();
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        if (authUser.getAuthority() != Authority.SYS_ADMIN) {
            if (authUser.getTenantId() == null ||
                    !tenantId.getId().equals(ModelConstants.NULL_UUID) && !authUser.getTenantId().equals(tenantId)) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);

            } else if (tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                plugin.setConfiguration(null);
            }
        }
        return plugin;
    }

    protected RuleMetaData checkRule(RuleMetaData rule) throws ThingsboardException {
        checkNotNull(rule);
        SecurityUser authUser = getCurrentUser();
        TenantId tenantId = rule.getTenantId();
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        if (authUser.getAuthority() != Authority.SYS_ADMIN) {
            if (authUser.getTenantId() == null ||
                    !tenantId.getId().equals(ModelConstants.NULL_UUID) && !authUser.getTenantId().equals(tenantId)) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);

            }
        }
        return rule;
    }

    protected String constructBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        if (request.getHeader("x-forwarded-proto") != null) {
            scheme = request.getHeader("x-forwarded-proto");
        }
        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {}
        }

        String baseUrl = String.format("%s://%s:%d",
                scheme,
                request.getServerName(),
                serverPort);
        return baseUrl;
    }
}
