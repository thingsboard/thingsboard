package com.iot.thingmodel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.auth.AccessGuard;
import com.iot.thingmodel.auth.AuthInterceptor;
import com.iot.thingmodel.auth.CurrentUser;
import com.iot.thingmodel.common.GlobalExceptionHandler;
import com.iot.thingmodel.product.CreateProductCommand;
import com.iot.thingmodel.product.ProductResponse;
import com.iot.thingmodel.product.ProductService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID tenantId = UUID.randomUUID();
    private final ProductService productService = mock(ProductService.class);
    private final ProductController controller = new ProductController(new AccessGuard(), productService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @BeforeEach
    void setUp() {
        when(productService.create(any())).thenReturn(new ProductResponse(
                "P1", "1.0", null, null, null, 0, 0, 0, 0, "PUBLISHED", List.of()));
    }

    @Test
    void assignsTenantFromCurrentUser() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","name":"Product","tsl":{}}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateProductCommand> captor = ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(productService).create(captor.capture());
        assertEquals(tenantId, captor.getValue().tenantId());
    }

    @Test
    void systemAdminMustSpecifyTenant() throws Exception {
        CurrentUser user = new CurrentUser("root", null, "SYS_ADMIN", "root@b");

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","name":"Product","tsl":{}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tenantAdminCannotSpecifyAnotherTenant() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");
        String otherTenant = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "tenantId", otherTenant,
                                "productId", "P1",
                                "name", "Product",
                                "tsl", java.util.Map.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMissingTsl() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","name":"Product"}
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).create(any());
    }
}
