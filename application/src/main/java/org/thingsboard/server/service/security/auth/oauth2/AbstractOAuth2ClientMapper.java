// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.entitiy.tenant.TbTenantService;
import org.thingsboard.server.service.entitiy.user.TbUserService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractOAuth2ClientMapper {

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TbTenantService tbTenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TbUserService tbUserService;

    @Autowired
    protected TbTenantProfileCache tenantProfileCache;

    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;

    private final Lock userCreationLock = new ReentrantLock();

    protected SecurityUser getOrCreateSecurityUserFromOAuth2User(OAuth2User oauth2User, OAuth2Client oAuth2Client) {

        OAuth2MapperConfig config = oAuth2Client.getMapperConfig();

        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, oauth2User.getEmail());

        User user = findUserForClient(oauth2User.getEmail(), oAuth2Client);

        if (user == null && !config.isAllowUserCreation()) {
            throw new UsernameNotFoundException("User not found: " + oauth2User.getEmail());
        }

        boolean isNewUser = false;

        if (user == null) {
            userCreationLock.lock();
            try {
                user = findUserForClient(oauth2User.getEmail(), oAuth2Client);
                if (user == null) {
                    user = new User();
                    TenantId tenantId;
                    if (oAuth2Client.getTenantId().isSysTenantId()) {
                        tenantId = oauth2User.getTenantId() != null ? oauth2User.getTenantId() : getTenantId(oauth2User.getTenantName());
                    } else {
                        tenantId = oAuth2Client.getTenantId();
                    }
                    user.setTenantId(tenantId);
                    // getCustomerId creates customers and nothing here is transactional, so reject every
                    // out-of-scope id before creating anything - otherwise a refused login leaves a customer behind
                    CustomerId scopedParentCustomerId = oauth2User.getParentCustomerId() != null ?
                            checkCustomerInClientScope(tenantId, oauth2User.getParentCustomerId(), oAuth2Client) : null;
                    CustomerId scopedCustomerId = oauth2User.getCustomerId() != null ?
                            checkCustomerInClientScope(tenantId, oauth2User.getCustomerId(), oAuth2Client) : null;

                    CustomerId parentCustomerId = scopedParentCustomerId != null ? scopedParentCustomerId :
                            getCustomerId(tenantId, oauth2User.getParentCustomerName(), null, oAuth2Client);
                    CustomerId customerId = scopedCustomerId != null ? scopedCustomerId :
                            getCustomerId(tenantId, oauth2User.getCustomerName(), parentCustomerId, oAuth2Client);
                    user.setCustomerId(customerId);
                    // a customer-scoped client places the user in its own customer even when the mapper names none,
                    // so the authority has to follow the resolved customer rather than the raw provider fields
                    user.setAuthority(customerId != null && !customerId.isNullUid() ? Authority.CUSTOMER_USER : Authority.TENANT_ADMIN);
                    user.setEmail(oauth2User.getEmail());
                    user.setFirstName(oauth2User.getFirstName());
                    user.setLastName(oauth2User.getLastName());

                    ObjectNode additionalInfo = JacksonUtil.newObjectNode();

                    if (oAuth2Client.getAdditionalInfo() != null &&
                            oAuth2Client.getAdditionalInfo().has("providerName")) {
                        additionalInfo.put("authProviderName", oAuth2Client.getAdditionalInfo().get("providerName").asText());
                    }

                    user.setAdditionalInfo(additionalInfo);

                    user = tbUserService.save(tenantId, customerId, null, user, false, null, (EntityGroup) null, null);
                    if (config.isActivateUser()) {
                        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                        userService.activateUserCredentials(user.getTenantId(), userCredentials.getActivateToken(), passwordEncoder.encode(""));
                    }
                    isNewUser = true;
                }
            } catch (Exception e) {
                log.error("Can't get or create security user from oauth2 user", e);
                throw new RuntimeException("Can't get or create security user from oauth2 user", e);
            } finally {
                userCreationLock.unlock();
            }

            try {
                ListenableFuture<Void> future = addUserToUserGroups(oauth2User, user);
                future.get();
            } catch (Exception e) {
                log.error("Error while adding user to entity groups", e);
                throw new RuntimeException("Error while adding user to entity groups", e);
            }
        }

        SecurityUser securityUser;
        try {
            securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user));
        } catch (Exception e) {
            log.error("Can't get or create security user from oauth2 user", e);
            throw new RuntimeException("Can't get or create security user from oauth2 user", e);
        }

        if (isNewUser && !StringUtils.isEmpty(oauth2User.getDefaultDashboardName())) {
            TenantId tenantId = user.getTenantId();
            try {
                Optional<DashboardId> dashboardIdOpt = findDefaultDashboard(oauth2User, securityUser, tenantId);
                if (dashboardIdOpt.isPresent()) {
                    user = userService.findUserById(user.getTenantId(), user.getId());
                    JsonNode additionalInfo = user.getAdditionalInfo();
                    if (additionalInfo == null || additionalInfo instanceof NullNode) {
                        additionalInfo = JacksonUtil.newObjectNode();
                    }
                    ((ObjectNode) additionalInfo).put("defaultDashboardFullscreen", oauth2User.isAlwaysFullScreen());
                    ((ObjectNode) additionalInfo).put("defaultDashboardId", dashboardIdOpt.get().getId().toString());
                    user.setAdditionalInfo(additionalInfo);
                    user = userService.saveUser(tenantId, user);
                    securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user));
                }
            } catch (Exception e) {
                log.error("Error while setting default dashboard for user", e);
            }
        }

        try {
            return (SecurityUser) new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()).getPrincipal();
        } catch (Exception e) {
            log.error("Can't create authentication token from security user", e);
            throw new RuntimeException("Can't create authentication token from security user", e);
        }
    }

    private Optional<DashboardId> findDefaultDashboard(OAuth2User oauth2User, SecurityUser securityUser, TenantId tenantId) throws Exception {
        PageLink pageLink = new PageLink(1, 0, oauth2User.getDefaultDashboardName());
        return ownersCacheService.getGroupEntities(tenantId, securityUser,
                        EntityType.DASHBOARD, Operation.READ,
                        pageLink,
                        (groupIds) -> dashboardService.findDashboardsByEntityGroupIds(groupIds, pageLink))
                .getData()
                .stream()
                .findAny()
                .map(IdBased::getId);
    }

    private ListenableFuture<Void> addUserToUserGroups(OAuth2User oauth2User, User user) {
        List<EntityGroupId> addedEntityGroups = new ArrayList<>();
        try {
            EntityGroup allUserGroup = entityGroupService.findOrCreateUserGroup(user.getTenantId(), user.getOwnerId(), EntityGroup.GROUP_ALL_NAME, "");
            addedEntityGroups.add(allUserGroup.getId());
            if (oauth2User.getUserGroups() != null && !oauth2User.getUserGroups().isEmpty()) {
                for (String group : oauth2User.getUserGroups()) {
                    EntityGroup userGroup = entityGroupService.findOrCreateUserGroup(user.getTenantId(), user.getOwnerId(), group, "");
                    if (userGroup != null) {
                        addedEntityGroups.add(userGroup.getId());
                        entityGroupService.addEntityToEntityGroup(user.getTenantId(), userGroup.getId(), user.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Can't add user [{}] to user groups", user.getEmail(), e);
            throw new RuntimeException("Can't add user to user groups", e);
        }

        ListenableFuture<List<EntityGroupId>> future = entityGroupService.findEntityGroupsForEntityAsync(user.getTenantId(), user.getId());

        return Futures.transformAsync(future, currentEntityGroups -> {
            if (currentEntityGroups != null && !currentEntityGroups.isEmpty()) {
                for (EntityGroupId currentEntityGroupId : currentEntityGroups) {
                    if (!addedEntityGroups.contains(currentEntityGroupId)) {
                        entityGroupService.removeEntityFromEntityGroup(user.getTenantId(), currentEntityGroupId, user.getId());
                    }
                }
            }
            return Futures.immediateFuture(null);
        }, MoreExecutors.directExecutor());
    }

    private MergedUserPermissions getMergedUserPermissions(User user) {
        try {
            return userPermissionsService.getMergedPermissions(user, false);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }
    }

    private TenantId getTenantId(String name) throws Exception {
        Tenant tenant = tenantService.findTenantByName(name);
        if (tenant != null) {
            return tenant.getId();
        }
        tenant = new Tenant();
        tenant.setTitle(name);
        tenant = tbTenantService.save(tenant);
        return tenant.getId();
    }

    /**
     * The user is matched by email alone, and the email is whatever the provider chose to send. A client registered by a
     * tenant or a customer must therefore only ever resolve users within its own scope; a system client is platform-wide
     * by design.
     */
    private User findUserForClient(String email, OAuth2Client oAuth2Client) {
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        if (user == null || oAuth2Client.getTenantId().isSysTenantId()) {
            return user;
        }
        EntityId clientOwnerId = oAuth2Client.getOwnerId();
        if (user.getTenantId().equals(oAuth2Client.getTenantId())
                && ownersCacheService.getOwners(user.getTenantId(), user.getId(), user).contains(clientOwnerId)) {
            return user;
        }
        log.warn("OAuth2 client [{}] owned by [{}] cannot resolve user [{}] [{}] owned by [{}]: outside of the client scope",
                oAuth2Client.getId(), clientOwnerId, user.getId(), email, user.getOwnerId());
        throw new UsernameNotFoundException("User not found: " + email);
    }

    /**
     * A CUSTOM mapper takes the customer straight from the response of an operator-supplied endpoint, so a customer-scoped
     * client could otherwise place the new user into a sibling customer.
     */
    private CustomerId checkCustomerInClientScope(TenantId tenantId, CustomerId customerId, OAuth2Client oauth2Client) {
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new IllegalStateException("Customer with id '" + customerId.getId() + "' is not found");
        }
        if (isCustomerScoped(oauth2Client)) {
            checkCustomerIsOwnedByClient(tenantId, customer, oauth2Client);
        }
        return customer.getId();
    }

    private boolean isCustomerScoped(OAuth2Client oauth2Client) {
        return oauth2Client.getCustomerId() != null && !oauth2Client.getCustomerId().isNullUid();
    }

    private void checkCustomerIsOwnedByClient(TenantId tenantId, Customer customer, OAuth2Client oauth2Client) {
        EntityId clientOwnerId = oauth2Client.getOwnerId();
        if (!customer.getId().equals(clientOwnerId) && !ownersCacheService.getOwners(tenantId, customer.getId(), customer).contains(clientOwnerId)) {
            throw new IllegalStateException("Customer with id '" + customer.getId().getId() + "' is not owned by OAuth2 client owner");
        }
    }

    private CustomerId getCustomerId(TenantId tenantId, String customerName, CustomerId parentCustomerId, OAuth2Client oauth2Client) {
        boolean customerScopedClient = isCustomerScoped(oauth2Client);
        if (StringUtils.isEmpty(customerName)) {
            return customerScopedClient ? oauth2Client.getCustomerId() : null;
        }
        Optional<Customer> customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, customerName);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (customerScopedClient) {
                checkCustomerIsOwnedByClient(tenantId, customer, oauth2Client);
            }
            return customer.getId();
        } else {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle(customerName);
            customer.setParentCustomerId(parentCustomerId != null ? parentCustomerId :
                    (customerScopedClient ? oauth2Client.getCustomerId() : null));
            return customerService.saveCustomer(customer).getId();
        }
    }

}
