// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAuthenticationProvider implements AuthenticationProvider {

    private final CustomerService customerService;
    private final UserAuthDetailsCache userAuthDetailsCache;
    private final UserPermissionsService userPermissionsService;

    protected SecurityUser authenticateByPublicId(String publicId, String authContextName, UserPrincipal userPrincipal) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException(authContextName + " is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(systemId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException(authContextName + " is not valid");
        }

        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        UserPrincipal principal = userPrincipal == null ? new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId) : userPrincipal;

        MergedUserPermissions userPermissions;
        try {
            userPermissions = userPermissionsService.getMergedPermissions(user, true);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }

        return new SecurityUser(user, true, principal, userPermissions);
    }

    protected SecurityUser authenticateByUserId(TenantId tenantId, UserId userId, AuthorityPermissionsInfo permissionsInfo, boolean ignoreDisabled) {
        UserAuthDetails userAuthDetails = userAuthDetailsCache.getUserAuthDetails(tenantId, userId);
        if (userAuthDetails == null) {
            throw new UsernameNotFoundException("User with credentials not found");
        }
        if (!userAuthDetails.credentialsEnabled() && !ignoreDisabled) {
            throw new DisabledException("User is not active");
        }

        User user = userAuthDetails.user();
        if (user.getAuthority() == null) {
            throw new InsufficientAuthenticationException("User has no authority assigned");
        }

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());

        MergedUserPermissions mergedPermissions;
        try {
            mergedPermissions = userPermissionsService.getMergedPermissions(user, false);
        } catch (Exception e) {
            log.warn("[{}] Failed to fetch merged user permissions", user.getId(), e);
            throw new BadCredentialsException("Failed to get user permissions", e);
        }

        MergedUserPermissions userPermissions;
        if (permissionsInfo != null) {
            Map<Resource, Set<Operation>> permissions = permissionsInfo.getPermissionsForAuthority(user.getAuthority());
            if (permissions != null && !permissions.isEmpty()) {
                Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions = ceilGroupPermissions(
                        mergedPermissions.getGroupPermissions(),
                        permissions
                );
                userPermissions = new MergedUserPermissions(permissions, groupPermissions);
                return new SecurityUser(user, true, userPrincipal, userPermissions);
            }
        }

        userPermissions = mergedPermissions;
        return new SecurityUser(user, true, userPrincipal, userPermissions);
    }


    /**
     * Filters the user's group permissions so that only operations allowed by the API key survive.
     * The API key's generic permission map (Resource → Set&lt;Operation&gt;) acts as a ceiling:
     * a group-specific WRITE on a DASHBOARD group, for example, is stripped out if the API key
     * does not grant WRITE (or ALL) on the DASHBOARD resource.
     * Groups whose entire operation set is removed by the filter are dropped from the result.
     * <p>
     * The ceiling check uses the entity-level resource (e.g. {@code EntityType.DEVICE → Resource.DEVICE}),
     * not the group-level resource ({@code Resource.DEVICE_GROUP}). This matches the semantics of group
     * permissions: they grant access to the <em>entities inside</em> the group, so the API key must
     * allow the entity resource, not the group resource. Consequently, an API key configured with only
     * {@code Resource.DEVICE_GROUP: READ} and no {@code Resource.DEVICE} entry will drop all device-group
     * permissions — intentional, but easy to misread.
     */
    private static Map<EntityGroupId, MergedGroupPermissionInfo> ceilGroupPermissions(
            Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions,
            Map<Resource, Set<Operation>> apiKeyPermissions
    ) {
        Map<EntityGroupId, MergedGroupPermissionInfo> result = new HashMap<>();
        for (Map.Entry<EntityGroupId, MergedGroupPermissionInfo> entry : groupPermissions.entrySet()) {
            EntityGroupId entityGroupId = entry.getKey();
            MergedGroupPermissionInfo info = entry.getValue();

            Resource resource = Resource.resourceFromEntityType(info.getEntityType());
            if (resource == null) {
                continue;
            }
            Set<Operation> filtered = intersectWithApiKey(info.getOperations(), resource, apiKeyPermissions);
            if (!filtered.isEmpty()) {
                result.put(entityGroupId, new MergedGroupPermissionInfo(info.getEntityType(), filtered));
            }
        }
        return result;
    }

    /**
     * Returns the subset of {@code groupOps} that the API key permits for {@code resource}.
     * The effective API key allowance is the union of its Resource.ALL entry and the resource-specific entry.
     * Three cases:
     * <ul>
     *   <li>API key allows ALL for this resource → group operations pass through unchanged.</li>
     *   <li>Group role grants ALL operations → constrained down to whatever the API key explicitly lists.</li>
     *   <li>Both sides list specific operations → plain set intersection.</li>
     * </ul>
     */
    private static Set<Operation> intersectWithApiKey(
            Set<Operation> groupOps, Resource resource, Map<Resource, Set<Operation>> apiKeyPermissions
    ) {
        Set<Operation> keyOps = EnumSet.noneOf(Operation.class);
        Set<Operation> allResourceOps = apiKeyPermissions.get(Resource.ALL);
        if (allResourceOps != null) {
            keyOps.addAll(allResourceOps);
        }
        Set<Operation> specificOps = apiKeyPermissions.get(resource);
        if (specificOps != null) {
            keyOps.addAll(specificOps);
        }

        if (keyOps.isEmpty()) {
            return Collections.emptySet();
        }
        if (keyOps.contains(Operation.ALL)) {
            return groupOps;
        }
        if (groupOps.contains(Operation.ALL)) {
            return keyOps;
        }
        return groupOps.stream()
                .filter(keyOps::contains)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Operation.class)));
    }
}
