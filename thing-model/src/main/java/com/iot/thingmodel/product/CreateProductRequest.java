package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.thingmodel.common.Constants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @Size(max = Constants.Validation.TENANT_ID_MAX_LENGTH) String tenantId,
        @Size(max = Constants.Validation.PRODUCT_ID_MAX_LENGTH) String productId,
        @Size(max = Constants.Validation.PRODUCT_NAME_MAX_LENGTH) String name,
        @Size(max = Constants.Validation.MODEL_MAX_LENGTH) String model,
        @Size(max = Constants.Validation.CATEGORY_ID_MAX_LENGTH) String categoryId,
        @Size(max = Constants.Validation.TSL_VERSION_MAX_LENGTH) String tslVersion,
        @NotNull JsonNode tsl,
        Boolean strictUnknown) {
}
