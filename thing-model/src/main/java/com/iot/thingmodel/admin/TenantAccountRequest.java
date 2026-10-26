package com.iot.thingmodel.admin;

import com.iot.thingmodel.common.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantAccountRequest(
        @NotBlank @Size(max = Constants.Validation.USERNAME_MAX_LENGTH) String username,
        @NotBlank String password) {
}
