package com.iot.thingmodel.controller;

import com.iot.thingmodel.admin.TenantAccountRequest;
import com.iot.thingmodel.admin.TenantAccountService;
import com.iot.thingmodel.auth.AccessGuard;
import com.iot.thingmodel.auth.RequireAuthority;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.Api.ADMIN_PATH)
@RequireAuthority(Constants.Auth.ROLE_SYS_ADMIN)
@Tag(name = "Admin", description = "Platform administration")
public class AdminController extends BaseController {

    private final TenantAccountService accountService;
    private final ThingsboardClientProvider clientProvider;

    public AdminController(
            AccessGuard accessGuard,
            TenantAccountService accountService,
            ThingsboardClientProvider clientProvider) {
        super(accessGuard);
        this.accountService = accountService;
        this.clientProvider = clientProvider;
    }

    @Operation(summary = "Register or update a vendor ThingsBoard account",
            description = "Stores an encrypted ThingsBoard tenant administrator account used by the platform.")
    @PostMapping("/tenants/{tenantId}/account")
    public TenantAccountService.MaskedAccount registerTenantAccount(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantAccountRequest request) {
        UUID id = parseTenantId(tenantId);
        TenantAccountService.MaskedAccount saved = accountService.register(id, request.username(), request.password());
        clientProvider.evict(id);
        return saved;
    }

    @Operation(summary = "Get a vendor ThingsBoard account",
            description = "Returns the masked account information. The password is never returned.")
    @GetMapping("/tenants/{tenantId}/account")
    public TenantAccountService.MaskedAccount getTenantAccount(@PathVariable String tenantId) {
        UUID id = parseTenantId(tenantId);
        return checkNotNull(accountService.findMasked(id).orElse(null), "厂商租户未配置服务账号");
    }
}
