// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecurityUser extends User {

    private static final long serialVersionUID = -797397440703066079L;

    private Collection<GrantedAuthority> authorities;
    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private UserPrincipal userPrincipal;
    @Getter @Setter
    private MergedUserPermissions userPermissions;
    @Getter @Setter
    private String sessionId = UUID.randomUUID().toString();
    @Getter @Setter
    private String clientAddress;

    public SecurityUser() {
        super();
    }

    public SecurityUser(UserId id) {
        super(id);
    }

    public SecurityUser(User user, boolean enabled, UserPrincipal userPrincipal, MergedUserPermissions userPermissions) {
        super(user);
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
        this.userPermissions = userPermissions;
    }

    public Collection<GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            authorities = Stream.of(SecurityUser.this.getAuthority())
                    .map(authority -> new SimpleGrantedAuthority(authority.name()))
                    .collect(Collectors.toList());
        }
        return authorities;
    }

}
