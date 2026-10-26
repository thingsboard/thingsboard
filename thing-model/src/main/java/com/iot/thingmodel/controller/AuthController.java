package com.iot.thingmodel.controller;

import com.iot.thingmodel.auth.AccessGuard;
import com.iot.thingmodel.auth.CurrentUser;
import com.iot.thingmodel.common.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.Api.AUTH_PATH)
@Tag(name = "Auth", description = "Current user information")
public class AuthController extends BaseController {

    public AuthController(AccessGuard accessGuard) {
        super(accessGuard);
    }

    @Operation(summary = "Get the current ThingsBoard user")
    @GetMapping("/me")
    public CurrentUser me() {
        return getCurrentUser();
    }
}
