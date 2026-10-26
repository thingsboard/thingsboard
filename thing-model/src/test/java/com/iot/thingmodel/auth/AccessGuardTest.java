package com.iot.thingmodel.auth;

import com.iot.thingmodel.common.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessGuardTest {

    private final AccessGuard guard = new AccessGuard();
    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final CurrentUser tenantAdmin = new CurrentUser("u1", tenantA.toString(), "TENANT_ADMIN", "a@b");
    private final CurrentUser systemAdmin = new CurrentUser("u2", null, "SYS_ADMIN", "root@b");

    @Test
    void tenantAdminUsesOwnTenant() {
        assertEquals(tenantA, guard.resolveWriteTenant(tenantAdmin, null));
        assertEquals(tenantA, guard.resolveWriteTenant(tenantAdmin, tenantA.toString()));
    }

    @Test
    void tenantAdminCannotWriteAnotherTenant() {
        ApiException ex = assertThrows(ApiException.class,
                () -> guard.resolveWriteTenant(tenantAdmin, tenantB.toString()));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void systemAdminMustSpecifyTenant() {
        ApiException ex = assertThrows(ApiException.class,
                () -> guard.resolveWriteTenant(systemAdmin, null));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void systemAdminCanUseRequestedTenant() {
        assertEquals(tenantB, guard.resolveWriteTenant(systemAdmin, tenantB.toString()));
    }

    @Test
    void tenantAccessIsScoped() {
        guard.checkTenantAccess(tenantAdmin, tenantA);
        guard.checkTenantAccess(systemAdmin, tenantB);

        ApiException ex = assertThrows(ApiException.class,
                () -> guard.checkTenantAccess(tenantAdmin, tenantB));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void systemAdminCheckRejectsTenantAdmin() {
        ApiException ex = assertThrows(ApiException.class,
                () -> guard.checkSystemAdmin(tenantAdmin));
        assertEquals(403, ex.getStatus().value());
    }
}
