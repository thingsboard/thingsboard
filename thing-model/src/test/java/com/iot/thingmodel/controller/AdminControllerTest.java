package com.iot.thingmodel.controller;

import com.iot.thingmodel.admin.TenantAccountService;
import com.iot.thingmodel.auth.AccessGuard;
import com.iot.thingmodel.auth.AuthInterceptor;
import com.iot.thingmodel.auth.CurrentUser;
import com.iot.thingmodel.auth.TbUserVerifier;
import com.iot.thingmodel.common.GlobalExceptionHandler;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private final UUID tenantId = UUID.randomUUID();
    private final TbUserVerifier verifier = mock(TbUserVerifier.class);
    private final TenantAccountService accountService = mock(TenantAccountService.class);
    private final ThingsboardClientProvider clientProvider = mock(ThingsboardClientProvider.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new AdminController(new AccessGuard(), accountService, clientProvider))
            .addInterceptors(new AuthInterceptor(verifier))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @BeforeEach
    void setUp() {
        when(accountService.register(any(), anyString(), anyString()))
                .thenReturn(new TenantAccountService.MaskedAccount(tenantId, "vendor@example.com", true));
        when(accountService.findMasked(tenantId)).thenReturn(Optional.of(
                new TenantAccountService.MaskedAccount(tenantId, "vendor@example.com", true)));
    }

    @Test
    void systemAdminCanRegisterAccountAndPasswordIsNotReturned() throws Exception {
        CurrentUser admin = new CurrentUser("root", null, "SYS_ADMIN", "root@b");
        when(verifier.verify("Bearer token")).thenReturn(admin);

        mockMvc.perform(post("/api/admin/tenants/{tenantId}/account", tenantId)
                        .header("X-Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"vendor@example.com","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("vendor@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(accountService).register(eq(tenantId), eq("vendor@example.com"), eq("secret"));
        verify(clientProvider).evict(tenantId);
    }

    @Test
    void tenantAdminCannotRegisterAccount() throws Exception {
        CurrentUser tenantAdmin = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");
        when(verifier.verify("Bearer token")).thenReturn(tenantAdmin);

        mockMvc.perform(post("/api/admin/tenants/{tenantId}/account", tenantId)
                        .header("X-Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"vendor@example.com","password":"secret"}
                                """))
                .andExpect(status().isForbidden());

        verify(accountService, never()).register(any(), anyString(), anyString());
    }

    @Test
    void returnsNotFoundWhenAccountIsMissing() throws Exception {
        CurrentUser admin = new CurrentUser("root", null, "SYS_ADMIN", "root@b");
        when(verifier.verify("Bearer token")).thenReturn(admin);
        when(accountService.findMasked(tenantId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/tenants/{tenantId}/account", tenantId)
                        .header("X-Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidTenantId() throws Exception {
        CurrentUser admin = new CurrentUser("root", null, "SYS_ADMIN", "root@b");
        when(verifier.verify("Bearer token")).thenReturn(admin);

        mockMvc.perform(get("/api/admin/tenants/not-a-uuid/account")
                        .header("X-Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_ID_INVALID"));
    }

    @Test
    void systemAdminCanReadMaskedAccount() throws Exception {
        CurrentUser admin = new CurrentUser("root", null, "SYS_ADMIN", "root@b");
        when(verifier.verify("Bearer token")).thenReturn(admin);

        mockMvc.perform(get("/api/admin/tenants/{tenantId}/account", tenantId)
                        .header("X-Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("vendor@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(accountService).findMasked(tenantId);
    }
}
