/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.settings.UserDashboardAction;
import org.thingsboard.server.common.data.settings.UserDashboardsInfo;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.user.TbUserService;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.USER_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.USER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class UserController extends BaseController {

    public static final String USER_ID = "userId";
    public static final String PATHS = "paths";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    public static final String ACTIVATE_URL_PATTERN = "%s/api/noauth/activate?activateToken=%s";

    @Value("${security.user_token_access_enabled}")
    private boolean userTokenAccessEnabled;

    private final MailService mailService;
    private final JwtTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;
    private final ApplicationEventPublisher eventPublisher;
    private final TbUserService tbUserService;

    @Autowired
    private EntityQueryService entityQueryService;

    @Autowired
    private EntityService entityService;

    @ApiOperation(value = "Get User (getUserById)",
            notes = "Fetch the User object based on the provided User Id. " +
                    "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public User getUserById(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.READ);
        if (user.getAdditionalInfo().isObject()) {
            ObjectNode additionalInfo = (ObjectNode) user.getAdditionalInfo();
            processDashboardIdFromAdditionalInfo(additionalInfo, DEFAULT_DASHBOARD);
            processDashboardIdFromAdditionalInfo(additionalInfo, HOME_DASHBOARD);
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
            if (userCredentials.isEnabled() && !additionalInfo.has("userCredentialsEnabled")) {
                additionalInfo.put("userCredentialsEnabled", true);
            }
        }
        return user;
    }

    @ApiOperation(value = "Check Token Access Enabled (isUserTokenAccessEnabled)",
            notes = "Checks that the system is configured to allow administrators to impersonate themself as other users. " +
                    "If the user who performs the request has the authority of 'SYS_ADMIN', it is possible to login as any tenant administrator. " +
                    "If the user who performs the request has the authority of 'TENANT_ADMIN', it is possible to login as any customer user. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/tokenAccessEnabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isUserTokenAccessEnabled() {
        return userTokenAccessEnabled;
    }

    @ApiOperation(value = "Get User Token (getUserToken)",
            notes = "Returns the token of the User based on the provided User Id. " +
                    "If the user who performs the request has the authority of 'SYS_ADMIN', it is possible to get the token of any tenant administrator. " +
                    "If the user who performs the request has the authority of 'TENANT_ADMIN', it is possible to get the token of any customer user that belongs to the same tenant. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/token", method = RequestMethod.GET)
    @ResponseBody
    public JwtPair getUserToken(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        if (!userTokenAccessEnabled) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        UserId userId = new UserId(toUUID(strUserId));
        SecurityUser authUser = getCurrentUser();
        User user = checkUserId(userId, Operation.READ);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        UserCredentials credentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), userId);
        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Save Or update User (saveUser)",
            notes = "Create or update the User. When creating user, platform generates User Id as " + UUID_WIKI_LINK +
                    "The newly created User Id will be present in the response. " +
                    "Specify existing User Id to update the user. " +
                    "Referencing non-existing User Id will cause 'Not Found' error." +
                    "\n\nUser email is unique for entire platform setup." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new User entity." +
                    "\n\nAvailable for users with 'SYS_ADMIN', 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public User saveUser(
            @ApiParam(value = "A JSON value representing the User.", required = true)
            @RequestBody User user,
            @ApiParam(value = "Send activation email (or use activation link)", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail, HttpServletRequest request) throws ThingsboardException {
        if (!Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            user.setTenantId(getCurrentUser().getTenantId());
        }
        checkEntity(user.getId(), user, Resource.USER);
        return tbUserService.save(getTenantId(), getCurrentUser().getCustomerId(), user, sendActivationMail, request, getCurrentUser());
    }

    @ApiOperation(value = "Send or re-send the activation email",
            notes = "Force send the activation email to the user. Useful to resend the email if user has accidentally deleted it. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/sendActivationMail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void sendActivationEmail(
            @ApiParam(value = "Email of the user", required = true)
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws ThingsboardException {
        User user = checkNotNull(userService.findUserByEmail(getCurrentUser().getTenantId(), email));

        accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ,
                user.getId(), user);

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(getCurrentUser().getTenantId(), user.getId());
        if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
            String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
            String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                    userCredentials.getActivateToken());
            mailService.sendActivationEmail(activateUrl, email);
        } else {
            throw new ThingsboardException("User is already activated!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @ApiOperation(value = "Get the activation link (getActivationLink)",
            notes = "Get the activation link for the user. " +
                    "The base url for activation link is configurable in the general settings of system administrator. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/activationLink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getActivationLink(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.READ);
        SecurityUser authUser = getCurrentUser();
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), user.getId());
        if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
            String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
            String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                    userCredentials.getActivateToken());
            return activateUrl;
        } else {
            throw new ThingsboardException("User is already activated!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @ApiOperation(value = "Delete User (deleteUser)",
            notes = "Deletes the User, it's credentials and all the relations (from and to the User). " +
                    "Referencing non-existing User Id will cause an error. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteUser(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.DELETE);
        if (user.getAuthority() == Authority.SYS_ADMIN && getCurrentUser().getId().equals(userId)) {
            throw new ThingsboardException("Sysadmin is not allowed to delete himself", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        tbUserService.delete(getTenantId(), getCurrentUser().getCustomerId(), user, getCurrentUser());
    }

    @ApiOperation(value = "Get Users (getUsers)",
            notes = "Returns a page of users owned by tenant or customer. The scope depends on authority of the user that performs the request." +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getUsers(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        if (Authority.TENANT_ADMIN.equals(currentUser.getAuthority())) {
            return checkNotNull(userService.findUsersByTenantId(currentUser.getTenantId(), pageLink));
        } else {
            return checkNotNull(userService.findCustomerUsers(currentUser.getTenantId(), currentUser.getCustomerId(), pageLink));
        }
    }

    @ApiOperation(value = "Find users by query (findUsersByQuery)",
            notes = "Returns page of user data objects. Search is been executed by email, firstName and " +
                    "lastName fields. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users/info", method = RequestMethod.GET)
    @ResponseBody
    public PageData<UserEmailInfo> findUsersByQuery(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();

        EntityTypeFilter entityFilter = new EntityTypeFilter();
        entityFilter.setEntityType(EntityType.USER);
        EntityDataPageLink pageLink = new EntityDataPageLink(pageSize, page, textSearch, createEntityDataSortOrder(sortProperty, sortOrder));
        List<EntityKey> entityFields = Arrays.asList(new EntityKey(ENTITY_FIELD, "firstName"),
                new EntityKey(ENTITY_FIELD, "lastName"),
                new EntityKey(ENTITY_FIELD, "email"));

        EntityDataQuery query = new EntityDataQuery(entityFilter, pageLink, entityFields, null, null);

        return entityQueryService.findEntityDataByQuery(securityUser, query).mapData(entityData ->
        {
            Map<String, TsValue> fieldValues = entityData.getLatest().get(ENTITY_FIELD);
            return new UserEmailInfo(UserId.fromString(entityData.getEntityId().getId().toString()),
                    fieldValues.get("email").getValue(),
                    fieldValues.get("firstName").getValue(),
                    fieldValues.get("lastName").getValue());
        });
    }

    @ApiOperation(value = "Get Tenant Users (getTenantAdmins)",
            notes = "Returns a page of users owned by tenant. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getTenantAdmins(
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(userService.findTenantAdmins(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Customer Users (getCustomerUsers)",
            notes = "Returns a page of users owned by customer. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getCustomerUsers(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(userService.findCustomerUsers(tenantId, customerId, pageLink));
    }

    @ApiOperation(value = "Enable/Disable User credentials (setUserCredentialsEnabled)",
            notes = "Enables or Disables user credentials. Useful when you would like to block user account without deleting it. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/userCredentialsEnabled", method = RequestMethod.POST)
    @ResponseBody
    public void setUserCredentialsEnabled(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId,
            @ApiParam(value = "Disable (\"true\") or enable (\"false\") the credentials.", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean userCredentialsEnabled) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.WRITE);
        TenantId tenantId = getCurrentUser().getTenantId();
        userService.setUserCredentialsEnabled(tenantId, userId, userCredentialsEnabled);

        if (!userCredentialsEnabled) {
            eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(userId));
        }
    }

    @ApiOperation(value = "Get usersForAssign (getUsersForAssign)",
            notes = "Returns page of user data objects that can be assigned to provided alarmId. " +
                    "Search is been executed by email, firstName and lastName fields. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users/assign/{alarmId}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<UserEmailInfo> getUsersForAssign(
            @ApiParam(value = ALARM_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("alarmId") String strAlarmId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            checkParameter("alarmId", strAlarmId);
            AlarmId alarmEntityId = new AlarmId(toUUID(strAlarmId));
            Alarm alarm = checkAlarmId(alarmEntityId, Operation.READ);
            SecurityUser currentUser = getCurrentUser();
            TenantId tenantId = currentUser.getTenantId();
            CustomerId originatorCustomerId = entityService.fetchEntityCustomerId(tenantId, alarm.getOriginator()).get();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            PageData<User> pageData;
            if (Authority.TENANT_ADMIN.equals(currentUser.getAuthority())) {
                if (alarm.getCustomerId() == null) {
                    pageData = userService.findTenantAdmins(tenantId, pageLink);
                } else {
                    ArrayList<CustomerId> customerIds = new ArrayList<>(Collections.singletonList(new CustomerId(CustomerId.NULL_UUID)));
                    if (!CustomerId.NULL_UUID.equals(originatorCustomerId.getId())) {
                        customerIds.add(originatorCustomerId);
                    }
                    pageData = userService.findUsersByCustomerIds(tenantId, customerIds, pageLink);
                }
            } else {
                pageData = userService.findCustomerUsers(tenantId, alarm.getCustomerId(), pageLink);
            }
            return pageData.mapData(user -> new UserEmailInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save user settings (saveUserSettings)",
            notes = "Save user settings represented in json format for authorized user. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/user/settings")
    public JsonNode saveUserSettings(@RequestBody JsonNode settings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();

        UserSettings userSettings = new UserSettings();
        userSettings.setType(UserSettingsType.GENERAL);
        userSettings.setSettings(settings);
        userSettings.setUserId(currentUser.getId());
        return userSettingsService.saveUserSettings(currentUser.getTenantId(), userSettings).getSettings();
    }

    @ApiOperation(value = "Update user settings (saveUserSettings)",
            notes = "Update user settings for authorized user. Only specified json elements will be updated." +
                    "Example: you have such settings: {A:5, B:{C:10, D:20}}. Updating it with {B:{C:10, D:30}} will result in" +
                    "{A:5, B:{C:10, D:30}}. The same could be achieved by putting {B.D:30}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/user/settings")
    public void putUserSettings(@RequestBody JsonNode settings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        userSettingsService.updateUserSettings(currentUser.getTenantId(), currentUser.getId(), UserSettingsType.GENERAL, settings);
    }

    @ApiOperation(value = "Get user settings (getUserSettings)",
            notes = "Fetch the User settings based on authorized user. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/settings")
    public JsonNode getUserSettings() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();

        UserSettings userSettings = userSettingsService.findUserSettings(currentUser.getTenantId(), currentUser.getId(), UserSettingsType.GENERAL);
        return userSettings == null ? JacksonUtil.newObjectNode() : userSettings.getSettings();
    }

    @ApiOperation(value = "Delete user settings (deleteUserSettings)",
            notes = "Delete user settings by specifying list of json element xpaths. \n " +
                    "Example: to delete B and C element in { \"A\": {\"B\": 5}, \"C\": 15} send A.B,C in jsonPaths request parameter")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/settings/{paths}", method = RequestMethod.DELETE)
    public void deleteUserSettings(@ApiParam(value = PATHS)
                                   @PathVariable(PATHS) String paths) throws ThingsboardException {
        checkParameter(USER_ID, paths);

        SecurityUser currentUser = getCurrentUser();
        userSettingsService.deleteUserSettings(currentUser.getTenantId(), currentUser.getId(), UserSettingsType.GENERAL, Arrays.asList(paths.split(",")));
    }

    @ApiOperation(value = "Update user settings (saveUserSettings)",
            notes = "Update user settings for authorized user. Only specified json elements will be updated." +
                    "Example: you have such settings: {A:5, B:{C:10, D:20}}. Updating it with {B:{C:10, D:30}} will result in" +
                    "{A:5, B:{C:10, D:30}}. The same could be achieved by putting {B.D:30}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/user/settings/{type}")
    public void putUserSettings(@ApiParam(value = "Settings type, case insensitive, one of: \"general\", \"quick_links\", \"doc_links\" or \"dashboards\".")
                                @PathVariable("type") String strType, @RequestBody JsonNode settings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        UserSettingsType type = checkEnumParameter("Settings type", strType, UserSettingsType::valueOf);
        checkNotReserved(strType, type);
        userSettingsService.updateUserSettings(currentUser.getTenantId(), currentUser.getId(), type, settings);
    }

    @ApiOperation(value = "Get user settings (getUserSettings)",
            notes = "Fetch the User settings based on authorized user. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/settings/{type}")
    public JsonNode getUserSettings(@ApiParam(value = "Settings type, case insensitive, one of: \"general\", \"quick_links\", \"doc_links\" or \"dashboards\".")
                                    @PathVariable("type") String strType) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        UserSettingsType type = checkEnumParameter("Settings type", strType, UserSettingsType::valueOf);
        checkNotReserved(strType, type);
        UserSettings userSettings = userSettingsService.findUserSettings(currentUser.getTenantId(), currentUser.getId(), type);
        return userSettings == null ? JacksonUtil.newObjectNode() : userSettings.getSettings();
    }

    @ApiOperation(value = "Delete user settings (deleteUserSettings)",
            notes = "Delete user settings by specifying list of json element xpaths. \n " +
                    "Example: to delete B and C element in { \"A\": {\"B\": 5}, \"C\": 15} send A.B,C in jsonPaths request parameter")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/settings/{type}/{paths}", method = RequestMethod.DELETE)
    public void deleteUserSettings(@ApiParam(value = PATHS)
                                   @PathVariable(PATHS) String paths,
                                   @ApiParam(value = "Settings type, case insensitive, one of: \"general\", \"quick_links\", \"doc_links\" or \"dashboards\".")
                                   @PathVariable("type") String strType) throws ThingsboardException {
        checkParameter(USER_ID, paths);
        UserSettingsType type = checkEnumParameter("Settings type", strType, UserSettingsType::valueOf);
        checkNotReserved(strType, type);
        SecurityUser currentUser = getCurrentUser();
        userSettingsService.deleteUserSettings(currentUser.getTenantId(), currentUser.getId(), type, Arrays.asList(paths.split(",")));
    }

    @ApiOperation(value = "Get information about last visited and starred dashboards (getLastVisitedDashboards)",
            notes = "Fetch the list of last visited and starred dashboards. Both lists are limited to 10 items." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/dashboards")
    public UserDashboardsInfo getUserDashboardsInfo() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        return userSettingsService.findUserDashboardsInfo(currentUser.getTenantId(), currentUser.getId());
    }

    @ApiOperation(value = "Report action of User over the dashboard (reportUserDashboardAction)",
            notes = "Report action of User over the dashboard. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/dashboards/{dashboardId}/{action}", method = RequestMethod.GET)
    @ResponseBody
    public UserDashboardsInfo reportUserDashboardAction(
            @ApiParam(value = DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DashboardController.DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "Dashboard action, one of: \"visit\", \"star\" or \"unstar\".")
            @PathVariable("action") String strAction) throws ThingsboardException {
        checkParameter(DashboardController.DASHBOARD_ID, strDashboardId);
        checkParameter("action", strAction);
        UserDashboardAction action = checkEnumParameter("Action", strAction, UserDashboardAction::valueOf);
        DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        checkDashboardInfoId(dashboardId, Operation.READ);
        SecurityUser currentUser = getCurrentUser();
        return userSettingsService.reportUserDashboardAction(currentUser.getTenantId(), currentUser.getId(), dashboardId, action);
    }

    private void checkNotReserved(String strType, UserSettingsType type) throws ThingsboardException {
        if (type.isReserved()) {
            throw new ThingsboardException("Settings with type: " + strType + " are reserved for internal use!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

}
